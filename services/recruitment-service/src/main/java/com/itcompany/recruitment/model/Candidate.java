package com.itcompany.recruitment.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Document(indexName = "candidates")
@Setting(settingPath = "elasticsearch-settings.json")
public class Candidate {
    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String firstName;

    @Field(type = FieldType.Text)
    private String lastName;

    @Field(type = FieldType.Keyword)
    private String email;

    @Field(type = FieldType.Text)
    private String phone;

    @Field(type = FieldType.Keyword)
    private String location;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String cvContent;

    @Field(type = FieldType.Keyword)
    private List<String> skills;

    @Field(type = FieldType.Integer)
    private Integer yearsOfExperience;

    @Field(type = FieldType.Nested)
    private List<Education> educationHistory;

    @Field(type = FieldType.Keyword)
    private List<String> certifications;

    @Field(type = FieldType.Keyword)
    private String currentPosition;

    @Field(type = FieldType.Double)
    private Double expectedSalary;

    @Field(type = FieldType.Keyword)
    private String preferredEmploymentType;

    @Field(type = FieldType.Boolean)
    private Boolean willingToRelocate;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate dateOfBirth;

    @Field(type = FieldType.Keyword)
    private String linkedinProfile;

    @Field(type = FieldType.Keyword)
    private String githubProfile;

    @Field(type = FieldType.Double)
    private Double matchScore;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime registrationDate;

    public Candidate() {}

    public Candidate(String id, String firstName, String lastName, String email, String phone, String location,
                     String cvContent, List<String> skills, Integer yearsOfExperience,
                     List<Education> educationHistory, List<String> certifications, String currentPosition,
                     Double expectedSalary, String preferredEmploymentType, Boolean willingToRelocate,
                     LocalDate dateOfBirth, String linkedinProfile, String githubProfile, Double matchScore,
                     LocalDateTime registrationDate) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.location = location;
        this.cvContent = cvContent;
        this.skills = skills;
        this.yearsOfExperience = yearsOfExperience;
        this.educationHistory = educationHistory;
        this.certifications = certifications;
        this.currentPosition = currentPosition;
        this.expectedSalary = expectedSalary;
        this.preferredEmploymentType = preferredEmploymentType;
        this.willingToRelocate = willingToRelocate;
        this.dateOfBirth = dateOfBirth;
        this.linkedinProfile = linkedinProfile;
        this.githubProfile = githubProfile;
        this.matchScore = matchScore;
        this.registrationDate = registrationDate;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getCvContent() { return cvContent; }
    public void setCvContent(String cvContent) { this.cvContent = cvContent; }
    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }
    public Integer getYearsOfExperience() { return yearsOfExperience; }
    public void setYearsOfExperience(Integer yearsOfExperience) { this.yearsOfExperience = yearsOfExperience; }
    public List<Education> getEducationHistory() { return educationHistory; }
    public void setEducationHistory(List<Education> educationHistory) { this.educationHistory = educationHistory; }
    public List<String> getCertifications() { return certifications; }
    public void setCertifications(List<String> certifications) { this.certifications = certifications; }
    public String getCurrentPosition() { return currentPosition; }
    public void setCurrentPosition(String currentPosition) { this.currentPosition = currentPosition; }
    public Double getExpectedSalary() { return expectedSalary; }
    public void setExpectedSalary(Double expectedSalary) { this.expectedSalary = expectedSalary; }
    public String getPreferredEmploymentType() { return preferredEmploymentType; }
    public void setPreferredEmploymentType(String preferredEmploymentType) { this.preferredEmploymentType = preferredEmploymentType; }
    public Boolean getWillingToRelocate() { return willingToRelocate; }
    public void setWillingToRelocate(Boolean willingToRelocate) { this.willingToRelocate = willingToRelocate; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getLinkedinProfile() { return linkedinProfile; }
    public void setLinkedinProfile(String linkedinProfile) { this.linkedinProfile = linkedinProfile; }
    public String getGithubProfile() { return githubProfile; }
    public void setGithubProfile(String githubProfile) { this.githubProfile = githubProfile; }
    public Double getMatchScore() { return matchScore; }
    public void setMatchScore(Double matchScore) { this.matchScore = matchScore; }
    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDateTime registrationDate) { this.registrationDate = registrationDate; }
}
