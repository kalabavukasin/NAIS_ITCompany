package com.itcompany.recruitment.repository;

import com.itcompany.recruitment.model.Application;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ApplicationRepository extends ElasticsearchRepository<Application, String> {
    List<Application> findByCandidateId(String candidateId);

    List<Application> findByJobPostingId(String jobPostingId);

    List<Application> findByStatus(String status);

    List<Application> findByJobPostingIdAndStatus(String jobPostingId, String status);

    List<Application> findByIsShortlistedTrue();

    Application findByCandidateIdAndJobPostingId(String candidateId, String jobPostingId);

    Long countByJobPostingId(String jobPostingId);

    Long countByJobPostingIdAndStatus(String jobPostingId, String status);
}
