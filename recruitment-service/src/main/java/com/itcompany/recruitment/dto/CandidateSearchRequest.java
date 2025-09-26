package com.itcompany.recruitment.dto;

import lombok.Data;
import java.util.List;

@Data
public class CandidateSearchRequest {
    private String jobPostingId;
    private String searchText; //  za semanticku pretragu
    private List<String> requiredSkills;
    private String location;
    private Integer minExperience;
    private Integer maxExperience;
    private Double maxExpectedSalary;
    private Boolean willingToRelocate;
    private Double minMatchScore; // Minimalni skor slicnosti (0-1)
    private Integer maxResults = 20;
}
