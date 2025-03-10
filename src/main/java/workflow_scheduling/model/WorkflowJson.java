// File: workflow_scheduling/model/WorkflowJson.java
package workflow_scheduling.model;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Root class for the workflow JSON structure
 * Represents a complete workflow with all its components
 */
public class WorkflowJson {
    
    @JsonProperty("directed")
    private boolean directed;
    
    @JsonProperty("graph")
    private GraphInfo graph;
    
    @JsonProperty("nodes")
    private NodeInfo[] nodes;
    
    @JsonProperty("links")
    private LinkInfo[] links;
    
    // Default constructor needed by Jackson
    public WorkflowJson() {
    }
    
    // Constructor with parameters
    public WorkflowJson(boolean directed, GraphInfo graph, NodeInfo[] nodes, LinkInfo[] links) {
        this.directed = directed;
        this.graph = graph;
        this.nodes = nodes;
        this.links = links;
    }
    
    // Getters and setters
    public boolean isDirected() {
        return directed;
    }
    
    public void setDirected(boolean directed) {
        this.directed = directed;
    }
    
    public GraphInfo getGraph() {
        return graph;
    }
    
    public void setGraph(GraphInfo graph) {
        this.graph = graph;
    }
    
    public NodeInfo[] getNodes() {
        return nodes;
    }
    
    public void setNodes(NodeInfo[] nodes) {
        this.nodes = nodes;
    }
    
    public LinkInfo[] getLinks() {
        return links;
    }
    
    public void setLinks(LinkInfo[] links) {
        this.links = links;
    }
    
    @Override
    public String toString() {
        return "WorkflowJson{" +
                "directed=" + directed +
                ", graph=" + graph +
                ", nodes=" + Arrays.toString(nodes) +
                ", links=" + Arrays.toString(links) +
                '}';
    }
}