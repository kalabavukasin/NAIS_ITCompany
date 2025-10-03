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
import java.util.HashMap;
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
            Candidate savedCandidate = candidateRepository.save(candidate);
            candidateId = savedCandidate.getId();
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
            point.put("id", candidate.getId());
            // Combine CV and skills vectors
            float[] combinedVector = combineVectors(
                vectorizationService.vectorizeText(candidate.getCvContent()),
                candidate.getSkills() != null ? vectorizationService.vectorizeSkills(candidate.getSkills()) : new float[384]
            );
            point.put("vector", combinedVector);
            
            // Minimal payload for Qdrant
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", candidate.getId());
            payload.put("name", candidate.getFirstName() + " " + candidate.getLastName());
            payload.put("email", candidate.getEmail());
            payload.put("skills", candidate.getSkills());
            payload.put("location", candidate.getLocation());
            payload.put("experience", candidate.getYearsOfExperience());
            
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
            logger.error("Error saving candidate to Qdrant: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save candidate to Qdrant", e);
        }
    }

    /**
     * Update candidate in Qdrant
     */
    private void updateInQdrant(Candidate candidate) {
        try {
            String url = qdrantUrl + "/collections/candidates/points/" + candidate.getId();
            
            Map<String, Object> point = new HashMap<>();
            point.put("id", candidate.getId());
            // Combine CV and skills vectors
            float[] combinedVector = combineVectors(
                vectorizationService.vectorizeText(candidate.getCvContent()),
                candidate.getSkills() != null ? vectorizationService.vectorizeSkills(candidate.getSkills()) : new float[384]
            );
            point.put("vector", combinedVector);
            
            // Minimal payload for Qdrant
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", candidate.getId());
            payload.put("name", candidate.getFirstName() + " " + candidate.getLastName());
            payload.put("email", candidate.getEmail());
            payload.put("skills", candidate.getSkills());
            payload.put("location", candidate.getLocation());
            payload.put("experience", candidate.getYearsOfExperience());
            
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
            logger.error("Error updating candidate in Qdrant: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update candidate in Qdrant", e);
        }
    }

    /**
     * Delete candidate from Qdrant
     */
    private void deleteFromQdrant(String candidateId) {
        try {
            String url = qdrantUrl + "/collections/candidates/points/" + candidateId;
            
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = qdrantRestTemplate.exchange(
                url, HttpMethod.DELETE, request, String.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to delete from Qdrant: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("Error deleting candidate from Qdrant: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete candidate from Qdrant", e);
        }
    }
    
    /**
     * Combine CV and skills vectors into a single vector
     */
    private float[] combineVectors(float[] vector1, float[] vector2) {
        if (vector1 == null && vector2 == null) return new float[384];
        if (vector1 == null) return vector2;
        if (vector2 == null) return vector1;
        
        float[] combined = new float[384];
        int half = 192;
        
        // First half from vector1, second half from vector2
        System.arraycopy(vector1, 0, combined, 0, Math.min(half, vector1.length));
        System.arraycopy(vector2, 0, combined, half, Math.min(half, vector2.length));
        
        return combined;
    }
}

