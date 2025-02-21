package workflow_scheduling;

// Classes to match JSON structure
public class WorkflowJson {
    private boolean directed;
    private GraphInfo graph;
    private NodeInfo[] nodes;
    private LinkInfo[] links;
    
    // Getters and setters
    public boolean isDirected() { return directed; }
    public void setDirected(boolean directed) { this.directed = directed; }
    public GraphInfo getGraph() { return graph; }
    public void setGraph(GraphInfo graph) { this.graph = graph; }
    public NodeInfo[] getNodes() { return nodes; }
    public void setNodes(NodeInfo[] nodes) { this.nodes = nodes; }
    public LinkInfo[] getLinks() { return links; }
    public void setLinks(LinkInfo[] links) { this.links = links; }
}
