// File: workflow_scheduling/model/NodeInfo.java
package workflow_scheduling.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a task or activity node in the workflow
 */
public class NodeInfo {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("execution_time")
    private int executionTime;
    
    // Default constructor needed by Jackson
    public NodeInfo() {
    }
    
    // Constructor with parameters
    public NodeInfo(String id, int executionTime) {
        this.id = id;
        this.executionTime = executionTime;
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public int getExecutionTime() {
        return executionTime;
    }
    
    public void setExecutionTime(int executionTime) {
        this.executionTime = executionTime;
    }
    
    @Override
    public String toString() {
        return "NodeInfo{id='" + id + "', executionTime=" + executionTime + "}";
    }
}