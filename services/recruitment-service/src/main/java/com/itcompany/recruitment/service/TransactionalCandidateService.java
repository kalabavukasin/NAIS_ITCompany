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
 * Transakciona obrada podataka za Candidate entitet
 * Implementira Saga pattern (orkestracija) za čuvanje u Elasticsearch i Qdrant
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
     * SAGA STEP 1: Kreiranje kandidata sa transakcionom obradom
     * 1. Vektorizacija
     * 2. Čuvanje u Elasticsearch
     * 3. Čuvanje u Qdrant
     * 4. Rollback ako nešto ne uspe
     */
    @Transactional
    public Candidate createCandidate(Candidate candidate) {
        String candidateId = null;
        
        try {
            logger.info("Starting transactional creation of candidate: {}", candidate.getEmail());
            
            // STEP 1: Vektorizacija
            if (candidate.getCvContent() != null) {
                candidate.setCvVector(vectorizationService.vectorizeText(candidate.getCvContent()));
                logger.debug("CV vectorized successfully");
            }
            if (candidate.getSkills() != null) {
                candidate.setSkillsVector(vectorizationService.vectorizeSkills(candidate.getSkills()));
                logger.debug("Skills vectorized successfully");
            }
            candidate.setRegistrationDate(LocalDateTime.now());
            
            // STEP 2: Čuvanje u Elasticsearch (glavna baza)
            Candidate savedCandidate = candidateRepository.save(candidate);
            candidateId = savedCandidate.getId();
            logger.info("Candidate saved to Elasticsearch with ID: {}", candidateId);
            
            // STEP 3: Čuvanje u Qdrant (vektorska baza)
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
     * SAGA STEP 2: Ažuriranje kandidata sa transakcionom obradom
     */
    @Transactional
    public Candidate updateCandidate(String id, Candidate candidate) {
        try {
            logger.info("Starting transactional update of candidate: {}", id);
            
            // STEP 1: Proveri da li kandidat postoji
            Optional<Candidate> existingOpt = candidateRepository.findById(id);
            if (existingOpt.isEmpty()) {
                throw new IllegalArgumentException("Candidate not found: " + id);
            }
            
            // STEP 2: Vektorizacija (ako su promenjeni CV ili skills)
            if (candidate.getCvContent() != null) {
                candidate.setCvVector(vectorizationService.vectorizeText(candidate.getCvContent()));
            }
            if (candidate.getSkills() != null) {
                candidate.setSkillsVector(vectorizationService.vectorizeSkills(candidate.getSkills()));
            }
            candidate.setId(id);
            
            // STEP 3: Ažuriranje u Elasticsearch
            Candidate updatedCandidate = candidateRepository.save(candidate);
            logger.info("Candidate updated in Elasticsearch: {}", id);
            
            // STEP 4: Ažuriranje u Qdrant
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
     * SAGA STEP 3: Brisanje kandidata sa transakcionom obradom
     */
    @Transactional
    public void deleteCandidate(String id) {
        try {
            logger.info("Starting transactional deletion of candidate: {}", id);
            
            // STEP 1: Proveri da li kandidat postoji
            Optional<Candidate> existingOpt = candidateRepository.findById(id);
            if (existingOpt.isEmpty()) {
                throw new IllegalArgumentException("Candidate not found: " + id);
            }
            
            // STEP 2: Brisanje iz Qdrant
            deleteFromQdrant(id);
            logger.info("Candidate deleted from Qdrant: {}", id);
            
            // STEP 3: Brisanje iz Elasticsearch
            candidateRepository.deleteById(id);
            logger.info("Candidate deleted from Elasticsearch: {}", id);
            
            logger.info("Transactional deletion completed successfully for candidate: {}", id);
            
        } catch (Exception e) {
            logger.error("Error in transactional deletion of candidate: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete candidate from both databases", e);
        }
    }

    /**
     * Čuvanje kandidata u Qdrant
     */
    private void saveToQdrant(Candidate candidate) {
        try {
            String url = qdrantUrl + "/collections/candidates/points";
            
            Map<String, Object> point = new HashMap<>();
            point.put("id", candidate.getId());
            point.put("vector", candidate.getCvVector()); // Koristi CV vektor
            
            // Minimalni payload za Qdrant
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
     * Ažuriranje kandidata u Qdrant
     */
    private void updateInQdrant(Candidate candidate) {
        try {
            String url = qdrantUrl + "/collections/candidates/points/" + candidate.getId();
            
            Map<String, Object> point = new HashMap<>();
            point.put("id", candidate.getId());
            point.put("vector", candidate.getCvVector());
            
            // Minimalni payload za Qdrant
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
     * Brisanje kandidata iz Qdrant
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
}
