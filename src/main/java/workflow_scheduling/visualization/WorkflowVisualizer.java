// File: workflow_scheduling/visualization/WorkflowVisualizer.java
package workflow_scheduling.visualization;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxStylesheet;

import workflow_scheduling.model.Edge;
import workflow_scheduling.model.Node;
import workflow_scheduling.model.WorkflowGraph;

/**
 * Provides visualization capability for workflow graphs
 */
public class WorkflowVisualizer extends JFrame {
    private static final long serialVersionUID = 1L;
    
    // Graph components
    private mxGraph graph;
    private Object parent;
    private Map<String, Object> nodeObjects;
    private WorkflowGraph workflowGraph;
    private mxGraphComponent graphComponent;
    
    // Style names
    private static final String STYLE_NODE_NORMAL = "NODE_NORMAL";
    private static final String STYLE_NODE_CRITICAL = "NODE_CRITICAL";
    private static final String STYLE_NODE_SELECTED = "NODE_SELECTED";
    private static final String STYLE_EDGE_NORMAL = "EDGE_NORMAL";
    private static final String STYLE_EDGE_CRITICAL = "EDGE_CRITICAL";
    private static final String STYLE_EDGE_SELECTED = "EDGE_SELECTED";
    
    // UI components
    private JPanel controlPanel;
    private JCheckBox showCriticalPathCheckbox;
    private JLabel statusLabel;
    
    /**
     * Creates a new workflow visualizer
     * 
     * @param workflowGraph The workflow graph to visualize
     */
    public WorkflowVisualizer(WorkflowGraph workflowGraph) {
        super("Workflow Visualization: " + workflowGraph.getName());
        this.workflowGraph = workflowGraph;
        initialize();
    }
    
    /**
     * Initializes the visualizer components
     */
    private void initialize() {
        // Set up the main frame
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        getContentPane().setLayout(new BorderLayout());
        
        // Initialize the graph
        graph = new mxGraph();
        parent = graph.getDefaultParent();
        nodeObjects = new HashMap<>();
        
        // Disable editing capabilities
        graph.setCellsEditable(false);
        graph.setCellsMovable(false);
        graph.setCellsResizable(false);
        graph.setCellsSelectable(true);
        
        // Create custom styles
        setupStyles();
        
        // Start graph update
        graph.getModel().beginUpdate();
        try {
            createVisualization();
        } finally {
            graph.getModel().endUpdate();
        }

        graph.setHtmlLabels(true);
        
        // Create the graph component and add it to the frame
        graphComponent = new mxGraphComponent(graph);
        graphComponent.setConnectable(false);
        graphComponent.getViewport().setOpaque(true);
        graphComponent.getViewport().setBackground(Color.WHITE);
        
        // Create a scroll pane for the graph
        JScrollPane scrollPane = new JScrollPane(graphComponent);
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        
        // Apply hierarchical layout
        mxHierarchicalLayout layout = new mxHierarchicalLayout(graph);
        layout.execute(parent);
        
        // Add control panel
        createControlPanel();
        getContentPane().add(controlPanel, BorderLayout.SOUTH);
        
        // Add status bar
        statusLabel = new JLabel("Workflow: " + workflowGraph.getName() + 
                               " | Nodes: " + workflowGraph.getNodes().size() + 
                               " | Edges: " + workflowGraph.getEdges().size());
        statusLabel.setBorder(new EmptyBorder(5, 10, 5, 10));
        getContentPane().add(statusLabel, BorderLayout.NORTH);
    }
    
    /**
     * Creates the control panel with buttons and checkboxes
     */
    private void createControlPanel() {
        controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.setBorder(new EmptyBorder(5, 10, 5, 10));
        
        // Critical path toggle
        showCriticalPathCheckbox = new JCheckBox("Show Critical Path", true);
        showCriticalPathCheckbox.addActionListener((ActionEvent e) -> {
            highlightCriticalPath(showCriticalPathCheckbox.isSelected());
        });
        controlPanel.add(showCriticalPathCheckbox);
        
        // Reset zoom button
        JButton resetZoomButton = new JButton("Reset Zoom");
        resetZoomButton.addActionListener((ActionEvent e) -> {
            graphComponent.zoomActual();
        });
        controlPanel.add(resetZoomButton);
        
        // Zoom in button
        JButton zoomInButton = new JButton("Zoom In");
        zoomInButton.addActionListener((ActionEvent e) -> {
            graphComponent.zoomIn();
        });
        controlPanel.add(zoomInButton);
        
        // Zoom out button
        JButton zoomOutButton = new JButton("Zoom Out");
        zoomOutButton.addActionListener((ActionEvent e) -> {
            graphComponent.zoomOut();
        });
        controlPanel.add(zoomOutButton);
    }
    
    /**
     * Sets up the graph styles
     */
    private void setupStyles() {
        mxStylesheet stylesheet = graph.getStylesheet();
        
        // Normal node style
        Map<String, Object> normalNodeStyle = new HashMap<>();
        normalNodeStyle.put(mxConstants.STYLE_FILLCOLOR, "#E0E0E0");
        normalNodeStyle.put(mxConstants.STYLE_STROKECOLOR, "#000000");
        normalNodeStyle.put(mxConstants.STYLE_STROKEWIDTH, 1);
        normalNodeStyle.put(mxConstants.STYLE_FONTCOLOR, "#000000");
        normalNodeStyle.put(mxConstants.STYLE_FONTSIZE, 12);
        normalNodeStyle.put(mxConstants.STYLE_ROUNDED, true);
        normalNodeStyle.put(mxConstants.STYLE_SHADOW, true);
        stylesheet.putCellStyle(STYLE_NODE_NORMAL, normalNodeStyle);
        
        // Critical node style
        Map<String, Object> criticalNodeStyle = new HashMap<>();
        criticalNodeStyle.put(mxConstants.STYLE_FILLCOLOR, "#FF9999");
        criticalNodeStyle.put(mxConstants.STYLE_STROKECOLOR, "#FF0000");
        criticalNodeStyle.put(mxConstants.STYLE_STROKEWIDTH, 2);
        criticalNodeStyle.put(mxConstants.STYLE_FONTCOLOR, "#000000");
        criticalNodeStyle.put(mxConstants.STYLE_FONTSIZE, 12);
        criticalNodeStyle.put(mxConstants.STYLE_ROUNDED, true);
        criticalNodeStyle.put(mxConstants.STYLE_SHADOW, true);
        stylesheet.putCellStyle(STYLE_NODE_CRITICAL, criticalNodeStyle);
        
        // Selected node style
        Map<String, Object> selectedNodeStyle = new HashMap<>();
        selectedNodeStyle.put(mxConstants.STYLE_FILLCOLOR, "#FFFF99");
        selectedNodeStyle.put(mxConstants.STYLE_STROKECOLOR, "#FF9900");
        selectedNodeStyle.put(mxConstants.STYLE_STROKEWIDTH, 3);
        selectedNodeStyle.put(mxConstants.STYLE_FONTCOLOR, "#000000");
        selectedNodeStyle.put(mxConstants.STYLE_FONTSIZE, 12);
        selectedNodeStyle.put(mxConstants.STYLE_ROUNDED, true);
        selectedNodeStyle.put(mxConstants.STYLE_SHADOW, true);
        stylesheet.putCellStyle(STYLE_NODE_SELECTED, selectedNodeStyle);
        
        // Normal edge style
        Map<String, Object> normalEdgeStyle = new HashMap<>();
        normalEdgeStyle.put(mxConstants.STYLE_STROKECOLOR, "#808080");
        normalEdgeStyle.put(mxConstants.STYLE_STROKEWIDTH, 1);
        normalEdgeStyle.put(mxConstants.STYLE_FONTCOLOR, "#000000");
        normalEdgeStyle.put(mxConstants.STYLE_FONTSIZE, 11);
        normalEdgeStyle.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_CLASSIC);
        normalEdgeStyle.put(mxConstants.STYLE_ROUNDED, true);
        stylesheet.putCellStyle(STYLE_EDGE_NORMAL, normalEdgeStyle);
        
        // Critical edge style
        Map<String, Object> criticalEdgeStyle = new HashMap<>();
        criticalEdgeStyle.put(mxConstants.STYLE_STROKECOLOR, "#FF0000");
        criticalEdgeStyle.put(mxConstants.STYLE_STROKEWIDTH, 2);
        criticalEdgeStyle.put(mxConstants.STYLE_FONTCOLOR, "#FF0000");
        criticalEdgeStyle.put(mxConstants.STYLE_FONTSIZE, 11);
        criticalEdgeStyle.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_CLASSIC);
        criticalEdgeStyle.put(mxConstants.STYLE_ROUNDED, true);
        stylesheet.putCellStyle(STYLE_EDGE_CRITICAL, criticalEdgeStyle);
        
        // Selected edge style
        Map<String, Object> selectedEdgeStyle = new HashMap<>();
        selectedEdgeStyle.put(mxConstants.STYLE_STROKECOLOR, "#FF9900");
        selectedEdgeStyle.put(mxConstants.STYLE_STROKEWIDTH, 3);
        selectedEdgeStyle.put(mxConstants.STYLE_FONTCOLOR, "#FF9900");
        selectedEdgeStyle.put(mxConstants.STYLE_FONTSIZE, 11);
        selectedEdgeStyle.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_CLASSIC);
        selectedEdgeStyle.put(mxConstants.STYLE_ROUNDED, true);
        stylesheet.putCellStyle(STYLE_EDGE_SELECTED, selectedEdgeStyle);
    }
    
    /**
     * Creates the graph visualization
     */
    private void createVisualization() {
        // Create nodes
        for (Node node : workflowGraph.getNodes().values()) {
            String label = formatNodeLabel(node);
            String style = node.isOnCriticalPath() ? STYLE_NODE_CRITICAL : STYLE_NODE_NORMAL;
            
            Object nodeObj = graph.insertVertex(parent, null, label, 0, 0, 180, 90, style);
            nodeObjects.put(node.getId(), nodeObj);
        }
        
        // Create edges
        for (Edge edge : workflowGraph.getEdges()) {
            Object source = nodeObjects.get(edge.getSource());
            Object target = nodeObjects.get(edge.getTarget());
            String label = formatEdgeLabel(edge);
            
            String style = edge.isOnCriticalPath() ? STYLE_EDGE_CRITICAL : STYLE_EDGE_NORMAL;
            
            graph.insertEdge(parent, null, label, source, target, style);
        }
    }
    
    /**
     * Formats the label for a node
     * 
     * @param node The node to format
     * @return Formatted HTML label
     */
    private String formatNodeLabel(Node node) {
        StringBuilder html = new StringBuilder("<html>");
        html.append("<div style='text-align:center'>");
        html.append("<b>").append(node.getId()).append("</b><br>");
        html.append("Time: ").append(node.getExecutionTime()).append("<br>");        
        html.append("</div></html>");
        return html.toString();
    }
    
    /**
     * Formats the label for an edge
     * 
     * @param edge The edge to format
     * @return Formatted HTML label
     */
    private String formatEdgeLabel(Edge edge) {
        StringBuilder html = new StringBuilder("<html>");
        html.append("<div style='text-align:center'>");
        html.append("Data: ").append(edge.getDataAmount()).append("<br>");
        //html.append("Time: ").append(String.format("%.1f", edge.getTransferTime()));
        html.append("</div></html>");
        return html.toString();
    }
    
    /**
     * Highlights or unhighlights the critical path
     * 
     * @param highlight Whether to highlight the critical path
     */
    public void highlightCriticalPath(boolean highlight) {
        graph.getModel().beginUpdate();
        try {
            // Update all nodes
            for (String nodeId : workflowGraph.getNodes().keySet()) {
                Node node = workflowGraph.getNode(nodeId);
                Object cell = nodeObjects.get(nodeId);
                
                String style = STYLE_NODE_NORMAL;
                if (highlight && node.isOnCriticalPath()) {
                    style = STYLE_NODE_CRITICAL;
                }
                
                graph.setCellStyle(style, new Object[] { cell });
            }
            
            // Update all edges
            for (Object cell : graph.getChildCells(parent, false, true)) {
                Object source = graph.getModel().getTerminal(cell, true);
                Object target = graph.getModel().getTerminal(cell, false);
                
                if (source != null && target != null) {
                    String sourceId = null;
                    String targetId = null;
                    
                    // Find node IDs from cell objects
                    for (Map.Entry<String, Object> entry : nodeObjects.entrySet()) {
                        if (entry.getValue().equals(source)) {
                            sourceId = entry.getKey();
                        }
                        if (entry.getValue().equals(target)) {
                            targetId = entry.getKey();
                        }
                    }
                    
                    if (sourceId != null && targetId != null) {
                        // Find the corresponding edge
                        Edge edge = findEdge(sourceId, targetId);
                        
                        if (edge != null) {
                            String style = STYLE_EDGE_NORMAL;
                            if (highlight && edge.isOnCriticalPath()) {
                                style = STYLE_EDGE_CRITICAL;
                            }
                            
                            graph.setCellStyle(style, new Object[] { cell });
                        }
                    }
                }
            }
        } finally {
            graph.getModel().endUpdate();
        }
    }
    
    /**
     * Finds an edge between two nodes
     * 
     * @param sourceId The source node ID
     * @param targetId The target node ID
     * @return The edge, or null if not found
     */
    private Edge findEdge(String sourceId, String targetId) {
        for (Edge edge : workflowGraph.getEdges()) {
            if (edge.getSource().equals(sourceId) && edge.getTarget().equals(targetId)) {
                return edge;
            }
        }
        return null;
    }
    
    /**
     * Highlights a specific path in the graph
     * 
     * @param path List of nodes in the path
     */
    public void highlightPath(List<Node> path) {
        if (path == null || path.isEmpty()) {
            return;
        }
        
        // First, reset all styles
        resetStyles();
        
        graph.getModel().beginUpdate();
        try {
            // Highlight path nodes
            for (Node node : path) {
                Object cell = nodeObjects.get(node.getId());
                if (cell != null) {
                    graph.setCellStyle(STYLE_NODE_SELECTED, new Object[] { cell });
                }
            }
            
            // Highlight path edges
            for (int i = 0; i < path.size() - 1; i++) {
                Node source = path.get(i);
                Node target = path.get(i + 1);
                
                // Find edge between these nodes
                Edge edge = findEdge(source.getId(), target.getId());
                if (edge != null) {
                    // Find the cell for this edge
                    for (Object cell : graph.getChildCells(parent, false, true)) {
                        Object sourceCell = graph.getModel().getTerminal(cell, true);
                        Object targetCell = graph.getModel().getTerminal(cell, false);
                        
                        if (sourceCell != null && targetCell != null &&
                            sourceCell.equals(nodeObjects.get(source.getId())) && 
                            targetCell.equals(nodeObjects.get(target.getId()))) {
                            
                            graph.setCellStyle(STYLE_EDGE_SELECTED, new Object[] { cell });
                            break;
                        }
                    }
                }
            }
        } finally {
            graph.getModel().endUpdate();
        }
    }
    
    /**
     * Resets all styles to their default values
     */
    public void resetStyles() {
        graph.getModel().beginUpdate();
        try {
            // Reset node styles
            for (String nodeId : workflowGraph.getNodes().keySet()) {
                Node node = workflowGraph.getNode(nodeId);
                Object cell = nodeObjects.get(nodeId);
                
                String style = node.isOnCriticalPath() ? STYLE_NODE_CRITICAL : STYLE_NODE_NORMAL;
                graph.setCellStyle(style, new Object[] { cell });
            }
            
            // Reset edge styles
            for (Object cell : graph.getChildCells(parent, false, true)) {
                Object source = graph.getModel().getTerminal(cell, true);
                Object target = graph.getModel().getTerminal(cell, false);
                
                if (source != null && target != null) {
                    String sourceId = null;
                    String targetId = null;
                    
                    // Find node IDs from cell objects
                    for (Map.Entry<String, Object> entry : nodeObjects.entrySet()) {
                        if (entry.getValue().equals(source)) {
                            sourceId = entry.getKey();
                        }
                        if (entry.getValue().equals(target)) {
                            targetId = entry.getKey();
                        }
                    }
                    
                    if (sourceId != null && targetId != null) {
                        // Find the corresponding edge
                        Edge edge = findEdge(sourceId, targetId);
                        
                        if (edge != null) {
                            String style = edge.isOnCriticalPath() ? STYLE_EDGE_CRITICAL : STYLE_EDGE_NORMAL;
                            graph.setCellStyle(style, new Object[] { cell });
                        }
                    }
                }
            }
        } finally {
            graph.getModel().endUpdate();
        }
    }
    
    /**
     * Updates the graph if the workflow has changed
     */
    public void updateGraph() {
        graph.getModel().beginUpdate();
        try {
            // Clear existing graph
            graph.removeCells(graph.getChildCells(parent));
            nodeObjects.clear();
            
            // Recreate visualization
            createVisualization();
            
            // Apply layout
            mxHierarchicalLayout layout = new mxHierarchicalLayout(graph);
            layout.execute(parent);
            
            // Update status
            statusLabel.setText("Workflow: " + workflowGraph.getName() + 
                              " | Nodes: " + workflowGraph.getNodes().size() + 
                              " | Edges: " + workflowGraph.getEdges().size());
        } finally {
            graph.getModel().endUpdate();
        }
    }
    
    /**
     * Displays the visualization
     */
    public void display() {
        SwingUtilities.invokeLater(() -> {
            setVisible(true);
        });
    }
}