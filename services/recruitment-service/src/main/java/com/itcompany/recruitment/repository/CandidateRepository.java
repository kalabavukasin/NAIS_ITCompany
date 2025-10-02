package com.itcompany.recruitment.repository;

import com.itcompany.recruitment.model.Candidate;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface CandidateRepository extends ElasticsearchRepository<Candidate, String> {

    List<Candidate> findByLocation(String location);

    List<Candidate> findBySkillsContaining(String skill);

    List<Candidate> findByYearsOfExperienceGreaterThanEqual(Integer years);

    List<Candidate> findByExpectedSalaryLessThanEqual(Double salary);

    List<Candidate> findByWillingToRelocate(Boolean willingToRelocate);

    List<Candidate> findByLocationAndYearsOfExperienceGreaterThanEqual(
            String location, Integer years);
}
