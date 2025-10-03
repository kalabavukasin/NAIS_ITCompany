package com.itcompany.recruitment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itcompany.recruitment.model.qdrant.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class QdrantService {

    private static final Logger logger = LoggerFactory.getLogger(QdrantService.class);

    @Autowired
    @Qualifier("qdrantRestTemplate")
    private RestTemplate qdrantRestTemplate;

    @Value("${qdrant.url}")
    private String qdrantUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Store candidate vectors in Qdrant
     */
    public void storeCandidateVector(CandidateVector candidateVector) {
        try {
            String url = qdrantUrl + "/collections/candidates/points";
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("points", List.of(createPoint(candidateVector.getCandidateId(), candidateVector)));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            
            ResponseEntity<String> response = qdrantRestTemplate.exchange(url, HttpMethod.PUT, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully stored candidate vector for ID: {}", candidateVector.getCandidateId());
            } else {
                logger.warn("Failed to store candidate vector for ID: {}, status: {}", 
                    candidateVector.getCandidateId(), response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Error storing candidate vector for ID: {}", candidateVector.getCandidateId(), e);
            throw new RuntimeException("Failed to store candidate vector", e);
        }
    }

    /**
     * Store job posting vectors in Qdrant
     */
    public void storeJobPostingVector(JobPostingVector jobPostingVector) {
        try {
            String url = qdrantUrl + "/collections/job_advertisements/points";
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("points", List.of(createPoint(jobPostingVector.getJobPostingId(), jobPostingVector)));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            
            ResponseEntity<String> response = qdrantRestTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully stored job posting vector for ID: {}", jobPostingVector.getJobPostingId());
            } else {
                logger.warn("Failed to store job posting vector for ID: {}, status: {}", 
                    jobPostingVector.getJobPostingId(), response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Error storing job posting vector for ID: {}", jobPostingVector.getJobPostingId(), e);
            throw new RuntimeException("Failed to store job posting vector", e);
        }
    }

    /**
     * Store application vectors in Qdrant
     */
    public void storeApplicationVector(ApplicationVector applicationVector) {
        try {
            String url = qdrantUrl + "/collections/applications/points";
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("points", List.of(createPoint(applicationVector.getApplicationId(), applicationVector)));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            
            ResponseEntity<String> response = qdrantRestTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully stored application vector for ID: {}", applicationVector.getApplicationId());
            } else {
                logger.warn("Failed to store application vector for ID: {}, status: {}", 
                    applicationVector.getApplicationId(), response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Error storing application vector for ID: {}", applicationVector.getApplicationId(), e);
            throw new RuntimeException("Failed to store application vector", e);
        }
    }

    /**
     * Search similar candidates based on CV vector
     */
    public List<CandidateVector> searchSimilarCandidates(float[] cvVector, int limit) {
        try {
            String url = qdrantUrl + "/collections/candidates/points/search";
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("vector", cvVector);
            payload.put("limit", limit);
            payload.put("with_payload", true);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            
            ResponseEntity<Map> response = qdrantRestTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseCandidateSearchResults(response.getBody());
            } else {
                logger.warn("Failed to search similar candidates, status: {}", response.getStatusCode());
                return new ArrayList<>();
            }
        } catch (Exception e) {
            logger.error("Error searching similar candidates", e);
            throw new RuntimeException("Failed to search similar candidates", e);
        }
    }

    /**
     * Search similar job postings based on description vector
     */
    public List<JobPostingVector> searchSimilarJobPostings(float[] descriptionVector, int limit) {
        try {
            String url = qdrantUrl + "/collections/job_advertisements/points/search";
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("vector", descriptionVector);
            payload.put("limit", limit);
            payload.put("with_payload", true);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            
            ResponseEntity<Map> response = qdrantRestTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseJobPostingSearchResults(response.getBody());
            } else {
                logger.warn("Failed to search similar job postings, status: {}", response.getStatusCode());
                return new ArrayList<>();
            }
        } catch (Exception e) {
            logger.error("Error searching similar job postings", e);
            throw new RuntimeException("Failed to search similar job postings", e);
        }
    }

    private Map<String, Object> createPoint(String id, Object vectorData) {
        Map<String, Object> point = new HashMap<>();
        // Convert string ID to integer for Qdrant compatibility
        point.put("id", Math.abs(id.hashCode()));
        
        float[] vector = extractVector(vectorData);
        if (vector == null || vector.length == 0) {
            logger.warn("Empty or null vector for ID: {}, skipping", id);
            throw new IllegalArgumentException("Vector cannot be null or empty");
        }
        point.put("vector", vector);
        point.put("payload", extractPayload(vectorData));
        
        logger.debug("Created point for ID: {} with vector length: {}", id, vector.length);
        return point;
    }

    private float[] extractVector(Object vectorData) {
        if (vectorData instanceof CandidateVector) {
            CandidateVector cv = (CandidateVector) vectorData;
            // Combine CV and skills vectors
            return combineVectors(cv.getCvVector(), cv.getSkillsVector());
        } else if (vectorData instanceof JobPostingVector) {
            return ((JobPostingVector) vectorData).getDescriptionVector();
        } else if (vectorData instanceof ApplicationVector) {
            return ((ApplicationVector) vectorData).getCoverLetterVector();
        }
        return new float[0];
    }

    private Map<String, Object> extractPayload(Object vectorData) {
        Map<String, Object> payload = new HashMap<>();
        
        if (vectorData instanceof CandidateVector) {
            CandidateVector cv = (CandidateVector) vectorData;
            payload.put("candidate_id", cv.getCandidateId());
            payload.put("cv_content", cv.getCvContent());
            payload.put("skills", cv.getSkills());
        } else if (vectorData instanceof JobPostingVector) {
            JobPostingVector jpv = (JobPostingVector) vectorData;
            payload.put("job_posting_id", jpv.getJobPostingId());
            payload.put("title", jpv.getTitle());
            payload.put("description", jpv.getDescription());
            payload.put("required_skills", jpv.getRequiredSkills());
            payload.put("preferred_skills", jpv.getPreferredSkills());
        } else if (vectorData instanceof ApplicationVector) {
            ApplicationVector av = (ApplicationVector) vectorData;
            payload.put("application_id", av.getApplicationId());
            payload.put("candidate_id", av.getCandidateId());
            payload.put("job_posting_id", av.getJobPostingId());
            payload.put("cover_letter", av.getCoverLetter());
        }
        
        return payload;
    }

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

    private List<CandidateVector> parseCandidateSearchResults(Map<String, Object> response) {
        List<CandidateVector> results = new ArrayList<>();
        
        try {
            List<Map<String, Object>> resultList = (List<Map<String, Object>>) response.get("result");
            if (resultList != null) {
                for (Map<String, Object> result : resultList) {
                    Map<String, Object> payload = (Map<String, Object>) result.get("payload");
                    if (payload != null) {
                        CandidateVector cv = new CandidateVector();
                        cv.setCandidateId((String) payload.get("candidate_id"));
                        cv.setCvContent((String) payload.get("cv_content"));
                        cv.setSkills((String) payload.get("skills"));
                        results.add(cv);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing candidate search results", e);
        }
        
        return results;
    }

    private List<JobPostingVector> parseJobPostingSearchResults(Map<String, Object> response) {
        List<JobPostingVector> results = new ArrayList<>();
        
        try {
            List<Map<String, Object>> resultList = (List<Map<String, Object>>) response.get("result");
            if (resultList != null) {
                for (Map<String, Object> result : resultList) {
                    Map<String, Object> payload = (Map<String, Object>) result.get("payload");
                    if (payload != null) {
                        JobPostingVector jpv = new JobPostingVector();
                        jpv.setJobPostingId((String) payload.get("job_posting_id"));
                        jpv.setTitle((String) payload.get("title"));
                        jpv.setDescription((String) payload.get("description"));
                        jpv.setRequiredSkills((String) payload.get("required_skills"));
                        jpv.setPreferredSkills((String) payload.get("preferred_skills"));
                        results.add(jpv);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing job posting search results", e);
        }
        
        return results;
    }
}
