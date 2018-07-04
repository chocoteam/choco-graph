package org.chocosolver.utils;

import gnu.trove.list.array.TIntArrayList;
import org.chocosolver.solver.Model;
import org.chocosolver.util.objects.graphs.DirectedGraph;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;


/**
 * Graph Generator for test purposes.
 *
 * @author Jean-Guillaume Fages
 * @author Dimitri Justeau-Allaire
 */
public class GraphGenerator {

	public enum InitialProperty {
		HamiltonianCircuit, Tree, None
	}

	/**
	 * Creates a backtrackable undirected graph from an array of nodes and an array of couples (edges).
	 *
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
	 *
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
	 *
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
	 *
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

	/**
	 * Generate a random undirected graph (backtrackable) containing nbCC connected components.

	 * @param model The Choco model (providing the backtracking environment).
	 * @param n The max number of nodes.
	 * @param type Data structure for storing nodes' neighbors.
	 * @param nbCC The number of connected components.
	 * @param density The wanted density of the connected components
	 * @param maxSizeCC The maximum size of the CCs.
	 * @return A randomly generated undirected graph (backtrackable) containing nbCC connected components.
	 */
	public static UndirectedGraph generateRandomUndirectedGraphFromNbCC(Model model, int n, SetType type, int nbCC,
																		double density, int maxSizeCC) {
		assert (nbCC <= n);
		int remaining = n;
		int next_node = 0;
		boolean[][] edges = new boolean[n][n];
		for (int cc = 0; cc < nbCC; cc++) {
			int size = ThreadLocalRandom.current().nextInt(1, Math.min(remaining - nbCC + cc + 1, maxSizeCC));
			remaining -= size;
			boolean[][] adj = generateRandomUndirectedAdjacencyMatrix(size, density);
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
	public static boolean[][] generateRandomUndirectedAdjacencyMatrix(int n, double density) {
		assert (density >= 0 && density <= 1);
		int[] nodes = IntStream.range(0, n).toArray();
		boolean[][] edges = new boolean[n][n];
		for (int i : nodes) {
			for (int j : nodes) {
				double r = Math.random();
				if (r < density) {
					edges[i][j] = true;
					edges[j][i] = true;
				}
			}
		}
		return edges;
	}

	/**
	 * randomly generate a boolean matrix representing a directed graph
	 *
	 * @param density arc ratio among all the possible
	 * @param rand A Random generator
	 * @param prop property insured by the generator
	 * @return a boolean matrix
	 */
	public static boolean[][] arcBasedGenerator(int size, double density, InitialProperty prop, Random rand) {
		boolean[][] graph;
		switch (prop) {
			case HamiltonianCircuit:
				graph = generateInitialHamiltonianCircuit(size, rand);
				break;
			case Tree:
				graph = generateInitialTree(size, rand);
				break;
			default:
				graph = new boolean[size][size];
				break;
		}
		// on ajoute des arcs
		int nb = (int) (density * ((size * size) - size));
		int cur = 0;
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				if (graph[i][j]) {
					cur++;
				}
			}
		}
		nb = nb - cur;
		while (nb > 0) {
			int i = rand.nextInt(size);
			BitSet seti = new BitSet(size);
			for (int j = 0; j < size; j++) {
				if (graph[i][j]) {
					seti.set(j, false);
				} else {
					seti.set(j, true);
				}
			}
			seti.set(i, false);
			int y = rand.nextInt(size);
			int j = seti.nextSetBit(y);
			if (j == -1) {
				j = seti.nextSetBit(0);
			}
			if (j != -1) {
				seti.set(j, false);
				graph[i][j] = true;
				nb--;
			}
		}
		return graph;
	}

	/**
	 * randomly generate a boolean matrix representing a directed graph
	 *
	 * @param size number of nodes in the directed graph generated
	 * @param rand A Random generator
	 * @param prop property insured by the generator
	 * @param nb number of neighbor for each node, necessarily < size
	 * @return a boolean matrix
	 */
	public static boolean[][] neighborBasedGenerator(int size, int nb, InitialProperty prop, Random rand) {
		boolean[][] graph;
		switch (prop) {
			case HamiltonianCircuit:
				graph = generateInitialHamiltonianCircuit(size, rand);
				break;
			case Tree:
				graph = generateInitialTree(size, rand);
				break;
			default:
				graph = new boolean[size][size];
				break;
		}		// on ajoute des arcs: exactement nb pour chaque sommet
		for (int i = 0; i < size; i++) {
			int ni = 0;
			BitSet seti = new BitSet(size);
			for (int j = 0; j < size; j++) {
				if (graph[i][j]) {
					seti.set(j, false);
					ni++;
				} else {
					seti.set(j, true);
				}
			}
			seti.set(i, false);
			int ti = nb - ni;
			while (ti > 0) {
				int y = rand.nextInt(size);
				int j = seti.nextSetBit(y);
				if (j == -1) {
					j = seti.nextSetBit(0);
				}
				seti.set(j, false);
				graph[i][j] = true;
				ti--;
			}
		}
		return graph;
	}

	/**
	 * Provide an initial Hamiltonian circuit in graph
	 */
	private static boolean[][] generateInitialHamiltonianCircuit(int size, Random rand) {
		boolean[][] graph = new boolean[size][size];
		TIntArrayList nodes = new TIntArrayList(size);
		for (int i = 0; i < size; i++) {
			nodes.add(i);
		}
		nodes.shuffle(rand);
		for (int i = 0; i < size - 1; i++) {
			graph[nodes.get(i)][nodes.get(i + 1)] = true;
		}
		graph[nodes.get(size - 1)][nodes.get(0)] = true;
		return graph;
	}

	/**
	 * Provide an initial tree in graph
	 */
	private static boolean[][] generateInitialTree(int size, Random rand) {
		boolean[][] graph = new boolean[size][size];
		BitSet notIn = new BitSet(size);
		BitSet in = new BitSet(size);
		for (int i = 0; i < size; i++) {
			notIn.set(i, true);
			in.set(i, false);
		}
		while (notIn.cardinality() > 0) {
			int i = pickOneTrue(notIn, rand);
			notIn.set(i, false);
			// relier i a un sommet de in quelconque.
			int j;
			int sj = rand.nextInt(size);
			j = in.nextSetBit(sj);
			if (j == -1) {
				j = in.nextSetBit(0);
			}
			// cas du premier sommet ajoute dans in
			if (j > -1) {
				// pas de pbs car i et j ne peuvent pas etre tous deux dans in
				graph[i][j] = true;
			}
			in.set(i, true);
		}
		return graph;
	}

	private static int pickOneTrue(BitSet tab, Random rand) {
		int start = rand.nextInt(tab.length());
		int i = tab.nextSetBit(start);
		if (i == -1) {
			i = tab.nextSetBit(0);
		}
		return i;
	}
}

