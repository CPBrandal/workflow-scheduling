// File: workflow_scheduling/utils/ArgoYamlConverter.java
package workflow_scheduling.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import workflow_scheduling.model.LinkInfo;
import workflow_scheduling.model.NodeInfo;
import workflow_scheduling.model.WorkflowJson;

/**
 * Utility class for converting workflow data to Argo Workflow YAML format for Kubernetes
 */
public class ArgoYamlConverter {
    
    /**
     * Converts a workflow to Argo Workflow YAML format
     * 
     * @param workflow The workflow to convert
     * @param criticalPathHostname The hostname to use for critical path nodes
     * @param criticalPathNodes Optional array of node IDs that form the critical path
     * @return Argo Workflow YAML representation as a string
     */
    public static String workflowToArgoYaml(
            WorkflowJson workflow, 
            String criticalPathHostname, 
            String[] criticalPathNodes) {
        
        StringBuilder yaml = new StringBuilder();
        
        // Get workflow name (sanitized for Kubernetes)
        String workflowName = sanitizeKubernetesName(workflow.getGraph().getName());
        
        // Add Argo Workflow header
        yaml.append("apiVersion: argoproj.io/v1alpha1\n");
        yaml.append("kind: Workflow\n");
        yaml.append("metadata:\n");
        yaml.append("  name: ").append(workflowName).append("\n");
        yaml.append("spec:\n");
        yaml.append("  entrypoint: workflow-dag\n");
        
        yaml.append("  volumes:\n");
        yaml.append("  - name: workflow-data\n");
        yaml.append("    emptyDir: {}\n");
        
        // Add templates section
        yaml.append("  templates:\n");
        
        // Create DAG template
        yaml.append("    - name: workflow-dag\n");
        yaml.append("      dag:\n");
        yaml.append("        tasks:\n");
        
        // Find dependency relations for each node
        Map<String, String[]> dependencies = findDependencies(workflow);
        
        // Add task definitions to DAG
        for (NodeInfo node : workflow.getNodes()) {
            String nodeId = node.getId();
            String taskName = sanitizeKubernetesName(nodeId).toLowerCase();
            String templateName = taskName + "-template";
            
            yaml.append("        - name: ").append(taskName).append("\n");
            
            // Add dependencies if any
            String[] deps = dependencies.get(nodeId);
            if (deps != null && deps.length > 0) {
                yaml.append("          dependencies: [");
                for (int i = 0; i < deps.length; i++) {
                    if (i > 0) {
                        yaml.append(", ");
                    }
                    yaml.append(sanitizeKubernetesName(deps[i]).toLowerCase());
                }
                yaml.append("]\n");
            }
            
            yaml.append("          template: ").append(templateName).append("\n");
        }
        
        // Add task templates
        for (NodeInfo node : workflow.getNodes()) {
            String nodeId = node.getId();
            String taskName = sanitizeKubernetesName(nodeId).toLowerCase();
            String templateName = taskName + "-template";
            
            yaml.append("\n");
            yaml.append("    - name: ").append(templateName).append("\n");
            
            // Determine if this node is on the critical path
            boolean isOnCriticalPath = false;
            if (criticalPathNodes != null) {
                for (String cpNode : criticalPathNodes) {
                    if (nodeId.equals(cpNode)) {
                        isOnCriticalPath = true;
                        break;
                    }
                }
            }
            
            // Add nodeSelector ONLY for critical path nodes
            if (isOnCriticalPath && criticalPathHostname != null && !criticalPathHostname.trim().isEmpty()) {
                yaml.append("      nodeSelector:\n");
                yaml.append("        kubernetes.io/hostname: ").append(criticalPathHostname);
                yaml.append("  # Critical path node\n");
            }
            
            // Add container specification
            yaml.append("      container:\n");
            yaml.append("        image: ubuntu:22.04\n");
            yaml.append("        command: [\"/bin/bash\", \"-c\"]\n");
            yaml.append("        args: [\"echo 'Executing ").append(nodeId)
                .append(" task'; sleep ").append(node.getExecutionTime() / 1000.0).append("\"]\n");
            
            yaml.append("        volumeMounts:\n");
            yaml.append("        - name: workflow-data\n");
            yaml.append("          mountPath: /data\n");
        }
        
        return yaml.toString();
    }
    
    /**
     * Finds dependencies for each node based on incoming links
     * 
     * @param workflow The workflow
     * @return Map of node ID to array of dependency node IDs
     */
    private static Map<String, String[]> findDependencies(WorkflowJson workflow) {
        // Create a map to track dependencies (target -> [sources])
        Map<String, String[]> dependencyMap = new HashMap<>();
        
        // First, create multimap of target to sources
        Map<String, java.util.List<String>> targetToSources = new HashMap<>();
        
        // Process all links to find dependencies
        for (LinkInfo link : workflow.getLinks()) {
            String target = link.getTarget();
            String source = link.getSource();
            
            if (!targetToSources.containsKey(target)) {
                targetToSources.put(target, new java.util.ArrayList<>());
            }
            targetToSources.get(target).add(source);
        }
        
        // Convert to array format expected by the output
        for (Map.Entry<String, java.util.List<String>> entry : targetToSources.entrySet()) {
            String target = entry.getKey();
            java.util.List<String> sources = entry.getValue();
            dependencyMap.put(target, sources.toArray(new String[0]));
        }
        
        return dependencyMap;
    }
    
    /**
     * Sanitizes a name for Kubernetes (lowercase, alphanumeric with dashes)
     * 
     * @param name The name to sanitize
     * @return Sanitized name
     */
    private static String sanitizeKubernetesName(String name) {
        // Replace spaces, underscores, and other non-alphanumeric chars with dashes
        String sanitized = name.replaceAll("[^a-zA-Z0-9]", "-")
                              .replaceAll("-+", "-") // Collapse multiple dashes
                              .replaceAll("^-|-$", ""); // Remove leading/trailing dashes
        
        // Ensure it's not empty
        if (sanitized.isEmpty()) {
            sanitized = "task";
        }
        
        return sanitized;
    }
    
    /**
     * Saves a workflow to an Argo Workflow YAML file
     * If only a filename is provided (no path separators), it saves to the default directory
     * 
     * @param workflow The workflow to save
     * @param filePath Path or filename where the YAML file should be saved
     * @param criticalPathHostname The hostname to use for critical path nodes
     * @param criticalPathNodes Optional array of node IDs that form the critical path
     * @return The full path where the file was saved
     * @throws IOException If the file cannot be written
     */
    public static String saveToArgoYamlFile(
            WorkflowJson workflow, 
            String filePath, 
            String criticalPathHostname,
            String[] criticalPathNodes) throws IOException {
        
        // Get the default YAML directory
        String defaultYamlDir = WorkflowLoader.getDefaultYamlDir();
        
        // Resolve the file path
        String fullPath = WorkflowLoader.resolveFilePath(filePath, defaultYamlDir, ".yaml");
        
        // Generate YAML content
        String yamlContent = workflowToArgoYaml(workflow, criticalPathHostname, criticalPathNodes);
        
        // Ensure the directory exists
        File file = new File(fullPath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        
        // Write the content
        Files.writeString(Paths.get(fullPath), yamlContent);
        
        return fullPath;
    }
}