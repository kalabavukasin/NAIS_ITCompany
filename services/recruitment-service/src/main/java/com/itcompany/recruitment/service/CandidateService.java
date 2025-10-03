package com.itcompany.recruitment.service;

import com.itcompany.recruitment.model.Candidate;
import com.itcompany.recruitment.model.JobPosting;
import com.itcompany.recruitment.model.qdrant.CandidateVector;
import com.itcompany.recruitment.model.qdrant.JobPostingVector;
import com.itcompany.recruitment.dto.CandidateSearchRequest;
import com.itcompany.recruitment.dto.SimpleCandidateSearchRequest;
import com.itcompany.recruitment.repository.CandidateRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CandidateService {

    private static final Logger logger = LoggerFactory.getLogger(CandidateService.class);
    private final CandidateRepository candidateRepository;
    private final VectorizationService vectorizationService;
    private final ElasticsearchOperations elasticsearchOperations;
    private final JobPostingService jobPostingService;
    private final QdrantService qdrantService;
    private final ObjectMapper objectMapper;
    
    public CandidateService(CandidateRepository candidateRepository,
                            VectorizationService vectorizationService,
                            ElasticsearchOperations elasticsearchOperations,
                            JobPostingService jobPostingService,
                            QdrantService qdrantService) {
        this.candidateRepository = candidateRepository;
        this.vectorizationService = vectorizationService;
        this.elasticsearchOperations = elasticsearchOperations;
        this.jobPostingService = jobPostingService;
        this.qdrantService = qdrantService;
        this.objectMapper = new ObjectMapper();
    }
    
    // CRUD Operations
    public Candidate createCandidate(Candidate candidate) {
        candidate.setRegistrationDate(LocalDateTime.now());
        
        // Save to Elasticsearch (without vectors)
        Candidate savedCandidate = candidateRepository.save(candidate);
        
        // Create and store vectors in Qdrant
        try {
            float[] cvVector = candidate.getCvContent() != null ? 
                vectorizationService.vectorizeText(candidate.getCvContent()) : new float[384];
            float[] skillsVector = candidate.getSkills() != null ? 
                vectorizationService.vectorizeSkills(candidate.getSkills()) : new float[384];
            
            String skillsJson = candidate.getSkills() != null ? 
                objectMapper.writeValueAsString(candidate.getSkills()) : "[]";
            
            CandidateVector candidateVector = new CandidateVector(
                savedCandidate.getId(),
                cvVector,
                skillsVector,
                candidate.getCvContent(),
                skillsJson
            );
            
            qdrantService.storeCandidateVector(candidateVector);
            logger.info("Successfully stored candidate vectors for ID: {}", savedCandidate.getId());
            
        } catch (JsonProcessingException e) {
            logger.error("Error serializing skills for candidate: {}", savedCandidate.getId(), e);
        } catch (Exception e) {
            logger.error("Error storing candidate vectors for ID: {}", savedCandidate.getId(), e);
        }
        
        return savedCandidate;
    }
    
    public Candidate updateCandidate(String id, Candidate candidate) {
        candidate.setId(id);
        // Update vectors in Qdrant if CV content has changed
        if (candidate.getCvContent() != null) {
            try {
                float[] cvVector = vectorizationService.vectorizeText(candidate.getCvContent());
                float[] skillsVector = candidate.getSkills() != null ? 
                    vectorizationService.vectorizeSkills(candidate.getSkills()) : new float[384];
                
                String skillsJson = candidate.getSkills() != null ? 
                    objectMapper.writeValueAsString(candidate.getSkills()) : "[]";
                
                CandidateVector candidateVector = new CandidateVector(
                    candidate.getId(),
                    cvVector,
                    skillsVector,
                    candidate.getCvContent(),
                    skillsJson
                );
                
                qdrantService.storeCandidateVector(candidateVector);
                logger.info("Successfully updated candidate vectors for ID: {}", candidate.getId());
                
            } catch (Exception e) {
                logger.error("Error updating candidate vectors for ID: {}", candidate.getId(), e);
            }
        }
        return candidateRepository.save(candidate);
    }
    
    public Optional<Candidate> findById(String id) {
        return candidateRepository.findById(id);
    }
    
    public void deleteCandidate(String id) {
        candidateRepository.deleteById(id);
    }
    
    public List<Candidate> findAll() {
        List<Candidate> candidates = new ArrayList<>();
        candidateRepository.findAll().forEach(candidates::add);
        return candidates;
    }
    
    // COMPLEX QUERY 1: Vector search combined with filtering (2+ conditions)
    public List<Candidate> searchCandidatesWithVectorAndFilters(CandidateSearchRequest request) {
        Criteria criteria = new Criteria();
        
        // Add filters (minimum 2 conditions as specified)
        if (request.getRequiredSkills() != null && !request.getRequiredSkills().isEmpty()) {
            criteria.and("skills").in(request.getRequiredSkills());
        }
        
        if (request.getLocation() != null) {
            criteria.and("location").is(request.getLocation());
        }
        
        if (request.getMinExperience() != null) {
            criteria.and("yearsOfExperience").greaterThanEqual(request.getMinExperience());
        }
        
        if (request.getMaxExpectedSalary() != null) {
            criteria.and("expectedSalary").lessThanEqual(request.getMaxExpectedSalary());
        }
        
        Query query = new CriteriaQuery(criteria)
            .setPageable(PageRequest.of(0, request.getMaxResults()));
        
        SearchHits<Candidate> searchHits = elasticsearchOperations.search(
            query, 
            Candidate.class
        );
        
        List<Candidate> results = searchHits.stream()
            .map(SearchHit::getContent)
            .collect(Collectors.toList());
        
        // Vector search if we have a job posting
        if (request.getJobPostingId() != null) {
            var jobPosting = jobPostingService.findById(request.getJobPostingId());
            if (jobPosting.isPresent() && jobPosting.get().getDescription() != null) {
                // Sort by vector similarity using Qdrant
                try {
                    float[] jobVector = vectorizationService.vectorizeText(jobPosting.get().getDescription());
                    List<CandidateVector> similarVectors = qdrantService.searchSimilarCandidates(jobVector, results.size());
                    
                    Map<String, Double> vectorScores = new HashMap<>();
                    similarVectors.forEach(cv -> {
                        vectorScores.put(cv.getCandidateId(), 0.8); // Placeholder score
                    });
                    
                    results.sort((a, b) -> {
                        Double scoreA = vectorScores.getOrDefault(a.getId(), 0.0);
                        Double scoreB = vectorScores.getOrDefault(b.getId(), 0.0);
                        return Double.compare(scoreB, scoreA);
                    });
                } catch (Exception e) {
                    logger.error("Error in vector similarity calculation", e);
                }
            }
        }
        
        return results;
    }
    
    // SIMPLE SEARCH: Basic filtering without vector search
    public List<Candidate> simpleSearchCandidates(SimpleCandidateSearchRequest request) {
        Criteria criteria = new Criteria();
        boolean hasFilters = false;
        
        // Add filters based on provided criteria
        if (request.getSkills() != null && !request.getSkills().isEmpty()) {
            criteria.and("skills").in(request.getSkills());
            hasFilters = true;
        }
        
        if (request.getLocation() != null && !request.getLocation().trim().isEmpty()) {
            logger.info("Adding location filter: {}", request.getLocation());
            criteria.and("location").is(request.getLocation()); // Changed from matches to is
            hasFilters = true;
        }
        
        if (request.getMinExperience() != null) {
            criteria.and("yearsOfExperience").greaterThanEqual(request.getMinExperience());
            hasFilters = true;
        }
        
        if (request.getMaxExpectedSalary() != null) {
            criteria.and("expectedSalary").lessThanEqual(request.getMaxExpectedSalary());
            hasFilters = true;
        }
        
        // Text search in CV content if provided
        if (request.getSearchText() != null && !request.getSearchText().trim().isEmpty()) {
            criteria.and("cvContent").matches(request.getSearchText());
            hasFilters = true;
        }
        
        // If no filters provided, return empty list or all candidates (depending on requirement)
        if (!hasFilters) {
            logger.warn("No search criteria provided, returning all candidates");
            // Return all candidates if no filters
            return findAll();
        }
        
        // Try using NativeSearchQuery instead of CriteriaQuery
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder()
            .withPageable(PageRequest.of(0, request.getMaxResults()));
        
        // Add filters
        if (request.getLocation() != null && !request.getLocation().trim().isEmpty()) {
            queryBuilder.withFilter(QueryBuilders.termQuery("location", request.getLocation()));
        }
        
        if (request.getSkills() != null && !request.getSkills().isEmpty()) {
            queryBuilder.withFilter(QueryBuilders.termsQuery("skills", request.getSkills()));
        }
        
        if (request.getMinExperience() != null) {
            queryBuilder.withFilter(QueryBuilders.rangeQuery("yearsOfExperience").gte(request.getMinExperience()));
        }
        
        if (request.getMaxExpectedSalary() != null) {
            queryBuilder.withFilter(QueryBuilders.rangeQuery("expectedSalary").lte(request.getMaxExpectedSalary()));
        }
        
        if (request.getSearchText() != null && !request.getSearchText().trim().isEmpty()) {
            queryBuilder.withQuery(QueryBuilders.matchQuery("cvContent", request.getSearchText()));
        }
        
        NativeSearchQuery query = queryBuilder.build();
        
        logger.info("Searching candidates with native query: {}", query.getQuery());
        
        SearchHits<Candidate> searchHits = elasticsearchOperations.search(
            query, 
            Candidate.class
        );
        
        List<Candidate> results = searchHits.stream()
            .map(SearchHit::getContent)
            .collect(Collectors.toList());
            
        logger.info("Found {} candidates matching criteria", results.size());
        
        return results;
    }
    
    // COMPLEX QUERY 2: Hybrid search - combines vector and text search
    public List<Candidate> hybridSearch(String searchText, List<String> skills, String location) {
        Criteria criteria = new Criteria();
        
        // Text search - uses match query instead of contains
        if (searchText != null && !searchText.isEmpty()) {
            criteria.and("cvContent").matches(searchText);
        }
        
        // Filters
        if (skills != null && !skills.isEmpty()) {
            criteria.and("skills").in(skills);
        }
        if (location != null) {
            criteria.and("location").is(location);
        }
        
        Query query = new CriteriaQuery(criteria)
            .setPageable(PageRequest.of(0, 20));
        
        SearchHits<Candidate> searchHits = elasticsearchOperations.search(
            query,
            Candidate.class
        );
        
        List<Candidate> results = searchHits.stream()
            .map(SearchHit::getContent)
            .collect(Collectors.toList());
        
        // Vector search if we have search text using Qdrant
        if (searchText != null && !searchText.isEmpty()) {
            try {
                float[] searchVector = vectorizationService.vectorizeText(searchText);
                List<CandidateVector> similarVectors = qdrantService.searchSimilarCandidates(searchVector, 50);
                
                // Create a map of candidate IDs to their vector similarity scores
                Map<String, Double> vectorScores = new HashMap<>();
                similarVectors.forEach(cv -> {
                    // Use a placeholder score - in real implementation, Qdrant returns similarity scores
                    vectorScores.put(cv.getCandidateId(), 0.8);
                });
                
                // Sort results by vector similarity
                results.sort((a, b) -> {
                    Double scoreA = vectorScores.getOrDefault(a.getId(), 0.0);
                    Double scoreB = vectorScores.getOrDefault(b.getId(), 0.0);
                    return Double.compare(scoreB, scoreA);
                });
                
            } catch (Exception e) {
                logger.error("Error in vector search for candidates", e);
            }
        }
        
        return results;
    }
    
    // Simple search method for fallback
    public List<Candidate> searchCandidates(String searchText, List<String> skills, String location, Integer limit) {
        Criteria criteria = new Criteria();
        
        if (skills != null && !skills.isEmpty()) {
            criteria.and("skills").in(skills);
        }
        if (location != null) {
            criteria.and("location").is(location);
        }
        
        Query query = new CriteriaQuery(criteria)
            .setPageable(PageRequest.of(0, limit != null ? limit : 20));
        
        SearchHits<Candidate> searchHits = elasticsearchOperations.search(query, Candidate.class);
        
        return searchHits.stream()
            .map(SearchHit::getContent)
            .collect(Collectors.toList());
    }
    
    // HYBRID SEARCH: Combine Elasticsearch filtering with Qdrant vector search
    public List<Candidate> hybridSearch(CandidateSearchRequest request) {
        List<Candidate> results = new ArrayList<>();
        
        try {
            // Step 1: Use Qdrant for vector similarity search
            float[] searchVector = vectorizationService.vectorizeText(request.getSearchText());
            List<CandidateVector> similarVectors = qdrantService.searchSimilarCandidates(searchVector, 50);
            
            // Step 2: Get candidate IDs from vector search results
            Set<String> candidateIds = similarVectors.stream()
                .map(CandidateVector::getCandidateId)
                .collect(Collectors.toSet());
            
            // Step 3: Use Elasticsearch for structured filtering on similar candidates
            if (!candidateIds.isEmpty()) {
                Criteria criteria = new Criteria("id").in(candidateIds);
                
                // Add additional filters
                if (request.getLocation() != null) {
                    criteria = criteria.and(new Criteria("location").is(request.getLocation()));
                }
                if (request.getMinExperience() != null) {
                    criteria = criteria.and(new Criteria("yearsOfExperience").greaterThanEqual(request.getMinExperience()));
                }
                if (request.getRequiredSkills() != null && !request.getRequiredSkills().isEmpty()) {
                    criteria = criteria.and(new Criteria("skills").in(request.getRequiredSkills()));
                }
                
                Query query = new CriteriaQuery(criteria)
                    .setPageable(PageRequest.of(0, request.getMaxResults()));
                
                SearchHits<Candidate> searchHits = elasticsearchOperations.search(query, Candidate.class);
                
                results = searchHits.stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());
                
                // Step 4: Recalculate match scores from Qdrant results
                Map<String, Double> vectorScores = new HashMap<>();
                similarVectors.forEach(cv -> {
                    // Calculate similarity score (simplified)
                    vectorScores.put(cv.getCandidateId(), 0.8); // Placeholder score
                });
                
                results.forEach(candidate -> {
                    Double score = vectorScores.get(candidate.getId());
                    candidate.setMatchScore(score != null ? score : 0.0);
                });
                
                // Sort by match score
                results.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));
            }
            
        } catch (Exception e) {
            logger.error("Error in hybrid search", e);
            // Fallback to regular Elasticsearch search
            return searchCandidates(request.getSearchText(), request.getRequiredSkills(), request.getLocation(), request.getMaxResults());
        }
        
        return results;
    }
    
    // SIMPLE QUERY 1: Count by location
    public long countByLocation(String location) {
        return candidateRepository.findByLocation(location).size();
    }
    
    // SIMPLE QUERY 2: Single vector search
    public List<Candidate> singleVectorSearch(String cvText) {
        float[] vector = vectorizationService.vectorizeText(cvText);
        
        Query query = new CriteriaQuery(new Criteria())
            .setPageable(PageRequest.of(0, 10));
        
        SearchHits<Candidate> searchHits = elasticsearchOperations.search(
            query, 
            Candidate.class
        );
        
        List<Candidate> results = searchHits.stream()
            .map(SearchHit::getContent)
            .collect(Collectors.toList());
        
        // Sort by vector similarity using Qdrant
        try {
            List<CandidateVector> similarVectors = qdrantService.searchSimilarCandidates(vector, results.size());
            
            Map<String, Double> vectorScores = new HashMap<>();
            similarVectors.forEach(cv -> {
                vectorScores.put(cv.getCandidateId(), 0.8); // Placeholder score
            });
            
            results.sort((a, b) -> {
                Double scoreA = vectorScores.getOrDefault(a.getId(), 0.0);
                Double scoreB = vectorScores.getOrDefault(b.getId(), 0.0);
                return Double.compare(scoreB, scoreA);
            });
        } catch (Exception e) {
            logger.error("Error in vector similarity calculation", e);
        }
        
        return results;
    }
    
    // Additional function for ranking candidates
    public List<Candidate> rankCandidates(String jobPostingId) {
        try {
            var jobPosting = jobPostingService.findById(jobPostingId);
            if (jobPosting.isEmpty()) {
                logger.warn("Job posting with ID {} not found", jobPostingId);
                return new ArrayList<>();
            }
            
            var job = jobPosting.get();
            logger.info("Ranking candidates for job: {}", job.getTitle());
            
            // Get only first 20 candidates for testing
            Query query = new CriteriaQuery(new Criteria())
                .setPageable(PageRequest.of(0, 20));
            
            SearchHits<Candidate> searchHits = elasticsearchOperations.search(
                query,
                Candidate.class
            );
            
            List<Candidate> rankedCandidates = new ArrayList<>();
            searchHits.forEach(hit -> {
                try {
                    Candidate candidate = hit.getContent();
                    double score = calculateRankingScore(candidate, job);
                    candidate.setMatchScore(score);
                    rankedCandidates.add(candidate);
                } catch (Exception e) {
                    logger.error("Error processing candidate: {}", e.getMessage());
                }
            });
            
            // Sort by match score
            rankedCandidates.sort((a, b) -> {
                double scoreA = a.getMatchScore() != null ? a.getMatchScore() : 0.0;
                double scoreB = b.getMatchScore() != null ? b.getMatchScore() : 0.0;
                return Double.compare(scoreB, scoreA);
            });
            
            logger.info("Successfully ranked {} candidates", rankedCandidates.size());
            return rankedCandidates;
            
        } catch (Exception e) {
            logger.error("Error in rankCandidates: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    // Helper method for vector similarity
    private double calculateVectorSimilarity(float[] vec1, float[] vec2) {
        if (vec1 == null || vec2 == null) {
            return 0.0;
        }
        return vectorizationService.calculateCosineSimilarity(vec1, vec2);
    }
    
    // Helper method for ranking candidates
    private double calculateRankingScore(Candidate candidate, JobPosting job) {
        double score = 0.0;
        double totalWeight = 0.0;
        
        // CV Vector similarity (40% weight) using Qdrant
        try {
            // Get candidate CV vector from Qdrant
            List<CandidateVector> candidateVectors = qdrantService.searchSimilarCandidates(
                vectorizationService.vectorizeText(candidate.getCvContent()), 1);
            
            // Get job description vector from Qdrant
            List<JobPostingVector> jobVectors = qdrantService.searchSimilarJobPostings(
                vectorizationService.vectorizeText(job.getDescription()), 1);
            
            if (!candidateVectors.isEmpty() && !jobVectors.isEmpty()) {
                float[] cvVector = candidateVectors.get(0).getCvVector();
                float[] jobVector = jobVectors.get(0).getDescriptionVector();
                
                double cvScore = vectorizationService.calculateCosineSimilarity(cvVector, jobVector);
                score += cvScore * 0.4;
                totalWeight += 0.4;
            }
        } catch (Exception e) {
            logger.error("Error calculating CV similarity for candidate: {}", candidate.getId(), e);
        }
        
        // Skills matching (30% weight)
        if (candidate.getSkills() != null && job.getRequiredSkills() != null) {
            Set<String> candidateSkills = new HashSet<>(candidate.getSkills());
            Set<String> requiredSkills = new HashSet<>(job.getRequiredSkills());
            long matchedSkills = candidateSkills.stream()
                .filter(requiredSkills::contains)
                .count();
            double skillScore = requiredSkills.isEmpty() ? 1.0 : 
                (double) matchedSkills / requiredSkills.size();
            score += skillScore * 0.3;
            totalWeight += 0.3;
        }
        
        // Experience matching (20% weight)
        if (candidate.getYearsOfExperience() != null && job.getMinYearsExperience() != null) {
            double expScore = candidate.getYearsOfExperience() >= job.getMinYearsExperience() ? 1.0 :
                (double) candidate.getYearsOfExperience() / job.getMinYearsExperience();
            score += expScore * 0.2;
            totalWeight += 0.2;
        }
        
        // Location matching (10% weight)
        if (candidate.getLocation() != null && job.getLocation() != null && 
            candidate.getLocation().equals(job.getLocation())) {
            score += 1.0 * 0.1;
            totalWeight += 0.1;
        }
        
        return totalWeight > 0 ? score / totalWeight : 0.0;
    }
}
