package com.itcompany.recruitment.service;

import com.itcompany.recruitment.model.JobPosting;
import com.itcompany.recruitment.repository.JobPostingRepository;
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
 * Transakciona obrada podataka za JobPosting entitet
 * Implementira Saga pattern (orkestracija) za čuvanje u Elasticsearch i Qdrant
 */
@Service
public class TransactionalJobPostingService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionalJobPostingService.class);

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @Autowired
    private VectorizationService vectorizationService;

    @Autowired
    @Qualifier("qdrantRestTemplate")
    private RestTemplate qdrantRestTemplate;

    @Value("${qdrant.url}")
    private String qdrantUrl;

    /**
     * SAGA STEP 1: Kreiranje job posting-a sa transakcionom obradom
     */
    @Transactional
    public JobPosting createJobPosting(JobPosting jobPosting) {
        String jobId = null;
        
        try {
            logger.info("Starting transactional creation of job posting: {}", jobPosting.getTitle());
            
            // STEP 1: Vektorizacija
            if (jobPosting.getDescription() != null) {
                jobPosting.setDescriptionVector(
                    vectorizationService.vectorizeText(jobPosting.getDescription())
                );
                logger.debug("Job description vectorized successfully");
            }
            jobPosting.setPostedDate(LocalDateTime.now());
            if (jobPosting.getIsActive() == null) {
                jobPosting.setIsActive(true);
            }
            
            // STEP 2: Čuvanje u Elasticsearch (glavna baza)
            JobPosting savedJob = jobPostingRepository.save(jobPosting);
            jobId = savedJob.getId();
            logger.info("Job posting saved to Elasticsearch with ID: {}", jobId);
            
            // STEP 3: Čuvanje u Qdrant (vektorska baza)
            saveToQdrant(savedJob);
            logger.info("Job posting saved to Qdrant successfully");
            
            logger.info("Transactional creation completed successfully for job posting: {}", jobId);
            return savedJob;
            
        } catch (Exception e) {
            logger.error("Error in transactional creation of job posting: {}", e.getMessage(), e);
            
            // SAGA COMPENSATION: Rollback
            if (jobId != null) {
                try {
                    jobPostingRepository.deleteById(jobId);
                    logger.info("Rollback: Job posting deleted from Elasticsearch: {}", jobId);
                } catch (Exception rollbackException) {
                    logger.error("Rollback failed for job posting: {}", jobId, rollbackException);
                }
            }
            
            throw new RuntimeException("Failed to create job posting in both databases", e);
        }
    }

    /**
     * SAGA STEP 2: Ažuriranje job posting-a sa transakcionom obradom
     */
    @Transactional
    public JobPosting updateJobPosting(String id, JobPosting jobPosting) {
        try {
            logger.info("Starting transactional update of job posting: {}", id);
            
            // STEP 1: Proveri da li job posting postoji
            Optional<JobPosting> existingOpt = jobPostingRepository.findById(id);
            if (existingOpt.isEmpty()) {
                throw new IllegalArgumentException("Job posting not found: " + id);
            }
            
            // STEP 2: Vektorizacija (ako je promenjen opis)
            if (jobPosting.getDescription() != null) {
                jobPosting.setDescriptionVector(
                    vectorizationService.vectorizeText(jobPosting.getDescription())
                );
            }
            jobPosting.setId(id);
            
            // STEP 3: Ažuriranje u Elasticsearch
            JobPosting updatedJob = jobPostingRepository.save(jobPosting);
            logger.info("Job posting updated in Elasticsearch: {}", id);
            
            // STEP 4: Ažuriranje u Qdrant
            updateInQdrant(updatedJob);
            logger.info("Job posting updated in Qdrant: {}", id);
            
            logger.info("Transactional update completed successfully for job posting: {}", id);
            return updatedJob;
            
        } catch (Exception e) {
            logger.error("Error in transactional update of job posting: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update job posting in both databases", e);
        }
    }

    /**
     * SAGA STEP 3: Brisanje job posting-a sa transakcionom obradom
     */
    @Transactional
    public void deleteJobPosting(String id) {
        try {
            logger.info("Starting transactional deletion of job posting: {}", id);
            
            // STEP 1: Proveri da li job posting postoji
            Optional<JobPosting> existingOpt = jobPostingRepository.findById(id);
            if (existingOpt.isEmpty()) {
                throw new IllegalArgumentException("Job posting not found: " + id);
            }
            
            // STEP 2: Brisanje iz Qdrant
            deleteFromQdrant(id);
            logger.info("Job posting deleted from Qdrant: {}", id);
            
            // STEP 3: Brisanje iz Elasticsearch
            jobPostingRepository.deleteById(id);
            logger.info("Job posting deleted from Elasticsearch: {}", id);
            
            logger.info("Transactional deletion completed successfully for job posting: {}", id);
            
        } catch (Exception e) {
            logger.error("Error in transactional deletion of job posting: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete job posting from both databases", e);
        }
    }

    /**
     * Čuvanje job posting-a u Qdrant
     */
    private void saveToQdrant(JobPosting jobPosting) {
        try {
            String url = qdrantUrl + "/collections/job_advertisements/points";
            
            Map<String, Object> point = new HashMap<>();
            point.put("id", jobPosting.getId());
            point.put("vector", jobPosting.getDescriptionVector()); // Koristi description vektor
            
            // Minimalni payload za Qdrant
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", jobPosting.getId());
            payload.put("title", jobPosting.getTitle());
            payload.put("location", jobPosting.getLocation());
            payload.put("experienceLevel", jobPosting.getExperienceLevel());
            payload.put("requiredSkills", jobPosting.getRequiredSkills());
            payload.put("minSalary", jobPosting.getMinSalary());
            payload.put("maxSalary", jobPosting.getMaxSalary());
            payload.put("employmentType", jobPosting.getEmploymentType());
            payload.put("isActive", jobPosting.getIsActive());
            
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
            logger.error("Error saving job posting to Qdrant: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save job posting to Qdrant", e);
        }
    }

    /**
     * Ažuriranje job posting-a u Qdrant
     */
    private void updateInQdrant(JobPosting jobPosting) {
        try {
            String url = qdrantUrl + "/collections/job_advertisements/points/" + jobPosting.getId();
            
            Map<String, Object> point = new HashMap<>();
            point.put("id", jobPosting.getId());
            point.put("vector", jobPosting.getDescriptionVector());
            
            // Minimalni payload za Qdrant
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", jobPosting.getId());
            payload.put("title", jobPosting.getTitle());
            payload.put("location", jobPosting.getLocation());
            payload.put("experienceLevel", jobPosting.getExperienceLevel());
            payload.put("requiredSkills", jobPosting.getRequiredSkills());
            payload.put("minSalary", jobPosting.getMinSalary());
            payload.put("maxSalary", jobPosting.getMaxSalary());
            payload.put("employmentType", jobPosting.getEmploymentType());
            payload.put("isActive", jobPosting.getIsActive());
            
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
            logger.error("Error updating job posting in Qdrant: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update job posting in Qdrant", e);
        }
    }

    /**
     * Brisanje job posting-a iz Qdrant
     */
    private void deleteFromQdrant(String jobId) {
        try {
            String url = qdrantUrl + "/collections/job_advertisements/points/" + jobId;
            
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = qdrantRestTemplate.exchange(
                url, HttpMethod.DELETE, request, String.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to delete from Qdrant: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("Error deleting job posting from Qdrant: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete job posting from Qdrant", e);
        }
    }
}
