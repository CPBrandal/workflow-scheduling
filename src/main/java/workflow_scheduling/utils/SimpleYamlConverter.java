// File: workflow_scheduling/utils/SimpleYamlConverter.java
package workflow_scheduling.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import workflow_scheduling.model.LinkInfo;
import workflow_scheduling.model.NodeInfo;
import workflow_scheduling.model.WorkflowJson;

/**
 * Utility class for converting workflow data to YAML format
 */
public class SimpleYamlConverter {
    
    /**
     * Converts a workflow to a basic YAML format
     * 
     * @param workflow The workflow to convert
     * @return YAML representation as a string
     */
    public static String workflowToYaml(WorkflowJson workflow) {
        StringBuilder yaml = new StringBuilder();
        
        // Add top level properties
        yaml.append("directed: ").append(workflow.isDirected()).append("\n");
        yaml.append("graph:\n");
        yaml.append("  name: ").append(workflow.getGraph().getName()).append("\n\n");
        
        // Add nodes
        yaml.append("nodes:\n");
        for (NodeInfo node : workflow.getNodes()) {
            yaml.append("  - id: ").append(node.getId()).append("\n");
            yaml.append("    execution_time: ").append(node.getExecutionTime()).append("\n");
        }
        yaml.append("\n");
        
        // Add links
        yaml.append("links:\n");
        for (LinkInfo link : workflow.getLinks()) {
            yaml.append("  - source: ").append(link.getSource()).append("\n");
            yaml.append("    target: ").append(link.getTarget()).append("\n");
            yaml.append("    data_amount: ").append(link.getDataAmount()).append("\n");
        }
        
        return yaml.toString();
    }
    
    /**
     * Saves a workflow to a YAML file
     * 
     * @param workflow The workflow to save
     * @param filePath Path where the YAML file should be saved
     * @throws IOException If the file cannot be written
     */
    public static void saveToYamlFile(WorkflowJson workflow, String filePath) throws IOException {
        String yamlContent = workflowToYaml(workflow);
        
        // Ensure the directory exists
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        
        // Write the content
        Files.writeString(Paths.get(filePath), yamlContent);
    }
}