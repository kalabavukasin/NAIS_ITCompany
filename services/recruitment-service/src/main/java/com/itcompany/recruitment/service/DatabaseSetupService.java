package com.itcompany.recruitment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class DatabaseSetupService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSetupService.class);

    @Autowired
    @Qualifier("qdrantRestTemplate")
    private RestTemplate qdrantRestTemplate;

    @Autowired
    @Qualifier("elasticsearchRestTemplate")
    private RestTemplate elasticsearchRestTemplate;

    @Value("${qdrant.url}")
    private String qdrantUrl;

    @Value("${elasticsearch.url}")
    private String elasticsearchUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void initializeDatabases() {
        logger.info("Starting database initialization...");
        
        try {
            setupQdrantCollections();
            setupElasticsearchIndices();
            logger.info("Database initialization completed successfully!");
        } catch (Exception e) {
            logger.error("Error during database initialization: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize databases", e);
        }
    }

    private void setupQdrantCollections() {
        logger.info("Setting up Qdrant collections...");
        
        // Setup candidates collection
        createQdrantCollection("candidates", 768);
        
        // Setup job_advertisements collection
        createQdrantCollection("job_advertisements", 768);
        
        // Setup applications collection
        createQdrantCollection("applications", 768);
        
        logger.info("Qdrant collections created successfully!");
    }

    private void createQdrantCollection(String collectionName, int vectorSize) {
        try {
            String url = qdrantUrl + "/collections/" + collectionName;
            
            Map<String, Object> config = new HashMap<>();
            Map<String, Object> vectors = new HashMap<>();
            vectors.put("size", vectorSize);
            vectors.put("distance", "Cosine");
            config.put("vectors", vectors);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(config, headers);
            
            ResponseEntity<String> response = qdrantRestTemplate.exchange(
                url, HttpMethod.PUT, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Created Qdrant collection: {}", collectionName);
            } else {
                logger.warn("Collection {} might already exist or failed to create: {}", 
                    collectionName, response.getStatusCode());
            }
        } catch (Exception e) {
            logger.warn("Failed to create collection {}: {}", collectionName, e.getMessage());
        }
    }

    private void setupElasticsearchIndices() {
        logger.info("Setting up Elasticsearch indices...");
        
        // Setup candidates index
        createElasticsearchIndex("candidates", getCandidatesMapping());
        
        // Setup job_advertisements index
        createElasticsearchIndex("job_advertisements", getJobAdvertisementsMapping());
        
        logger.info("Elasticsearch indices created successfully!");
    }

    private void createElasticsearchIndex(String indexName, Map<String, Object> mapping) {
        try {
            String url = elasticsearchUrl + "/" + indexName;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(mapping, headers);
            
            ResponseEntity<String> response = elasticsearchRestTemplate.exchange(
                url, HttpMethod.PUT, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Created Elasticsearch index: {}", indexName);
            } else {
                logger.warn("Index {} might already exist or failed to create: {}", 
                    indexName, response.getStatusCode());
            }
        } catch (Exception e) {
            logger.warn("Failed to create index {}: {}", indexName, e.getMessage());
        }
    }

    private Map<String, Object> getCandidatesMapping() {
        Map<String, Object> mapping = new HashMap<>();
        Map<String, Object> properties = new HashMap<>();
        
        properties.put("name", Map.of("type", "text"));
        properties.put("email", Map.of("type", "keyword"));
        properties.put("phone", Map.of("type", "keyword"));
        properties.put("skills", Map.of("type", "text"));
        properties.put("experience", Map.of("type", "integer"));
        properties.put("location", Map.of("type", "keyword"));
        properties.put("cv_content", Map.of("type", "text"));
        properties.put("education", Map.of("type", "text"));
        properties.put("languages", Map.of("type", "keyword"));
        properties.put("created_at", Map.of("type", "date"));
        
        mapping.put("mappings", Map.of("properties", properties));
        return mapping;
    }

    private Map<String, Object> getJobAdvertisementsMapping() {
        Map<String, Object> mapping = new HashMap<>();
        Map<String, Object> properties = new HashMap<>();
        
        properties.put("title", Map.of("type", "text"));
        properties.put("description", Map.of("type", "text"));
        properties.put("requirements", Map.of("type", "text"));
        properties.put("location", Map.of("type", "keyword"));
        properties.put("salary_min", Map.of("type", "integer"));
        properties.put("salary_max", Map.of("type", "integer"));
        properties.put("company", Map.of("type", "keyword"));
        properties.put("employment_type", Map.of("type", "keyword"));
        properties.put("experience_level", Map.of("type", "keyword"));
        properties.put("created_at", Map.of("type", "date"));
        
        mapping.put("mappings", Map.of("properties", properties));
        return mapping;
    }
}
