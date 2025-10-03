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
 * Transactional processing of data for Application entity
 * Implements Saga pattern (orchestration) for saving to Elasticsearch and Qdrant
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
     * SAGA STEP 1: Create application with transactional processing
     */
    @Transactional
    public Application createApplication(Application application) {
        String applicationId = null;
        
        try {
            logger.info("Starting transactional creation of application: {} -> {}", 
                       application.getCandidateId(), application.getJobPostingId());
            
            // STEP 1: Check if candidate and job posting exist
            Optional<Candidate> candidateOpt = candidateService.findById(application.getCandidateId());
            Optional<JobPosting> jobOpt = jobPostingService.findById(application.getJobPostingId());
            
            if (candidateOpt.isEmpty()) {
                throw new IllegalArgumentException("Candidate not found: " + application.getCandidateId());
            }
            if (jobOpt.isEmpty()) {
                throw new IllegalArgumentException("Job posting not found: " + application.getJobPostingId());
            }
            
            // STEP 2: Check if candidate already has an application for this job
            Application existing = applicationRepository.findByCandidateIdAndJobPostingId(
                application.getCandidateId(), 
                application.getJobPostingId()
            );
            
            if (existing != null) {
                throw new IllegalStateException("Candidate already applied for this position");
            }
            
            // STEP 3: Cover letter will be vectorized when saving to Qdrant
            if (application.getCoverLetter() != null) {
                logger.debug("Cover letter will be vectorized when saving to Qdrant");
            }
            
            // STEP 4: Calculate match scores
            calculateMatchScores(application, candidateOpt.get(), jobOpt.get());
            
            application.setApplicationDate(LocalDateTime.now());
            application.setStatus("PENDING");
            
            // STEP 5: Save to Elasticsearch (main database)
            Application savedApplication = applicationRepository.save(application);
            applicationId = savedApplication.getId();
            logger.info("Application saved to Elasticsearch with ID: {}", applicationId);
            
            // STEP 6: Save to Qdrant (vector database)
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
     * SAGA STEP 2: Update application with transactional processing
     */
    @Transactional
    public Application updateApplication(String id, Application application) {
        try {
            logger.info("Starting transactional update of application: {}", id);
            
            // STEP 1: Check if application exists
            Optional<Application> existingOpt = applicationRepository.findById(id);
            if (existingOpt.isEmpty()) {
                throw new IllegalArgumentException("Application not found: " + id);
            }
            
            // STEP 2: Cover letter will be vectorized when saving to Qdrant (if cover letter is changed)
            if (application.getCoverLetter() != null) {
                logger.debug("Cover letter will be vectorized when saving to Qdrant");
            }
            application.setId(id);
            
            // STEP 3: Update in Elasticsearch
            Application updatedApplication = applicationRepository.save(application);
            logger.info("Application updated in Elasticsearch: {}", id);
            
            // STEP 4: Update in Qdrant
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
     * SAGA STEP 3: Delete application with transactional processing
     */
    @Transactional
    public void deleteApplication(String id) {
        try {
            logger.info("Starting transactional deletion of application: {}", id);
            
            // STEP 1: Check if application exists
            Optional<Application> existingOpt = applicationRepository.findById(id);
            if (existingOpt.isEmpty()) {
                throw new IllegalArgumentException("Application not found: " + id);
            }
            
            // STEP 2: Delete from Qdrant
            deleteFromQdrant(id);
            logger.info("Application deleted from Qdrant: {}", id);
            
            // STEP 3: Delete from Elasticsearch
            applicationRepository.deleteById(id);
            logger.info("Application deleted from Elasticsearch: {}", id);
            
            logger.info("Transactional deletion completed successfully for application: {}", id);
            
        } catch (Exception e) {
            logger.error("Error in transactional deletion of application: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete application from both databases", e);
        }
    }

    /**
     * Calculate match scores
     */
    private void calculateMatchScores(Application application, Candidate candidate, JobPosting job) {
        // CV Match Score (vector similarity using Qdrant)
        try {
            // Calculate similarity using vectorization service directly
            float[] cvVector = vectorizationService.vectorizeText(candidate.getCvContent());
            float[] jobVector = vectorizationService.vectorizeText(job.getDescription());
            
            double cvScore = vectorizationService.calculateCosineSimilarity(cvVector, jobVector);
            application.setCvMatchScore(cvScore);
        } catch (Exception e) {
            logger.error("Error calculating CV match score for application: {}", application.getId(), e);
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
     * Save application to Qdrant
     */
    private void saveToQdrant(Application application, Candidate candidate, JobPosting job) {
        try {
            String url = qdrantUrl + "/collections/applications/points";
            
            Map<String, Object> point = new HashMap<>();
            point.put("id", application.getId());
            point.put("vector", vectorizationService.vectorizeText(application.getCoverLetter()));
            
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
     * Update application in Qdrant
     */
    private void updateInQdrant(Application application) {
        try {
            String url = qdrantUrl + "/collections/applications/points/" + application.getId();
            
            Map<String, Object> point = new HashMap<>();
            point.put("id", application.getId());
            point.put("vector", vectorizationService.vectorizeText(application.getCoverLetter()));
            
            // Minimal payload for Qdrant
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
     * Delete application from Qdrant
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
