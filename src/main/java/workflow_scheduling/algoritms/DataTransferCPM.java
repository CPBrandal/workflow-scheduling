// File: workflow_scheduling/algorithm/DataTransferCPM.java
package workflow_scheduling.algoritms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import workflow_scheduling.model.Edge;
import workflow_scheduling.model.Node;
import workflow_scheduling.model.WorkflowGraph;

/**
 * Implements the Critical Path Method (CPM) for workflow analysis
 * with consideration for data transfer times
 */
public class DataTransferCPM {
    private WorkflowGraph graph;
    
    /**
     * Creates a new CPM analyzer for the given workflow graph
     * 
     * @param graph The workflow graph to analyze
     */
    public DataTransferCPM(WorkflowGraph graph) {
        this.graph = graph;
    }
    
    /**
     * Calculates the critical path of the workflow
     * Incorporating data transfer amounts in execution times
     */
    public void calculateCriticalPath() {
        // Topological sort
        List<String> sortedNodes = topologicalSort();
        
        /* Forward pass - including data transfer amounts */
        for (String nodeId : sortedNodes) {
            Node node = graph.getNode(nodeId);
            double maxPredecessorFinish = 0;
            
            // Check all predecessors
            for (Edge edge : graph.getIncomingEdges(nodeId)) {
                Node predecessor = graph.getNode(edge.getSource());
                
                // Include both the predecessor finish time and the data transfer time
                double finishTime = predecessor.getEarliestFinish() + edge.getDataAmount();
                maxPredecessorFinish = Math.max(maxPredecessorFinish, finishTime);
            }
            
            node.setEarliestStart(maxPredecessorFinish);
            
            // Calculate earliest finish time including node execution time
            node.setEarliestFinish(maxPredecessorFinish + node.getExecutionTime());
        }
        
        // Initialize latest finish times
        double maxFinish = 0;
        for (Node node : graph.getNodes().values()) {
            maxFinish = Math.max(maxFinish, node.getEarliestFinish());
        }
        
        // Set all latest finish times to max
        for (Node node : graph.getNodes().values()) {
            node.setLatestFinish(maxFinish);
        }
        
        /* Backward pass - including data transfer amounts */
        for (int i = sortedNodes.size() - 1; i >= 0; i--) {
            String nodeId = sortedNodes.get(i);
            Node node = graph.getNode(nodeId);
            
            double minSuccessorStart = node.getLatestFinish();
            
            // Check all successors
            for (Edge edge : graph.getOutgoingEdges(nodeId)) {
                Node successor = graph.getNode(edge.getTarget());
                
                // Include data transfer time when calculating latest finish
                double latestFinishForSuccessor = successor.getLatestStart() - edge.getDataAmount();
                minSuccessorStart = Math.min(minSuccessorStart, latestFinishForSuccessor);
            }
            
            node.setLatestFinish(minSuccessorStart);
            node.setLatestStart(minSuccessorStart - node.getExecutionTime());
            
            // Calculate slack
            node.setSlack(node.getLatestStart() - node.getEarliestStart());
            
            // Mark as critical path if slack is near zero
            if (Math.abs(node.getSlack()) < 0.001) {
                node.setOnCriticalPath(true);
            }
        }
        
        // Mark edges on critical path
        markCriticalEdges();
    }
    
    /**
     * Marks edges that are on the critical path
     * An edge is critical if it connects two critical nodes and
     * the timing including transfer time is consistent
     */
    private void markCriticalEdges() {
        // For each critical node, check which outgoing edges connect to another critical node
        for (Node node : getCriticalPath()) {
            for (Edge edge : graph.getOutgoingEdges(node.getId())) {
                Node target = graph.getNode(edge.getTarget());
                
                // If target is on critical path and timing is consistent with critical path
                if (target.isOnCriticalPath()) {
                    // Expected start time of target should equal the source finish time plus transfer time
                    double expectedStart = node.getEarliestFinish() + edge.getDataAmount();
                    if (Math.abs(expectedStart - target.getEarliestStart()) < 0.001) {
                        edge.setOnCriticalPath(true);
                    }
                }
            }
        }
    }
    
    /**
     * Gets all nodes that are on the critical path
     * 
     * @return List of nodes on the critical path
     */
    public List<Node> getCriticalPath() {
        List<Node> criticalPath = new ArrayList<>();
        for (Node node : graph.getNodes().values()) {
            if (node.isOnCriticalPath()) {
                criticalPath.add(node);
            }
        }
        return criticalPath;
    }
    
    /**
     * Gets the critical path as a sequence of connected nodes
     * 
     * @return Ordered list of nodes in the critical path
     */
    public List<Node> getOrderedCriticalPath() {
        List<Node> criticalPath = getCriticalPath();
        List<Node> ordered = new ArrayList<>();
        
        // Find a node with no incoming critical edges (should be the start)
        Node start = null;
        for (Node node : criticalPath) {
            boolean hasIncomingCritical = false;
            for (Edge edge : graph.getIncomingEdges(node.getId())) {
                if (edge.isOnCriticalPath()) {
                    hasIncomingCritical = true;
                    break;
                }
            }
            
            if (!hasIncomingCritical) {
                start = node;
                break;
            }
        }
        
        if (start == null && !criticalPath.isEmpty()) {
            // If no clear start, use the earliest start time
            start = criticalPath.get(0);
            for (Node node : criticalPath) {
                if (node.getEarliestStart() < start.getEarliestStart()) {
                    start = node;
                }
            }
        }
        
        // Build the ordered path
        if (start != null) {
            ordered.add(start);
            buildOrderedPath(start, ordered);
        }
        
        return ordered;
    }
    
    /**
     * Recursively builds the ordered critical path
     */
    private void buildOrderedPath(Node current, List<Node> path) {
        for (Edge edge : graph.getOutgoingEdges(current.getId())) {
            if (edge.isOnCriticalPath()) {
                Node next = graph.getNode(edge.getTarget());
                if (!path.contains(next)) {
                    path.add(next);
                    buildOrderedPath(next, path);
                }
            }
        }
    }
    
    /**
     * Performs a topological sort of the graph
     * 
     * @return List of node IDs in topological order
     */
    private List<String> topologicalSort() {
        Set<String> visited = new HashSet<>();
        Stack<String> stack = new Stack<>();
        
        for (String nodeId : graph.getNodes().keySet()) {
            if (!visited.contains(nodeId)) {
                topologicalSortUtil(nodeId, visited, stack);
            }
        }
        
        List<String> result = new ArrayList<>();
        while (!stack.isEmpty()) {
            result.add(stack.pop());
        }
        return result;
    }
    
    /**
     * Utility method for topological sort using DFS
     */
    private void topologicalSortUtil(String nodeId, Set<String> visited, Stack<String> stack) {
        visited.add(nodeId);
        
        for (Edge edge : graph.getOutgoingEdges(nodeId)) {
            if (!visited.contains(edge.getTarget())) {
                topologicalSortUtil(edge.getTarget(), visited, stack);
            }
        }
        
        stack.push(nodeId);
    }
    
    /**
     * Gets the earliest completion time for the entire workflow
     * This includes both execution times and data transfer times
     * 
     * @return The earliest completion time
     */
    public double getEarliestCompletionTime() {
        double maxFinish = 0;
        for (Node node : graph.getNodes().values()) {
            maxFinish = Math.max(maxFinish, node.getEarliestFinish());
        }
        return maxFinish;
    }
    
    /**
     * Calculates the total data transfer time on the critical path
     * 
     * @return The total data transfer time
     */
    public double getTotalCriticalPathTransferTime() {
        double totalTransferTime = 0;
        
        List<Node> orderedPath = getOrderedCriticalPath();
        for (int i = 0; i < orderedPath.size() - 1; i++) {
            Node current = orderedPath.get(i);
            Node next = orderedPath.get(i + 1);
            
            for (Edge edge : graph.getOutgoingEdges(current.getId())) {
                if (edge.getTarget().equals(next.getId()) && edge.isOnCriticalPath()) {
                    totalTransferTime += edge.getDataAmount();
                    break;
                }
            }
        }
        
        return totalTransferTime;
    }
    
    /**
     * Calculates the total execution time on the critical path
     * (without transfer times)
     * 
     * @return The total execution time
     */
    public double getTotalCriticalPathExecutionTime() {
        double totalExecutionTime = 0;
        
        for (Node node : getOrderedCriticalPath()) {
            totalExecutionTime += node.getExecutionTime();
        }
        
        return totalExecutionTime;
    }
    
    /**
     * Prints the critical path analysis results
     */
    public void printAnalysisResults() {
        System.out.println("=== Critical Path Analysis Results ===");
        System.out.println("(Including data transfer times in calculations)");
        
        for (Node node : graph.getNodes().values()) {
            System.out.printf("Node %s: ES=%.1f, EF=%.1f, LS=%.1f, LF=%.1f, Slack=%.1f%n",
                node.getId(), node.getEarliestStart(), node.getEarliestFinish(),
                node.getLatestStart(), node.getLatestFinish(), node.getSlack());
                
            if (node.isOnCriticalPath()) {
                System.out.println("  This node is on the critical path!");
            }
        }
        
        System.out.println("\n=== Critical Path ===");
        List<Node> orderedPath = getOrderedCriticalPath();
        if (orderedPath.isEmpty()) {
            System.out.println("No critical path found.");
        } else {
            System.out.println("Critical path sequence:");
            StringBuilder pathStr = new StringBuilder();
            for (int i = 0; i < orderedPath.size(); i++) {
                pathStr.append(orderedPath.get(i).getId());
                if (i < orderedPath.size() - 1) {
                    pathStr.append(" -> ");
                }
            }
            System.out.println(pathStr.toString());
            
            double totalExecTime = getTotalCriticalPathExecutionTime();
            double totalTransferTime = getTotalCriticalPathTransferTime();
            double totalTime = getEarliestCompletionTime();
            
            System.out.printf("Total execution time: %.1f%n", totalExecTime);
            System.out.printf("Total data transfer time: %.1f%n", totalTransferTime);
            System.out.printf("Total workflow completion time: %.1f%n", totalTime);
        }
    }
}