// File: workflow_scheduling/model/GraphInfo.java
package workflow_scheduling.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents metadata about the workflow graph
 */
public class GraphInfo {
    
    @JsonProperty("name")
    private String name;
    
    // Default constructor needed by Jackson
    public GraphInfo() {
    }
    
    // Constructor with parameters
    public GraphInfo(String name) {
        this.name = name;
    }
    
    // Getters and setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String toString() {
        return "GraphInfo{name='" + name + "'}";
    }
}