package com.itcompany.recruitment.dto;

import java.util.List;

public class JobSearchRequest {
    private String candidateCvText;
    private List<String> skills;
    private String location;
    private String experienceLevel;
    private String employmentType;
    private Double minSalary;
    private Double minMatchScore;
    private Integer maxResults = 20;

    public JobSearchRequest() {}

    public String getCandidateCvText() { return candidateCvText; }
    public void setCandidateCvText(String candidateCvText) { this.candidateCvText = candidateCvText; }
    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(String experienceLevel) { this.experienceLevel = experienceLevel; }
    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }
    public Double getMinSalary() { return minSalary; }
    public void setMinSalary(Double minSalary) { this.minSalary = minSalary; }
    public Double getMinMatchScore() { return minMatchScore; }
    public void setMinMatchScore(Double minMatchScore) { this.minMatchScore = minMatchScore; }
    public Integer getMaxResults() { return maxResults; }
    public void setMaxResults(Integer maxResults) { this.maxResults = maxResults; }
}
