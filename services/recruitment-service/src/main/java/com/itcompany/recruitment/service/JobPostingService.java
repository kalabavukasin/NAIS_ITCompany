package com.itcompany.recruitment.service;

import com.itcompany.recruitment.dto.JobSearchRequest;
import com.itcompany.recruitment.model.JobPosting;
import com.itcompany.recruitment.repository.JobPostingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JobPostingService {

    private static final Logger logger = LoggerFactory.getLogger(JobPostingService.class);

    private final JobPostingRepository jobPostingRepository;
    private final VectorizationService vectorizationService;
    private final ElasticsearchOperations elasticsearchOperations;
    private final QdrantService qdrantService;
    private final TransactionalJobPostingService transactionalJobPostingService;

    public JobPostingService(JobPostingRepository jobPostingRepository,
                             VectorizationService vectorizationService,
                             ElasticsearchOperations elasticsearchOperations,
                             QdrantService qdrantService,
                             TransactionalJobPostingService transactionalJobPostingService) {
        this.jobPostingRepository = jobPostingRepository;
        this.vectorizationService = vectorizationService;
        this.elasticsearchOperations = elasticsearchOperations;
        this.qdrantService = qdrantService;
        this.transactionalJobPostingService = transactionalJobPostingService;
    }

    // CRUD
    public JobPosting createJobPosting(JobPosting jobPosting) {
        // Use transactional service for CRUD operations
        return transactionalJobPostingService.createJobPosting(jobPosting);
    }

    public JobPosting updateJobPosting(String id, JobPosting jobPosting) {
        // Use transactional service for CRUD operations
        return transactionalJobPostingService.updateJobPosting(id, jobPosting);
    }

    public Optional<JobPosting> findById(String id) {
        return jobPostingRepository.findById(id);
    }
    
    public void deleteJobPosting(String id) {
        // Use transactional service for CRUD operations
        transactionalJobPostingService.deleteJobPosting(id);
    }

    public List<JobPosting> findAll() {
        List<JobPosting> results = new ArrayList<>();
        jobPostingRepository.findAll().forEach(results::add);
        return results;
    }

    public List<JobPosting> findActivePostings() {
        return jobPostingRepository.findByIsActiveTrue();
    }

    public List<JobPosting> findByDepartment(String department) {
        return jobPostingRepository.findByDepartment(department);
    }

    // Simple search with basic filtering
    public List<JobPosting> simpleSearchJobs(JobSearchRequest request) {
        // Start with an empty criteria
        Criteria criteria = null;
        boolean hasFilters = false;
        
        // Add filters based on provided criteria
        if (request.getSkills() != null && !request.getSkills().isEmpty()) {
            Criteria skillsCriteria = Criteria.where("requiredSkills").in(request.getSkills());
            criteria = (criteria == null) ? skillsCriteria : criteria.and(skillsCriteria);
            hasFilters = true;
        }
        
        if (request.getLocation() != null && !request.getLocation().trim().isEmpty()) {
            Criteria locationCriteria = Criteria.where("location").is(request.getLocation());
            criteria = (criteria == null) ? locationCriteria : criteria.and(locationCriteria);
            hasFilters = true;
        }
        
        if (request.getExperienceLevel() != null && !request.getExperienceLevel().trim().isEmpty()) {
            Criteria expLevelCriteria = Criteria.where("experienceLevel").is(request.getExperienceLevel());
            criteria = (criteria == null) ? expLevelCriteria : criteria.and(expLevelCriteria);
            hasFilters = true;
        }
        
        if (request.getEmploymentType() != null && !request.getEmploymentType().trim().isEmpty()) {
            Criteria empTypeCriteria = Criteria.where("employmentType").is(request.getEmploymentType());
            criteria = (criteria == null) ? empTypeCriteria : criteria.and(empTypeCriteria);
            hasFilters = true;
        }
        
        if (request.getMinSalary() != null) {
            Criteria salaryCriteria = Criteria.where("minSalary").greaterThanEqual(request.getMinSalary());
            criteria = (criteria == null) ? salaryCriteria : criteria.and(salaryCriteria);
            hasFilters = true;
        }
        
        if (request.getMinMatchScore() != null) {
            // This would require a custom field or calculation
            logger.warn("MinMatchScore filtering not implemented for job postings");
        }
        
        // Always filter for active job postings
        Criteria activeCriteria = Criteria.where("isActive").is(true);
        criteria = (criteria == null) ? activeCriteria : criteria.and(activeCriteria);
        hasFilters = true;
        
        // If no other filters provided, still search for active jobs
        if (!hasFilters) {
            logger.warn("No search criteria provided, returning active job postings only");
        }
        
        Query query = new CriteriaQuery(criteria)
            .setPageable(PageRequest.of(0, Optional.ofNullable(request.getMaxResults()).orElse(20)));
        
        logger.info("Searching job postings with criteria: {}", criteria.toString());
        
        try {
            SearchHits<JobPosting> searchHits = elasticsearchOperations.search(
                query, 
                JobPosting.class
            );
            
            List<JobPosting> results = searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
                
            logger.info("Found {} job postings matching criteria", results.size());
            
            return results;
        } catch (Exception e) {
            logger.error("Error searching job postings: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    // Hybrid search combining vector search with filtering
    public List<JobPosting> hybridSearchJobs(JobSearchRequest request) {
        List<JobPosting> results = new ArrayList<>();
        try {
            // Step 1: Use Qdrant for vector similarity search if candidate CV text is provided
            if (request.getCandidateCvText() != null && !request.getCandidateCvText().trim().isEmpty()) {
                float[] searchVector = vectorizationService.vectorizeText(request.getCandidateCvText());
                List<Double> searchVectorList = new ArrayList<>();
                for (float f : searchVector) {
                    searchVectorList.add((double) f);
                }
                List<Map<String, Object>> similarJobPostings = qdrantService.searchSimilarJobPostings(searchVectorList, 50);
                
                // Step 2: Get job posting IDs from vector search results
                Set<String> jobPostingIds = similarJobPostings.stream()
                    .map(jobPosting -> (String) jobPosting.get("id"))
                    .collect(Collectors.toSet());
                
                // Step 3: Use Elasticsearch for structured filtering on similar job postings
                if (!jobPostingIds.isEmpty()) {
                    Criteria criteria = new Criteria("id").in(jobPostingIds);
                    
                    // Add additional filters
                    if (request.getLocation() != null) {
                        criteria = criteria.and(new Criteria("location").is(request.getLocation()));
                    }
                    if (request.getExperienceLevel() != null) {
                        criteria = criteria.and(new Criteria("experienceLevel").is(request.getExperienceLevel()));
                    }
                    if (request.getEmploymentType() != null) {
                        criteria = criteria.and(new Criteria("employmentType").is(request.getEmploymentType()));
                    }
                    if (request.getMinSalary() != null) {
                        criteria = criteria.and(new Criteria("minSalary").greaterThanEqual(request.getMinSalary()));
                    }
                    
                    // Always filter for active jobs
                    criteria = criteria.and(new Criteria("isActive").is(true));
                    
                    Query query = new CriteriaQuery(criteria)
                        .setPageable(PageRequest.of(0, Optional.ofNullable(request.getMaxResults()).orElse(20)));
                    
                    SearchHits<JobPosting> searchHits = elasticsearchOperations.search(query, JobPosting.class);
                    
                    results = searchHits.stream()
                        .map(SearchHit::getContent)
                        .collect(Collectors.toList());
                    
                    // Step 4: Recalculate match scores from Qdrant results
                    Map<String, Double> vectorScores = new HashMap<>();
                    similarJobPostings.forEach(jobPosting -> {
                        // Calculate similarity score (simplified)
                        vectorScores.put((String) jobPosting.get("id"), 0.8); // Placeholder score
                    });
                    
                    results.forEach(jobPosting -> {
                        Double score = vectorScores.get(jobPosting.getId());
                        if (score != null) {
                            // jobPosting.setMatchScore(score);
                        }
                    });
                    
                    // Sort by match score
                    results.sort((a, b) -> {
                        Double scoreA = vectorScores.getOrDefault(a.getId(), 0.0);
                        Double scoreB = vectorScores.getOrDefault(b.getId(), 0.0);
                        return Double.compare(scoreB, scoreA);
                    });
                }
            } else {
                // Fallback to simple search if no candidate CV text provided
                return simpleSearchJobs(request);
            }
            
        } catch (Exception e) {
            logger.error("Error in hybrid search for job postings", e);
            // Fallback to simple search
            return simpleSearchJobs(request);
        }
        return results;
    }
    
    // Legacy method for backward compatibility
    public List<JobPosting> searchJobs(JobSearchRequest request) {
        return hybridSearchJobs(request);
    }
    
    // Search jobs by department with filtering
    public List<JobPosting> searchJobsByDepartment(String department, JobSearchRequest request) {
        // Create a new request with department filter
        JobSearchRequest departmentRequest = new JobSearchRequest();
        departmentRequest.setSkills(request.getSkills());
        departmentRequest.setLocation(request.getLocation());
        departmentRequest.setExperienceLevel(request.getExperienceLevel());
        departmentRequest.setEmploymentType(request.getEmploymentType());
        departmentRequest.setMinSalary(request.getMinSalary());
        departmentRequest.setCandidateCvText(request.getCandidateCvText());
        departmentRequest.setMaxResults(request.getMaxResults());
        
        // Add department filter
        Criteria criteria = new Criteria("department").is(department);
        criteria = criteria.and(new Criteria("isActive").is(true));
        
        Query query = new CriteriaQuery(criteria)
            .setPageable(PageRequest.of(0, Optional.ofNullable(request.getMaxResults()).orElse(20)));
        
        try {
            SearchHits<JobPosting> searchHits = elasticsearchOperations.search(query, JobPosting.class);
            return searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error searching jobs by department: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    // Search jobs by location with filtering
    public List<JobPosting> searchJobsByLocation(String location, JobSearchRequest request) {
        request.setLocation(location);
        return simpleSearchJobs(request);
    }
    
    // Search jobs by experience level with filtering
    public List<JobPosting> searchJobsByExperienceLevel(String experienceLevel, JobSearchRequest request) {
        request.setExperienceLevel(experienceLevel);
        return simpleSearchJobs(request);
    }
    
}


