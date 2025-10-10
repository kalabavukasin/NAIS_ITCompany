package com.itcompany.recruitment.model.qdrant;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class QdrantSearchRequest {
    
    @JsonProperty("vector")
    private List<Double> vector;
    
    @JsonProperty("limit")
    private Integer limit = 10;
    
    @JsonProperty("with_payload")
    private Boolean withPayload = true;
    
    @JsonProperty("with_vector")
    private Boolean withVector = false;
    
    @JsonProperty("filter")
    private Map<String, Object> filter;
    
    public QdrantSearchRequest() {}
    
    public QdrantSearchRequest(List<Double> vector, Integer limit) {
        this.vector = vector;
        this.limit = limit;
    }
    
    // Getters and Setters
    public List<Double> getVector() {
        return vector;
    }
    
    public void setVector(List<Double> vector) {
        this.vector = vector;
    }
    
    public Integer getLimit() {
        return limit;
    }
    
    public void setLimit(Integer limit) {
        this.limit = limit;
    }
    
    public Boolean getWithPayload() {
        return withPayload;
    }
    
    public void setWithPayload(Boolean withPayload) {
        this.withPayload = withPayload;
    }
    
    public Boolean getWithVector() {
        return withVector;
    }
    
    public void setWithVector(Boolean withVector) {
        this.withVector = withVector;
    }
    
    public Map<String, Object> getFilter() {
        return filter;
    }
    
    public void setFilter(Map<String, Object> filter) {
        this.filter = filter;
    }
}
