package com.itcompany.recruitment.service;

import com.itcompany.recruitment.model.qdrant.QdrantPoint;
import com.itcompany.recruitment.model.qdrant.QdrantSearchRequest;
import com.itcompany.recruitment.model.qdrant.QdrantSearchResponse;
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


    /**
     * Search similar candidates in Qdrant
     */
    public List<Map<String, Object>> searchSimilarCandidates(List<Double> queryVector, int limit) {
        try {
            String url = qdrantUrl + "/collections/candidates/points/search";
            
            QdrantSearchRequest request = new QdrantSearchRequest(queryVector, limit);
            request.setWithPayload(true);
            request.setWithVector(false);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<QdrantSearchRequest> httpRequest = new HttpEntity<>(request, headers);
            
            ResponseEntity<QdrantSearchResponse> response = qdrantRestTemplate.exchange(
                url, HttpMethod.POST, httpRequest, QdrantSearchResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> results = new ArrayList<>();
                for (QdrantPoint point : response.getBody().getResult()) {
                    results.add(point.getPayload());
                }
                return results;
            }
        } catch (Exception e) {
            logger.error("Error searching similar candidates: {}", e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    /**
     * Search similar job postings in Qdrant
     */
    public List<Map<String, Object>> searchSimilarJobPostings(List<Double> queryVector, int limit) {
        try {
            String url = qdrantUrl + "/collections/job_advertisements/points/search";
            
            QdrantSearchRequest request = new QdrantSearchRequest(queryVector, limit);
            request.setWithPayload(true);
            request.setWithVector(false);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<QdrantSearchRequest> httpRequest = new HttpEntity<>(request, headers);
            
            ResponseEntity<QdrantSearchResponse> response = qdrantRestTemplate.exchange(
                url, HttpMethod.POST, httpRequest, QdrantSearchResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> results = new ArrayList<>();
                for (QdrantPoint point : response.getBody().getResult()) {
                    results.add(point.getPayload());
                }
                return results;
            }
        } catch (Exception e) {
            logger.error("Error searching similar job postings: {}", e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    /**
     * Search candidates with filters
     */
    public List<Map<String, Object>> searchCandidatesWithFilters(List<Double> queryVector, 
                                                               Map<String, Object> filters, 
                                                               int limit) {
        try {
            String url = qdrantUrl + "/collections/candidates/points/search";
            
            QdrantSearchRequest request = new QdrantSearchRequest(queryVector, limit);
            request.setWithPayload(true);
            request.setWithVector(false);
            request.setFilter(filters);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<QdrantSearchRequest> httpRequest = new HttpEntity<>(request, headers);
            
            ResponseEntity<QdrantSearchResponse> response = qdrantRestTemplate.exchange(
                url, HttpMethod.POST, httpRequest, QdrantSearchResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> results = new ArrayList<>();
                for (QdrantPoint point : response.getBody().getResult()) {
                    results.add(point.getPayload());
                }
                return results;
            }
        } catch (Exception e) {
            logger.error("Error searching candidates with filters: {}", e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    /**
     * Get candidate by ID from Qdrant
     */
    public Map<String, Object> getCandidateById(String candidateId) {
        try {
            String url = qdrantUrl + "/collections/candidates/points/" + candidateId + "?with_payload=true";
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = qdrantRestTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) response.getBody().get("result");
                if (result != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> payload = (Map<String, Object>) result.get("payload");
                    return payload;
                }
            }
        } catch (Exception e) {
            logger.error("Error getting candidate by ID {}: {}", candidateId, e.getMessage(), e);
        }
        return null;
    }

    /**
     * Get job posting by ID from Qdrant
     */
    public Map<String, Object> getJobPostingById(String jobId) {
        try {
            String url = qdrantUrl + "/collections/job_advertisements/points/" + jobId + "?with_payload=true";
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = qdrantRestTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) response.getBody().get("result");
                if (result != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> payload = (Map<String, Object>) result.get("payload");
                    return payload;
                }
            }
        } catch (Exception e) {
            logger.error("Error getting job posting by ID {}: {}", jobId, e.getMessage(), e);
        }
        return null;
    }

    /**
     * Count candidates by location
     */
    public long countCandidatesByLocation(String location) {
        try {
            String url = qdrantUrl + "/collections/candidates/points/count";
            
            Map<String, Object> filter = new HashMap<>();
            Map<String, Object> must = new HashMap<>();
            Map<String, Object> key = new HashMap<>();
            key.put("key", "location");
            key.put("match", Map.of("value", location));
            must.put("must", List.of(key));
            filter.put("filter", must);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(filter, headers);
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = qdrantRestTemplate.exchange(
                url, HttpMethod.POST, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) response.getBody().get("result");
                if (result != null) {
                    return ((Number) result.get("count")).longValue();
                }
            }
        } catch (Exception e) {
            logger.error("Error counting candidates by location {}: {}", location, e.getMessage(), e);
        }
        return 0;
    }

    /**
     * Delete candidate from Qdrant
     */
    public boolean deleteCandidate(String candidateId) {
        try {
            String url = qdrantUrl + "/collections/candidates/points/delete";
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("points", List.of(candidateId));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            
            ResponseEntity<String> response = qdrantRestTemplate.exchange(
                url, HttpMethod.POST, request, String.class);
            
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.error("Error deleting candidate {}: {}", candidateId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Delete job posting from Qdrant
     */
    public boolean deleteJobPosting(String jobId) {
        try {
            String url = qdrantUrl + "/collections/job_advertisements/points/delete";
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("points", List.of(jobId));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            
            ResponseEntity<String> response = qdrantRestTemplate.exchange(
                url, HttpMethod.POST, request, String.class);
            
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.error("Error deleting job posting {}: {}", jobId, e.getMessage(), e);
            return false;
        }
    }
}