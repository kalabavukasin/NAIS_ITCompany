package com.itcompany.recruitment.service;

import com.itcompany.recruitment.dto.JobSearchRequest;
import com.itcompany.recruitment.model.JobPosting;
import com.itcompany.recruitment.model.qdrant.JobPostingVector;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JobPostingService {

    private static final Logger logger = LoggerFactory.getLogger(JobPostingService.class);

    private final JobPostingRepository jobPostingRepository;
    private final VectorizationService vectorizationService;
    private final ElasticsearchOperations elasticsearchOperations;
    private final QdrantService qdrantService;
    private final ObjectMapper objectMapper;

    public JobPostingService(JobPostingRepository jobPostingRepository,
                             VectorizationService vectorizationService,
                             ElasticsearchOperations elasticsearchOperations,
                             QdrantService qdrantService) {
        this.jobPostingRepository = jobPostingRepository;
        this.vectorizationService = vectorizationService;
        this.elasticsearchOperations = elasticsearchOperations;
        this.qdrantService = qdrantService;
        this.objectMapper = new ObjectMapper();
    }

    // CRUD
    public JobPosting createJobPosting(JobPosting jobPosting) {
        jobPosting.setPostedDate(LocalDateTime.now());
        if (jobPosting.getIsActive() == null) {
            jobPosting.setIsActive(true);
        }
        
        // Save to Elasticsearch (without vectors)
        JobPosting savedJobPosting = jobPostingRepository.save(jobPosting);
        
        // Create and store vectors in Qdrant
        try {
            float[] descriptionVector = jobPosting.getDescription() != null ? 
                vectorizationService.vectorizeText(jobPosting.getDescription()) : new float[384];
            
            String requiredSkillsJson = jobPosting.getRequiredSkills() != null ? 
                objectMapper.writeValueAsString(jobPosting.getRequiredSkills()) : "[]";
            String preferredSkillsJson = jobPosting.getPreferredSkills() != null ? 
                objectMapper.writeValueAsString(jobPosting.getPreferredSkills()) : "[]";
            
            JobPostingVector jobPostingVector = new JobPostingVector(
                savedJobPosting.getId(),
                descriptionVector,
                jobPosting.getDescription(),
                jobPosting.getTitle(),
                requiredSkillsJson,
                preferredSkillsJson
            );
            
            qdrantService.storeJobPostingVector(jobPostingVector);
            logger.info("Successfully stored job posting vectors for ID: {}", savedJobPosting.getId());
            
        } catch (JsonProcessingException e) {
            logger.error("Error serializing skills for job posting: {}", savedJobPosting.getId(), e);
        } catch (Exception e) {
            logger.error("Error storing job posting vectors for ID: {}", savedJobPosting.getId(), e);
        }
        
        return savedJobPosting;
    }

    public JobPosting updateJobPosting(String id, JobPosting jobPosting) {
        jobPosting.setId(id);
        
        // Update in Elasticsearch (without vectors)
        JobPosting updatedJobPosting = jobPostingRepository.save(jobPosting);
        
        // Update vectors in Qdrant if description changed
        try {
            float[] descriptionVector = jobPosting.getDescription() != null ? 
                vectorizationService.vectorizeText(jobPosting.getDescription()) : new float[384];
            
            String requiredSkillsJson = jobPosting.getRequiredSkills() != null ? 
                objectMapper.writeValueAsString(jobPosting.getRequiredSkills()) : "[]";
            String preferredSkillsJson = jobPosting.getPreferredSkills() != null ? 
                objectMapper.writeValueAsString(jobPosting.getPreferredSkills()) : "[]";
            
            JobPostingVector jobPostingVector = new JobPostingVector(
                updatedJobPosting.getId(),
                descriptionVector,
                jobPosting.getDescription(),
                jobPosting.getTitle(),
                requiredSkillsJson,
                preferredSkillsJson
            );
            
            qdrantService.storeJobPostingVector(jobPostingVector);
            logger.info("Successfully updated job posting vectors for ID: {}", updatedJobPosting.getId());
            
        } catch (Exception e) {
            logger.error("Error updating job posting vectors for ID: {}", updatedJobPosting.getId(), e);
        }
        
        return updatedJobPosting;
    }

    public Optional<JobPosting> findById(String id) {
        return jobPostingRepository.findById(id);
    }

    public void deleteJobPosting(String id) {
        jobPostingRepository.deleteById(id);
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

    // Search jobs for a candidate profile/text (hybrid vector + filters)
    public List<JobPosting> searchJobs(JobSearchRequest request) {
        Criteria criteria = new Criteria();

        if (request.getSkills() != null && !request.getSkills().isEmpty()) {
            criteria.and("requiredSkills").in(request.getSkills());
        }
        if (request.getLocation() != null) {
            criteria.and("location").is(request.getLocation());
        }
        if (request.getExperienceLevel() != null) {
            criteria.and("experienceLevel").is(request.getExperienceLevel());
        }
        if (request.getEmploymentType() != null) {
            criteria.and("employmentType").is(request.getEmploymentType());
        }
        if (request.getMinSalary() != null) {
            criteria.and("minSalary").greaterThanEqual(request.getMinSalary());
        }

        Query query = new CriteriaQuery(criteria)
            .setPageable(PageRequest.of(0, Optional.ofNullable(request.getMaxResults()).orElse(20)));

        SearchHits<JobPosting> hits = elasticsearchOperations.search(query, JobPosting.class);
        List<JobPosting> results = hits.stream().map(SearchHit::getContent).collect(Collectors.toList());

        // Vector search by candidate CV text against job description vectors using Qdrant
        if (request.getCandidateCvText() != null && !request.getCandidateCvText().isEmpty()) {
            try {
                float[] searchVector = vectorizationService.vectorizeText(request.getCandidateCvText());
                List<JobPostingVector> similarVectors = qdrantService.searchSimilarJobPostings(searchVector, 50);
                
                // Create a map of job posting IDs to their vector similarity scores
                Map<String, Double> vectorScores = new HashMap<>();
                similarVectors.forEach(jpv -> {
                    // Use a placeholder score - in real implementation, Qdrant returns similarity scores
                    vectorScores.put(jpv.getJobPostingId(), 0.8);
                });
                
                // Sort results by vector similarity
                results.sort((a, b) -> {
                    Double scoreA = vectorScores.getOrDefault(a.getId(), 0.0);
                    Double scoreB = vectorScores.getOrDefault(b.getId(), 0.0);
                    return Double.compare(scoreB, scoreA);
                });
                
            } catch (Exception e) {
                logger.error("Error in vector search for job postings", e);
            }
        }

        return results;
    }
    
    // Helper method for vector similarity (now using Qdrant)
    private double calculateVectorSimilarity(float[] vec1, float[] vec2) {
        if (vec1 == null || vec2 == null) {
            return 0.0;
        }
        return vectorizationService.calculateCosineSimilarity(vec1, vec2);
    }
}


