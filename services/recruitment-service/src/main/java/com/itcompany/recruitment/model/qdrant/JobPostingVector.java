package com.itcompany.recruitment.model.qdrant;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JobPostingVector {
    
    @JsonProperty("job_posting_id")
    private String jobPostingId;
    
    @JsonProperty("description_vector")
    private float[] descriptionVector;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("required_skills")
    private String requiredSkills; // JSON string for easier storage in Qdrant
    
    @JsonProperty("preferred_skills")
    private String preferredSkills; // JSON string for easier storage in Qdrant
    
    public JobPostingVector() {}
    
    public JobPostingVector(String jobPostingId, float[] descriptionVector, 
                           String description, String title, String requiredSkills, String preferredSkills) {
        this.jobPostingId = jobPostingId;
        this.descriptionVector = descriptionVector;
        this.description = description;
        this.title = title;
        this.requiredSkills = requiredSkills;
        this.preferredSkills = preferredSkills;
    }

    // Getters and setters
    public String getJobPostingId() {
        return jobPostingId;
    }

    public void setJobPostingId(String jobPostingId) {
        this.jobPostingId = jobPostingId;
    }

    public float[] getDescriptionVector() {
        return descriptionVector;
    }

    public void setDescriptionVector(float[] descriptionVector) {
        this.descriptionVector = descriptionVector;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(String requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public String getPreferredSkills() {
        return preferredSkills;
    }

    public void setPreferredSkills(String preferredSkills) {
        this.preferredSkills = preferredSkills;
    }
}
