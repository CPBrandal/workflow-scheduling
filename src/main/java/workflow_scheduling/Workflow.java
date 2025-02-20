/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package workflow_scheduling;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;

/**
 *
 * @author philip
 */
public class Workflow {
    public static class Task {
        private final String id;
        private int executionTime;

        public Task(String id, int executionTime) {
            this.id = id;
            this.executionTime = executionTime;
        }

        @Override
        public String toString() {
            return id;
        }
    }

    public static void main(String[] args) {
        // Create a directed acyclic graph
        DirectedAcyclicGraph<Task, DefaultEdge> workflow = 
            new DirectedAcyclicGraph<>(DefaultEdge.class);

        // Create tasks
        Task taskA = new Task("A", 10);
        Task taskB = new Task("B", 25);
        Task taskC = new Task("C", 20);
        Task taskD = new Task("D", 40);
        Task taskE = new Task("E", 15);
        Task taskF = new Task("F", 30);
        Task taskG = new Task("G", 5);

        // Add vertices (tasks)
        workflow.addVertex(taskA);
        workflow.addVertex(taskB);
        workflow.addVertex(taskC);
        workflow.addVertex(taskD);
        workflow.addVertex(taskE);
        workflow.addVertex(taskF);
        workflow.addVertex(taskG);

        // Add edges (dependencies)
        workflow.addEdge(taskA, taskB);  // A -> B
        workflow.addEdge(taskA, taskC);  // A -> C
        workflow.addEdge(taskB, taskD);  // B -> D
        workflow.addEdge(taskC, taskD);  // C -> D
        workflow.addEdge(taskB, taskE);  // B -> E
        workflow.addEdge(taskE, taskF);  // E -> F
        workflow.addEdge(taskD, taskF);  // D -> F
        workflow.addEdge(taskF, taskG);  // F -> G

        // Now you have a complete workflow graph!
        System.out.println("Number of tasks: " + workflow.vertexSet().size());
        System.out.println("Number of dependencies: " + workflow.edgeSet().size());
    }
}
