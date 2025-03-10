// File: workflow_scheduling/model/Node.java
package workflow_scheduling.model;

/**
 * Represents a node in the workflow graph for algorithm operations
 * This is an operational node class that extends the basic information from NodeInfo
 */
public class Node {
    private String id;
    private int executionTime;
    
    // Fields for CPM analysis
    private double earliestStart = 0;
    private double earliestFinish = 0;
    private double latestStart = 0;
    private double latestFinish = 0;
    private double slack = 0;
    private boolean onCriticalPath = false;
    
    /**
     * Creates a new node
     * 
     * @param id The ID of the node
     * @param executionTime The execution time of the task
     */
    public Node(String id, int executionTime) {
        this.id = id;
        this.executionTime = executionTime;
    }
    
    /**
     * Creates a new node from a NodeInfo object
     * 
     * @param info The NodeInfo object
     */
    public Node(NodeInfo info) {
        this.id = info.getId();
        this.executionTime = info.getExecutionTime();
    }
    
    /**
     * Checks if this node is on the critical path
     * 
     * @return true if the node is on the critical path
     */
    public boolean isOnCriticalPath() {
        return Math.abs(slack) < 0.001 || onCriticalPath;
    }
    
    /**
     * Sets whether this node is on the critical path
     * 
     * @param onCriticalPath true if the node is on the critical path
     */
    public void setOnCriticalPath(boolean onCriticalPath) {
        this.onCriticalPath = onCriticalPath;
    }
    
    /**
     * Calculate slack time
     */
    public void calculateSlack() {
        this.slack = latestStart - earliestStart;
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public int getExecutionTime() {
        return executionTime;
    }
    
    public double getEarliestStart() {
        return earliestStart;
    }
    
    public void setEarliestStart(double earliestStart) {
        this.earliestStart = earliestStart;
    }
    
    public double getEarliestFinish() {
        return earliestFinish;
    }
    
    public void setEarliestFinish(double earliestFinish) {
        this.earliestFinish = earliestFinish;
    }
    
    public double getLatestStart() {
        return latestStart;
    }
    
    public void setLatestStart(double latestStart) {
        this.latestStart = latestStart;
    }
    
    public double getLatestFinish() {
        return latestFinish;
    }
    
    public void setLatestFinish(double latestFinish) {
        this.latestFinish = latestFinish;
    }
    
    public double getSlack() {
        return slack;
    }
    
    public void setSlack(double slack) {
        this.slack = slack;
    }
    
    @Override
    public String toString() {
        return String.format("Node{id='%s', time=%d, ES=%.1f, EF=%.1f, LS=%.1f, LF=%.1f, slack=%.1f%s}",
            id, executionTime, earliestStart, earliestFinish, latestStart, latestFinish, slack,
            isOnCriticalPath() ? ", CRITICAL" : "");
    }
}