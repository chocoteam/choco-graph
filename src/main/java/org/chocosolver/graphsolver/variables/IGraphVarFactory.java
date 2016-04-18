package org.chocosolver.graphsolver.variables;

import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.objects.graphs.DirectedGraph;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;

import java.util.Arrays;

public interface IGraphVarFactory {

	GraphModel _me();

	//*************************************************************************************
	// GRAPH VARIABLES CREATION
	//*************************************************************************************

	/**
	 * Creates a graph variable comprised between an empty graph and K_n (complete graph of n vertices)
	 * @param NAME	Name of the variable
	 * @param n		Maximum number of vertices
	 * @return a graph variable having n vertices
	 */
	default IUndirectedGraphVar undirected_graph_var(String NAME, int n) {
		return undirected_graph_var(NAME,n,false);
	}

	/**
	 * Creates a graph variable comprised between an empty graph and K_n (complete graph of n vertices)
	 * @param NAME	Name of the variable
	 * @param n		Maximum number of vertices
	 * @param allNodes If true then every vertex in [0,n-1] belongs to every solution.
	 * @return a graph variable having n vertices
	 */
	default IUndirectedGraphVar undirected_graph_var(String NAME, int n, boolean allNodes) {
		UndirectedGraph LB = new UndirectedGraph(_me(), n,SetType.BITSET,allNodes);
		UndirectedGraph UB = new UndirectedGraph(_me(), n,SetType.BITSET,allNodes);
		for(int i=0;i<n;i++){
			if(!allNodes) {
				UB.addNode(i);
			}
			for(int j=i;j<n;j++){
				UB.addEdge(i,j);
			}
		}
		return undirected_graph_var(NAME, LB, UB);
	}

	/**
	 * Create an undirected graph variable named NAME
	 * and whose domain is the graph interval [LB,UB]
	 * BEWARE: LB and UB graphs must be backtrackable
	 * (use the solver as an argument in their constructor)!
	 *
	 * @param NAME		Name of the variable
	 * @param LB		Undirected graph representing mandatory nodes and edges
	 * @param UB		Undirected graph representing possible nodes and edges
	 * @return	An undirected graph variable
	 */
	default IUndirectedGraphVar undirected_graph_var(String NAME, UndirectedGraph LB, UndirectedGraph UB) {
		return new UndirectedGraphVar(NAME, _me(), LB, UB);
	}

	/**
	 * Creates a directed graph variable comprised between an empty graph and K_n (complete graph of n vertices)
	 * @param NAME	Name of the variable
	 * @param n		Maximum number of vertices
	 * @return a directed graph variable having n vertices
	 */
	default IDirectedGraphVar directed_graph_var(String NAME, int n) {
		return directed_graph_var(NAME,n,false);
	}

	/**
	 * Creates a directed graph variable comprised between an empty graph and K_n (complete graph of n vertices)
	 * @param NAME	Name of the variable
	 * @param n		Maximum number of vertices
	 * @param allNodes If true then every vertex in [0,n-1] belongs to every solution.
	 * @return a directed graph variable having n vertices
	 */
	default IDirectedGraphVar directed_graph_var(String NAME, int n, boolean allNodes) {
		DirectedGraph LB = new DirectedGraph(_me(), n,SetType.BITSET,allNodes);
		DirectedGraph UB = new DirectedGraph(_me(), n,SetType.BITSET,allNodes);
		for(int i=0;i<n;i++){
			if(!allNodes) {
				UB.addNode(i);
			}
			for(int j=0;j<n;j++){
				UB.addArc(i,j);
			}
		}
		return directed_graph_var(NAME, LB, UB);
	}

	/**
	 * Create a directed graph variable named NAME
	 * and whose domain is the graph interval [LB,UB]
	 * BEWARE: LB and UB graphs must be backtrackable
	 * (use the solver as an argument in their constructor)!
	 *
	 * @param NAME		Name of the variable
	 * @param LB		Directed graph representing mandatory nodes and edges
	 * @param UB		Directed graph representing possible nodes and edges
	 * @return	An undirected graph variable
	 */
	default IDirectedGraphVar directed_graph_var(String NAME, DirectedGraph LB, DirectedGraph UB) {
		return new DirectedGraphVar(NAME, _me(), LB, UB);
	}

	//*************************************************************************************
	// OTHER
	//*************************************************************************************

	/**
	 * Iterate over the variable of <code>this</code> and build an array that contains the GraphVar only.
	 * It also contains FIXED variables and VIEWS, if any.
	 *
	 * @return array of SetVars of <code>this</code>
	 */
	default IGraphVar[] retrieveGraphVars(Model s) {
		int n = s.getNbVars();
		IGraphVar[] bvars = new IGraphVar[n];
		int k = 0;
		for (int i = 0; i < n; i++) {
			if ((s.getVar(i).getTypeAndKind() & Variable.KIND) == GraphVar.GRAPH) {
				bvars[k++] = (IGraphVar) s.getVar(i);
			}
		}
		return Arrays.copyOf(bvars, k);
	}

	//***********************************************************************************
	// SIMPLE COUNTS
	//***********************************************************************************

	/**
	 * Creates an integer variable representing the number of nodes in g
	 * @param g	a graph variable
	 * @return An integer variable representing the number of nodes in g
	 */
	default IntVar nb_nodes(IGraphVar g){
		IntVar nb = g.getModel().intVar("nb_nodes",0,g.getNbMaxNodes(),true);
		_me().nb_nodes(g, nb).post();
		return nb;
	}

	/**
	 * Creates an integer variable representing the number of nodes in g
	 * @param g	a directed graph variable
	 * @return An integer variable representing the number of nodes in g
	 */
	default IntVar nb_arcs(IDirectedGraphVar g){
		IntVar nb = _me().intVar("nb_arcs",0,g.getNbMaxNodes()*g.getNbMaxNodes(),true);
		_me().nb_arcs(g, nb).post();
		return nb;
	}

	/**
	 * Creates an integer variable representing the number of nodes in g
	 * @param g	an undirected graph variable
	 * @return An integer variable representing the number of nodes in g
	 */
	default IntVar nb_edges(IUndirectedGraphVar g){
		IntVar nb = _me().intVar("nb_edges",0,g.getNbMaxNodes()*g.getNbMaxNodes(),true);
		_me().nb_edges(g, nb).post();
		return nb;
	}

	/**
	 * Creates an integer variable representing the number of loops in g
	 * IntVar = |(i,i) in g|
	 * @param g	a graph variable
	 * @return An integer variable representing the number of loops in g
	 */
	default IntVar nb_loops(IGraphVar g){
		IntVar nb = _me().intVar("nb_loops",0,g.getNbMaxNodes(),true);
		_me().nb_loops(g,nb).post();
		return nb;
	}

	/**
	 * Creates a set variable representing nodes of g that have a loop,
	 * i.e. nodes 'i' having an arc of the form (i,i)
	 * @param g	a graph variable
	 * @return a set variable representing nodes of g that have a loop
	 */
	default SetVar loop_set(IGraphVar g){
		SetVar l = makeSetVar("loops",0,g.getNbMaxNodes());
		_me().loop_set(g,l).post();
		return l;
	}

	//***********************************************************************************
	// CHANNELING VARIABLES
	//***********************************************************************************

	// Vertices

	/**
	 * Creates a set variable representing the nodes of g
	 * @param g	a graph variable
	 * @return	a set variable representing the nodes of g
	 */
	default SetVar nodes_set(IGraphVar g){
		int n = g.getNbMaxNodes();
		SetVar nodes = makeSetVar("nodes",0,n-1);
		_me().nodes_channeling(g, nodes).post();
		return nodes;
	}

	/**
	 * Creates an array of boolean variables representing the nodes of g
	 * @param g	a graph variable
	 * @return	an array of boolean variables representing the nodes of g
	 */
	default BoolVar[] nodes_bool_array(IGraphVar g){
		BoolVar[] nodes = _me().boolVarArray("nodes",g.getNbMaxNodes());
		_me().nodes_channeling(g, nodes).post();
		return nodes;
	}

	/**
	 * Creates a boolean variable representing if the node 'vertex' is in g or not
	 * @param g	a graph variable
	 * @param vertex index of a potential vertex of g
	 * @return a boolean variable representing if the node 'vertex' is in g or not
	 */
	default BoolVar node_bool(IGraphVar g, int vertex){
		BoolVar node = _me().boolVar("nodes("+vertex+")");
		_me().node_channeling(g, node, vertex).post();
		return node;
	}

	// Arc


	/**
	 * Creates a boolean variable representing if the arc '(from,to)' is in g or not
	 * @param g	a directed graph variable
	 * @param from index of a potential vertex of g
	 * @param to index of a potential vertex of g
	 * @return a boolean variable representing if the arc '(from,to)' is in g or not
	 */
	default BoolVar arc_bool(IDirectedGraphVar g, int from, int to){
		BoolVar node = _me().boolVar("arc("+from+","+to+")");
		_me().arc_channeling(g, node, from, to).post();
		return node;
	}

	// Edge

	/**
	 * Creates a boolean variable representing if the edge '(v1,v2)' is in g or not
	 * @param g	an undirected graph variable
	 * @param v1 index of a potential vertex of g
	 * @param v2 index of a potential vertex of g
	 * @return a boolean variable representing if the edge '(v1,v2)' is in g or not
	 */
	default BoolVar edge_bool(IUndirectedGraphVar g, int v1, int v2){
		BoolVar node = _me().boolVar("edge("+v1+","+v2+")");
		_me().edge_channeling(g, node, v1, v2).post();
		return node;
	}

	// Neighbors

	/**
	 * Creates an array of integer variables representing the unique successor of each vertex
	 * This implicitly creates an orientation (even though the graph variable is undirected)
	 * IntVar[i] = j OR IntVar[j] = i <=> (i,j) in g
	 * @param g	an undirected graph variable having an orientation with exactly one successor per vertex
	 *          and for which every vertex is mandatory
	 * @return an array of integer variables representing the unique successor of each vertex
	 */
	default IntVar[] neigh_int_array(IUndirectedGraphVar g){
		int n = g.getNbMaxNodes();
		IntVar[] successors = _me().intVarArray("neighOf",n,0,n-1,false);
		_me().neighbors_channeling(g, successors).post();
		return successors;
	}

	/**
	 * Creates an array of set variables representing the neighborhood of every vertex
	 * int j in SetVar[i] <=> (i,j) in g
	 * @param g	an undirected graph variable
	 * @return an array of set variables representing the neighborhood of every vertex
	 */
	default SetVar[] neigh_set_array(IUndirectedGraphVar g){
		int n = g.getNbMaxNodes();
		SetVar[] neighbors = new SetVar[n];
		for(int i=0;i<n;i++){
			neighbors[i] = makeSetVar("neighOf("+i+")",0,n-1);
		}
		_me().neighbors_channeling(g, neighbors).post();
		return neighbors;
	}

	/**
	 * Creates a matrix of boolean variables representing the adjacency matrix of g
	 * BoolVar[i][j] = 1 <=> (i,j) in g
	 * @param g	an undirected graph variable
	 * @return a matrix of boolean variables representing the adjacency matrix of g
	 */
	default BoolVar[][] neigh_bool_matrix(IUndirectedGraphVar g){
		int n = g.getNbMaxNodes();
		BoolVar[][] neighbors = _me().boolVarMatrix("neighOf",n,n);
		_me().neighbors_channeling(g, neighbors).post();
		return neighbors;
	}

	/**
	 * Creates a set variable representing the neighborhood of 'node' in g
	 * int j in SetVar <=> (node,j) in g
	 * @param g	an undirected graph variable
	 * @return a set variable representing the neighborhood of 'node' in g
	 */
	default SetVar neighOf_set(IUndirectedGraphVar g, int node){
		int n = g.getNbMaxNodes();
		SetVar neighborsOf = makeSetVar("neighOf("+node+")",0,n-1);
		_me().neighbors_channeling(g, neighborsOf, node).post();
		return neighborsOf;
	}

	/**
	 * Creates an array of boolean variables representing the neighborhood of 'node' in g
	 * BoolVar[j] = 1 <=> (node,j) in g
	 * @param g	an undirected graph variable
	 * @return an array of boolean variables representing the neighborhood of 'node' in g
	 */
	default BoolVar[] neighborsChanneling(IUndirectedGraphVar g, int node){
		BoolVar[] neighborsOf = _me().boolVarArray("neighOf("+node+")",g.getNbMaxNodes());
		_me().neighbors_channeling(g, neighborsOf, node).post();
		return neighborsOf;
	}

	// Successors

	/**
	 * Creates an array of integer variables representing the unique successor of each vertex
	 * IntVar[i] = j <=> (i,j) in g
	 * @param g	a directed graph variable having exactly one successor per vertex and for which every
	 *          vertex is mandatory
	 * @return an array of integer variables representing the unique successor of each vertex
	 */
	default IntVar[] succ_int_array(IDirectedGraphVar g){
		int n = g.getNbMaxNodes();
		IntVar[] successors = _me().intVarArray("succOf",n,0,n-1,false);
		_me().successors_channeling(g, successors).post();
		return successors;
	}

	/**
	 * Creates an array of set variables representing the successors of every vertex
	 * int j in SetVar[i] <=> (i,j) in g
	 * @param g	a directed graph variable
	 * @return an array of set variables representing the successors of every vertex
	 */
	default SetVar[] succ_set_array(IDirectedGraphVar g){
		int n = g.getNbMaxNodes();
		SetVar[] successors = new SetVar[n];
		for(int i=0;i<n;i++){
			successors[i] = makeSetVar("succOf("+i+")",0,n-1);
		}
		_me().successors_channeling(g, successors).post();
		return successors;
	}

	/**
	 * Creates a matrix of boolean variables representing the adjacency matrix of g
	 * BoolVar[i][j] = 1 <=> (i,j) in g
	 * @param g	a directed graph variable
	 * @return a matrix of boolean variables representing the adjacency matrix of g
	 */
	default BoolVar[][] succ_bool_matrix(IDirectedGraphVar g){
		int n = g.getNbMaxNodes();
		BoolVar[][] successors = _me().boolVarMatrix("succOf",n,n);
		_me().successors_channeling(g, successors).post();
		return successors;
	}

	/**
	 * Creates a set variable representing the successors of 'node' in g
	 * int j in SetVar <=> (node,j) in g
	 * @param g	a directed graph variable
	 * @return a set variable representing the successors of 'node' in g
	 */
	default SetVar succOf_set(IDirectedGraphVar g, int node){
		int n = g.getNbMaxNodes();
		SetVar successorsOf = makeSetVar("succOf("+node+")",0,n-1);
		_me().successors_channeling(g, successorsOf, node).post();
		return successorsOf;
	}

	/**
	 * Creates an array of boolean variables representing the successors of 'node' in g
	 * BoolVar[j] = 1 <=> (node,j) in g
	 * @param g	a directed graph variable
	 * @return an array of boolean variables representing the successors of 'node' in g
	 */
	default BoolVar[] successorsChanneling(IDirectedGraphVar g, int node){
		BoolVar[] successorsOf = _me().boolVarArray("succOf("+node+")",g.getNbMaxNodes());
		_me().successors_channeling(g, successorsOf, node).post();
		return successorsOf;
	}

	// Predecessors

	/**
	 * Creates a set variable representing the predecessors of 'node' in g
	 * int j in SetVar <=> (j,node) in g
	 * @param g	a directed graph variable
	 * @return a set variable representing the predecessors of 'node' in g
	 */
	default SetVar predOf_set(IDirectedGraphVar g, int node){
		int n = g.getNbMaxNodes();
		SetVar predecessorsOf = makeSetVar("predOf("+node+")",0,n-1);
		_me().predecessors_channeling(g, predecessorsOf, node).post();
		return predecessorsOf;
	}

	/**
	 * Creates an array of boolean variables representing the predecessors of 'node' in g
	 * BoolVar[j] = 1 <=> (j,node) in g
	 * @param g	a directed graph variable
	 * @return an array of boolean variables representing the predecessors of 'node' in g
	 */
	default BoolVar[] predecessorsChanneling(IDirectedGraphVar g, int node){
		BoolVar[] predecessorsOf = _me().boolVarArray("predOf("+node+")",g.getNbMaxNodes());
		_me().predecessors_channeling(g, predecessorsOf, node).post();
		return predecessorsOf;

	}

	//***********************************************************************************
	// DEGREE VARIABLES
	//***********************************************************************************

	/**
	 * Creates an array of integer variables representing the degree of each node in g
	 * IntVar[i] = k <=> |(i,j) in g| = k
	 * @param g	an undirected graph variable
	 * @return an array of integer variables representing the degree of each node in g
	 */
	default IntVar[] degrees(IUndirectedGraphVar g){
		int n = g.getNbMaxNodes();
		IntVar[] degrees = _me().intVarArray("degree",n,0,n,true);
		_me().degrees(g,degrees).post();
		return degrees;
	}

	/**
	 * Creates an array of integer variables representing the in-degree of each node in g
	 * IntVar[i] = k <=> |(i,j) in g| = k
	 * @param g	a directed graph variable
	 * @return an array of integer variables representing the in-degree of each node in g
	 */
	default IntVar[] in_degrees(IDirectedGraphVar g){
		int n = g.getNbMaxNodes();
		IntVar[] degrees = _me().intVarArray("in_degree",n,0,n,true);
		_me().in_degrees(g,degrees).post();
		return degrees;
	}

	/**
	 * Creates an array of integer variables representing the out-degree of each node in g
	 * IntVar[i] = k <=> |(j,i) in g| = k
	 * @param g	a directed graph variable
	 * @return an array of integer variables representing the out-degree of each node in g
	 */
	default IntVar[] out_degrees(IDirectedGraphVar g){
		int n = g.getNbMaxNodes();
		IntVar[] degrees = _me().intVarArray("out_degree",n,0,n,true);
		_me().out_degrees(g,degrees).post();
		return degrees;
	}

	//***********************************************************************************
	// UTILS
	//***********************************************************************************

	default SetVar makeSetVar(String name, int minInt, int maxInt){
		int[] lb = new int[0];
		int[] ub = new int[maxInt-minInt+1];
		for(int i=0;i<ub.length;i++){
			ub[i] = minInt+i;
		}
		return _me().setVar(name,lb,ub);
	}
}
