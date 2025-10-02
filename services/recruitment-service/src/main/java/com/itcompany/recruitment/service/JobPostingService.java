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

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JobPostingService {

    private static final Logger logger = LoggerFactory.getLogger(JobPostingService.class);

    private final JobPostingRepository jobPostingRepository;
    private final VectorizationService vectorizationService;
    private final ElasticsearchOperations elasticsearchOperations;

    public JobPostingService(JobPostingRepository jobPostingRepository,
                             VectorizationService vectorizationService,
                             ElasticsearchOperations elasticsearchOperations) {
        this.jobPostingRepository = jobPostingRepository;
        this.vectorizationService = vectorizationService;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    // CRUD
    public JobPosting createJobPosting(JobPosting jobPosting) {
        if (jobPosting.getDescription() != null) {
            jobPosting.setDescriptionVector(
                vectorizationService.vectorizeText(jobPosting.getDescription())
            );
        }
        jobPosting.setPostedDate(LocalDateTime.now());
        if (jobPosting.getIsActive() == null) {
            jobPosting.setIsActive(true);
        }
        return jobPostingRepository.save(jobPosting);
    }

    public JobPosting updateJobPosting(String id, JobPosting jobPosting) {
        jobPosting.setId(id);
        if (jobPosting.getDescription() != null) {
            jobPosting.setDescriptionVector(
                vectorizationService.vectorizeText(jobPosting.getDescription())
            );
        }
        return jobPostingRepository.save(jobPosting);
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

        // Vector search by candidate CV text against job description vectors
        if (request.getCandidateCvText() != null && !request.getCandidateCvText().isEmpty()) {
            float[] vector = vectorizationService.vectorizeText(request.getCandidateCvText());
            results.sort((a, b) -> {
                double scoreA = calculateVectorSimilarity(a.getDescriptionVector(), vector);
                double scoreB = calculateVectorSimilarity(b.getDescriptionVector(), vector);
                return Double.compare(scoreB, scoreA);
            });
        }

        return results;
    }
    
    // Helper metoda za vektorsku sličnost
    private double calculateVectorSimilarity(float[] vec1, float[] vec2) {
        if (vec1 == null || vec2 == null) {
            return 0.0;
        }
        return vectorizationService.calculateCosineSimilarity(vec1, vec2);
    }
}


