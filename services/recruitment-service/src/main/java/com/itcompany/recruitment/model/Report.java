package com.itcompany.recruitment.model;

import java.time.LocalDateTime;
import java.util.List;

public class Report {
    private String id;
    private String title;
    private String description;
    private LocalDateTime generatedAt;
    private String generatedBy;
    private List<SimpleSection> simpleSections;
    private List<ComplexSection> complexSections;
    private ReportMetadata metadata;

    public Report() {}

    public Report(String id, String title, String description, LocalDateTime generatedAt, 
                 String generatedBy, List<SimpleSection> simpleSections, 
                 List<ComplexSection> complexSections, ReportMetadata metadata) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.generatedAt = generatedAt;
        this.generatedBy = generatedBy;
        this.simpleSections = simpleSections;
        this.complexSections = complexSections;
        this.metadata = metadata;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(String generatedBy) {
        this.generatedBy = generatedBy;
    }

    public List<SimpleSection> getSimpleSections() {
        return simpleSections;
    }

    public void setSimpleSections(List<SimpleSection> simpleSections) {
        this.simpleSections = simpleSections;
    }

    public List<ComplexSection> getComplexSections() {
        return complexSections;
    }

    public void setComplexSections(List<ComplexSection> complexSections) {
        this.complexSections = complexSections;
    }

    public ReportMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ReportMetadata metadata) {
        this.metadata = metadata;
    }
}
