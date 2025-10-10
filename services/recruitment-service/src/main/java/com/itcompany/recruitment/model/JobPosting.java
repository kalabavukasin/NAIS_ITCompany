package com.itcompany.recruitment.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import java.time.LocalDateTime;
import java.util.List;

@Document(indexName = "job_advertisements")
@Setting(settingPath = "elasticsearch-settings.json")
public class JobPosting {
    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword)
    private String department;

    @Field(type = FieldType.Keyword)
    private String location;

    @Field(type = FieldType.Keyword)
    private String experienceLevel;

    @Field(type = FieldType.Integer)
    private Integer minYearsExperience;

    @Field(type = FieldType.Integer)
    private Integer maxYearsExperience;

    @Field(type = FieldType.Keyword)
    private List<String> requiredSkills;

    @Field(type = FieldType.Keyword)
    private List<String> preferredSkills;

    @Field(type = FieldType.Double)
    private Double minSalary;

    @Field(type = FieldType.Double)
    private Double maxSalary;

    @Field(type = FieldType.Keyword)
    private String employmentType;

    @Field(type = FieldType.Boolean)
    private Boolean isActive;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime postedDate;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime applicationDeadline;

    @Field(type = FieldType.Keyword)
    private String hrManagerId;

    @Field(type = FieldType.Integer)
    private Integer numberOfPositions;

    public JobPosting() {}

    public JobPosting(String id, String title, String description, String department,
                      String location, String experienceLevel, Integer minYearsExperience, Integer maxYearsExperience,
                      List<String> requiredSkills, List<String> preferredSkills, Double minSalary, Double maxSalary,
                      String employmentType, Boolean isActive, LocalDateTime postedDate, LocalDateTime applicationDeadline,
                      String hrManagerId, Integer numberOfPositions) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.department = department;
        this.location = location;
        this.experienceLevel = experienceLevel;
        this.minYearsExperience = minYearsExperience;
        this.maxYearsExperience = maxYearsExperience;
        this.requiredSkills = requiredSkills;
        this.preferredSkills = preferredSkills;
        this.minSalary = minSalary;
        this.maxSalary = maxSalary;
        this.employmentType = employmentType;
        this.isActive = isActive;
        this.postedDate = postedDate;
        this.applicationDeadline = applicationDeadline;
        this.hrManagerId = hrManagerId;
        this.numberOfPositions = numberOfPositions;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(String experienceLevel) { this.experienceLevel = experienceLevel; }
    public Integer getMinYearsExperience() { return minYearsExperience; }
    public void setMinYearsExperience(Integer minYearsExperience) { this.minYearsExperience = minYearsExperience; }
    public Integer getMaxYearsExperience() { return maxYearsExperience; }
    public void setMaxYearsExperience(Integer maxYearsExperience) { this.maxYearsExperience = maxYearsExperience; }
    public List<String> getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(List<String> requiredSkills) { this.requiredSkills = requiredSkills; }
    public List<String> getPreferredSkills() { return preferredSkills; }
    public void setPreferredSkills(List<String> preferredSkills) { this.preferredSkills = preferredSkills; }
    public Double getMinSalary() { return minSalary; }
    public void setMinSalary(Double minSalary) { this.minSalary = minSalary; }
    public Double getMaxSalary() { return maxSalary; }
    public void setMaxSalary(Double maxSalary) { this.maxSalary = maxSalary; }
    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public LocalDateTime getPostedDate() { return postedDate; }
    public void setPostedDate(LocalDateTime postedDate) { this.postedDate = postedDate; }
    public LocalDateTime getApplicationDeadline() { return applicationDeadline; }
    public void setApplicationDeadline(LocalDateTime applicationDeadline) { this.applicationDeadline = applicationDeadline; }
    public String getHrManagerId() { return hrManagerId; }
    public void setHrManagerId(String hrManagerId) { this.hrManagerId = hrManagerId; }
    public Integer getNumberOfPositions() { return numberOfPositions; }
    public void setNumberOfPositions(Integer numberOfPositions) { this.numberOfPositions = numberOfPositions; }
}
