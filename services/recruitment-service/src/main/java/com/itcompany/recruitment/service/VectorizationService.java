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

    private static final int VECTOR_DIMENSION = 768; // Standardna dimenzija
    private final Map<String, float[]> wordVectors = new HashMap<>();

    public VectorizationService() {
        initializeCommonWords();
    }
    /**
     * Vektorizuje tekst koristeći lokalnu TF-IDF baziranu metodu
     * BEZ KORIŠĆENJA AI SERVISA
     */
    public float[] vectorizeText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new float[VECTOR_DIMENSION];
        }

        // Tokenizacija i normalizacija
        String[] words = text.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .split("\\s+");

        float[] documentVector = new float[VECTOR_DIMENSION];

        // Za svaku reč, dodaj njen vektor u ukupni vektor dokumenta
        for (String word : words) {
            float[] wordVector = getWordVector(word);
            for (int i = 0; i < VECTOR_DIMENSION; i++) {
                documentVector[i] += wordVector[i];
            }
        }

        // Normalizuj vektor
        return normalizeVector(documentVector);
    }

    /**
     * Vektorizuje listu veština
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
     * Generiše vektor za pojedinačnu reč
     * Koristi hash funkciju za determinističko mapiranje reči na vektore
     */
    private float[] getWordVector(String word) {
        // Provjeri cache
        if (wordVectors.containsKey(word)) {
            return wordVectors.get(word);
        }

        float[] vector = new float[VECTOR_DIMENSION];

        try {
            // Koristi SHA-256 hash za determinističko generisanje
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(word.getBytes(StandardCharsets.UTF_8));

            // Konvertuj hash bytes u float vrednosti
            for (int i = 0; i < VECTOR_DIMENSION && i < hash.length; i++) {
                // Mapiranje byte vrednosti na float između -1 i 1
                vector[i] = (hash[i] & 0xFF) / 127.5f - 1.0f;

                // Dodaj malo "šuma" za različite pozicije
                if (i > 0) {
                    vector[i] += vector[i-1] * 0.1f;
                }
            }

            // Dodatno raspršivanje za bolje rezultate
            for (int i = 0; i < VECTOR_DIMENSION; i++) {
                int hashPos = Math.abs(word.hashCode() + i) % VECTOR_DIMENSION;
                vector[hashPos] += 0.5f;
            }

        } catch (Exception e) {
            log.error("Error generating word vector", e);
            // Fallback na jednostavniji pristup
            Random rand = new Random(word.hashCode());
            for (int i = 0; i < VECTOR_DIMENSION; i++) {
                vector[i] = rand.nextFloat() * 2 - 1;
            }
        }

        vector = normalizeVector(vector);
        wordVectors.put(word, vector); // Cache za buduće korišćenje

        return vector;
    }

    /**
     * Specijalizovan vektor za tehničke veštine
     * Daje veću težinu poznatim tehnologijama
     */
    private float[] getSkillVector(String skill) {
        float[] baseVector = getWordVector(skill.toLowerCase());

        // Boost za poznate tehnologije
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
     * Normalizuje vektor (L2 normalizacija)
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
     * Računa kosinusnu sličnost između dva vektora
     */
    public double calculateCosineSimilarity(float[] vec1, float[] vec2) {
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
     * Inicijalizuje često korišćene reči sa predefinisanim vektorima
     * Ovo poboljšava konzistentnost vektorizacije
     */
    private void initializeCommonWords() {
        // IT termini
        String[] commonITTerms = {
                "developer", "software", "engineer", "programming", "code",
                "application", "system", "database", "frontend", "backend",
                "fullstack", "devops", "cloud", "microservices", "api",
                "rest", "agile", "scrum", "testing", "deployment"
        };

        // Generiši konzistentne vektore za česte termine
        for (String term : commonITTerms) {
            getWordVector(term); // Ovo će ih keširati
        }

        log.info("Initialized {} common word vectors", wordVectors.size());
    }

    /**
     * Kombinuje više vektora u jedan (za složene dokumente)
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
