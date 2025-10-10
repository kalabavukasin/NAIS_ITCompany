package com.itcompany.recruitment.model.qdrant;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class QdrantPoint {
    
    @JsonProperty("id")
    private Object id;
    
    @JsonProperty("vector")
    private List<Double> vector;
    
    @JsonProperty("payload")
    private Map<String, Object> payload;
    
    public QdrantPoint() {}
    
    public QdrantPoint(Object id, List<Double> vector, Map<String, Object> payload) {
        this.id = id;
        this.vector = vector;
        this.payload = payload;
    }
    
    // Getters and Setters
    public Object getId() {
        return id;
    }
    
    public void setId(Object id) {
        this.id = id;
    }
    
    public List<Double> getVector() {
        return vector;
    }
    
    public void setVector(List<Double> vector) {
        this.vector = vector;
    }
    
    public Map<String, Object> getPayload() {
        return payload;
    }
    
    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}
