package com.itcompany.recruitment.model;

import java.util.List;
import java.util.Map;

public class SimpleSection {
    private String id;
    private String title;
    private String description;
    private String queryType;
    private Map<String, Object> filters;
    private List<Map<String, Object>> data;
    private int totalRecords;
    private String visualizationType; // "table", "chart", "list"
    private Map<String, Object> chartConfig;

    public SimpleSection() {}

    public SimpleSection(String id, String title, String description, String queryType, 
                        Map<String, Object> filters, List<Map<String, Object>> data, 
                        int totalRecords, String visualizationType, Map<String, Object> chartConfig) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.queryType = queryType;
        this.filters = filters;
        this.data = data;
        this.totalRecords = totalRecords;
        this.visualizationType = visualizationType;
        this.chartConfig = chartConfig;
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

    public Map<String, Object> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
    }

    public List<Map<String, Object>> getData() {
        return data;
    }

    public void setData(List<Map<String, Object>> data) {
        this.data = data;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
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
}
