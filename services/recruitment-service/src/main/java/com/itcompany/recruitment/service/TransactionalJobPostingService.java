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
 * Transactional processing of data for JobPosting entity
 * Implements Saga pattern (orchestration) for saving to Elasticsearch and Qdrant
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
     * SAGA STEP 1: Create job posting with transactional processing
     */
    @Transactional
    public JobPosting createJobPosting(JobPosting jobPosting) {
        String jobId = null;
        
        try {
            logger.info("Starting transactional creation of job posting: {}", jobPosting.getTitle());
            
            // STEP 1: Vector will be created when saving to Qdrant
            if (jobPosting.getDescription() != null) {
                logger.debug("Job description will be vectorized when saving to Qdrant");
            }
            jobPosting.setPostedDate(LocalDateTime.now());
            if (jobPosting.getIsActive() == null) {
                jobPosting.setIsActive(true);
            }
            
            // STEP 2: Save to Elasticsearch (main database)
            JobPosting savedJob = jobPostingRepository.save(jobPosting);
            jobId = savedJob.getId();
            logger.info("Job posting saved to Elasticsearch with ID: {}", jobId);
            
            // STEP 3: Save to Qdrant (vector database)
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
     * SAGA STEP 2: Update job posting with transactional processing
     */
    @Transactional
    public JobPosting updateJobPosting(String id, JobPosting jobPosting) {
        try {
            logger.info("Starting transactional update of job posting: {}", id);
            
            // STEP 1: Check if job posting exists
            Optional<JobPosting> existingOpt = jobPostingRepository.findById(id);
            if (existingOpt.isEmpty()) {
                throw new IllegalArgumentException("Job posting not found: " + id);
            }
            
            // STEP 2: Vector will be created when saving to Qdrant (if description is changed)
            if (jobPosting.getDescription() != null) {
                logger.debug("Job description will be vectorized when saving to Qdrant");
            }
            jobPosting.setId(id);
            
            // STEP 3: Update in Elasticsearch
            JobPosting updatedJob = jobPostingRepository.save(jobPosting);
            logger.info("Job posting updated in Elasticsearch: {}", id);
            
            // STEP 4: Update in Qdrant
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
     * SAGA STEP 3: Delete job posting with transactional processing
     */
    @Transactional
    public void deleteJobPosting(String id) {
        try {
            logger.info("Starting transactional deletion of job posting: {}", id);
            
            // STEP 1: Check if job posting exists
            Optional<JobPosting> existingOpt = jobPostingRepository.findById(id);
            if (existingOpt.isEmpty()) {
                throw new IllegalArgumentException("Job posting not found: " + id);
            }
            
            // STEP 2: Delete from Qdrant
            deleteFromQdrant(id);
            logger.info("Job posting deleted from Qdrant: {}", id);
            
            // STEP 3: Delete from Elasticsearch
            jobPostingRepository.deleteById(id);
            logger.info("Job posting deleted from Elasticsearch: {}", id);
            
            logger.info("Transactional deletion completed successfully for job posting: {}", id);
            
        } catch (Exception e) {
            logger.error("Error in transactional deletion of job posting: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete job posting from both databases", e);
        }
    }

    /**
     * Save job posting to Qdrant
     */
    private void saveToQdrant(JobPosting jobPosting) {
        try {
            String url = qdrantUrl + "/collections/job_advertisements/points";
            
            Map<String, Object> point = new HashMap<>();
            point.put("id", jobPosting.getId());
            point.put("vector", vectorizationService.vectorizeText(jobPosting.getDescription()));
            
            // Minimal payload for Qdrant
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
     * Update job posting in Qdrant
     */
    private void updateInQdrant(JobPosting jobPosting) {
        try {
            String url = qdrantUrl + "/collections/job_advertisements/points/" + jobPosting.getId();
            
            Map<String, Object> point = new HashMap<>();
            point.put("id", jobPosting.getId());
            point.put("vector", vectorizationService.vectorizeText(jobPosting.getDescription()));
            
            // Minimal payload for Qdrant
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
     * Delete job posting from Qdrant
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
