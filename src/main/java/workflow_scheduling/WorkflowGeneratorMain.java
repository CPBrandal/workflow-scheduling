// File: workflow_scheduling/WorkflowGeneratorMain.java
package workflow_scheduling;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    // Default hostname for critical path nodes
    private static final String DEFAULT_HOSTNAME = "wf-scheduling";
    // Default Kubernetes namespace
    private static final String DEFAULT_NAMESPACE = "argo";
    // SSH connection details using SSH config names
    private static final String VM_HOST = "nrec-vm1";
    
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
                System.out.println("4. Deploy workflow to NREC VM Kubernetes cluster");
                System.out.println("5. Exit");
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
                            System.out.print("Enter hostname for critical path nodes (e.g., wf-scheduling) or press Enter for default: ");
                            String criticalPathHostname = scanner.nextLine().trim();
                            
                            // If user just pressed Enter, use the default hostname
                            if (criticalPathHostname.isEmpty()) {
                                criticalPathHostname = DEFAULT_HOSTNAME;
                                System.out.println("Using default hostname: " + DEFAULT_HOSTNAME);
                            }
                            
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
                            
                            String savedYamlPath = ArgoYamlConverter.saveToArgoYamlFile(
                                workflow, yamlFileName, criticalPathHostname, criticalPathNodes);
                                
                            System.out.println("Workflow saved as Argo Workflow YAML to: " + savedYamlPath);
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
                        // Deploy workflow to NREC VM Kubernetes cluster
                        String yamlFileToSave = "deploy_" + System.currentTimeMillis() + ".yaml";
                        System.out.print("Enter Kubernetes namespace (press Enter for default): ");
                        String namespace = scanner.nextLine().trim();
                        if (namespace.isEmpty()) {
                            namespace = DEFAULT_NAMESPACE;
                            System.out.println("Using default namespace: " + DEFAULT_NAMESPACE);
                        }
                        
                        System.out.print("Analyze and include critical path information (y/n)? ");
                        boolean includeCriticalPathForDeployment = scanner.nextLine().trim().toLowerCase().startsWith("y");
                        
                        String[] criticalPathNodesForDeployment = null;
                        String criticalPathHostnameForDeployment = null;
                        
                        if (includeCriticalPathForDeployment) {
                            System.out.print("Enter hostname for critical path nodes (e.g., wf-scheduling) or press Enter for default: ");
                            criticalPathHostnameForDeployment = scanner.nextLine().trim();
                            
                            // If user just pressed Enter, use the default hostname
                            if (criticalPathHostnameForDeployment.isEmpty()) {
                                criticalPathHostnameForDeployment = DEFAULT_HOSTNAME;
                                System.out.println("Using default hostname: " + DEFAULT_HOSTNAME);
                            }
                            
                            // Create the operational graph and run CPM analysis
                            WorkflowGraph graphForDeployment = WorkflowLoader.createGraph(workflow, TRANSFER_RATE);
                            CpmAnalyzer cpmAnalyzerForDeployment = new CpmAnalyzer(graphForDeployment);
                            cpmAnalyzerForDeployment.calculateCriticalPath();
                            
                            // Get critical path nodes and convert to string array
                            java.util.List<workflow_scheduling.model.Node> cpNodesForDeployment = 
                                cpmAnalyzerForDeployment.getOrderedCriticalPath();
                            criticalPathNodesForDeployment = new String[cpNodesForDeployment.size()];
                            for (int i = 0; i < cpNodesForDeployment.size(); i++) {
                                criticalPathNodesForDeployment[i] = cpNodesForDeployment.get(i).getId();
                            }
                            
                            System.out.println("Critical path analyzed: " + cpNodesForDeployment.size() + 
                                              " nodes on critical path");
                        }
                        
                        // Save the YAML file temporarily
                        String tempYamlPath = ArgoYamlConverter.saveToArgoYamlFile(
                            workflow, yamlFileToSave, criticalPathHostnameForDeployment, criticalPathNodesForDeployment);
                        
                        // Deploy to VM's Kubernetes cluster
                        try {
                            deployToVirtualMachine(tempYamlPath, namespace, name);
                        } finally {
                            // Clean up the temporary file
                            try {
                                Files.deleteIfExists(Paths.get(tempYamlPath));
                            } catch (IOException e) {
                                System.out.println("Note: Could not delete temporary YAML file: " + tempYamlPath);
                            }
                        }
                        break;
                        
                    case 5:
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
    

    private static void deployToVirtualMachine(String yamlFilePath, String namespace, String name)
    throws IOException, InterruptedException {
        System.out.println("Deploying workflow to NREC VM Kubernetes cluster...");
        File yamlFile = new File(yamlFilePath);
        if (!yamlFile.exists()) {
            System.err.println("Error: YAML file not found at " + yamlFilePath);
            return;
        }
        
        // Generate remote filename
        String remoteFileName = name;
        
        // Create a comprehensive deployment script
        String tempScriptPath = createDeploymentScript(yamlFilePath, remoteFileName, namespace);
        
        // Deployment retry mechanism
        int maxAttempts = 5;
        boolean deploymentSuccessful = false;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            System.out.println("Deployment attempt " + attempt + " of " + maxAttempts + "...");
            
            Process sshProcess = null;
            try {
                ProcessBuilder sshBuilder = new ProcessBuilder("ssh", VM_HOST, "bash -s");
                sshBuilder.redirectInput(new File(tempScriptPath));
                sshBuilder.redirectErrorStream(true);
                
                sshProcess = sshBuilder.start();
                
                // Read output
                try (Scanner sshScanner = new Scanner(sshProcess.getInputStream()).useDelimiter("\\A")) {
                    String output = sshScanner.hasNext() ? sshScanner.next() : "";
                    System.out.println("Output from remote server:\n" + output);
                }
                
                // Wait for the process with a timeout
                boolean completed = sshProcess.waitFor(5, java.util.concurrent.TimeUnit.MINUTES);
                
                if (!completed) {
                    System.err.println("SSH session timed out after 5 minutes");
                    sshProcess.destroyForcibly();
                    continue;
                }
                
                int exitCode = sshProcess.exitValue();
                
                if (exitCode == 0) {
                    System.out.println("Workflow successfully deployed!");
                    deploymentSuccessful = true;
                    break;
                } else {
                    System.err.println("Deployment failed with exit code: " + exitCode);
                }
            } catch (Exception e) {
                System.err.println("Deployment attempt " + attempt + " error: " + e.getMessage());
            } finally {
                // Ensure process is destroyed if it's still running
                if (sshProcess != null) {
                    sshProcess.destroyForcibly();
                }
            }
            
            // Wait before next attempt
            if (attempt < maxAttempts) {
                System.out.println("Waiting 5 seconds before next attempt...");
                Thread.sleep(5000);
            }
        }
        
        if (!deploymentSuccessful) {
            System.err.println("Failed to deploy workflow after " + maxAttempts + " attempts");
        }
        
        // Clean up the local temporary script
        try {
            Files.deleteIfExists(Paths.get(tempScriptPath));
        } catch (IOException e) {
            System.out.println("Could not delete temporary script: " + e.getMessage());
        }
    }
/**
 * Creates a comprehensive deployment script to be executed in a single SSH session
 * 
 * @param yamlFilePath Local path to the YAML file
 * @param remoteFileName Name of the file on the remote server
 * @param namespace Kubernetes namespace
 * @return Path to the created temporary script
 * @throws IOException If there's an error creating the script
 */
private static String createDeploymentScript(String yamlFilePath, String remoteFileName, String namespace) 
throws IOException {
    // Read the YAML file content
    String yamlContent = new String(Files.readAllBytes(Paths.get(yamlFilePath)));
    
    // Create a temporary script path
    String tempScriptPath = System.getProperty("java.io.tmpdir") + File.separator + 
                           "deploy_script_" + System.currentTimeMillis() + ".sh";
    
    // Create a script with all the operations
    try (PrintWriter writer = new PrintWriter(tempScriptPath)) {
        // Bash script setup for robust execution
        writer.println("#!/bin/bash");
        writer.println("set -e  # Exit immediately if a command exits with a non-zero status");
        writer.println("set -o pipefail  # Fail on any component of a pipe");
        
        // Logging and tracing
        writer.println("echo 'Starting deployment process...'");
        
        // Create the remote YAML file
        writer.println("echo 'Creating YAML file on remote server...'");
        writer.println("cat << 'EOF' > ~/" + remoteFileName);
        writer.println(yamlContent);
        writer.println("EOF");
        
        // Verify file creation
        writer.println("echo 'Verifying YAML file...'");
        writer.println("ls -l ~/" + remoteFileName);
        
        // Apply the workflow
        writer.println("echo 'Applying workflow with kubectl...'");
        writer.println("microk8s kubectl apply -f ~/" + remoteFileName + " -n " + namespace);
        
        // Check workflow status
        writer.println("echo 'Current workflow status:'");
        writer.println("microk8s kubectl get workflows -n " + namespace);
        
        // Clean up the file
        writer.println("echo 'Cleaning up temporary files...'");
        writer.println("rm -f ~/" + remoteFileName);
    }
    
    return tempScriptPath;
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