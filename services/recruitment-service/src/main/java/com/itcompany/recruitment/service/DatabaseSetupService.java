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
        
        int maxRetries = 5;
        int retryDelay = 3000; // 3 seconds
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                logger.info("Database initialization attempt {} of {}", attempt, maxRetries);
                
                setupQdrantCollections();
                setupElasticsearchIndices();
                
                logger.info("Database initialization completed successfully!");
                return;
            } catch (Exception e) {
                logger.warn("Database initialization attempt {} failed: {}", attempt, e.getMessage());
                
                if (attempt < maxRetries) {
                    try {
                        logger.info("Retrying in {} seconds...", retryDelay / 1000);
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Database initialization interrupted", ie);
                    }
                } else {
                    logger.error("Database initialization failed after {} attempts", maxRetries);
                    throw new RuntimeException("Failed to initialize databases after " + maxRetries + " attempts", e);
                }
            }
        }
    }

    private void setupQdrantCollections() {
        logger.info("Setting up Qdrant collections...");
        
        // Setup candidates collection
        createQdrantCollection("candidates", 384);
        
        // Setup job_advertisements collection
        createQdrantCollection("job_advertisements", 384);
        
        // Setup applications collection
        createQdrantCollection("applications", 384);
        
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
        
        
        logger.info("Elasticsearch indices will be created automatically by Spring Data Elasticsearch!");
    }

}
