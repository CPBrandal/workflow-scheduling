package workflow_scheduling;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import com.fasterxml.jackson.databind.ObjectMapper;

public class WorkflowAnalyzer {
    private String filePath;
    private double transferRate;
    private WorkflowJson workflow;
    private Map<String, cpmAnalyzer.Node> nodes;
    private List<cpmAnalyzer.Edge> edges;

    public WorkflowAnalyzer(String filePath, double transferRate) {
        this.filePath = filePath;
        this.transferRate = transferRate;
    }

    public void loadWorkflow() throws IOException {
        // Read JSON file
        ObjectMapper mapper = new ObjectMapper();
        workflow = mapper.readValue(new File(filePath), WorkflowJson.class);
        
        // Create nodes map
        nodes = new HashMap<>();
        for (NodeInfo nodeInfo : workflow.getNodes()) {
            nodes.put(nodeInfo.getId(), 
                     new cpmAnalyzer.Node(nodeInfo.getId(), nodeInfo.getExecution_time()));
        }
        
        // Create edges list
        edges = new ArrayList<>();
        for (LinkInfo link : workflow.getLinks()) {
            edges.add(new cpmAnalyzer.Edge(link.getSource(), link.getTarget(),
                             link.getData_amount(), transferRate));
        }
    }

    public void analyzeCPM() {
        cpmAnalyzer cpm = new cpmAnalyzer(nodes, edges);
        cpm.calculateCriticalPath();
        cpm.printCriticalPath();
    }

    public void visualize() {
        SwingUtilities.invokeLater(() -> {
            WorkflowVisualizer visualizer = new WorkflowVisualizer(workflow);
            visualizer.setVisible(true);
        });
    }

    // Supporting classes for JSON structure
    public static class WorkflowJson {
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

    public static class GraphInfo {
        private String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class NodeInfo {
        private String id;
        private int execution_time;
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public int getExecution_time() { return execution_time; }
        public void setExecution_time(int execution_time) { this.execution_time = execution_time; }
    }

    public static class LinkInfo {
        private String source;
        private String target;
        private int data_amount;
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
        public int getData_amount() { return data_amount; }
        public void setData_amount(int data_amount) { this.data_amount = data_amount; }
    }

    public static void main(String[] args) {
        try {
            WorkflowAnalyzer analyzer = new WorkflowAnalyzer(
                "src/main/java/workflow_scheduling/exampleWorkflows/complexWF.json", 
                10.0
            );
            
            // Load and analyze the workflow
            analyzer.loadWorkflow();
            
            // Show visualization
            analyzer.visualize();
            
            // Run CPM analysis
            analyzer.analyzeCPM();
            
        } catch (IOException e) {
            System.err.println("Error analyzing workflow: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
