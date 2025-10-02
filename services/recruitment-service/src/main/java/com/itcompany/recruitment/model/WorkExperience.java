package com.itcompany.recruitment.model;
import org.springframework.data.elasticsearch.annotations.*;
import java.time.LocalDate;
import java.util.List;

public class WorkExperience {
    @Field(type = FieldType.Text)
    private String company;

    @Field(type = FieldType.Text)
    private String position;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private List<String> technologies;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate startDate;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate endDate;

    @Field(type = FieldType.Boolean)
    private Boolean isCurrent;

    public WorkExperience() {}

    public WorkExperience(String company, String position, String description, List<String> technologies, LocalDate startDate, LocalDate endDate, Boolean isCurrent) {
        this.company = company;
        this.position = position;
        this.description = description;
        this.technologies = technologies;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isCurrent = isCurrent;
    }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getTechnologies() { return technologies; }
    public void setTechnologies(List<String> technologies) { this.technologies = technologies; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Boolean getIsCurrent() { return isCurrent; }
    public void setIsCurrent(Boolean isCurrent) { this.isCurrent = isCurrent; }
}
