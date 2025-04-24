// File: workflow_scheduling/generator/WorkflowGenerator.java
package workflow_scheduling.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import workflow_scheduling.model.GraphInfo;
import workflow_scheduling.model.LinkInfo;
import workflow_scheduling.model.NodeInfo;
import workflow_scheduling.model.WorkflowJson;

/**
 * Generates streamlined DAG workflow graphs with clear data flow
 * Creates workflows with fewer edges and more linear structure
 */
public class WorkflowGenerator {
    
    private Random random;
    private int seed;
    
    /**
     * Creates a new workflow generator with a random seed
     */
    public WorkflowGenerator() {
        this.seed = (int) System.currentTimeMillis();
        this.random = new Random(seed);
    }
    
    /**
     * Creates a new workflow generator with a specified seed for reproducible results
     * 
     * @param seed The random seed to use
     */
    public WorkflowGenerator(int seed) {
        this.seed = seed;
        this.random = new Random(seed);
    }
    
    /**
     * Generates a streamlined workflow as a directed acyclic graph (DAG)
     * 
     * @param name The name of the workflow
     * @param numNodes The number of nodes to generate
     * @param minExecTime Minimum task execution time
     * @param maxExecTime Maximum task execution time
     * @param connectivity Connectivity level (0.0-1.0) controlling edge density
     * @param minDataAmount Minimum data transfer amount
     * @param maxDataAmount Maximum data transfer amount
     * @return A WorkflowJson object representing the generated workflow
     */
    public WorkflowJson generateWorkflow(
            String name,
            int numNodes,
            int minExecTime,
            int maxExecTime,
            double connectivity,
            int minDataAmount,
            int maxDataAmount) {
        
        // Input validation
        if (numNodes < 2) {
            throw new IllegalArgumentException("Number of nodes must be at least 2");
        }
        
        if (connectivity < 0.0 || connectivity > 1.0) {
            throw new IllegalArgumentException("Connectivity must be between 0.0 and 1.0");
        }
        
        // Create workflow structure
        GraphInfo graphInfo = new GraphInfo(name);
        NodeInfo[] nodes = new NodeInfo[numNodes];
        List<LinkInfo> links = new ArrayList<>();
        
        // Create nodes with random execution times
        for (int i = 0; i < numNodes; i++) {
            String nodeId;
            if (i == 0) {
                nodeId = "Start";
            } else if (i == numNodes - 1) {
                nodeId = "End";
            } else {
                nodeId = getNodeId(i);
            }
            
            int execTime = minExecTime + random.nextInt(maxExecTime - minExecTime + 1);
            nodes[i] = new NodeInfo(nodeId, execTime);
        }
        
        // Create a more vertical structure with fewer parallel paths
        // Connectivity affects how many layers - higher connectivity = fewer layers
        int layerInfluence = connectivity > 0.5 ? -1 : (connectivity < 0.2 ? 1 : 0);
        Map<Integer, List<Integer>> layerMap = createVerticalStructure(numNodes, layerInfluence);
        
        // Create primary flow path with minimal branching
        // Connectivity affects the probability of secondary connections
        createPrimaryFlowPaths(nodes, links, layerMap, connectivity, minDataAmount, maxDataAmount);
        
        // Add additional connections based on connectivity
        addAdditionalConnections(nodes, links, layerMap, connectivity, minDataAmount, maxDataAmount);
        
        // Ensure all nodes reach the end
        ensureAllNodesReachEnd(nodes, links, minDataAmount, maxDataAmount);
        
        // Return the workflow
        return new WorkflowJson(true, graphInfo, nodes, links.toArray(new LinkInfo[0]));
    }
    
    /**
     * Creates a more vertical structure with fewer nodes per layer
     */
    private Map<Integer, List<Integer>> createVerticalStructure(int numNodes, int layerInfluence) {
        Map<Integer, List<Integer>> layerMap = new HashMap<>();
        
        // Prefer more layers with fewer nodes per layer for a more vertical structure
        // Higher connectivity results in fewer layers (more width)
        // Lower connectivity results in more layers (more height)
        int numLayers = Math.max(4, (int)Math.sqrt(numNodes) * 2 + layerInfluence);
        
        // Start and End nodes are fixed
        layerMap.put(0, List.of(0)); // Start node
        layerMap.put(numLayers - 1, List.of(numNodes - 1)); // End node
        
        // Distribute remaining nodes with a preference for sequential flow
        int remainingNodes = numNodes - 2;
        int currentNode = 1;
        
        // Maximum nodes per layer - adjusted by layer influence
        int maxNodesPerLayer = Math.max(2, (int)Math.sqrt(numNodes) / 2 + (layerInfluence * -1));
        
        for (int layer = 1; layer < numLayers - 1 && remainingNodes > 0; layer++) {
            // Determine number of nodes in this layer
            // Limit layer width based on our parameters
            int nodesInLayer = Math.min(1 + random.nextInt(maxNodesPerLayer), remainingNodes);
            remainingNodes -= nodesInLayer;
            
            List<Integer> layerNodes = new ArrayList<>();
            for (int i = 0; i < nodesInLayer; i++) {
                layerNodes.add(currentNode++);
            }
            
            layerMap.put(layer, layerNodes);
        }
        
        // If we have remaining nodes, add one to each layer starting from the middle
        if (remainingNodes > 0) {
            int middleLayer = numLayers / 2;
            for (int layer = middleLayer; layer < numLayers - 1 && remainingNodes > 0; layer++) {
                List<Integer> layerNodes = layerMap.get(layer);
                layerNodes.add(currentNode++);
                remainingNodes--;
            }
            
            // Continue with earlier layers if needed
            for (int layer = middleLayer - 1; layer > 0 && remainingNodes > 0; layer--) {
                List<Integer> layerNodes = layerMap.get(layer);
                layerNodes.add(currentNode++);
                remainingNodes--;
            }
        }
        
        return layerMap;
    }
    
    /**
     * Creates primary flow paths with minimal branching
     */
    private void createPrimaryFlowPaths(
            NodeInfo[] nodes, 
            List<LinkInfo> links, 
            Map<Integer, List<Integer>> layerMap,
            double connectivity,
            int minDataAmount, 
            int maxDataAmount) {
        
        int numLayers = layerMap.size();
        
        // Track which nodes have incoming connections
        Set<Integer> nodesWithIncoming = new HashSet<>();
        nodesWithIncoming.add(0); // Start node doesn't need incoming connections
        
        // Calculate secondary connection probability based on connectivity
        double secondaryConnectionProb = 0.2 + connectivity * 0.5; // 0.2 to 0.7 based on connectivity
        
        // Create primarily linear paths through the workflow
        for (int layer = 1; layer < numLayers; layer++) {
            List<Integer> currentLayerNodes = layerMap.get(layer);
            List<Integer> previousLayerNodes = layerMap.get(layer - 1);
            
            // Strategy: Each node primarily receives from one source
            // If there are more nodes in this layer than previous, some sources feed multiple targets
            // If there are fewer nodes in this layer than previous, some sources won't feed forward
            
            // Distribute previous layer nodes as sources
            for (int targetIndex = 0; targetIndex < currentLayerNodes.size(); targetIndex++) {
                int targetNodeIdx = currentLayerNodes.get(targetIndex);
                
                // Determine source node - distribute evenly
                int sourceNodeIdx = previousLayerNodes.get(targetIndex % previousLayerNodes.size());
                
                // Create primary connection
                int dataAmount = minDataAmount + random.nextInt(maxDataAmount - minDataAmount + 1);
                links.add(new LinkInfo(nodes[sourceNodeIdx].getId(), nodes[targetNodeIdx].getId(), dataAmount));
                
                // Mark this node as having an incoming connection
                nodesWithIncoming.add(targetNodeIdx);
                
                // Add second source based on connectivity level
                if (previousLayerNodes.size() > 1 && random.nextDouble() < secondaryConnectionProb) {
                    // Find a different source
                    int secondSourceIndex;
                    do {
                        secondSourceIndex = random.nextInt(previousLayerNodes.size());
                    } while (previousLayerNodes.get(secondSourceIndex) == sourceNodeIdx);
                    
                    int secondSourceNodeIdx = previousLayerNodes.get(secondSourceIndex);
                    
                    // Create secondary connection with less data
                    int secondaryDataAmount = minDataAmount + random.nextInt((maxDataAmount - minDataAmount) / 2 + 1);
                    links.add(new LinkInfo(nodes[secondSourceNodeIdx].getId(), nodes[targetNodeIdx].getId(), secondaryDataAmount));
                }
            }
        }
        
        // Ensure all nodes (except start) have at least one incoming connection
        // This fixes any issues with disconnected nodes in any layer
        for (int i = 1; i < nodes.length; i++) {
            if (!nodesWithIncoming.contains(i)) {
                // Find a suitable source node from the previous layer
                int nodeLayer = -1;
                for (int layer = 1; layer < numLayers; layer++) {
                    if (layerMap.get(layer).contains(i)) {
                        nodeLayer = layer;
                        break;
                    }
                }
                
                if (nodeLayer > 0) {
                    List<Integer> previousLayerNodes = layerMap.get(nodeLayer - 1);
                    if (!previousLayerNodes.isEmpty()) {
                        int sourceNodeIdx = previousLayerNodes.get(random.nextInt(previousLayerNodes.size()));
                        int dataAmount = minDataAmount + random.nextInt(maxDataAmount - minDataAmount + 1);
                        links.add(new LinkInfo(nodes[sourceNodeIdx].getId(), nodes[i].getId(), dataAmount));
                        nodesWithIncoming.add(i);
                    }
                }
            }
        }
    }
    
    /**
     * Adds a very limited number of additional connections
     */
    private void addAdditionalConnections(
            NodeInfo[] nodes, 
            List<LinkInfo> links, 
            Map<Integer, List<Integer>> layerMap,
            double connectivity,
            int minDataAmount, 
            int maxDataAmount) {
        
        int numLayers = layerMap.size();
        int endNodeIndex = nodes.length - 1;
        
        // Skip layer connections - probability directly affected by connectivity
        // and max skip distance influenced by connectivity
        int maxSkipDistance = 1 + (int)(connectivity * 2); // 1-3 layers depending on connectivity
        double skipConnectionProb = connectivity * 0.3; // 0-30% chance based on connectivity
        
        for (int sourceLayer = 0; sourceLayer < numLayers - 2; sourceLayer++) {
            List<Integer> sourceLayerNodes = layerMap.get(sourceLayer);
            
            // For each potential target layer within skip distance
            for (int skipDist = 2; skipDist <= Math.min(maxSkipDistance, numLayers - sourceLayer - 1); skipDist++) {
                int targetLayer = sourceLayer + skipDist;
                List<Integer> targetLayerNodes = layerMap.get(targetLayer);
                
                // Reduce probability for longer skips
                double adjustedProb = skipConnectionProb / skipDist;
                
                for (int sourceNodeIdx : sourceLayerNodes) {
                    // Try to create skip connection based on connectivity
                    if (random.nextDouble() < adjustedProb) {
                        // Pick one random target in the target layer
                        if (!targetLayerNodes.isEmpty()) {
                            int targetNodeIdx = targetLayerNodes.get(random.nextInt(targetLayerNodes.size()));
                            
                            // Don't connect to end node here
                            if (targetNodeIdx != endNodeIndex) {
                                // Check if connection already exists
                                boolean exists = connectionExists(links, nodes[sourceNodeIdx].getId(), 
                                                                nodes[targetNodeIdx].getId());
                                
                                if (!exists) {
                                    int dataAmount = minDataAmount + random.nextInt((maxDataAmount - minDataAmount) / 2 + 1);
                                    links.add(new LinkInfo(nodes[sourceNodeIdx].getId(), 
                                                         nodes[targetNodeIdx].getId(), dataAmount));
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Connect all penultimate layer nodes to end
        List<Integer> penultimateLayerNodes = layerMap.get(numLayers - 2);
        for (int nodeIdx : penultimateLayerNodes) {
            // Ensure connection exists
            boolean exists = connectionExists(links, nodes[nodeIdx].getId(), nodes[endNodeIndex].getId());
            
            if (!exists) {
                int dataAmount = minDataAmount + random.nextInt(maxDataAmount - minDataAmount + 1);
                links.add(new LinkInfo(nodes[nodeIdx].getId(), nodes[endNodeIndex].getId(), dataAmount));
            }
        }
        
        // Add direct connections to end from earlier layers - based on connectivity
        int earlierConnections = Math.max(1, (int)(nodes.length / 10 * connectivity) + 1);
        if (earlierConnections > 0 && numLayers > 3) {
            // Consider nodes from layers not adjacent to start or end
            List<Integer> candidates = new ArrayList<>();
            for (int layer = 1; layer < numLayers - 2; layer++) {
                candidates.addAll(layerMap.get(layer));
            }
            
            // Shuffle and take the first few
            for (int i = candidates.size() - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                int temp = candidates.get(i);
                candidates.set(i, candidates.get(j));
                candidates.set(j, temp);
            }
            
            // Create direct connections for these selected nodes
            for (int i = 0; i < Math.min(earlierConnections, candidates.size()); i++) {
                int nodeIdx = candidates.get(i);
                
                // Check if connection already exists
                boolean exists = connectionExists(links, nodes[nodeIdx].getId(), nodes[endNodeIndex].getId());
                
                if (!exists) {
                    int dataAmount = minDataAmount + random.nextInt((maxDataAmount - minDataAmount) / 3 + 1);
                    links.add(new LinkInfo(nodes[nodeIdx].getId(), nodes[endNodeIndex].getId(), dataAmount));
                }
            }
        }
    }
    
    /**
     * Ensures all nodes have a path to the end node
     */
    private void ensureAllNodesReachEnd(
            NodeInfo[] nodes, 
            List<LinkInfo> links, 
            int minDataAmount, 
            int maxDataAmount) {
        
        int endNodeIndex = nodes.length - 1;
        
        // First, ensure all nodes have at least one incoming connection (except Start)
        ensureAllNodesHaveIncomingConnections(nodes, links, minDataAmount, maxDataAmount);
        
        // Build a reverse reachability map from the end node
        boolean[] canReachEnd = new boolean[nodes.length];
        canReachEnd[endNodeIndex] = true;
        
        // Use breadth-first search from the end node (backwards)
        Queue<Integer> queue = new LinkedList<>();
        queue.add(endNodeIndex);
        
        // Mapping from node ID to index
        Map<String, Integer> idToIndex = new HashMap<>();
        for (int i = 0; i < nodes.length; i++) {
            idToIndex.put(nodes[i].getId(), i);
        }
        
        // BFS to mark all nodes that can reach the end
        while (!queue.isEmpty()) {
            int targetIdx = queue.poll();
            
            // Find all sources that connect to this target
            for (LinkInfo link : links) {
                if (link.getTarget().equals(nodes[targetIdx].getId())) {
                    int sourceIdx = idToIndex.get(link.getSource());
                    
                    if (!canReachEnd[sourceIdx]) {
                        canReachEnd[sourceIdx] = true;
                        queue.add(sourceIdx);
                    }
                }
            }
        }
        
        // Fix any nodes that cannot reach the end - prefer connecting to the next layer
        for (int i = 0; i < nodes.length; i++) {
            if (!canReachEnd[i] && i != endNodeIndex) {
                // Find a suitable target to connect to - prefer next layer
                int targetIdx = findNearestForwardNodeThatReachesEnd(nodes, links, i, canReachEnd, idToIndex);
                
                // Create a connection
                int dataAmount = minDataAmount + random.nextInt((maxDataAmount - minDataAmount) / 2 + 1);
                links.add(new LinkInfo(nodes[i].getId(), nodes[targetIdx].getId(), dataAmount));
            }
        }
    }
    
    /**
     * Ensures all nodes (except Start) have at least one incoming connection
     */
    private void ensureAllNodesHaveIncomingConnections(
            NodeInfo[] nodes, 
            List<LinkInfo> links, 
            int minDataAmount, 
            int maxDataAmount) {
        
        // Mapping from node ID to index
        Map<String, Integer> idToIndex = new HashMap<>();
        for (int i = 0; i < nodes.length; i++) {
            idToIndex.put(nodes[i].getId(), i);
        }
        
        // Check each node for incoming connections
        boolean[] hasIncoming = new boolean[nodes.length];
        hasIncoming[0] = true; // Start node doesn't need incoming
        
        // Mark nodes with incoming connections
        for (LinkInfo link : links) {
            int targetIdx = idToIndex.get(link.getTarget());
            hasIncoming[targetIdx] = true;
        }
        
        // Find nodes without incoming connections and create them
        for (int i = 1; i < nodes.length; i++) {
            if (!hasIncoming[i]) {
                // Need to create an incoming connection for this node
                
                // Find a suitable source node that isn't itself
                List<Integer> potentialSources = new ArrayList<>();
                for (int j = 0; j < i; j++) {
                    // Only consider nodes that come before this one to avoid cycles
                    potentialSources.add(j);
                }
                
                if (!potentialSources.isEmpty()) {
                    // Pick a random source from potential sources
                    int sourceIdx = potentialSources.get(random.nextInt(potentialSources.size()));
                    
                    // Create a connection
                    int dataAmount = minDataAmount + random.nextInt(maxDataAmount - minDataAmount + 1);
                    links.add(new LinkInfo(nodes[sourceIdx].getId(), nodes[i].getId(), dataAmount));
                    hasIncoming[i] = true;
                    
                    System.out.println("Fixed missing incoming connection for node: " + nodes[i].getId() +
                                     " by connecting from: " + nodes[sourceIdx].getId());
                }
            }
        }
    }
    
    /**
     * Finds the nearest forward node that can reach the end
     */
    private int findNearestForwardNodeThatReachesEnd(
            NodeInfo[] nodes, 
            List<LinkInfo> links, 
            int sourceIdx,
            boolean[] canReachEnd,
            Map<String, Integer> idToIndex) {
        
        int endNodeIndex = nodes.length - 1;
        
        // Get all existing outgoing connections
        Set<Integer> existingTargets = new HashSet<>();
        for (LinkInfo link : links) {
            if (link.getSource().equals(nodes[sourceIdx].getId())) {
                existingTargets.add(idToIndex.get(link.getTarget()));
            }
        }
        
        // If very few connections, connect directly to end
        if (existingTargets.isEmpty() || random.nextDouble() < 0.2) {
            return endNodeIndex;
        }
        
        // Try to find nodes in the next layer that can reach the end
        // (Use both existing connections and the layer map to infer current layer)
        // Since we don't have direct layer information of sourceIdx, infer from connections
        
        // Find nodes that are connected and can reach the end
        List<Integer> reachableTargets = new ArrayList<>();
        for (int i = 0; i < nodes.length; i++) {
            if (canReachEnd[i] && i != sourceIdx && i != endNodeIndex &&
                !connectionExists(links, nodes[sourceIdx].getId(), nodes[i].getId())) {
                reachableTargets.add(i);
            }
        }
        
        if (!reachableTargets.isEmpty()) {
            return reachableTargets.get(random.nextInt(reachableTargets.size()));
        }
        
        // Fall back to the end node
        return endNodeIndex;
    }
    
    /**
     * Checks if a connection already exists between two nodes
     */
    private boolean connectionExists(List<LinkInfo> links, String sourceId, String targetId) {
        for (LinkInfo link : links) {
            if (link.getSource().equals(sourceId) && link.getTarget().equals(targetId)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gets a node ID for a node at a specific index
     * For the first 26 nodes, uses alphabetical IDs (A-Z)
     * For subsequent nodes, uses Node1, Node2, etc.
     * 
     * @param index The index of the node
     * @return A node ID
     */
    private String getNodeId(int index) {
        if (index < 26) {
            // A-Z for first 26 nodes
            return String.valueOf((char)('A' + index));
        } else {
            // Node1, Node2, etc. for subsequent nodes
            return "Node" + (index - 25);
        }
    }
    
    /**
     * Gets the random seed being used by this generator
     * 
     * @return The random seed
     */
    public int getSeed() {
        return seed;
    }
}