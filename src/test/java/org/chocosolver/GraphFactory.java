package org.chocosolver;

import org.chocosolver.solver.Model;
import org.chocosolver.util.objects.graphs.DirectedGraph;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;


public class GraphFactory {

    /**
     * Creates a backtrackable undirected graph from an array of nodes and an array of couples (edges).
     * @param model The Choco model (providing the backtracking environment).
     * @param n The max number of nodes.
     * @param type Data structure for storing nodes' neighbors.
     * @param nodes An array of integers containing the nodes to initialize the graph with.
     * @param edges An array of integer couples describing the edges to initialize the graph with.
     * @return A backtrackable undirected graph initialized with nodes and edges.
     */
    public static UndirectedGraph makeUndirectedGraph(Model model, int n, SetType type, int[] nodes, int[][] edges) {
        UndirectedGraph graph = new UndirectedGraph(model, n, type, false);
        Arrays.stream(nodes).forEach(i -> graph.addNode(i));
        Arrays.stream(edges).forEach(e -> graph.addEdge(e[0], e[1]));
        return graph;
    }

    /**
     * Creates a backtrackable undirected graph from an array of nodes and an boolean matrix (edges).
     * @param model The Choco model (providing the backtracking environment).
     * @param n The max number of nodes.
     * @param type Data structure for storing nodes' neighbors.
     * @param nodes An array of integers containing the nodes to initialize the graph with.
     * @param edges An matrix of boolean describing the edges to initialize the graph with.
     * @return A backtrackable undirected graph initialized with nodes and edges.
     */
    public static UndirectedGraph makeUndirectedGraph(Model model, int n, SetType type, int[] nodes,
                                                      boolean[][] edges) {
        assert (edges.length <= n && nodes.length <= n);
        UndirectedGraph graph = new UndirectedGraph(model, n, type, false);
        Arrays.stream(nodes).forEach(i -> graph.addNode(i));
        for (int i = 0; i < nodes.length; i ++) {
            for (int j = i; j < nodes.length; j++) {
                if (edges[i][j]) {
                    graph.addEdge(i, j);
                }
            }
        }
        return graph;
    }

    /**
     * Creates a backtrackable directed graph from an array of nodes and an array of couples (arcs).
     * @param model The Choco model (providing the backtracking environment).
     * @param n The max number of nodes.
     * @param type Data structure for storing nodes' neighbors.
     * @param nodes An array of integers containing the nodes to initialize the graph with.
     * @param arcs An array of integer couples describing the arcs to initialize the graph with.
     * @return A backtrackable directed graph initialized with nodes and arcs.
     */
    public static DirectedGraph makeDirectedGraph(Model model, int n, SetType type, int[] nodes, int[][] arcs) {
        DirectedGraph graph = new DirectedGraph(model, n, type, false);
        Arrays.stream(nodes).forEach(i -> graph.addNode(i));
        Arrays.stream(arcs).forEach(a -> graph.addArc(a[0], a[1]));
        return graph;
    }

    /**
     * Creates a backtrackable directed graph from an array of nodes and an boolean matrix (arcs).
     * @param model The Choco model (providing the backtracking environment).
     * @param n The max number of nodes.
     * @param type Data structure for storing nodes' neighbors.
     * @param nodes An array of integers containing the nodes to initialize the graph with.
     * @param arcs A boolean matrix describing the arcs to initialize the graph with.
     * @return A backtrackable directed graph initialized with nodes and arcs.
     */
    public static DirectedGraph makeDirectedGraph(Model model, int n, SetType type, int[] nodes, boolean[][] arcs) {
        assert (nodes.length == arcs.length);
        DirectedGraph graph = new DirectedGraph(model, n, type, false);
        Arrays.stream(nodes).forEach(i -> graph.addNode(i));
        for (int i : nodes) {
            for (int j : nodes) {
                if (arcs[i][j]) {
                    graph.addArc(i, j);
                }
            }
        }
        return graph;
    }

    public static UndirectedGraph makeRandomUndirectedGraphFromDensity(Model model, int n, SetType type,
                                                                       int nbNodes, double density) {
        assert (n >= nbNodes);
        int[] nodes = IntStream.range(0, nbNodes).toArray();
        boolean[][] edges = generateUndirectedAdjacencyMatrix(nbNodes, density);
        return makeUndirectedGraph(model, n, type, nodes, edges);
    }

    public static UndirectedGraph makeRandomUndirectedGraphFromNbCC(Model model, int n, SetType type, int nbCC,
                                                                    double density, int maxSizeCC) {
        assert (nbCC <= n);
        int remaining = n;
        int next_node = 0;
        boolean[][] edges = new boolean[n][n];
        for (int cc = 0; cc < nbCC; cc++) {
            int size = ThreadLocalRandom.current().nextInt(1, Math.min(remaining - nbCC + cc + 1, maxSizeCC));
            remaining -= size;
            boolean[][] adj = generateUndirectedAdjacencyMatrix(size, density);
            for (int i = 0; i < size; i++) {
                for (int j = i; j < size; j++) {
                    edges[i + next_node][j + next_node] = adj[i][j];
                }
            }
            next_node += size;
        }
        next_node = (next_node == 0) ? 1 : next_node;
        return makeUndirectedGraph(model, n, type, IntStream.range(0, next_node).toArray(), edges);
    }

    /**
     * Randomly generate an undirected graph adjacency matrix from a given density.
     * @param n The number of nodes
     * @param density The wanted density
     * @return
     */
    public static boolean[][] generateUndirectedAdjacencyMatrix(int n, double density) {
        assert (density >= 0 && density <= 1);
        int[] nodes = IntStream.range(0, n).toArray();
        boolean[][] edges = new boolean[n][n];
        for (int i : nodes) {
            for (int j = i; j < n; j++) {
                double r = Math.random();
                if (r < density) {
                    edges[i][j] = true;
                }
            }
        }
        return edges;
    }
}

