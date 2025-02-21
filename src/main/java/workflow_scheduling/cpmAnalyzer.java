package workflow_scheduling;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.fasterxml.jackson.databind.ObjectMapper;

public class cpmAnalyzer {
    static class Node {
        String id;
        int executionTime;
        double earliestStart = 0;
        double earliestFinish = 0;
        double latestStart = 0;
        double latestFinish = 0;
        
        Node(String id, int executionTime) {
            this.id = id;
            this.executionTime = executionTime;
        }
    }
    
    static class Edge {
        String source;
        String target;
        int dataAmount;
        double transferTime; // Calculated from dataAmount and transfer rate
        
        Edge(String source, String target, int dataAmount, double transferRate) {
            this.source = source;
            this.target = target;
            this.dataAmount = dataAmount;
            this.transferTime = dataAmount / transferRate;
        }
    }
    
    private Map<String, Node> nodes;
    private Map<String, List<Edge>> adjacencyList;
    private Map<String, List<Edge>> reverseAdjList;
    
    public cpmAnalyzer(Map<String, Node> nodes, List<Edge> edges) {
        this.nodes = nodes;
        this.adjacencyList = new HashMap<>();
        this.reverseAdjList = new HashMap<>();
        
        // Initialize adjacency lists
        nodes.keySet().forEach(id -> {
            adjacencyList.put(id, new ArrayList<>());
            reverseAdjList.put(id, new ArrayList<>());
        });
        
        // Build adjacency lists
        for (Edge edge : edges) {
            adjacencyList.get(edge.source).add(edge);
            reverseAdjList.get(edge.target).add(edge);
        }
    }
    
    public void calculateCriticalPath() {
        // Topological sort
        List<String> sortedNodes = topologicalSort();
        
        // Forward pass
        for (String nodeId : sortedNodes) {
            Node node = nodes.get(nodeId);
            double maxPredecessorFinish = 0;
            
            // Check all predecessors
            for (Edge edge : reverseAdjList.get(nodeId)) {
                Node predecessor = nodes.get(edge.source);
                double finishTime = predecessor.earliestFinish + edge.transferTime;
                maxPredecessorFinish = Math.max(maxPredecessorFinish, finishTime);
            }
            
            node.earliestStart = maxPredecessorFinish;
            node.earliestFinish = maxPredecessorFinish + node.executionTime;
        }
        
        // Initialize latest finish times
        double maxFinish = 0;
        for (Node node : nodes.values()) {
            maxFinish = Math.max(maxFinish, node.earliestFinish);
        }
        
        // Set all latest finish times to max
        for (Node node : nodes.values()) {
            node.latestFinish = maxFinish;
        }
        
        // Backward pass
        for (int i = sortedNodes.size() - 1; i >= 0; i--) {
            String nodeId = sortedNodes.get(i);
            Node node = nodes.get(nodeId);
            
            double minSuccessorStart = node.latestFinish;
            
            // Check all successors
            for (Edge edge : adjacencyList.get(nodeId)) {
                Node successor = nodes.get(edge.target);
                double startTime = successor.latestStart - edge.transferTime;
                minSuccessorStart = Math.min(minSuccessorStart, startTime);
            }
            
            node.latestFinish = minSuccessorStart;
            node.latestStart = minSuccessorStart - node.executionTime;
        }
    }
    
    private List<String> topologicalSort() {
        Set<String> visited = new HashSet<>();
        Stack<String> stack = new Stack<>();
        
        for (String nodeId : nodes.keySet()) {
            if (!visited.contains(nodeId)) {
                topologicalSortUtil(nodeId, visited, stack);
            }
        }
        
        List<String> result = new ArrayList<>();
        while (!stack.empty()) {
            result.add(stack.pop());
        }
        return result;
    }
    
    private void topologicalSortUtil(String nodeId, Set<String> visited, Stack<String> stack) {
        visited.add(nodeId);
        
        for (Edge edge : adjacencyList.get(nodeId)) {
            if (!visited.contains(edge.target)) {
                topologicalSortUtil(edge.target, visited, stack);
            }
        }
        
        stack.push(nodeId);
    }
    
    public void printCriticalPath() {
        for (Node node : nodes.values()) {
            double slack = node.latestStart - node.earliestStart;
            System.out.printf("Node %s: ES=%.1f, EF=%.1f, LS=%.1f, LF=%.1f, Slack=%.1f%n",
                node.id, node.earliestStart, node.earliestFinish,
                node.latestStart, node.latestFinish, slack);
            if (Math.abs(slack) < 0.001) {
                System.out.println("This node is on the critical path!");
            }
        }
    }

    public static void main(String[] args) {
        try {
            // Read JSON file
            ObjectMapper mapper = new ObjectMapper();
            WorkflowJson workflow = mapper.readValue(new File("src/main/java/workflow_scheduling/exampleWorkflows/complexWF.json"), WorkflowJson.class);
            
            // Create nodes map
            Map<String, Node> nodes = new HashMap<>();
            for (NodeInfo nodeInfo : workflow.getNodes()) {
                nodes.put(nodeInfo.getId(), 
                        new Node(nodeInfo.getId(), nodeInfo.getExecution_time()));
            }
            
            // Create edges list with transfer rate
            double transferRate = 10.0; // You might want to make this configurable
            List<Edge> edges = new ArrayList<>();
            for (LinkInfo link : workflow.getLinks()) {
                edges.add(new Edge(link.getSource(), link.getTarget(), 
                                link.getData_amount(), transferRate));
            }
            
            // Create and run CPM analysis
            cpmAnalyzer cpm = new cpmAnalyzer(nodes, edges);
            cpm.calculateCriticalPath();
            cpm.printCriticalPath();
            
        } catch (IOException e) {
            System.err.println("Error reading workflow file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

