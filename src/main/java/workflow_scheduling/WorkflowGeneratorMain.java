// File: workflow_scheduling/WorkflowGeneratorMain.java
package workflow_scheduling;

import java.util.Scanner;

import workflow_scheduling.algoritms.CpmAnalyzer;
import workflow_scheduling.generator.WorkflowGenerator;
import workflow_scheduling.model.WorkflowGraph;
import workflow_scheduling.model.WorkflowJson;
import workflow_scheduling.utils.ArgoYamlConverter;
import workflow_scheduling.utils.WorkflowLoader;
import workflow_scheduling.visualization.WorkflowVisualizer;

/**
 * Main class for generating workflow graphs
 * Allows multiple operations on a single generated workflow
 */
public class WorkflowGeneratorMain {
    // Default transfer rate for data
    private static final double TRANSFER_RATE = 10.0;
    
    public static void main(String[] args) {
        System.out.println("=== Workflow Generator ===");
        
        try {
            Scanner scanner = new Scanner(System.in);
            
            // Get workflow parameters
            System.out.print("Enter workflow name: ");
            String name = scanner.nextLine();
            
            System.out.print("Enter number of nodes: ");
            int numNodes = scanner.nextInt();
            
            System.out.print("Enter minimum execution time: ");
            int minExecTime = scanner.nextInt();
            
            System.out.print("Enter maximum execution time: ");
            int maxExecTime = scanner.nextInt();
            
            System.out.print("Enter connectivity level (0.0-1.0): ");
            double connectivity = scanner.nextDouble();
            
            System.out.print("Enter minimum data amount: ");
            int minDataAmount = scanner.nextInt();
            
            System.out.print("Enter maximum data amount: ");
            int maxDataAmount = scanner.nextInt();
            
            // Generate the workflow
            WorkflowGenerator generator = new WorkflowGenerator();
            WorkflowJson workflow = generator.generateWorkflow(
                name, numNodes, minExecTime, maxExecTime, 
                connectivity, minDataAmount, maxDataAmount);
            
            System.out.println("\nWorkflow generated with " + workflow.getNodes().length + 
                              " nodes and " + workflow.getLinks().length + " edges.");
            
            // Keep presenting options until user exits
            boolean running = true;
            WorkflowVisualizer visualizer = null;
            
            while (running) {
                // Output options
                System.out.println("\nSelect an option:");
                System.out.println("1. Save as JSON");
                System.out.println("2. Save as Argo Workflow YAML");
                System.out.println("3. Visualize workflow");
                System.out.println("4. Exit");
                System.out.print("Choice: ");
                
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline
                
                switch (choice) {
                    case 1:
                        System.out.print("Enter filename to save (will be saved in exampleWorkflows folder): ");
                        String jsonFileName = scanner.nextLine();
                        String savedJsonPath = WorkflowLoader.saveToFile(workflow, jsonFileName);
                        System.out.println("Workflow saved as JSON to: " + savedJsonPath);
                        break;
                    
                    case 2:
                        System.out.print("Enter filename to save (will be saved in exampleYamlFiles folder): ");
                        String yamlFileName = scanner.nextLine();
                        
                        System.out.print("Analyze and include critical path information (y/n)? ");
                        boolean includeCriticalPath = scanner.nextLine().trim().toLowerCase().startsWith("y");
                        
                        String[] criticalPathNodes = null;
                        WorkflowGraph graph = null;
                        CpmAnalyzer cpmAnalyzer = null;
                        double criticalPathLength = 0;
                        
                        if (includeCriticalPath) {
                            System.out.print("Enter hostname for critical path nodes (e.g., wf-scheduling): ");
                            String criticalPathHostname = scanner.nextLine().trim();
                            
                            // Create the operational graph and run CPM analysis
                            graph = WorkflowLoader.createGraph(workflow, TRANSFER_RATE);
                            cpmAnalyzer = new CpmAnalyzer(graph);
                            cpmAnalyzer.calculateCriticalPath();
                            
                            // Get critical path nodes and convert to string array
                            java.util.List<workflow_scheduling.model.Node> cpNodes = cpmAnalyzer.getOrderedCriticalPath();
                            criticalPathNodes = new String[cpNodes.size()];
                            for (int i = 0; i < cpNodes.size(); i++) {
                                criticalPathNodes[i] = cpNodes.get(i).getId();
                            }
                            
                            criticalPathLength = cpmAnalyzer.getEarliestCompletionTime();
                            System.out.println("Critical path analyzed: " + cpNodes.size() + 
                                              " nodes on critical path with total length " + 
                                              criticalPathLength);
                            
                            // Ask for parallelism constraints
                            System.out.print("Do you want to set custom parallelism (y/n)? ");
                            if (scanner.nextLine().trim().toLowerCase().startsWith("y")) {
                                System.out.print("Enter parallelism value: ");
                                int parallelism = scanner.nextInt();
                                scanner.nextLine(); // Consume newline
                                
                                String savedYamlPath = ArgoYamlConverter.saveToArgoYamlFile(
                                    workflow, yamlFileName, criticalPathHostname, 
                                    criticalPathNodes, parallelism);
                                    
                                System.out.println("Workflow saved as Argo Workflow YAML to: " + savedYamlPath);
                            } else {
                                // Use auto-calculation
                                int calculatedValue = Math.max(1, workflow.getNodes().length / 3);
                                System.out.println("Using auto-calculated parallelism: " + calculatedValue);
                                
                                String savedYamlPath = ArgoYamlConverter.saveToArgoYamlFile(
                                    workflow, yamlFileName, criticalPathHostname, 
                                    criticalPathNodes);
                                    
                                System.out.println("Workflow saved as Argo Workflow YAML to: " + savedYamlPath);
                            }
                            
                            System.out.println("Critical path nodes will run on: " + criticalPathHostname);
                            System.out.println("Non-critical path nodes will be scheduled by Kubernetes.");
                        } else {
                            // No critical path analysis - just save a basic Argo workflow
                            String savedYamlPath = ArgoYamlConverter.saveToArgoYamlFile(workflow, yamlFileName, null, null);
                            System.out.println("Basic workflow saved as Argo Workflow YAML to: " + savedYamlPath);
                            System.out.println("All nodes will be scheduled by Kubernetes.");
                        }
                        break;
                    
                    case 3:
                        // Create or use existing visualizer
                        if (visualizer == null) {
                            visualizer = visualizeWorkflow(workflow);
                        } else {
                            // If visualization window was closed, create a new one
                            if (!visualizer.isVisible()) {
                                visualizer.display();
                            } else {
                                System.out.println("Visualization window is already open.");
                            }
                        }
                        System.out.println("You can continue to use the menu while viewing the visualization.");
                        break;
                    
                    case 4:
                        running = false;
                        System.out.println("Exiting...");
                        break;
                    
                    default:
                        System.out.println("Invalid choice.");
                }
            }
            
            // Clean up by closing any open visualizer
            if (visualizer != null && visualizer.isVisible()) {
                visualizer.dispose();
            }
            
            scanner.close();
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Analyzes and visualizes a workflow
     * 
     * @param workflow The workflow to visualize
     * @return The visualizer instance for future reference
     */
    private static WorkflowVisualizer visualizeWorkflow(WorkflowJson workflow) {
        System.out.println("Analyzing workflow: " + workflow.getGraph().getName());
        
        // Create the operational graph
        WorkflowGraph graph = WorkflowLoader.createGraph(workflow, TRANSFER_RATE);
        
        // Run CPM analysis
        CpmAnalyzer cpmAnalyzer = new CpmAnalyzer(graph);
        cpmAnalyzer.calculateCriticalPath();
        cpmAnalyzer.printAnalysisResults();
        
        // Visualize
        System.out.println("Displaying workflow visualization...");
        WorkflowVisualizer visualizer = new WorkflowVisualizer(graph);
        visualizer.display();
        
        return visualizer;
    }
}