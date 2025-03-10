// File: workflow_scheduling/model/LinkInfo.java
package workflow_scheduling.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a directed edge (dependency) between two tasks in the workflow
 */
public class LinkInfo {
    
    @JsonProperty("source")
    private String source;
    
    @JsonProperty("target")
    private String target;
    
    @JsonProperty("data_amount")
    private int dataAmount;
    
    // Default constructor needed by Jackson
    public LinkInfo() {
    }
    
    // Constructor with parameters
    public LinkInfo(String source, String target, int dataAmount) {
        this.source = source;
        this.target = target;
        this.dataAmount = dataAmount;
    }
    
    // Getters and setters
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public String getTarget() {
        return target;
    }
    
    public void setTarget(String target) {
        this.target = target;
    }
    
    public int getDataAmount() {
        return dataAmount;
    }
    
    public void setDataAmount(int dataAmount) {
        this.dataAmount = dataAmount;
    }
    
    @Override
    public String toString() {
        return "LinkInfo{source='" + source + "', target='" + target + "', dataAmount=" + dataAmount + "}";
    }
}