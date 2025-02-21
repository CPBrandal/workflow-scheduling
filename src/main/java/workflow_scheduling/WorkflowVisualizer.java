// WorkflowVisualizer.java
package workflow_scheduling;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

public class WorkflowVisualizer extends JFrame {
    private mxGraph graph;
    private Object parent;
    private Map<String, Object> nodeObjects;

    public WorkflowVisualizer(WorkflowAnalyzer.WorkflowJson workflow) {
        super("Workflow Visualization");
        
        // Initialize the graph
        graph = new mxGraph();
        parent = graph.getDefaultParent();
        nodeObjects = new HashMap<>();

        // Start graph update
        graph.getModel().beginUpdate();
        try {
            createVisualization(workflow);
        } finally {
            graph.getModel().endUpdate();
        }

        // Create the graph component and add it to the frame
        mxGraphComponent graphComponent = new mxGraphComponent(graph);
        getContentPane().add(graphComponent);

        // Apply hierarchical layout
        mxHierarchicalLayout layout = new mxHierarchicalLayout(graph);
        layout.execute(parent);

        // Set up the frame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
    }

    private void createVisualization(WorkflowAnalyzer.WorkflowJson workflow) {
        // Create nodes
        for (WorkflowAnalyzer.NodeInfo node : workflow.getNodes()) {
            String label = String.format("%s\n(Time: %d)", 
                node.getId(), node.getExecution_time());
            Object nodeObj = graph.insertVertex(parent, null, label, 
                0, 0, 120, 50, "rounded=1;fillColor=#C0C0C0");
            nodeObjects.put(node.getId(), nodeObj);
        }

        // Create edges
        for (WorkflowAnalyzer.LinkInfo link : workflow.getLinks()) {
            Object source = nodeObjects.get(link.getSource());
            Object target = nodeObjects.get(link.getTarget());
            String label = "Data: " + link.getData_amount();
            graph.insertEdge(parent, null, label, source, target, 
                "strokeWidth=2;endArrow=classic;strokeColor=#808080");
        }
    }
}