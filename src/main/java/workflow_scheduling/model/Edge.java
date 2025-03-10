// File: workflow_scheduling/model/Edge.java
package workflow_scheduling.model;

/**
 * Represents an edge in the workflow graph for algorithm operations
 * This is an operational edge class that extends the basic information from LinkInfo
 */
public class Edge {
    private String source;
    private String target;
    private int dataAmount;
    private double transferTime;
    private double weight;
    private boolean onCriticalPath = false;
    
    /**
     * Creates a new edge
     * 
     * @param source The ID of the source node
     * @param target The ID of the target node
     * @param dataAmount The amount of data to transfer
     * @param transferRate The transfer rate (used to calculate transfer time)
     */
    public Edge(String source, String target, int dataAmount, double transferRate) {
        this.source = source;
        this.target = target;
        this.dataAmount = dataAmount;
        this.transferTime = calculateTransferTime(dataAmount, transferRate);
        this.weight = this.transferTime; // Default weight is transfer time
    }
    
    /**
     * Creates a new edge from a LinkInfo object
     * 
     * @param info The LinkInfo object
     * @param transferRate The transfer rate (used to calculate transfer time)
     */
    public Edge(LinkInfo info, double transferRate) {
        this.source = info.getSource();
        this.target = info.getTarget();
        this.dataAmount = info.getDataAmount();
        this.transferTime = calculateTransferTime(dataAmount, transferRate);
        this.weight = this.transferTime;
    }
    
    /**
     * Calculates the transfer time based on data amount and transfer rate
     * 
     * @param dataAmount The amount of data to transfer
     * @param transferRate The transfer rate
     * @return The calculated transfer time
     */
    private double calculateTransferTime(int dataAmount, double transferRate) {
        if (transferRate <= 0) {
            return 0; // Avoid division by zero
        }
        return dataAmount / transferRate;
    }
    
    /**
     * Checks if this edge is on the critical path
     * 
     * @return true if the edge is on the critical path
     */
    public boolean isOnCriticalPath() {
        return onCriticalPath;
    }
    
    /**
     * Sets whether this edge is on the critical path
     * 
     * @param onCriticalPath true if the edge is on the critical path
     */
    public void setOnCriticalPath(boolean onCriticalPath) {
        this.onCriticalPath = onCriticalPath;
    }
    
    // Getters and setters
    public String getSource() {
        return source;
    }
    
    public String getTarget() {
        return target;
    }
    
    public int getDataAmount() {
        return dataAmount;
    }
    
    public double getTransferTime() {
        return transferTime;
    }
    
    public double getWeight() {
        return weight;
    }
    
    public void setWeight(double weight) {
        this.weight = weight;
    }
    
    @Override
    public String toString() {
        return String.format("Edge{%s -> %s, data=%d, time=%.1f, weight=%.1f%s}",
            source, target, dataAmount, transferTime, weight,
            onCriticalPath ? ", CRITICAL" : "");
    }
}