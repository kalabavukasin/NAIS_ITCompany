package com.itcompany.recruitment.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class VectorizationService {

    private static final Logger log = LoggerFactory.getLogger(VectorizationService.class);

    private static final int VECTOR_DIMENSION = 384; // Standardna dimenzija
    private final Map<String, float[]> wordVectors = new HashMap<>();

    public VectorizationService() {
        initializeCommonWords();
    }
    /**
     * Vectorize text using local TF-IDF based method
     */
    public float[] vectorizeText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new float[VECTOR_DIMENSION];
        }

        // Tokenization and normalization
        String[] words = text.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .split("\\s+");

        float[] documentVector = new float[VECTOR_DIMENSION];

        // For each word, add its vector to the document vector
        for (String word : words) {
            float[] wordVector = getWordVector(word);
            for (int i = 0; i < VECTOR_DIMENSION; i++) {
                documentVector[i] += wordVector[i];
            }
        }

        // Normalize vector
        return normalizeVector(documentVector);
    }

    /**
     * Vectorize list of skills
     */
    public float[] vectorizeSkills(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return new float[VECTOR_DIMENSION];
        }

        float[] skillsVector = new float[VECTOR_DIMENSION];

        for (String skill : skills) {
            float[] skillVector = getSkillVector(skill);
            for (int i = 0; i < VECTOR_DIMENSION; i++) {
                skillsVector[i] += skillVector[i];
            }
        }

        return normalizeVector(skillsVector);
    }

    /**
     * Generate vector for a single word
     * Uses hash function for deterministic mapping of words to vectors
     */
    private float[] getWordVector(String word) {
        // Check cache
        if (wordVectors.containsKey(word)) {
            return wordVectors.get(word);
        }

        float[] vector = new float[VECTOR_DIMENSION];

        try {
            // Use SHA-256 hash for deterministic generation
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(word.getBytes(StandardCharsets.UTF_8));

            // Convert hash bytes to float values
            for (int i = 0; i < VECTOR_DIMENSION && i < hash.length; i++) {
                // Map byte values to float between -1 and 1
                vector[i] = (hash[i] & 0xFF) / 127.5f - 1.0f;

                // Add some "noise" for different positions
                if (i > 0) {
                    vector[i] += vector[i-1] * 0.1f;
                }
            }

            // Additional scattering for better results
            for (int i = 0; i < VECTOR_DIMENSION; i++) {
                int hashPos = Math.abs(word.hashCode() + i) % VECTOR_DIMENSION;
                vector[hashPos] += 0.5f;
            }

        } catch (Exception e) {
            log.error("Error generating word vector", e);
            // Fallback to simpler approach
            Random rand = new Random(word.hashCode());
            for (int i = 0; i < VECTOR_DIMENSION; i++) {
                vector[i] = rand.nextFloat() * 2 - 1;
            }
        }

        vector = normalizeVector(vector);
        wordVectors.put(word, vector); // Cache for future use

        return vector;
    }

    /**
     * Specialized vector for technical skills
     * Gives higher weight to known technologies
     */
    private float[] getSkillVector(String skill) {
        float[] baseVector = getWordVector(skill.toLowerCase());

        // Boost for known technologies
        Map<String, Float> skillWeights = Map.of(
                "java", 1.5f,
                "python", 1.5f,
                "javascript", 1.5f,
                "spring", 1.4f,
                "react", 1.4f,
                "docker", 1.3f,
                "kubernetes", 1.3f,
                "sql", 1.2f,
                "git", 1.2f,
                "aws", 1.3f
        );

        String skillLower = skill.toLowerCase();
        if (skillWeights.containsKey(skillLower)) {
            float weight = skillWeights.get(skillLower);
            for (int i = 0; i < baseVector.length; i++) {
                baseVector[i] *= weight;
            }
        }

        return normalizeVector(baseVector);
    }

    /**
     * Normalize vector (L2 normalization)
     */
    private float[] normalizeVector(float[] vector) {
        float sum = 0;
        for (float v : vector) {
            sum += v * v;
        }

        if (sum == 0) {
            return vector;
        }

        float norm = (float) Math.sqrt(sum);
        float[] normalized = new float[vector.length];

        for (int i = 0; i < vector.length; i++) {
            normalized[i] = vector[i] / norm;
        }

        return normalized;
    }

    /**
     * Calculate cosine similarity between two vectors
     */
    public double calculateCosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1 == null || vec2 == null) {
            throw new IllegalArgumentException("Vectors cannot be null");
        }
        if (vec1.length != vec2.length) {
            throw new IllegalArgumentException("Vectors must have the same dimension");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        if (norm1 == 0 || norm2 == 0) {
            return 0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * Initialize commonly used words with predefined vectors
     * This improves consistency of vectorization
     */
    private void initializeCommonWords() {
        // IT termini
        String[] commonITTerms = {
                "developer", "software", "engineer", "programming", "code",
                "application", "system", "database", "frontend", "backend",
                "fullstack", "devops", "cloud", "microservices", "api",
                "rest", "agile", "scrum", "testing", "deployment"
        };

        // Generate consistent vectors for common terms
        for (String term : commonITTerms) {
            getWordVector(term); // This will cache them
        }

        log.info("Initialized {} common word vectors", wordVectors.size());
    }

    /**
     * Combine multiple vectors into one (for complex documents)
     */
    public float[] combineVectors(List<float[]> vectors, List<Float> weights) {
        if (vectors.isEmpty()) {
            return new float[VECTOR_DIMENSION];
        }

        float[] combined = new float[VECTOR_DIMENSION];

        for (int i = 0; i < vectors.size(); i++) {
            float[] vector = vectors.get(i);
            float weight = (weights != null && i < weights.size()) ? weights.get(i) : 1.0f;

            for (int j = 0; j < VECTOR_DIMENSION; j++) {
                combined[j] += vector[j] * weight;
            }
        }

        return normalizeVector(combined);
    }
}
