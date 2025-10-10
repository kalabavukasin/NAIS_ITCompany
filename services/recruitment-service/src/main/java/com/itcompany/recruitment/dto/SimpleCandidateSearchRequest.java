package com.itcompany.recruitment.dto;

import java.util.List;

public class SimpleCandidateSearchRequest {
    private String searchText;
    private List<String> skills;
    private String location;
    private Integer minExperience;
    private Double maxExpectedSalary;
    private Integer maxResults = 20;

    public SimpleCandidateSearchRequest() {}

    // Getters and setters
    public String getSearchText() { return searchText; }
    public void setSearchText(String searchText) { this.searchText = searchText; }
    
    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public Integer getMinExperience() { return minExperience; }
    public void setMinExperience(Integer minExperience) { this.minExperience = minExperience; }
    
    public Double getMaxExpectedSalary() { return maxExpectedSalary; }
    public void setMaxExpectedSalary(Double maxExpectedSalary) { this.maxExpectedSalary = maxExpectedSalary; }
    
    public Integer getMaxResults() { return maxResults; }
    public void setMaxResults(Integer maxResults) { this.maxResults = maxResults; }
}
