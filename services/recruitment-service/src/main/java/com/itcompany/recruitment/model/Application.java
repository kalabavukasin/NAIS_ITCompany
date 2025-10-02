package com.itcompany.recruitment.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import java.time.LocalDateTime;

@Document(indexName = "applications")
public class Application {
    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String candidateId;

    @Field(type = FieldType.Keyword)
    private String jobPostingId;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime applicationDate;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Text)
    private String coverLetter;

    @Field(type = FieldType.Dense_Vector, dims = 768)
    private float[] coverLetterVector;

    @Field(type = FieldType.Double)
    private Double overallMatchScore;

    @Field(type = FieldType.Double)
    private Double skillMatchScore;

    @Field(type = FieldType.Double)
    private Double experienceMatchScore;

    @Field(type = FieldType.Double)
    private Double cvMatchScore;

    @Field(type = FieldType.Integer)
    private Integer ranking;

    @Field(type = FieldType.Text)
    private String hrNotes;

    @Field(type = FieldType.Keyword)
    private String reviewedBy;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime reviewDate;

    @Field(type = FieldType.Boolean)
    private Boolean isShortlisted;

    public Application() {}

    public Application(String id, String candidateId, String jobPostingId, LocalDateTime applicationDate,
                       String status, String coverLetter, float[] coverLetterVector, Double overallMatchScore,
                       Double skillMatchScore, Double experienceMatchScore, Double cvMatchScore, Integer ranking,
                       String hrNotes, String reviewedBy, LocalDateTime reviewDate, Boolean isShortlisted) {
        this.id = id;
        this.candidateId = candidateId;
        this.jobPostingId = jobPostingId;
        this.applicationDate = applicationDate;
        this.status = status;
        this.coverLetter = coverLetter;
        this.coverLetterVector = coverLetterVector;
        this.overallMatchScore = overallMatchScore;
        this.skillMatchScore = skillMatchScore;
        this.experienceMatchScore = experienceMatchScore;
        this.cvMatchScore = cvMatchScore;
        this.ranking = ranking;
        this.hrNotes = hrNotes;
        this.reviewedBy = reviewedBy;
        this.reviewDate = reviewDate;
        this.isShortlisted = isShortlisted;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCandidateId() { return candidateId; }
    public void setCandidateId(String candidateId) { this.candidateId = candidateId; }
    public String getJobPostingId() { return jobPostingId; }
    public void setJobPostingId(String jobPostingId) { this.jobPostingId = jobPostingId; }
    public LocalDateTime getApplicationDate() { return applicationDate; }
    public void setApplicationDate(LocalDateTime applicationDate) { this.applicationDate = applicationDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCoverLetter() { return coverLetter; }
    public void setCoverLetter(String coverLetter) { this.coverLetter = coverLetter; }
    public float[] getCoverLetterVector() { return coverLetterVector; }
    public void setCoverLetterVector(float[] coverLetterVector) { this.coverLetterVector = coverLetterVector; }
    public Double getOverallMatchScore() { return overallMatchScore; }
    public void setOverallMatchScore(Double overallMatchScore) { this.overallMatchScore = overallMatchScore; }
    public Double getSkillMatchScore() { return skillMatchScore; }
    public void setSkillMatchScore(Double skillMatchScore) { this.skillMatchScore = skillMatchScore; }
    public Double getExperienceMatchScore() { return experienceMatchScore; }
    public void setExperienceMatchScore(Double experienceMatchScore) { this.experienceMatchScore = experienceMatchScore; }
    public Double getCvMatchScore() { return cvMatchScore; }
    public void setCvMatchScore(Double cvMatchScore) { this.cvMatchScore = cvMatchScore; }
    public Integer getRanking() { return ranking; }
    public void setRanking(Integer ranking) { this.ranking = ranking; }
    public String getHrNotes() { return hrNotes; }
    public void setHrNotes(String hrNotes) { this.hrNotes = hrNotes; }
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public LocalDateTime getReviewDate() { return reviewDate; }
    public void setReviewDate(LocalDateTime reviewDate) { this.reviewDate = reviewDate; }
    public Boolean getIsShortlisted() { return isShortlisted; }
    public void setIsShortlisted(Boolean isShortlisted) { this.isShortlisted = isShortlisted; }
}
