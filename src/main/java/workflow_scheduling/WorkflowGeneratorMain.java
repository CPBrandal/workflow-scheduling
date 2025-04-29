// File: workflow_scheduling/WorkflowGeneratorMain.java
package workflow_scheduling;

import java.io.File;
import java.io.IOException;
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
                            deployToVirtualMachine(tempYamlPath, namespace);
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
    
 /**
 * Deploys an Argo Workflow YAML file to the remote NREC VM Kubernetes cluster
 * Using SSH config for simplified connection
 * 
 * @param yamlFilePath Path to the YAML file to deploy
 * @param namespace Kubernetes namespace to deploy to
 * @throws IOException If an error occurs executing the command
 * @throws InterruptedException If the process is interrupted
 */
private static void deployToVirtualMachine(String yamlFilePath, String namespace) 
throws IOException, InterruptedException {

        System.out.println("Deploying workflow to NREC VM Kubernetes cluster...");
        File yamlFile = new File(yamlFilePath);

        if (!yamlFile.exists()) {
        System.err.println("Error: YAML file not found at " + yamlFilePath);
        return;
        }

        // Generate remote filename
        String remoteFileName = "workflow_" + System.currentTimeMillis() + ".yaml";

        // Step 1: Test connection to VM using SSH config
        System.out.println("Testing connection to VM...");

        ProcessBuilder testBuilder = new ProcessBuilder(
        "ssh", VM_HOST, "echo Connection successful");

        testBuilder.redirectErrorStream(true);
        Process testProcess = testBuilder.start();

        Scanner testScanner = new Scanner(testProcess.getInputStream()).useDelimiter("\\A");
        String testOutput = testScanner.hasNext() ? testScanner.next() : "";

        int testExitCode = testProcess.waitFor();
        if (testExitCode != 0) {
            System.err.println("Error connecting to VM. Exit code: " + testExitCode);
            System.err.println("Output: " + testOutput);
            return;
        }

        System.out.println("VM connection successful.");

        // Step 2: Copy the YAML file to the VM using SCP with SSH config
        System.out.println("Transferring YAML file to VM...");

        ProcessBuilder scpBuilder = new ProcessBuilder(
        "scp", yamlFilePath, VM_HOST + ":~/" + remoteFileName);

        scpBuilder.redirectErrorStream(true);
        Process scpProcess = scpBuilder.start();

        Scanner scpScanner = new Scanner(scpProcess.getInputStream()).useDelimiter("\\A");
        String scpOutput = scpScanner.hasNext() ? scpScanner.next() : "";

        // Wait for the SCP command to complete with a timeout
        boolean completed = scpProcess.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
        if (!completed) {
        System.err.println("SCP command timed out after 30 seconds");
        scpProcess.destroyForcibly();

        // Try alternative approach
        transferFileAlternative(yamlFilePath, remoteFileName, namespace);
        return; // Return after attempting alternative approach
        }

        int scpExitCode = scpProcess.exitValue();
        if (scpExitCode != 0) {
        System.err.println("Error transferring YAML file to VM. Exit code: " + scpExitCode);
        System.err.println("Output: " + scpOutput);

        // Try alternative approach
        transferFileAlternative(yamlFilePath, remoteFileName, namespace);
        return; // Return after attempting alternative approach
        }

        System.out.println("YAML file transferred successfully.");

        // Step 3: Apply the YAML file using kubectl on the VM
        System.out.println("Applying the workflow with kubectl on VM...");

        String kubectlCommand = "microk8s kubectl apply -f ~/" + remoteFileName + " -n " + namespace;

        ProcessBuilder sshBuilder = new ProcessBuilder(
        "ssh", VM_HOST, kubectlCommand);

        sshBuilder.redirectErrorStream(true);
        Process sshProcess = sshBuilder.start();

        Scanner sshScanner = new Scanner(sshProcess.getInputStream()).useDelimiter("\\A");
        String sshOutput = sshScanner.hasNext() ? sshScanner.next() : "";

        int sshExitCode = sshProcess.waitFor();

        if (sshExitCode == 0) {
        System.out.println("Workflow successfully deployed to VM Kubernetes cluster!");
        System.out.println("Command output: " + sshOutput);

        // Step 4: Clean up the remote file
        System.out.println("Cleaning up remote file...");
        ProcessBuilder cleanupBuilder = new ProcessBuilder(
            "ssh", VM_HOST, "rm -f ~/" + remoteFileName);

        Process cleanupProcess = cleanupBuilder.start();
        cleanupProcess.waitFor();

        // Show workflow status
        System.out.println("Checking workflow status...");
        ProcessBuilder statusBuilder = new ProcessBuilder(
            "ssh", VM_HOST, "kubectl get workflows -n " + namespace);

        statusBuilder.redirectErrorStream(true);
        Process statusProcess = statusBuilder.start();

        Scanner statusScanner = new Scanner(statusProcess.getInputStream()).useDelimiter("\\A");
        String statusOutput = statusScanner.hasNext() ? statusScanner.next() : "";

        System.out.println("Workflow status: \n" + statusOutput);
        } else {
        System.err.println("Error deploying workflow to VM Kubernetes. Exit code: " + sshExitCode);
        System.err.println("Command output: " + sshOutput);
        }
    }
    
    /**
     * Alternative approach to transfer file when SCP fails
     */
    private static void transferFileAlternative(String yamlFilePath, String remoteFileName, String namespace) 
            throws IOException, InterruptedException {
        
        System.out.println("Using alternative file transfer approach...");
        
        // Read file content
        String content = new String(Files.readAllBytes(Paths.get(yamlFilePath)));
        
        // Create a temporary script for the transfer
        String scriptPath = System.getProperty("java.io.tmpdir") + File.separator + "transfer_" + System.currentTimeMillis() + ".sh";
        
        try (java.io.PrintWriter writer = new java.io.PrintWriter(scriptPath)) {
            writer.println("cat << 'EOF' > ~/" + remoteFileName);
            writer.println(content);
            writer.println("EOF");
            writer.println("echo 'File created successfully'");
            writer.println("kubectl apply -f ~/" + remoteFileName + " -n " + namespace);
            writer.println("rm -f ~/" + remoteFileName);
        }
        
        System.out.println("Executing script on remote host...");
        
        ProcessBuilder builder = new ProcessBuilder(
            "ssh", VM_HOST, "bash -s");
        
        builder.redirectInput(new File(scriptPath));
        builder.redirectErrorStream(true);
        
        Process process = builder.start();
        
        Scanner scanner = new Scanner(process.getInputStream()).useDelimiter("\\A");
        String output = scanner.hasNext() ? scanner.next() : "";
        
        int exitCode = process.waitFor();
        
        // Clean up the script
        new File(scriptPath).delete();
        
        if (exitCode == 0) {
            System.out.println("Workflow successfully deployed using alternative approach!");
            System.out.println("Output: " + output);
        } else {
            System.err.println("Error deploying workflow. Exit code: " + exitCode);
            System.err.println("Output: " + output);
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