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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
     * 1. Vectorize
     * 2. Save to Elasticsearch
     * 3. Save to Qdrant
     * 4. Rollback if something fails
     */
    @Transactional
    public JobPosting createJobPosting(JobPosting jobPosting) {
        String jobId = null;
        
        try {
            logger.info("Starting transactional creation of job posting: {}", jobPosting.getTitle());
            
            // STEP 1: Set creation date
            jobPosting.setPostedDate(LocalDateTime.now());
            
            // STEP 2: Save to Elasticsearch (main database)
            JobPosting savedJobPosting = jobPostingRepository.save(jobPosting);
            jobId = savedJobPosting.getId();
            logger.info("Job posting saved to Elasticsearch with ID: {}", jobId);
            
            // STEP 3: Save to Qdrant (vector database)
            saveToQdrant(savedJobPosting);
            logger.info("Job posting saved to Qdrant successfully");
            
            logger.info("Transactional creation completed successfully for job posting: {}", jobId);
            return savedJobPosting;
            
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
     * 1. Update in Elasticsearch
     * 2. Update in Qdrant
     * 3. Rollback if something fails
     */
    @Transactional
    public JobPosting updateJobPosting(String id, JobPosting jobPosting) {
        try {
            logger.info("Starting transactional update of job posting: {}", id);
            
            // STEP 1: Update in Elasticsearch
            jobPosting.setId(id);
            JobPosting updatedJobPosting = jobPostingRepository.save(jobPosting);
            logger.info("Job posting updated in Elasticsearch: {}", id);
            
            // STEP 2: Update in Qdrant
            updateInQdrant(updatedJobPosting);
            logger.info("Job posting updated in Qdrant: {}", id);
            
            logger.info("Transactional update completed successfully for job posting: {}", id);
            return updatedJobPosting;
            
        } catch (Exception e) {
            logger.error("Error in transactional update of job posting: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update job posting in both databases", e);
        }
    }

    /**
     * SAGA STEP 3: Delete job posting with transactional processing
     * 1. Delete from Qdrant
     * 2. Delete from Elasticsearch
     * 3. Rollback if something fails
     */
    @Transactional
    public void deleteJobPosting(String id) {
        try {
            logger.info("Starting transactional deletion of job posting: {}", id);
            
            // STEP 1: Check if job posting exists
            Optional<JobPosting> jobPosting = jobPostingRepository.findById(id);
            if (jobPosting.isEmpty()) {
                logger.warn("Job posting not found: {}", id);
                return;
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
            logger.info("Converting Elasticsearch ID '{}' to Qdrant ID", jobPosting.getId());
            int qdrantId = convertToNumericId(jobPosting.getId());
            logger.info("Converted to Qdrant ID: {}", qdrantId);
            point.put("id", qdrantId);
            
            // Create combined vector from multiple fields (same logic as TestDataService)
            String title = jobPosting.getTitle() != null ? jobPosting.getTitle() : "";
            String description = jobPosting.getDescription() != null ? jobPosting.getDescription() : "";
            String requirements = jobPosting.getDepartment() != null ? jobPosting.getDepartment() : "";
            String skills = jobPosting.getRequiredSkills() != null ? String.join(", ", jobPosting.getRequiredSkills()) : "";
            
            // Create combined vector from multiple fields
            String combinedText = title + " " + description + " " + requirements + " " + skills;
            float[] combinedVector = vectorizationService.vectorizeText(combinedText);
            point.put("vector", combinedVector);
            
            // Store Elasticsearch ID in payload for mapping
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", jobPosting.getId()); // Store Elasticsearch ID as 'id' for vector search compatibility
            payload.put("qdrant_id", qdrantId); // Store numeric ID for reference
            payload.put("title", jobPosting.getTitle());
            payload.put("description", jobPosting.getDescription());
            payload.put("company", jobPosting.getDepartment());
            payload.put("location", jobPosting.getLocation());
            payload.put("skills_required", jobPosting.getRequiredSkills());
            payload.put("salary_min", jobPosting.getMinSalary());
            payload.put("salary_max", jobPosting.getMaxSalary());
            payload.put("employment_type", jobPosting.getEmploymentType());
            
            point.put("payload", payload);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("points", Arrays.asList(point));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
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
            // Find existing Qdrant ID by searching for job posting ID in payload
            int qdrantId = findQdrantIdByJobPostingId(jobPosting.getId());
            
            if (qdrantId == -1) {
                logger.warn("Job posting not found in Qdrant for update: {}, creating new entry", jobPosting.getId());
                // If not found, create new entry
                saveToQdrant(jobPosting);
                return;
            }
            
            // ISPRAVKA: Koristi PUT endpoint sa points array za update (isto kao za kreiranje)
            String url = qdrantUrl + "/collections/job_advertisements/points";
            
            Map<String, Object> point = new HashMap<>();
            point.put("id", qdrantId);
            
            // Create combined vector from multiple fields (same logic as TestDataService)
            String title = jobPosting.getTitle() != null ? jobPosting.getTitle() : "";
            String description = jobPosting.getDescription() != null ? jobPosting.getDescription() : "";
            String requirements = jobPosting.getDepartment() != null ? jobPosting.getDepartment() : "";
            String skills = jobPosting.getRequiredSkills() != null ? String.join(", ", jobPosting.getRequiredSkills()) : "";
            
            // Create combined vector from multiple fields
            String combinedText = title + " " + description + " " + requirements + " " + skills;
            float[] combinedVector = vectorizationService.vectorizeText(combinedText);
            point.put("vector", combinedVector);
            
            // Store Elasticsearch ID in payload for mapping
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", jobPosting.getId()); // Store Elasticsearch ID as 'id' for vector search compatibility
            payload.put("qdrant_id", qdrantId); // Store numeric ID for reference
            payload.put("title", jobPosting.getTitle());
            payload.put("description", jobPosting.getDescription());
            payload.put("company", jobPosting.getDepartment());
            payload.put("location", jobPosting.getLocation());
            payload.put("skills_required", jobPosting.getRequiredSkills());
            payload.put("salary_min", jobPosting.getMinSalary());
            payload.put("salary_max", jobPosting.getMaxSalary());
            payload.put("employment_type", jobPosting.getEmploymentType());
            
            point.put("payload", payload);
            
            // Kreiraj payload sa listom tačaka za update
            Map<String, Object> updateRequest = new HashMap<>();
            updateRequest.put("points", Arrays.asList(point));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(updateRequest, headers);
            
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
    private void deleteFromQdrant(String jobPostingId) {
        try {
            // Find Qdrant ID by searching for job posting ID in payload
            int qdrantId = findQdrantIdByJobPostingId(jobPostingId);
            
            if (qdrantId == -1) {
                logger.warn("Job posting not found in Qdrant: {}", jobPostingId);
                return; // Not found, but don't fail the operation
            }
            
            // ISPRAVKA: Koristi POST endpoint sa points array
            String url = qdrantUrl + "/collections/job_advertisements/points/delete";
            
            // Kreiraj payload sa listom ID-jeva za brisanje
            Map<String, Object> deleteRequest = new HashMap<>();
            deleteRequest.put("points", Arrays.asList(qdrantId));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(deleteRequest, headers);
            
            ResponseEntity<String> response = qdrantRestTemplate.exchange(
                url, HttpMethod.POST, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully deleted job posting from Qdrant: {} (Qdrant ID: {})", jobPostingId, qdrantId);
            } else {
                logger.warn("Failed to delete from Qdrant: {} - {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("Failed to delete job posting from Qdrant: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("Error deleting job posting from Qdrant: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete job posting from Qdrant", e);
        }
    }
    
    /**
     * Find Qdrant ID by searching for job posting ID in payload
     */
    private int findQdrantIdByJobPostingId(String jobPostingId) {
        try {
            logger.info("Finding Qdrant ID for job posting: {}", jobPostingId);
            String url = qdrantUrl + "/collections/job_advertisements/points/scroll";
            
            Map<String, Object> scrollRequest = new HashMap<>();
            scrollRequest.put("limit", 10000); // Get all points
            scrollRequest.put("with_payload", true);
            scrollRequest.put("with_vector", false);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(scrollRequest, headers);
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = qdrantRestTemplate.exchange(
                url, HttpMethod.POST, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseBody = response.getBody();
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) responseBody.get("result");
                
                if (result != null && result.containsKey("points")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> points = (List<Map<String, Object>>) result.get("points");
                    
                    for (Map<String, Object> point : points) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> payload = (Map<String, Object>) point.get("payload");
                        if (payload != null && jobPostingId.equals(payload.get("id"))) {
                            // Return the Qdrant ID (point ID), not the payload ID
                            int qdrantId = (Integer) point.get("id");
                            logger.info("Found Qdrant ID {} for job posting {}", qdrantId, jobPostingId);
                            return qdrantId;
                        }
                    }
                }
            }
            
            return -1; // Not found
            
        } catch (Exception e) {
            logger.error("Error finding Qdrant ID for job posting ID {}: {}", jobPostingId, e.getMessage());
            return -1;
        }
    }
    
    /**
     * Convert any ID to numeric ID for Qdrant
     */
    private int convertToNumericId(String jobPostingId) {
        try {
            if (jobPostingId.startsWith("job_")) {
                // For test data format: job_1 -> 1
                String numericPart = jobPostingId.substring("job_".length());
                return Integer.parseInt(numericPart);
            } else {
                // For UUID or other formats: use hash but ensure positive
                return Math.abs(jobPostingId.hashCode());
            }
        } catch (NumberFormatException e) {
            logger.warn("Could not convert ID to numeric: {}, using hash fallback", jobPostingId);
            return Math.abs(jobPostingId.hashCode());
        }
    }
}