package com.itcompany.recruitment.service;

import com.itcompany.recruitment.model.Application;
import com.itcompany.recruitment.model.Candidate;
import com.itcompany.recruitment.model.JobPosting;
import com.itcompany.recruitment.repository.ApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Transakciona obrada podataka za Application entitet
 * Implementira Saga pattern (orkestracija) za čuvanje u Elasticsearch i Qdrant
 */
@Service
public class TransactionalApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionalApplicationService.class);

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private VectorizationService vectorizationService;

    @Autowired
    private CandidateService candidateService;

    @Autowired
    private JobPostingService jobPostingService;

    @Autowired
    @Qualifier("qdrantRestTemplate")
    private RestTemplate qdrantRestTemplate;

    @Value("${qdrant.url}")
    private String qdrantUrl;

    /**
     * SAGA STEP 1: Kreiranje prijave sa transakcionom obradom
     */
    @Transactional
    public Application createApplication(Application application) {
        String applicationId = null;
        
        try {
            logger.info("Starting transactional creation of application: {} -> {}", 
                       application.getCandidateId(), application.getJobPostingId());
            
            // STEP 1: Proveri da li kandidat i job posting postoje
            Optional<Candidate> candidateOpt = candidateService.findById(application.getCandidateId());
            Optional<JobPosting> jobOpt = jobPostingService.findById(application.getJobPostingId());
            
            if (candidateOpt.isEmpty()) {
                throw new IllegalArgumentException("Candidate not found: " + application.getCandidateId());
            }
            if (jobOpt.isEmpty()) {
                throw new IllegalArgumentException("Job posting not found: " + application.getJobPostingId());
            }
            
            // STEP 2: Proveri da li kandidat već ima prijavu za ovaj posao
            Application existing = applicationRepository.findByCandidateIdAndJobPostingId(
                application.getCandidateId(), 
                application.getJobPostingId()
            );
            
            if (existing != null) {
                throw new IllegalStateException("Candidate already applied for this position");
            }
            
            // STEP 3: Vektorizacija cover letter-a
            if (application.getCoverLetter() != null) {
                application.setCoverLetterVector(
                    vectorizationService.vectorizeText(application.getCoverLetter())
                );
                logger.debug("Cover letter vectorized successfully");
            }
            
            // STEP 4: Izračunaj match skorove
            calculateMatchScores(application, candidateOpt.get(), jobOpt.get());
            
            application.setApplicationDate(LocalDateTime.now());
            application.setStatus("PENDING");
            
            // STEP 5: Čuvanje u Elasticsearch (glavna baza)
            Application savedApplication = applicationRepository.save(application);
            applicationId = savedApplication.getId();
            logger.info("Application saved to Elasticsearch with ID: {}", applicationId);
            
            // STEP 6: Čuvanje u Qdrant (vektorska baza)
            saveToQdrant(savedApplication, candidateOpt.get(), jobOpt.get());
            logger.info("Application saved to Qdrant successfully");
            
            logger.info("Transactional creation completed successfully for application: {}", applicationId);
            return savedApplication;
            
        } catch (Exception e) {
            logger.error("Error in transactional creation of application: {}", e.getMessage(), e);
            
            // SAGA COMPENSATION: Rollback
            if (applicationId != null) {
                try {
                    applicationRepository.deleteById(applicationId);
                    logger.info("Rollback: Application deleted from Elasticsearch: {}", applicationId);
                } catch (Exception rollbackException) {
                    logger.error("Rollback failed for application: {}", applicationId, rollbackException);
                }
            }
            
            throw new RuntimeException("Failed to create application in both databases", e);
        }
    }

    /**
     * SAGA STEP 2: Ažuriranje prijave sa transakcionom obradom
     */
    @Transactional
    public Application updateApplication(String id, Application application) {
        try {
            logger.info("Starting transactional update of application: {}", id);
            
            // STEP 1: Proveri da li prijava postoji
            Optional<Application> existingOpt = applicationRepository.findById(id);
            if (existingOpt.isEmpty()) {
                throw new IllegalArgumentException("Application not found: " + id);
            }
            
            // STEP 2: Vektorizacija (ako je promenjen cover letter)
            if (application.getCoverLetter() != null) {
                application.setCoverLetterVector(
                    vectorizationService.vectorizeText(application.getCoverLetter())
                );
            }
            application.setId(id);
            
            // STEP 3: Ažuriranje u Elasticsearch
            Application updatedApplication = applicationRepository.save(application);
            logger.info("Application updated in Elasticsearch: {}", id);
            
            // STEP 4: Ažuriranje u Qdrant
            updateInQdrant(updatedApplication);
            logger.info("Application updated in Qdrant: {}", id);
            
            logger.info("Transactional update completed successfully for application: {}", id);
            return updatedApplication;
            
        } catch (Exception e) {
            logger.error("Error in transactional update of application: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update application in both databases", e);
        }
    }

    /**
     * SAGA STEP 3: Brisanje prijave sa transakcionom obradom
     */
    @Transactional
    public void deleteApplication(String id) {
        try {
            logger.info("Starting transactional deletion of application: {}", id);
            
            // STEP 1: Proveri da li prijava postoji
            Optional<Application> existingOpt = applicationRepository.findById(id);
            if (existingOpt.isEmpty()) {
                throw new IllegalArgumentException("Application not found: " + id);
            }
            
            // STEP 2: Brisanje iz Qdrant
            deleteFromQdrant(id);
            logger.info("Application deleted from Qdrant: {}", id);
            
            // STEP 3: Brisanje iz Elasticsearch
            applicationRepository.deleteById(id);
            logger.info("Application deleted from Elasticsearch: {}", id);
            
            logger.info("Transactional deletion completed successfully for application: {}", id);
            
        } catch (Exception e) {
            logger.error("Error in transactional deletion of application: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete application from both databases", e);
        }
    }

    /**
     * Izračunavanje match skorova
     */
    private void calculateMatchScores(Application application, Candidate candidate, JobPosting job) {
        // CV Match Score (vektorska sličnost)
        if (candidate.getCvVector() != null && job.getDescriptionVector() != null) {
            double cvScore = vectorizationService.calculateCosineSimilarity(
                candidate.getCvVector(), 
                job.getDescriptionVector()
            );
            application.setCvMatchScore(cvScore);
        }
        
        // Skill Match Score
        if (candidate.getSkills() != null && job.getRequiredSkills() != null) {
            long matchedSkills = candidate.getSkills().stream()
                .filter(job.getRequiredSkills()::contains)
                .count();
            
            double skillScore = job.getRequiredSkills().isEmpty() ? 1.0 : 
                (double) matchedSkills / job.getRequiredSkills().size();
            application.setSkillMatchScore(skillScore);
        }
        
        // Experience Match Score
        if (candidate.getYearsOfExperience() != null && job.getMinYearsExperience() != null) {
            if (candidate.getYearsOfExperience() >= job.getMinYearsExperience()) {
                application.setExperienceMatchScore(1.0);
            } else {
                double expScore = (double) candidate.getYearsOfExperience() / 
                                 job.getMinYearsExperience();
                application.setExperienceMatchScore(Math.min(expScore, 1.0));
            }
        }
        
        // Overall Match Score (weighted average)
        double overallScore = 0.0;
        double weights = 0.0;
        
        if (application.getCvMatchScore() != null) {
            overallScore += application.getCvMatchScore() * 0.4;
            weights += 0.4;
        }
        if (application.getSkillMatchScore() != null) {
            overallScore += application.getSkillMatchScore() * 0.35;
            weights += 0.35;
        }
        if (application.getExperienceMatchScore() != null) {
            overallScore += application.getExperienceMatchScore() * 0.25;
            weights += 0.25;
        }
        
        if (weights > 0) {
            application.setOverallMatchScore(overallScore / weights);
        }
    }

    /**
     * Čuvanje prijave u Qdrant
     */
    private void saveToQdrant(Application application, Candidate candidate, JobPosting job) {
        try {
            String url = qdrantUrl + "/collections/applications/points";
            
            Map<String, Object> point = new HashMap<>();
            point.put("id", application.getId());
            point.put("vector", application.getCoverLetterVector()); // Koristi cover letter vektor
            
            // Minimalni payload za Qdrant
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", application.getId());
            payload.put("candidateId", application.getCandidateId());
            payload.put("jobPostingId", application.getJobPostingId());
            payload.put("status", application.getStatus());
            payload.put("overallMatchScore", application.getOverallMatchScore());
            payload.put("skillMatchScore", application.getSkillMatchScore());
            payload.put("cvMatchScore", application.getCvMatchScore());
            payload.put("experienceMatchScore", application.getExperienceMatchScore());
            payload.put("isShortlisted", application.getIsShortlisted());
            payload.put("applicationDate", application.getApplicationDate());
            
            point.put("payload", payload);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(point, headers);
            
            ResponseEntity<String> response = qdrantRestTemplate.exchange(
                url, HttpMethod.PUT, request, String.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to save to Qdrant: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("Error saving application to Qdrant: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save application to Qdrant", e);
        }
    }

    /**
     * Ažuriranje prijave u Qdrant
     */
    private void updateInQdrant(Application application) {
        try {
            String url = qdrantUrl + "/collections/applications/points/" + application.getId();
            
            Map<String, Object> point = new HashMap<>();
            point.put("id", application.getId());
            point.put("vector", application.getCoverLetterVector());
            
            // Minimalni payload za Qdrant
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", application.getId());
            payload.put("candidateId", application.getCandidateId());
            payload.put("jobPostingId", application.getJobPostingId());
            payload.put("status", application.getStatus());
            payload.put("overallMatchScore", application.getOverallMatchScore());
            payload.put("skillMatchScore", application.getSkillMatchScore());
            payload.put("cvMatchScore", application.getCvMatchScore());
            payload.put("experienceMatchScore", application.getExperienceMatchScore());
            payload.put("isShortlisted", application.getIsShortlisted());
            payload.put("applicationDate", application.getApplicationDate());
            
            point.put("payload", payload);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(point, headers);
            
            ResponseEntity<String> response = qdrantRestTemplate.exchange(
                url, HttpMethod.PUT, request, String.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to update in Qdrant: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("Error updating application in Qdrant: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update application in Qdrant", e);
        }
    }

    /**
     * Brisanje prijave iz Qdrant
     */
    private void deleteFromQdrant(String applicationId) {
        try {
            String url = qdrantUrl + "/collections/applications/points/" + applicationId;
            
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = qdrantRestTemplate.exchange(
                url, HttpMethod.DELETE, request, String.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to delete from Qdrant: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("Error deleting application from Qdrant: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete application from Qdrant", e);
        }
    }
}
