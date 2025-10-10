package com.itcompany.recruitment.model;

import java.time.LocalDateTime;
import java.util.Map;

public class ReportMetadata {
    private String reportVersion;
    private LocalDateTime dataExtractedAt;
    private String dataSource;
    private Map<String, Object> queryPerformance;
    private int totalSimpleSections;
    private int totalComplexSections;
    private Map<String, Object> additionalInfo;

    public ReportMetadata() {}

    public ReportMetadata(String reportVersion, LocalDateTime dataExtractedAt, String dataSource,
                         Map<String, Object> queryPerformance, int totalSimpleSections,
                         int totalComplexSections, Map<String, Object> additionalInfo) {
        this.reportVersion = reportVersion;
        this.dataExtractedAt = dataExtractedAt;
        this.dataSource = dataSource;
        this.queryPerformance = queryPerformance;
        this.totalSimpleSections = totalSimpleSections;
        this.totalComplexSections = totalComplexSections;
        this.additionalInfo = additionalInfo;
    }

    // Getters and Setters
    public String getReportVersion() {
        return reportVersion;
    }

    public void setReportVersion(String reportVersion) {
        this.reportVersion = reportVersion;
    }

    public LocalDateTime getDataExtractedAt() {
        return dataExtractedAt;
    }

    public void setDataExtractedAt(LocalDateTime dataExtractedAt) {
        this.dataExtractedAt = dataExtractedAt;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public Map<String, Object> getQueryPerformance() {
        return queryPerformance;
    }

    public void setQueryPerformance(Map<String, Object> queryPerformance) {
        this.queryPerformance = queryPerformance;
    }

    public int getTotalSimpleSections() {
        return totalSimpleSections;
    }

    public void setTotalSimpleSections(int totalSimpleSections) {
        this.totalSimpleSections = totalSimpleSections;
    }

    public int getTotalComplexSections() {
        return totalComplexSections;
    }

    public void setTotalComplexSections(int totalComplexSections) {
        this.totalComplexSections = totalComplexSections;
    }

    public Map<String, Object> getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(Map<String, Object> additionalInfo) {
        this.additionalInfo = additionalInfo;
    }
}
