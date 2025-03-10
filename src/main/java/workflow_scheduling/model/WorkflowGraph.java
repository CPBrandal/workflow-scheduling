// File: workflow_scheduling/model/WorkflowGraph.java
package workflow_scheduling.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a workflow graph with nodes and edges
 * This is the main operational model used for algorithms
 */
public class WorkflowGraph {
    private String name;
    private Map<String, Node> nodes;
    private List<Edge> edges;
    private Map<String, List<Edge>> outgoingEdges; // adjacency list
    private Map<String, List<Edge>> incomingEdges; // reverse adjacency list
    
    /**
     * Creates a new workflow graph
     * 
     * @param name Name of the workflow
     */
    public WorkflowGraph(String name) {
        this.name = name;
        this.nodes = new HashMap<>();
        this.edges = new ArrayList<>();
        this.outgoingEdges = new HashMap<>();
        this.incomingEdges = new HashMap<>();
    }
    
    /**
     * Adds a node to the graph
     * 
     * @param node The node to add
     */
    public void addNode(Node node) {
        nodes.put(node.getId(), node);
        outgoingEdges.put(node.getId(), new ArrayList<>());
        incomingEdges.put(node.getId(), new ArrayList<>());
    }
    
    /**
     * Adds an edge to the graph
     * 
     * @param edge The edge to add
     */
    public void addEdge(Edge edge) {
        edges.add(edge);
        outgoingEdges.get(edge.getSource()).add(edge);
        incomingEdges.get(edge.getTarget()).add(edge);
    }
    
    /**
     * Gets a node by its ID
     * 
     * @param id The ID of the node to get
     * @return The node, or null if not found
     */
    public Node getNode(String id) {
        return nodes.get(id);
    }
    
    /**
     * Gets all outgoing edges from a node
     * 
     * @param nodeId The ID of the node
     * @return List of outgoing edges
     */
    public List<Edge> getOutgoingEdges(String nodeId) {
        return outgoingEdges.getOrDefault(nodeId, new ArrayList<>());
    }
    
    /**
     * Gets all incoming edges to a node
     * 
     * @param nodeId The ID of the node
     * @return List of incoming edges
     */
    public List<Edge> getIncomingEdges(String nodeId) {
        return incomingEdges.getOrDefault(nodeId, new ArrayList<>());
    }
    
    // Getters
    public String getName() {
        return name;
    }
    
    public Map<String, Node> getNodes() {
        return nodes;
    }
    
    public List<Edge> getEdges() {
        return edges;
    }
    
    public Map<String, List<Edge>> getOutgoingEdges() {
        return outgoingEdges;
    }
    
    public Map<String, List<Edge>> getIncomingEdges() {
        return incomingEdges;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Workflow Graph: ").append(name).append("\n");
        sb.append("Nodes (").append(nodes.size()).append("):\n");
        
        for (Node node : nodes.values()) {
            sb.append("  ").append(node).append("\n");
        }
        
        sb.append("Edges (").append(edges.size()).append("):\n");
        for (Edge edge : edges) {
            sb.append("  ").append(edge).append("\n");
        }
        
        return sb.toString();
    }
}