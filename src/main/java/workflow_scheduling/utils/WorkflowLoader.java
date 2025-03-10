// File: workflow_scheduling/util/WorkflowLoader.java
package workflow_scheduling.utils;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

import workflow_scheduling.model.Edge;
import workflow_scheduling.model.LinkInfo;
import workflow_scheduling.model.Node;
import workflow_scheduling.model.NodeInfo;
import workflow_scheduling.model.WorkflowGraph;
import workflow_scheduling.model.WorkflowJson;

/**
 * Utility class for loading workflow data from JSON files
 */
public class WorkflowLoader {
    
    /**
     * Loads a workflow from a JSON file
     * 
     * @param filePath Path to the JSON file
     * @return The parsed WorkflowJson object
     * @throws IOException If the file cannot be read or parsed
     */
    public static WorkflowJson loadFromFile(String filePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File(filePath), WorkflowJson.class);
    }
    
    /**
     * Loads a workflow from a JSON string
     * 
     * @param jsonContent JSON content as a string
     * @return The parsed WorkflowJson object
     * @throws IOException If the JSON cannot be parsed
     */
    public static WorkflowJson loadFromString(String jsonContent) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonContent, WorkflowJson.class);
    }
    
    /**
     * Saves a workflow to a JSON file
     * 
     * @param workflow The workflow to save
     * @param filePath Path where the JSON file should be saved
     * @throws IOException If the file cannot be written
     */
    public static void saveToFile(WorkflowJson workflow, String filePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), workflow);
    }
    
    /**
     * Creates a graph representation from the workflow JSON
     * 
     * @param workflow The workflow JSON object
     * @param transferRate The transfer rate for calculating edge weights
     * @return A WorkflowGraph object
     */
    public static WorkflowGraph createGraph(WorkflowJson workflow, double transferRate) {
        WorkflowGraph graph = new WorkflowGraph(workflow.getGraph().getName());
        
        // Add nodes
        for (NodeInfo nodeInfo : workflow.getNodes()) {
            Node node = new Node(nodeInfo.getId(), nodeInfo.getExecutionTime());
            graph.addNode(node);
        }
        
        // Add edges
        for (LinkInfo linkInfo : workflow.getLinks()) {
            Edge edge = new Edge(
                linkInfo.getSource(), 
                linkInfo.getTarget(),
                linkInfo.getDataAmount(),
                transferRate
            );
            graph.addEdge(edge);
        }
        
        return graph;
    }
}