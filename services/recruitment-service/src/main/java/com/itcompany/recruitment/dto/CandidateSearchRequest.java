package com.itcompany.recruitment.dto;

import java.util.List;

public class CandidateSearchRequest {
    private String jobPostingId;
    private String searchText;
    private List<String> requiredSkills;
    private String location;
    private Integer minExperience;
    private Integer maxExperience;
    private Double maxExpectedSalary;
    private Boolean willingToRelocate;
    private Double minMatchScore;
    private Integer maxResults = 20;

    public CandidateSearchRequest() {}

    public String getJobPostingId() { return jobPostingId; }
    public void setJobPostingId(String jobPostingId) { this.jobPostingId = jobPostingId; }
    public String getSearchText() { return searchText; }
    public void setSearchText(String searchText) { this.searchText = searchText; }
    public List<String> getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(List<String> requiredSkills) { this.requiredSkills = requiredSkills; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Integer getMinExperience() { return minExperience; }
    public void setMinExperience(Integer minExperience) { this.minExperience = minExperience; }
    public Integer getMaxExperience() { return maxExperience; }
    public void setMaxExperience(Integer maxExperience) { this.maxExperience = maxExperience; }
    public Double getMaxExpectedSalary() { return maxExpectedSalary; }
    public void setMaxExpectedSalary(Double maxExpectedSalary) { this.maxExpectedSalary = maxExpectedSalary; }
    public Boolean getWillingToRelocate() { return willingToRelocate; }
    public void setWillingToRelocate(Boolean willingToRelocate) { this.willingToRelocate = willingToRelocate; }
    public Double getMinMatchScore() { return minMatchScore; }
    public void setMinMatchScore(Double minMatchScore) { this.minMatchScore = minMatchScore; }
    public Integer getMaxResults() { return maxResults; }
    public void setMaxResults(Integer maxResults) { this.maxResults = maxResults; }
}
