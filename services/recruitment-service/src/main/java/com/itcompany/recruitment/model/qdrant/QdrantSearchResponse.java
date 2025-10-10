package com.itcompany.recruitment.model.qdrant;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class QdrantSearchResponse {
    
    @JsonProperty("result")
    private List<QdrantPoint> result;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("time")
    private Double time;
    
    public QdrantSearchResponse() {}
    
    // Getters and Setters
    public List<QdrantPoint> getResult() {
        return result;
    }
    
    public void setResult(List<QdrantPoint> result) {
        this.result = result;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Double getTime() {
        return time;
    }
    
    public void setTime(Double time) {
        this.time = time;
    }
}
