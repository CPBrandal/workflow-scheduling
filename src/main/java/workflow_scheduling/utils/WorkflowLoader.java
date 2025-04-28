// File: workflow_scheduling/utils/WorkflowLoader.java
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
    
    // Default directories for saving files
    private static final String DEFAULT_JSON_DIR = "src/main/java/workflow_scheduling/exampleWorkflows";
    private static final String DEFAULT_YAML_DIR = "src/main/java/workflow_scheduling/exampleYamlFiles";
    
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
     * If only a filename is provided (no path separators), it saves to the default directory
     * 
     * @param workflow The workflow to save
     * @param filePath Path or filename where the JSON file should be saved
     * @return The full path where the file was saved
     * @throws IOException If the file cannot be written
     */
    public static String saveToFile(WorkflowJson workflow, String filePath) throws IOException {
        // Check if filePath contains a directory separator
        String fullPath;
        if (filePath.contains("/") || filePath.contains("\\")) {
            fullPath = filePath; // Use the provided path as is
        } else {
            // Just a filename was provided, save to default directory
            ensureDirectoryExists(DEFAULT_JSON_DIR);
            fullPath = DEFAULT_JSON_DIR + "/" + filePath;
        }
        
        // Ensure it has .json extension
        if (!fullPath.toLowerCase().endsWith(".json")) {
            fullPath += ".json";
        }
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(fullPath), workflow);
        return fullPath;
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
    
    /**
     * Ensures that a directory exists, creating it if necessary
     *
     * @param dirPath The directory path to check/create
     */
    public static void ensureDirectoryExists(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    /**
     * Gets the default directory for saving YAML files
     * 
     * @return The default YAML directory path
     */
    public static String getDefaultYamlDir() {
        return DEFAULT_YAML_DIR;
    }
    
    /**
     * Gets the default directory for saving JSON files
     * 
     * @return The default JSON directory path
     */
    public static String getDefaultJsonDir() {
        return DEFAULT_JSON_DIR;
    }
    
    /**
     * Resolves a filename or path to a full path, using default directory if necessary
     * 
     * @param filePathOrName The file path or name to resolve
     * @param defaultDir The default directory to use if only a filename is provided
     * @param extension The extension to add if missing (e.g., ".json")
     * @return The resolved full path
     */
    public static String resolveFilePath(String filePathOrName, String defaultDir, String extension) {
        // Check if file path contains a directory separator
        String fullPath;
        if (filePathOrName.contains("/") || filePathOrName.contains("\\")) {
            fullPath = filePathOrName; // Use the provided path as is
        } else {
            // Just a filename was provided, use default directory
            ensureDirectoryExists(defaultDir);
            fullPath = defaultDir + "/" + filePathOrName;
        }
        
        // Ensure it has the proper extension
        if (!fullPath.toLowerCase().endsWith(extension.toLowerCase())) {
            fullPath += extension;
        }
        
        return fullPath;
    }
}