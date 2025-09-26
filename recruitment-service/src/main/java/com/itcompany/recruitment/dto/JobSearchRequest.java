package com.itcompany.recruitment.dto;

import lombok.Data;

import java.util.List;

@Data
public class JobSearchRequest {
    private String candidateCvText; // CV za vektorsku pretragu
    private List<String> skills;
    private String location;
    private String experienceLevel;
    private String employmentType;
    private Double minSalary;
    private Double minMatchScore;
    private Integer maxResults = 20;
}
