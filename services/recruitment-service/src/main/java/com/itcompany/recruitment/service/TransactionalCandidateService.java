package com.itcompany.recruitment.service;

import com.itcompany.recruitment.model.Candidate;
import com.itcompany.recruitment.repository.CandidateRepository;
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
 * Transactional processing of data for Candidate entity
 * Implements Saga pattern (orchestration) for saving to Elasticsearch and Qdrant
 */
@Service
public class TransactionalCandidateService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionalCandidateService.class);

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private VectorizationService vectorizationService;

    @Autowired
    @Qualifier("qdrantRestTemplate")
    private RestTemplate qdrantRestTemplate;

    @Value("${qdrant.url}")
    private String qdrantUrl;

    /**
     * SAGA STEP 1: Create candidate with transactional processing
     * 1. Vectorize
     * 2. Save to Elasticsearch
     * 3. Save to Qdrant
     * 4. Rollback if something fails
     */
    @Transactional
    public Candidate createCandidate(Candidate candidate) {
        String candidateId = null;
        
        try {
            logger.info("Starting transactional creation of candidate: {}", candidate.getEmail());
            
            // STEP 1: Vectors will be created when saving to Qdrant
            if (candidate.getCvContent() != null) {
                logger.debug("CV will be vectorized when saving to Qdrant");
            }
            if (candidate.getSkills() != null) {
                logger.debug("Skills will be vectorized when saving to Qdrant");
            }
            candidate.setRegistrationDate(LocalDateTime.now());
            
            // STEP 2: Save to Elasticsearch (main database)
            logger.info("Before save - Candidate ID: {}", candidate.getId());
            Candidate savedCandidate = candidateRepository.save(candidate);
            candidateId = savedCandidate.getId();
            logger.info("After save - Candidate ID: {}", candidateId);
            logger.info("Candidate saved to Elasticsearch with ID: {}", candidateId);
            
            // STEP 3: Save to Qdrant (vector database)
            saveToQdrant(savedCandidate);
            logger.info("Candidate saved to Qdrant successfully");
            
            logger.info("Transactional creation completed successfully for candidate: {}", candidateId);
            return savedCandidate;
            
        } catch (Exception e) {
            logger.error("Error in transactional creation of candidate: {}", e.getMessage(), e);
            
            // SAGA COMPENSATION: Rollback
            if (candidateId != null) {
                try {
                    candidateRepository.deleteById(candidateId);
                    logger.info("Rollback: Candidate deleted from Elasticsearch: {}", candidateId);
                } catch (Exception rollbackException) {
                    logger.error("Rollback failed for candidate: {}", candidateId, rollbackException);
                }
            }
            
            throw new RuntimeException("Failed to create candidate in both databases", e);
        }
    }

    /**
     * SAGA STEP 2: Update candidate with transactional processing
     */
    @Transactional
    public Candidate updateCandidate(String id, Candidate candidate) {
        try {
            logger.info("Starting transactional update of candidate: {}", id);
            
            // STEP 1: Check if candidate exists
            Optional<Candidate> existingOpt = candidateRepository.findById(id);
            if (existingOpt.isEmpty()) {
                throw new IllegalArgumentException("Candidate not found: " + id);
            }
            
            // STEP 2: Vectors will be created when saving to Qdrant (if CV or skills are changed)
            if (candidate.getCvContent() != null) {
                logger.debug("CV will be vectorized when saving to Qdrant");
            }
            if (candidate.getSkills() != null) {
                logger.debug("Skills will be vectorized when saving to Qdrant");
            }
            candidate.setId(id);
            
            // STEP 3: Update in Elasticsearch
            Candidate updatedCandidate = candidateRepository.save(candidate);
            logger.info("Candidate updated in Elasticsearch: {}", id);
            
            // STEP 4: Update in Qdrant
            updateInQdrant(updatedCandidate);
            logger.info("Candidate updated in Qdrant: {}", id);
            
            logger.info("Transactional update completed successfully for candidate: {}", id);
            return updatedCandidate;
            
        } catch (Exception e) {
            logger.error("Error in transactional update of candidate: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update candidate in both databases", e);
        }
    }

    /**
     * SAGA STEP 3: Delete candidate with transactional processing
     */
    @Transactional
    public void deleteCandidate(String id) {
        try {
            logger.info("Starting transactional deletion of candidate: {}", id);
            
            // STEP 1: Check if candidate exists
            Optional<Candidate> existingOpt = candidateRepository.findById(id);
            if (existingOpt.isEmpty()) {
                throw new IllegalArgumentException("Candidate not found: " + id);
            }
            
            // STEP 2: Delete from Qdrant
            deleteFromQdrant(id);
            logger.info("Candidate deleted from Qdrant: {}", id);
            
            // STEP 3: Delete from Elasticsearch
            candidateRepository.deleteById(id);
            logger.info("Candidate deleted from Elasticsearch: {}", id);
            
            logger.info("Transactional deletion completed successfully for candidate: {}", id);
            
        } catch (Exception e) {
            logger.error("Error in transactional deletion of candidate: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete candidate from both databases", e);
        }
    }

    /**
     * Save candidate to Qdrant
     */
    private void saveToQdrant(Candidate candidate) {
        try {
            String url = qdrantUrl + "/collections/candidates/points";
            
            Map<String, Object> point = new HashMap<>();
            // Use Elasticsearch ID as Qdrant ID (convert to numeric if needed)
            logger.info("Converting Elasticsearch ID '{}' to Qdrant ID", candidate.getId());
            int qdrantId = convertToNumericId(candidate.getId());
            logger.info("Converted to Qdrant ID: {}", qdrantId);
            point.put("id", qdrantId);
            
            // Create combined vector from multiple fields (same logic as TestDataService)
            String cvContent = candidate.getCvContent() != null ? candidate.getCvContent() : "";
            //String skills = candidate.getSkills() != null ? String.join(", ", candidate.getSkills()) : "";
            //String workExperience = generateWorkExperienceText(candidate);
            
            // Create combined vector from multiple fields
            //String combinedText = cvContent + " " + skills + " " + workExperience;
            float[] combinedVector = vectorizationService.vectorizeText(cvContent);
            point.put("vector", combinedVector);
            
            // Store Elasticsearch ID in payload for mapping
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", candidate.getId()); // Store Elasticsearch ID as 'id' for vector search compatibility
          //  payload.put("elasticsearch_id", candidate.getId()); // Store original Elasticsearch ID for backward compatibility
            payload.put("qdrant_id", qdrantId); // Store numeric ID for reference
            payload.put("name", candidate.getFirstName() + " " + candidate.getLastName());
            payload.put("email", candidate.getEmail());
            payload.put("skills", candidate.getSkills());
            payload.put("location", candidate.getLocation());
            payload.put("experience", candidate.getYearsOfExperience());
            
            point.put("payload", payload);
            
            // Wrap point in PointInsertOperations format
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
            logger.error("Error saving candidate to Qdrant: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save candidate to Qdrant", e);
        }
    }

    /**
     * Update candidate in Qdrant
     */
    private void updateInQdrant(Candidate candidate) {
        try {
            // Find existing Qdrant ID by searching for candidate ID in payload
            int qdrantId = findQdrantIdByCandidateId(candidate.getId());
            
            if (qdrantId == -1) {
                logger.warn("Candidate not found in Qdrant for update: {}, creating new entry", candidate.getId());
                // If not found, create new entry
                saveToQdrant(candidate);
                return;
            }
            
            // ISPRAVKA: Koristi PUT endpoint sa points array za update (isto kao za kreiranje)
            String url = qdrantUrl + "/collections/candidates/points";
            
            Map<String, Object> point = new HashMap<>();
            point.put("id", qdrantId);
            
            // Create combined vector from multiple fields (same logic as TestDataService)
            String cvContent = candidate.getCvContent() != null ? candidate.getCvContent() : "";
            //String skills = candidate.getSkills() != null ? String.join(", ", candidate.getSkills()) : "";
            //String workExperience = generateWorkExperienceText(candidate);
            
            // Create combined vector from multiple fields
            //String combinedText = cvContent + " " + skills + " " + workExperience;
            float[] combinedVector = vectorizationService.vectorizeText(cvContent);
            point.put("vector", combinedVector);
            
            // Store Elasticsearch ID in payload for mapping
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", candidate.getId()); // Store Elasticsearch ID as 'id' for vector search compatibility
          //  payload.put("elasticsearch_id", candidate.getId()); // Store original Elasticsearch ID for backward compatibility
            payload.put("qdrant_id", qdrantId); // Store numeric ID for reference
            payload.put("name", candidate.getFirstName() + " " + candidate.getLastName());
            payload.put("email", candidate.getEmail());
            payload.put("skills", candidate.getSkills());
            payload.put("location", candidate.getLocation());
            payload.put("experience", candidate.getYearsOfExperience());
            
            point.put("payload", payload);
            
            // Make update request
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
            logger.error("Error updating candidate in Qdrant: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update candidate in Qdrant", e);
        }
    }

    /**
     * Delete candidate from Qdrant
     */
    private void deleteFromQdrant(String candidateId) {
        try {
            // Find Qdrant ID by searching for candidate ID in payload
            int qdrantId = findQdrantIdByCandidateId(candidateId);
            
            if (qdrantId == -1) {
                logger.warn("Candidate not found in Qdrant: {}", candidateId);
                return; // Not found, but don't fail the operation
            }
            
            // ISPRAVKA: Koristi POST endpoint sa points array
            String url = qdrantUrl + "/collections/candidates/points/delete";
            
            // Kreiraj payload sa listom ID-jeva za brisanje
            Map<String, Object> deleteRequest = new HashMap<>();
            deleteRequest.put("points", Arrays.asList(qdrantId));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(deleteRequest, headers);
            
            ResponseEntity<String> response = qdrantRestTemplate.exchange(
                url, HttpMethod.POST, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully deleted candidate from Qdrant: {} (Qdrant ID: {})", candidateId, qdrantId);
            } else {
                logger.warn("Failed to delete from Qdrant: {} - {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("Failed to delete candidate from Qdrant: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("Error deleting candidate from Qdrant: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete candidate from Qdrant", e);
        }
    }
    
    /**
     * Find Qdrant ID by searching for candidate ID in payload
     */
    private int findQdrantIdByCandidateId(String candidateId) {
        try {
            logger.info("Finding Qdrant ID for candidate: {}", candidateId);
            String url = qdrantUrl + "/collections/candidates/points/scroll";
            
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
                        if (payload != null && candidateId.equals(payload.get("id"))) {
                            // Return the Qdrant ID (point ID), not the payload ID
                            int qdrantId = (Integer) point.get("id");
                            logger.info("Found Qdrant ID {} for candidate {}", qdrantId, candidateId);
                            return qdrantId;
                        }
                    }
                }
            }
            
            return -1; // Not found
            
        } catch (Exception e) {
            logger.error("Error finding Qdrant ID for candidate ID {}: {}", candidateId, e.getMessage());
            return -1;
        }
    }
    
    /**
     * Convert any ID to numeric ID for Qdrant
     */
    private int convertToNumericId(String candidateId) {
        try {
            if (candidateId.startsWith("candidate_")) {
                // For test data format: candidate_1 -> 1
                String numericPart = candidateId.substring("candidate_".length());
                return Integer.parseInt(numericPart);
            } else {
                // For UUID or other formats: use hash but ensure positive
                return Math.abs(candidateId.hashCode());
            }
        } catch (NumberFormatException e) {
            logger.warn("Could not convert ID to numeric: {}, using hash fallback", candidateId);
            return Math.abs(candidateId.hashCode());
        }
    }
    

    /**
     * Generate work experience text for vectorization (same logic as TestDataService)
     */
    private String generateWorkExperienceText(Candidate candidate) {
        StringBuilder expText = new StringBuilder();
        
        // Simple work experience generation based on years of experience
        if (candidate.getYearsOfExperience() != null && candidate.getYearsOfExperience() > 0) {
            int years = candidate.getYearsOfExperience();
            String position = candidate.getCurrentPosition() != null ? candidate.getCurrentPosition() : "Software Developer";
            
            expText.append(position).append(" with ").append(years).append(" years of experience ");
            expText.append("in software development and technology solutions. ");
            expText.append("Experienced in ").append(candidate.getSkills() != null ? String.join(", ", candidate.getSkills()) : "Programming").append(". ");
        }
        
        return expText.toString();
    }
}

