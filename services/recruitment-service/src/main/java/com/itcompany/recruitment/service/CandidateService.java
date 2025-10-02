package com.itcompany.recruitment.service;

import com.itcompany.recruitment.model.Candidate;
import com.itcompany.recruitment.model.JobPosting;
import com.itcompany.recruitment.dto.CandidateSearchRequest;
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
    public CandidateService(CandidateRepository candidateRepository,
                            VectorizationService vectorizationService,
                            ElasticsearchOperations elasticsearchOperations,
                            JobPostingService jobPostingService) {
        this.candidateRepository = candidateRepository;
        this.vectorizationService = vectorizationService;
        this.elasticsearchOperations = elasticsearchOperations;
        this.jobPostingService = jobPostingService;
    }
    
    // CRUD Operations
    public Candidate createCandidate(Candidate candidate) {
        // Vektorizuj CV i veštine pre čuvanja
        if (candidate.getCvContent() != null) {
            candidate.setCvVector(vectorizationService.vectorizeText(candidate.getCvContent()));
        }
        if (candidate.getSkills() != null) {
            candidate.setSkillsVector(vectorizationService.vectorizeSkills(candidate.getSkills()));
        }
        candidate.setRegistrationDate(LocalDateTime.now());
        
        return candidateRepository.save(candidate);
    }
    
    public Candidate updateCandidate(String id, Candidate candidate) {
        candidate.setId(id);
        // Re-vektorizuj ako su promenjeni CV ili veštine
        if (candidate.getCvContent() != null) {
            candidate.setCvVector(vectorizationService.vectorizeText(candidate.getCvContent()));
        }
        if (candidate.getSkills() != null) {
            candidate.setSkillsVector(vectorizationService.vectorizeSkills(candidate.getSkills()));
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
    
    // SLOŽENI UPIT 1: Vektorska pretraga kombinovana sa filtriranjem (2+ uslova)
    public List<Candidate> searchCandidatesWithVectorAndFilters(CandidateSearchRequest request) {
        Criteria criteria = new Criteria();
        
        // Dodaj filtere (minimum 2 uslova kao što zahteva specifikacija)
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
        
        // Vektorska pretraga ako imamo job posting
        if (request.getJobPostingId() != null) {
            var jobPosting = jobPostingService.findById(request.getJobPostingId());
            if (jobPosting.isPresent() && jobPosting.get().getDescriptionVector() != null) {
                // Sortiraj po vektorskoj sličnosti
                results.sort((a, b) -> {
                    double scoreA = calculateVectorSimilarity(a.getCvVector(), jobPosting.get().getDescriptionVector());
                    double scoreB = calculateVectorSimilarity(b.getCvVector(), jobPosting.get().getDescriptionVector());
                    return Double.compare(scoreB, scoreA);
                });
            }
        }
        
        return results;
    }
    
    // SLOŽENI UPIT 2: Hibridna pretraga - kombinuje vektorsku i tekstualnu pretragu
    public List<Candidate> hybridSearch(String searchText, List<String> skills, String location) {
        Criteria criteria = new Criteria();
        
        // Tekstualna pretraga - koristi match query umesto contains
        if (searchText != null && !searchText.isEmpty()) {
            criteria.and("cvContent").matches(searchText);
        }
        
        // Filteri
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
        
        // Vektorska pretraga ako imamo search text
        if (searchText != null && !searchText.isEmpty()) {
            float[] searchVector = vectorizationService.vectorizeText(searchText);
            results.sort((a, b) -> {
                double scoreA = calculateVectorSimilarity(a.getCvVector(), searchVector);
                double scoreB = calculateVectorSimilarity(b.getCvVector(), searchVector);
                return Double.compare(scoreB, scoreA);
            });
        }
        
        return results;
    }
    
    // SLOŽENI UPIT 3: Vektorska pretraga sa iteratorom za velike rezultate
    public List<Candidate> searchWithIterator(String jobDescription, int batchSize) {
        float[] jobVector = vectorizationService.vectorizeText(jobDescription);
        List<Candidate> allResults = new ArrayList<>();
        
        Query query = new CriteriaQuery(new Criteria())
            .setPageable(PageRequest.of(0, 1000)); // Limit za sigurnost
        
        SearchHits<Candidate> searchHits = elasticsearchOperations.search(query, Candidate.class);
        
        searchHits.forEach(hit -> {
            Candidate candidate = hit.getContent();
            double similarity = calculateVectorSimilarity(candidate.getCvVector(), jobVector);
            candidate.setMatchScore(similarity);
            allResults.add(candidate);
        });
        
        // Sortiraj po sličnosti
        allResults.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));
        
        return allResults;
    }
    
    // PROSTI UPIT 1: Prebrojavanje po lokaciji
    public long countByLocation(String location) {
        return candidateRepository.findByLocation(location).size();
    }
    
    // PROSTI UPIT 2: Single vector search
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
        
        // Sortiraj po vektorskoj sličnosti
        results.sort((a, b) -> {
            double scoreA = calculateVectorSimilarity(a.getCvVector(), vector);
            double scoreB = calculateVectorSimilarity(b.getCvVector(), vector);
            return Double.compare(scoreB, scoreA);
        });
        
        return results;
    }
    
    // Dodatna funkcija za rangiranje kandidata
    public List<Candidate> rankCandidates(String jobPostingId) {
        try {
            var jobPosting = jobPostingService.findById(jobPostingId);
            if (jobPosting.isEmpty()) {
                logger.warn("Job posting with ID {} not found", jobPostingId);
                return new ArrayList<>();
            }
            
            var job = jobPosting.get();
            logger.info("Ranking candidates for job: {}", job.getTitle());
            
            // Uzmi samo prvih 20 kandidata za testiranje
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
            
            // Sortiraj po match score
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
    
    // Helper metoda za vektorsku sličnost
    private double calculateVectorSimilarity(float[] vec1, float[] vec2) {
        if (vec1 == null || vec2 == null) {
            return 0.0;
        }
        return vectorizationService.calculateCosineSimilarity(vec1, vec2);
    }
    
    // Helper metoda za rangiranje kandidata
    private double calculateRankingScore(Candidate candidate, JobPosting job) {
        double score = 0.0;
        double totalWeight = 0.0;
        
        // CV Vector similarity (40% weight)
        if (candidate.getCvVector() != null && job.getDescriptionVector() != null) {
            double cvScore = calculateVectorSimilarity(candidate.getCvVector(), job.getDescriptionVector());
            score += cvScore * 0.4;
            totalWeight += 0.4;
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
