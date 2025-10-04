package com.itcompany.recruitment.service;

import com.itcompany.recruitment.model.Candidate;
import com.itcompany.recruitment.model.JobPosting;
import com.itcompany.recruitment.dto.CandidateSearchRequest;
import com.itcompany.recruitment.dto.SimpleCandidateSearchRequest;
import com.itcompany.recruitment.repository.CandidateRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final TransactionalCandidateService transactionalCandidateService;
    
    public CandidateService(CandidateRepository candidateRepository,
                            VectorizationService vectorizationService,
                            ElasticsearchOperations elasticsearchOperations,
                            JobPostingService jobPostingService,
                            QdrantService qdrantService,
                            TransactionalCandidateService transactionalCandidateService) {
        this.candidateRepository = candidateRepository;
        this.vectorizationService = vectorizationService;
        this.elasticsearchOperations = elasticsearchOperations;
        this.jobPostingService = jobPostingService;
        this.qdrantService = qdrantService;
        this.transactionalCandidateService = transactionalCandidateService;
    }
    
    // CRUD Operations
    public Candidate createCandidate(Candidate candidate) {
        // Use transactional service for CRUD operations
        return transactionalCandidateService.createCandidate(candidate);
    }
    
    public Candidate updateCandidate(String id, Candidate candidate) {
        // Use transactional service for CRUD operations
        return transactionalCandidateService.updateCandidate(id, candidate);
    }
    
    public Optional<Candidate> findById(String id) {
        return candidateRepository.findById(id);
    }
    
    public void deleteCandidate(String id) {
        // Use transactional service for CRUD operations
        transactionalCandidateService.deleteCandidate(id);
    }
    
    public List<Candidate> findAll() {
        List<Candidate> candidates = new ArrayList<>();
        candidateRepository.findAll().forEach(candidates::add);
        return candidates;
    }
    
    // COMPLEX QUERY 1: Vector search combined with filtering (2+ conditions)
    public List<Candidate> searchCandidatesWithVectorAndFilters(CandidateSearchRequest request) {
        // Pravilno kreiranje Criteria objekta
        Criteria criteria = null;
        
        // Add filters (minimum 2 conditions as specified)
        if (request.getRequiredSkills() != null && !request.getRequiredSkills().isEmpty()) {
            Criteria skillsCriteria = Criteria.where("skills").in(request.getRequiredSkills());
            criteria = (criteria == null) ? skillsCriteria : criteria.and(skillsCriteria);
        }
        
        if (request.getLocation() != null && !request.getLocation().trim().isEmpty()) {
            Criteria locationCriteria = Criteria.where("location").is(request.getLocation());
            criteria = (criteria == null) ? locationCriteria : criteria.and(locationCriteria);
        }
        
        if (request.getMinExperience() != null) {
            Criteria experienceCriteria = Criteria.where("yearsOfExperience").greaterThanEqual(request.getMinExperience());
            criteria = (criteria == null) ? experienceCriteria : criteria.and(experienceCriteria);
        }
        
        if (request.getMaxExpectedSalary() != null) {
            Criteria salaryCriteria = Criteria.where("expectedSalary").lessThanEqual(request.getMaxExpectedSalary());
            criteria = (criteria == null) ? salaryCriteria : criteria.and(salaryCriteria);
        }
        
        // Ako nema kriterija, vraćamo praznu listu
        if (criteria == null) {
            logger.warn("No filters provided for searchCandidatesWithVectorAndFilters");
            return new ArrayList<>();
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
            results = applyVectorRanking(results, request.getJobPostingId());
        }
        
        return results;
    }
    
    // SIMPLE SEARCH: Basic filtering without vector search
    public List<Candidate> simpleSearchCandidates(SimpleCandidateSearchRequest request) {
        // Start with an empty criteria
        Criteria criteria = null;
        boolean hasFilters = false;
        
        // Add filters based on provided criteria
        if (request.getSkills() != null && !request.getSkills().isEmpty()) {
            Criteria skillsCriteria = Criteria.where("skills").in(request.getSkills());
            criteria = (criteria == null) ? skillsCriteria : criteria.and(skillsCriteria);
            hasFilters = true;
        }
        
        if (request.getLocation() != null && !request.getLocation().trim().isEmpty()) {
            // Using is() for exact match
            Criteria locationCriteria = Criteria.where("location").is(request.getLocation());
            criteria = (criteria == null) ? locationCriteria : criteria.and(locationCriteria);
            hasFilters = true;
        }
        
        if (request.getMinExperience() != null) {
            Criteria experienceCriteria = Criteria.where("yearsOfExperience").greaterThanEqual(request.getMinExperience());
            criteria = (criteria == null) ? experienceCriteria : criteria.and(experienceCriteria);
            hasFilters = true;
        }
        
        if (request.getMaxExpectedSalary() != null) {
            Criteria salaryCriteria = Criteria.where("expectedSalary").lessThanEqual(request.getMaxExpectedSalary());
            criteria = (criteria == null) ? salaryCriteria : criteria.and(salaryCriteria);
            hasFilters = true;
        }
        
        // Text search in CV content if provided
        if (request.getSearchText() != null && !request.getSearchText().trim().isEmpty()) {
            // Using matches() for full-text search
            Criteria textCriteria = Criteria.where("cvContent").matches(request.getSearchText());
            criteria = (criteria == null) ? textCriteria : criteria.and(textCriteria);
            hasFilters = true;
        }
        
        // If no filters provided, return empty list
        if (!hasFilters || criteria == null) {
            logger.warn("No search criteria provided, returning empty list");
            return new ArrayList<>();
        }
        
        Query query = new CriteriaQuery(criteria)
            .setPageable(PageRequest.of(0, request.getMaxResults()));
        
        logger.info("Searching candidates with criteria: {}", criteria.toString());
        
        try {
            SearchHits<Candidate> searchHits = elasticsearchOperations.search(
                query, 
                Candidate.class
            );
            
            List<Candidate> results = searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
                
            logger.info("Found {} candidates matching criteria", results.size());
            
            return results;
        } catch (Exception e) {
            logger.error("Error searching candidates: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    // COMPLEX QUERY 2: Hybrid search - combines vector and text search
    public List<Candidate> hybridSearch(String searchText, List<String> skills, String location) {
        // Correct Criteria combination
        Criteria criteria = null;
        
        // Text search
        if (searchText != null && !searchText.trim().isEmpty()) {
            Criteria textCriteria = Criteria.where("cvContent").matches(searchText);
            criteria = textCriteria;
        }
        
        // Filters
        if (skills != null && !skills.isEmpty()) {
            Criteria skillsCriteria = Criteria.where("skills").in(skills);
            criteria = (criteria == null) ? skillsCriteria : criteria.and(skillsCriteria);
        }
        
        if (location != null && !location.trim().isEmpty()) {
            Criteria locationCriteria = Criteria.where("location").is(location);
            criteria = (criteria == null) ? locationCriteria : criteria.and(locationCriteria);
        }
        
        // If no criteria provided, return all candidates (or empty list)
        if (criteria == null) {
            logger.warn("No criteria provided for hybrid search");
            return new ArrayList<>();
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
        
        // Vector search if we have search text - improved logic
        if (searchText != null && !searchText.isEmpty()) {
            results = applyVectorSimilarity(results, searchText);
        }
        
        return results;
    }
    
    // Simple search method for fallback
    public List<Candidate> searchCandidates(String searchText, List<String> skills, String location, Integer limit) {
        Criteria criteria = null;
        
        if (searchText != null && !searchText.trim().isEmpty()) {
            criteria = Criteria.where("cvContent").matches(searchText);
        }
        
        if (skills != null && !skills.isEmpty()) {
            Criteria skillsCriteria = Criteria.where("skills").in(skills);
            criteria = (criteria == null) ? skillsCriteria : criteria.and(skillsCriteria);
        }
        
        if (location != null && !location.trim().isEmpty()) {
            Criteria locationCriteria = Criteria.where("location").is(location);
            criteria = (criteria == null) ? locationCriteria : criteria.and(locationCriteria);
        }
        
        if (criteria == null) {
            // Ako nema kriterija, vraćamo praznu listu ili ograničen broj kandidata
            logger.warn("No search criteria provided in searchCandidates");
            return new ArrayList<>();
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
            // If we have searchText, first do vector search
            if (request.getSearchText() != null && !request.getSearchText().trim().isEmpty()) {
                // Step 1: Vector search in Qdrant
                float[] searchVector = vectorizationService.vectorizeText(request.getSearchText());
                List<Double> searchVectorList = new ArrayList<>();
                for (float f : searchVector) {
                    searchVectorList.add((double) f);
                }
                List<Map<String, Object>> similarCandidates = qdrantService.searchSimilarCandidates(searchVectorList, 50);
                
                if (similarCandidates.isEmpty()) {
                    logger.info("No similar candidates found in vector search");
                    return new ArrayList<>();
                }
                
                // Step 2: Get candidate IDs from vector search
                Set<String> candidateIds = similarCandidates.stream()
                    .map(candidate -> (String) candidate.get("id"))
                    .collect(Collectors.toSet());
                
                // Step 3: Elasticsearch filtering only on candidates from vector search
                Criteria criteria = Criteria.where("id").in(candidateIds);
                
                // Add additional filters
                if (request.getLocation() != null && !request.getLocation().trim().isEmpty()) {
                    criteria = criteria.and(Criteria.where("location").is(request.getLocation()));
                }
                if (request.getMinExperience() != null) {
                    criteria = criteria.and(Criteria.where("yearsOfExperience").greaterThanEqual(request.getMinExperience()));
                }
                if (request.getRequiredSkills() != null && !request.getRequiredSkills().isEmpty()) {
                    criteria = criteria.and(Criteria.where("skills").in(request.getRequiredSkills()));
                }
                if (request.getMaxExpectedSalary() != null) {
                    criteria = criteria.and(Criteria.where("expectedSalary").lessThanEqual(request.getMaxExpectedSalary()));
                }
                
                Query query = new CriteriaQuery(criteria)
                    .setPageable(PageRequest.of(0, request.getMaxResults()));
                
                SearchHits<Candidate> searchHits = elasticsearchOperations.search(query, Candidate.class);
                
                results = searchHits.stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());
                
                // Step 4: Apply scores from vector search
                Map<String, Double> vectorScores = new HashMap<>();
                for (int i = 0; i < similarCandidates.size(); i++) {
                    Map<String, Object> candidate = similarCandidates.get(i);
                    double score = 1.0 - (i * 0.02);
                    vectorScores.put((String) candidate.get("id"), Math.max(score, 0.1));
                }
                
                results.forEach(candidate -> {
                    Double score = vectorScores.get(candidate.getId());
                    candidate.setMatchScore(score != null ? score : 0.0);
                });
                
                // Sort by match score
                results.sort((a, b) -> Double.compare(
                    b.getMatchScore() != null ? b.getMatchScore() : 0.0,
                    a.getMatchScore() != null ? a.getMatchScore() : 0.0
                ));
                
            } else {
                // If no searchText, do only Elasticsearch filtering
                logger.info("No search text provided, falling back to filter-only search");
                return searchCandidates(null, request.getRequiredSkills(), request.getLocation(), request.getMaxResults());
            }
            
        } catch (Exception e) {
            logger.error("Error in hybrid search", e);
            // Fallback to regular Elasticsearch search
            return searchCandidates(request.getSearchText(), request.getRequiredSkills(), 
                                   request.getLocation(), request.getMaxResults());
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
            List<Double> vectorList = new ArrayList<>();
            for (float f : vector) {
                vectorList.add((double) f);
            }
            List<Map<String, Object>> similarCandidates = qdrantService.searchSimilarCandidates(vectorList, results.size());
            
            Map<String, Double> vectorScores = new HashMap<>();
            similarCandidates.forEach(candidate -> {
                vectorScores.put((String) candidate.get("id"), 0.8); // Placeholder score
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
    
    // Helper method for ranking candidates
    private double calculateRankingScore(Candidate candidate, JobPosting job) {
        double score = 0.0;
        double totalWeight = 0.0;
        
        // CV Vector similarity (40% weight) using Qdrant
        try {
            // Get candidate CV vector
            float[] cvVector = vectorizationService.vectorizeText(candidate.getCvContent());
            float[] jobVector = vectorizationService.vectorizeText(job.getDescription());
            
            // Convert to List<Double> for QdrantService
            List<Double> cvVectorList = new ArrayList<>();
            for (float f : cvVector) {
                cvVectorList.add((double) f);
            }
            List<Double> jobVectorList = new ArrayList<>();
            for (float f : jobVector) {
                jobVectorList.add((double) f);
            }
            
            // Search for similar candidates and jobs
            List<Map<String, Object>> candidateResults = qdrantService.searchSimilarCandidates(cvVectorList, 1);
            List<Map<String, Object>> jobResults = qdrantService.searchSimilarJobPostings(jobVectorList, 1);
            
            if (!candidateResults.isEmpty() && !jobResults.isEmpty()) {
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
    private List<Candidate> applyVectorRanking(List<Candidate> candidates, String jobPostingId) {
        try {
            var jobPosting = jobPostingService.findById(jobPostingId);
            if (jobPosting.isEmpty() || jobPosting.get().getDescription() == null) {
                logger.warn("Job posting not found or has no description: {}", jobPostingId);
                return candidates;
            }
            
            float[] jobVector = vectorizationService.vectorizeText(jobPosting.get().getDescription());
            List<Double> jobVectorList = new ArrayList<>();
            for (float f : jobVector) {
                jobVectorList.add((double) f);
            }
            List<Map<String, Object>> similarCandidates = qdrantService.searchSimilarCandidates(jobVectorList, 100);
            
            Map<String, Double> vectorScores = new HashMap<>();
            for (int i = 0; i < similarCandidates.size(); i++) {
                Map<String, Object> candidate = similarCandidates.get(i);
                double score = 1.0 - (i * 0.01);
                vectorScores.put((String) candidate.get("id"), Math.max(score, 0.1));
            }
            
            // Applying scores only to candidates who passed filters
            for (Candidate candidate : candidates) {
                Double score = vectorScores.get(candidate.getId());
                candidate.setMatchScore(score != null ? score : 0.0);
            }
            
            candidates.sort((a, b) -> {
                Double scoreA = a.getMatchScore() != null ? a.getMatchScore() : 0.0;
                Double scoreB = b.getMatchScore() != null ? b.getMatchScore() : 0.0;
                return Double.compare(scoreB, scoreA);
            });
            
            return candidates;
            
        } catch (Exception e) {
            logger.error("Error in vector ranking: {}", e.getMessage(), e);
            return candidates;
        }
    }
    private List<Candidate> applyVectorSimilarity(List<Candidate> candidates, String searchText) {
        try {
            float[] searchVector = vectorizationService.vectorizeText(searchText);
            
            // Searching for top 50 similar candidates from Qdrant
            List<Double> searchVectorList = new ArrayList<>();
            for (float f : searchVector) {
                searchVectorList.add((double) f);
            }
            List<Map<String, Object>> similarCandidates = qdrantService.searchSimilarCandidates(searchVectorList, 50);
            
            // Creating a map with actual scores from Qdrant
            Map<String, Double> vectorScores = new HashMap<>();
            for (int i = 0; i < similarCandidates.size(); i++) {
                Map<String, Object> candidate = similarCandidates.get(i);
                // Score decreases with position (first has 1.0, last has lower score)
                double score = 1.0 - (i * 0.02); // Postepeno smanjenje skora
                vectorScores.put((String) candidate.get("id"), Math.max(score, 0.1));
            }
            
            // Filter only candidates who are in the results
            List<Candidate> scoredCandidates = new ArrayList<>();
            for (Candidate candidate : candidates) {
                if (vectorScores.containsKey(candidate.getId())) {
                    candidate.setMatchScore(vectorScores.get(candidate.getId()));
                    scoredCandidates.add(candidate);
                }
            }
            
            // Sort by score (highest first)
            scoredCandidates.sort((a, b) -> {
                Double scoreA = a.getMatchScore() != null ? a.getMatchScore() : 0.0;
                Double scoreB = b.getMatchScore() != null ? b.getMatchScore() : 0.0;
                return Double.compare(scoreB, scoreA);
            });
            
            return scoredCandidates;
            
        } catch (Exception e) {
            logger.error("Error in vector similarity calculation: {}", e.getMessage(), e);
            return candidates; // Returning original results if vector search fails
        }
    }
}
