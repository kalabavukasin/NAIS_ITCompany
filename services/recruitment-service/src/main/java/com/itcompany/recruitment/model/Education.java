package com.itcompany.recruitment.model;

import org.springframework.data.elasticsearch.annotations.*;
import java.time.LocalDate;

public class Education {
    @Field(type = FieldType.Text)
    private String institution;

    @Field(type = FieldType.Text)
    private String degree;

    @Field(type = FieldType.Text)
    private String fieldOfStudy;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate startDate;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate endDate;

    @Field(type = FieldType.Double)
    private Double gpa;

    public Education() {}

    public Education(String institution, String degree, String fieldOfStudy, LocalDate startDate, LocalDate endDate, Double gpa) {
        this.institution = institution;
        this.degree = degree;
        this.fieldOfStudy = fieldOfStudy;
        this.startDate = startDate;
        this.endDate = endDate;
        this.gpa = gpa;
    }

    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }
    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }
    public String getFieldOfStudy() { return fieldOfStudy; }
    public void setFieldOfStudy(String fieldOfStudy) { this.fieldOfStudy = fieldOfStudy; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Double getGpa() { return gpa; }
    public void setGpa(Double gpa) { this.gpa = gpa; }
}
