package com.itcompany.recruitment.model;

import java.util.List;
import java.util.Map;

public class ComplexSection {
    private String id;
    private String title;
    private String description;
    private String queryType;
    private Map<String, Object> parameters;
    private List<Map<String, Object>> aggregatedData;
    private List<Map<String, Object>> detailedData;
    private Map<String, Object> statistics;
    private String visualizationType; // "complex_chart", "dashboard", "multi_table"
    private Map<String, Object> chartConfig;
    private List<String> relatedQueries;

    public ComplexSection() {}

    public ComplexSection(String id, String title, String description, String queryType, 
                         Map<String, Object> parameters, List<Map<String, Object>> aggregatedData,
                         List<Map<String, Object>> detailedData, Map<String, Object> statistics,
                         String visualizationType, Map<String, Object> chartConfig, 
                         List<String> relatedQueries) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.queryType = queryType;
        this.parameters = parameters;
        this.aggregatedData = aggregatedData;
        this.detailedData = detailedData;
        this.statistics = statistics;
        this.visualizationType = visualizationType;
        this.chartConfig = chartConfig;
        this.relatedQueries = relatedQueries;
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

    public String getQueryType() {
        return queryType;
    }

    public void setQueryType(String queryType) {
        this.queryType = queryType;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public List<Map<String, Object>> getAggregatedData() {
        return aggregatedData;
    }

    public void setAggregatedData(List<Map<String, Object>> aggregatedData) {
        this.aggregatedData = aggregatedData;
    }

    public List<Map<String, Object>> getDetailedData() {
        return detailedData;
    }

    public void setDetailedData(List<Map<String, Object>> detailedData) {
        this.detailedData = detailedData;
    }

    public Map<String, Object> getStatistics() {
        return statistics;
    }

    public void setStatistics(Map<String, Object> statistics) {
        this.statistics = statistics;
    }

    public String getVisualizationType() {
        return visualizationType;
    }

    public void setVisualizationType(String visualizationType) {
        this.visualizationType = visualizationType;
    }

    public Map<String, Object> getChartConfig() {
        return chartConfig;
    }

    public void setChartConfig(Map<String, Object> chartConfig) {
        this.chartConfig = chartConfig;
    }

    public List<String> getRelatedQueries() {
        return relatedQueries;
    }

    public void setRelatedQueries(List<String> relatedQueries) {
        this.relatedQueries = relatedQueries;
    }
}
