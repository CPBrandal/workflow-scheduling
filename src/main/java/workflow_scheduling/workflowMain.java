// File: workflow_scheduling/WorkflowMain.java
package workflow_scheduling;

import java.io.File;
import java.io.IOException;

import workflow_scheduling.algoritms.CpmAnalyzer;
import workflow_scheduling.model.WorkflowGraph;
import workflow_scheduling.model.WorkflowJson;
import workflow_scheduling.utils.WorkflowLoader;
import workflow_scheduling.visualization.WorkflowVisualizer;

/**
 * Main class for the Workflow Scheduling System
 * Loads a workflow from JSON, runs CPM analysis, and visualizes it
 */
public class workflowMain {
    // Default transfer rate for data
    private static final double TRANSFER_RATE = 10.0;
    
    public static void main(String[] args) {
        System.out.println("=== Workflow Scheduling System ===");
        
        try {
            // Step 1: Determine which workflow file to use
            String workflowFile = getWorkflowFilePath(args);
            System.out.println("Using workflow file: " + workflowFile);
        
            // Step 2: Load the workflow JSON
            System.out.println("\nLoading workflow...");
            WorkflowJson workflowJson = WorkflowLoader.loadFromFile(workflowFile);
            System.out.println("Successfully loaded: " + workflowJson.getGraph().getName());
            System.out.println("Nodes: " + workflowJson.getNodes().length);
            System.out.println("Links: " + workflowJson.getLinks().length);
            
            // Step 3: Create the operational graph
            System.out.println("\nCreating operational graph...");
            WorkflowGraph graph = WorkflowLoader.createGraph(workflowJson, TRANSFER_RATE);
            System.out.println("Graph created with " + graph.getNodes().size() + " nodes and " 
                            + graph.getEdges().size() + " edges");
            
            // Step 4: Run CPM analysis
            System.out.println("\nRunning Critical Path Method (CPM) analysis...");
            CpmAnalyzer cpmAnalyzer = new CpmAnalyzer(graph);
            cpmAnalyzer.calculateCriticalPath();
            //cpmAnalyzer.printAnalysisResults();

/*             System.out.println("\nRunning Critical Path Method (CPM) analysis with transfer time...");
            DataTransferCPM cpm = new DataTransferCPM(graph);
            cpm.calculateCriticalPath();
            cpm.printAnalysisResults(); */
            cpmAnalyzer.printDetailedAnalysisResults();
            
            // Step 5: Visualize the workflow
            System.out.println("\nDisplaying workflow visualization...");
            System.out.println("(Close the visualization window to exit the program)");
            WorkflowVisualizer visualizer = new WorkflowVisualizer(graph);
            visualizer.display();
            
        } catch (IOException e) {
            System.err.println("Error reading workflow file: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Determines which workflow file to use
     * 
     * @param args Command line arguments
     * @return Path to the workflow file
     */
    private static String getWorkflowFilePath(String[] args) {
        // First priority: Command line argument
        if (args.length > 0) {
            return args[0];
        }
        
        // Second priority: Default file
        String defaultFile = "src/main/java/workflow_scheduling/exampleWorkflows/ill_example.json";
        File file = new File(defaultFile);
        
        if (file.exists()) {
            return defaultFile;
        }
        
        // If default file doesn't exist, look for any JSON file in the example directory
        File exampleDir = new File("src/main/java/workflow_scheduling/exampleWorkflows");
        if (exampleDir.exists() && exampleDir.isDirectory()) {
            File[] jsonFiles = exampleDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
            if (jsonFiles != null && jsonFiles.length > 0) {
                return jsonFiles[0].getAbsolutePath();
            }
        }
        
        // If all else fails, fall back to the default path and let the error handling catch it
        return defaultFile;
    }
}