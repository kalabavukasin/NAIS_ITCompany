package com.itcompany.recruitment.model.qdrant;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CandidateVector {
    
    @JsonProperty("candidate_id")
    private String candidateId;
    
    @JsonProperty("cv_vector")
    private float[] cvVector;
    
    @JsonProperty("skills_vector")
    private float[] skillsVector;
    
    @JsonProperty("cv_content")
    private String cvContent;
    
    @JsonProperty("skills")
    private String skills; // JSON string for easier storage in Qdrant
    
    public CandidateVector() {}
    
    public CandidateVector(String candidateId, float[] cvVector, float[] skillsVector, 
                          String cvContent, String skills) {
        this.candidateId = candidateId;
        this.cvVector = cvVector;
        this.skillsVector = skillsVector;
        this.cvContent = cvContent;
        this.skills = skills;
    }

    // Getters and setters
    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public float[] getCvVector() {
        return cvVector;
    }

    public void setCvVector(float[] cvVector) {
        this.cvVector = cvVector;
    }

    public float[] getSkillsVector() {
        return skillsVector;
    }

    public void setSkillsVector(float[] skillsVector) {
        this.skillsVector = skillsVector;
    }

    public String getCvContent() {
        return cvContent;
    }

    public void setCvContent(String cvContent) {
        this.cvContent = cvContent;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }
}
