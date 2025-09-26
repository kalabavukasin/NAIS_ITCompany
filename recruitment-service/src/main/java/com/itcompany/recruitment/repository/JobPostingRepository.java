package com.itcompany.recruitment.repository;

import com.itcompany.recruitment.model.JobPosting;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JobPostingRepository extends ElasticsearchRepository<JobPosting, String> {
    List<JobPosting> findByIsActiveTrue();

    List<JobPosting> findByDepartment(String department);

    List<JobPosting> findByLocation(String location);

    List<JobPosting> findByExperienceLevel(String experienceLevel);

    List<JobPosting> findByRequiredSkillsContaining(String skill);

    List<JobPosting> findByPostedDateAfter(LocalDateTime date);

    List<JobPosting> findByMinSalaryGreaterThanEqualAndMaxSalaryLessThanEqual(
            Double minSalary, Double maxSalary);

}
