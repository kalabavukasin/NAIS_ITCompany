package com.itcompany.recruitment.model.qdrant;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApplicationVector {
    
    @JsonProperty("application_id")
    private String applicationId;
    
    @JsonProperty("cover_letter_vector")
    private float[] coverLetterVector;
    
    @JsonProperty("cover_letter")
    private String coverLetter;
    
    @JsonProperty("candidate_id")
    private String candidateId;
    
    @JsonProperty("job_posting_id")
    private String jobPostingId;
    
    public ApplicationVector() {}
    
    public ApplicationVector(String applicationId, float[] coverLetterVector, 
                            String coverLetter, String candidateId, String jobPostingId) {
        this.applicationId = applicationId;
        this.coverLetterVector = coverLetterVector;
        this.coverLetter = coverLetter;
        this.candidateId = candidateId;
        this.jobPostingId = jobPostingId;
    }

    // Getters and setters
    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public float[] getCoverLetterVector() {
        return coverLetterVector;
    }

    public void setCoverLetterVector(float[] coverLetterVector) {
        this.coverLetterVector = coverLetterVector;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public String getJobPostingId() {
        return jobPostingId;
    }

    public void setJobPostingId(String jobPostingId) {
        this.jobPostingId = jobPostingId;
    }
}
