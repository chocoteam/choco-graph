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
	 * @param name	name of the variable
	 * @param n		Maximum number of vertices
	 * @return a graph variable having n vertices
	 */
	default UndirectedGraphVar graphVar(String name, int n) {
		return graphVar(name,n,false);
	}

	/**
	 * Creates a graph variable comprised between an empty graph and K_n (complete graph of n vertices)
	 * @param name	name of the variable
	 * @param n		Maximum number of vertices
	 * @param allNodes If true then every vertex in [0,n-1] belongs to every solution.
	 * @return a graph variable having n vertices
	 */
	default UndirectedGraphVar graphVar(String name, int n, boolean allNodes) {
		UndirectedGraph lb = new UndirectedGraph(_me(), n,SetType.BITSET,allNodes);
		UndirectedGraph ub = new UndirectedGraph(_me(), n,SetType.BITSET,allNodes);
		for(int i=0;i<n;i++){
			if(!allNodes) {
				ub.addNode(i);
			}
			for(int j=i;j<n;j++){
				ub.addEdge(i,j);
			}
		}
		return graphVar(name, lb, ub);
	}

	/**
	 * Create an undirected graph variable named name
	 * and whose domain is the graph interval [lb,ub]
	 * BEWARE: lb and ub graphs must be backtrackable
	 * (use the solver as an argument in their constructor)!
	 *
	 * @param name		name of the variable
	 * @param lb		Undirected graph representing mandatory nodes and edges
	 * @param ub		Undirected graph representing possible nodes and edges
	 * @return	An undirected graph variable
	 */
	default UndirectedGraphVar graphVar(String name, UndirectedGraph lb, UndirectedGraph ub) {
		return new UndirectedGraphVar(name, _me(), lb, ub);
	}

	/**
	 * Creates a directed graph variable comprised between an empty graph and K_n (complete graph of n vertices)
	 * @param name	name of the variable
	 * @param n		Maximum number of vertices
	 * @return a directed graph variable having n vertices
	 */
	default IDirectedGraphVar digraphVar(String name, int n) {
		return digraphVar(name,n,false);
	}

	/**
	 * Creates a directed graph variable comprised between an empty graph and K_n (complete graph of n vertices)
	 * @param name	name of the variable
	 * @param n		Maximum number of vertices
	 * @param allNodes If true then every vertex in [0,n-1] belongs to every solution.
	 * @return a directed graph variable having n vertices
	 */
	default IDirectedGraphVar digraphVar(String name, int n, boolean allNodes) {
		DirectedGraph lb = new DirectedGraph(_me(), n,SetType.BITSET,allNodes);
		DirectedGraph ub = new DirectedGraph(_me(), n,SetType.BITSET,allNodes);
		for(int i=0;i<n;i++){
			if(!allNodes) {
				ub.addNode(i);
			}
			for(int j=0;j<n;j++){
				ub.addArc(i,j);
			}
		}
		return digraphVar(name, lb, ub);
	}

	/**
	 * Create a directed graph variable named name
	 * and whose domain is the graph interval [lb,ub]
	 * BEWARE: lb and ub graphs must be backtrackable
	 * (use the solver as an argument in their constructor)!
	 *
	 * @param name		name of the variable
	 * @param lb		Directed graph representing mandatory nodes and edges
	 * @param ub		Directed graph representing possible nodes and edges
	 * @return	An undirected graph variable
	 */
	default IDirectedGraphVar digraphVar(String name, DirectedGraph lb, DirectedGraph ub) {
		return new DirectedGraphVar(name, _me(), lb, ub);
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
	default IntVar nbNodes(IGraphVar g){
		IntVar nb = g.getModel().intVar("nbNodes",0,g.getNbMaxNodes(),true);
		_me().nbNodes(g, nb).post();
		return nb;
	}

	/**
	 * Creates an integer variable representing the number of nodes in g
	 * @param g	a directed graph variable
	 * @return An integer variable representing the number of nodes in g
	 */
	default IntVar nbArcs(IDirectedGraphVar g){
		IntVar nb = _me().intVar("nbArcs",0,g.getNbMaxNodes()*g.getNbMaxNodes(),true);
		_me().nbArcs(g, nb).post();
		return nb;
	}

	/**
	 * Creates an integer variable representing the number of nodes in g
	 * @param g	an undirected graph variable
	 * @return An integer variable representing the number of nodes in g
	 */
	default IntVar nbEdges(UndirectedGraphVar g){
		IntVar nb = _me().intVar("nbEdges",0,g.getNbMaxNodes()*g.getNbMaxNodes(),true);
		_me().nbEdges(g, nb).post();
		return nb;
	}

	/**
	 * Creates an integer variable representing the number of loops in g
	 * IntVar = |(i,i) in g|
	 * @param g	a graph variable
	 * @return An integer variable representing the number of loops in g
	 */
	default IntVar nbLoops(IGraphVar g){
		IntVar nb = _me().intVar("nbLoops",0,g.getNbMaxNodes(),true);
		_me().nbLoops(g,nb).post();
		return nb;
	}

	/**
	 * Creates a set variable representing nodes of g that have a loop,
	 * i.e. nodes 'i' having an arc of the form (i,i)
	 * @param g	a graph variable
	 * @return a set variable representing nodes of g that have a loop
	 */
	default SetVar loopSet(IGraphVar g){
		SetVar l = makeSetVar("loops",0,g.getNbMaxNodes());
		_me().loopSet(g,l).post();
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
	default SetVar nodeSet(IGraphVar g){
		int n = g.getNbMaxNodes();
		SetVar nodes = makeSetVar("nodes",0,n-1);
		_me().nodesChanneling(g, nodes).post();
		return nodes;
	}

	/**
	 * Creates an array of boolean variables representing the nodes of g
	 * @param g	a graph variable
	 * @return	an array of boolean variables representing the nodes of g
	 */
	default BoolVar[] nodeSetBool(IGraphVar g){
		BoolVar[] nodes = _me().boolVarArray("nodes",g.getNbMaxNodes());
		_me().nodesChanneling(g, nodes).post();
		return nodes;
	}

	/**
	 * Creates a boolean variable representing if the node 'vertex' is in g or not
	 * @param g	a graph variable
	 * @param vertex index of a potential vertex of g
	 * @return a boolean variable representing if the node 'vertex' is in g or not
	 */
	default BoolVar isNode(IGraphVar g, int vertex){
		BoolVar node = _me().boolVar("nodes("+vertex+")");
		_me().nodeChanneling(g, node, vertex).post();
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
	default BoolVar isArc(IDirectedGraphVar g, int from, int to){
		BoolVar node = _me().boolVar("arc("+from+","+to+")");
		_me().arcChanneling(g, node, from, to).post();
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
	default BoolVar isEdge(UndirectedGraphVar g, int v1, int v2){
		BoolVar node = _me().boolVar("edge("+v1+","+v2+")");
		_me().edgeChanneling(g, node, v1, v2).post();
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
	default IntVar[] neighInts(UndirectedGraphVar g){
		int n = g.getNbMaxNodes();
		IntVar[] successors = _me().intVarArray("neighOf",n,0,n-1,false);
		_me().neighborsChanneling(g, successors).post();
		return successors;
	}

	/**
	 * Creates an array of set variables representing the neighborhood of every vertex
	 * int j in SetVar[i] <=> (i,j) in g
	 * @param g	an undirected graph variable
	 * @return an array of set variables representing the neighborhood of every vertex
	 */
	default SetVar[] neighSets(UndirectedGraphVar g){
		int n = g.getNbMaxNodes();
		SetVar[] neighbors = new SetVar[n];
		for(int i=0;i<n;i++){
			neighbors[i] = makeSetVar("neighOf("+i+")",0,n-1);
		}
		_me().neighborsChanneling(g, neighbors).post();
		return neighbors;
	}

	/**
	 * Creates a matrix of boolean variables representing the adjacency matrix of g
	 * BoolVar[i][j] = 1 <=> (i,j) in g
	 * @param g	an undirected graph variable
	 * @return a matrix of boolean variables representing the adjacency matrix of g
	 */
	default BoolVar[][] adjacencyMatrix(UndirectedGraphVar g){
		int n = g.getNbMaxNodes();
		BoolVar[][] neighbors = _me().boolVarMatrix("neighOf",n,n);
		_me().neighborsChanneling(g, neighbors).post();
		return neighbors;
	}

	/**
	 * Creates a set variable representing the neighborhood of 'node' in g
	 * int j in SetVar <=> (node,j) in g
	 * @param g	an undirected graph variable
	 * @return a set variable representing the neighborhood of 'node' in g
	 */
	default SetVar neighSet(UndirectedGraphVar g, int node){
		int n = g.getNbMaxNodes();
		SetVar neighborsOf = makeSetVar("neighOf("+node+")",0,n-1);
		_me().neighborsChanneling(g, neighborsOf, node).post();
		return neighborsOf;
	}

	/**
	 * Creates an array of boolean variables representing the neighborhood of 'node' in g
	 * BoolVar[j] = 1 <=> (node,j) in g
	 * @param g	an undirected graph variable
	 * @return an array of boolean variables representing the neighborhood of 'node' in g
	 */
	default BoolVar[] neighBools(UndirectedGraphVar g, int node){
		BoolVar[] neighborsOf = _me().boolVarArray("neighOf("+node+")",g.getNbMaxNodes());
		_me().neighborsChanneling(g, neighborsOf, node).post();
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
	default IntVar[] succInts(IDirectedGraphVar g){
		int n = g.getNbMaxNodes();
		IntVar[] successors = _me().intVarArray("succOf",n,0,n-1,false);
		_me().successorsChanneling(g, successors).post();
		return successors;
	}

	/**
	 * Creates an array of set variables representing the successors of every vertex
	 * int j in SetVar[i] <=> (i,j) in g
	 * @param g	a directed graph variable
	 * @return an array of set variables representing the successors of every vertex
	 */
	default SetVar[] succSets(IDirectedGraphVar g){
		int n = g.getNbMaxNodes();
		SetVar[] successors = new SetVar[n];
		for(int i=0;i<n;i++){
			successors[i] = makeSetVar("succOf("+i+")",0,n-1);
		}
		_me().successorsChanneling(g, successors).post();
		return successors;
	}

	/**
	 * Creates a matrix of boolean variables representing the adjacency matrix of g
	 * BoolVar[i][j] = 1 <=> (i,j) in g
	 * @param g	a directed graph variable
	 * @return a matrix of boolean variables representing the adjacency matrix of g
	 */
	default BoolVar[][] adjacencyMatrix(IDirectedGraphVar g){
		int n = g.getNbMaxNodes();
		BoolVar[][] successors = _me().boolVarMatrix("succOf",n,n);
		_me().successorsChanneling(g, successors).post();
		return successors;
	}

	/**
	 * Creates a set variable representing the successors of 'node' in g
	 * int j in SetVar <=> (node,j) in g
	 * @param g	a directed graph variable
	 * @return a set variable representing the successors of 'node' in g
	 */
	default SetVar succSet(IDirectedGraphVar g, int node){
		int n = g.getNbMaxNodes();
		SetVar successorsOf = makeSetVar("succOf("+node+")",0,n-1);
		_me().successorsChanneling(g, successorsOf, node).post();
		return successorsOf;
	}

	/**
	 * Creates an array of boolean variables representing the successors of 'node' in g
	 * BoolVar[j] = 1 <=> (node,j) in g
	 * @param g	a directed graph variable
	 * @return an array of boolean variables representing the successors of 'node' in g
	 */
	default BoolVar[] succBools(IDirectedGraphVar g, int node){
		BoolVar[] successorsOf = _me().boolVarArray("succOf("+node+")",g.getNbMaxNodes());
		_me().successorsChanneling(g, successorsOf, node).post();
		return successorsOf;
	}

	// Predecessors

	/**
	 * Creates a set variable representing the predecessors of 'node' in g
	 * int j in SetVar <=> (j,node) in g
	 * @param g	a directed graph variable
	 * @return a set variable representing the predecessors of 'node' in g
	 */
	default SetVar predSet(IDirectedGraphVar g, int node){
		int n = g.getNbMaxNodes();
		SetVar predecessorsOf = makeSetVar("predOf("+node+")",0,n-1);
		_me().predecessorsChanneling(g, predecessorsOf, node).post();
		return predecessorsOf;
	}

	/**
	 * Creates an array of boolean variables representing the predecessors of 'node' in g
	 * BoolVar[j] = 1 <=> (j,node) in g
	 * @param g	a directed graph variable
	 * @return an array of boolean variables representing the predecessors of 'node' in g
	 */
	default BoolVar[] predBools(IDirectedGraphVar g, int node){
		BoolVar[] predecessorsOf = _me().boolVarArray("predOf("+node+")",g.getNbMaxNodes());
		_me().predecessorsChanneling(g, predecessorsOf, node).post();
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
	default IntVar[] degrees(UndirectedGraphVar g){
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
	default IntVar[] inDegrees(IDirectedGraphVar g){
		int n = g.getNbMaxNodes();
		IntVar[] degrees = _me().intVarArray("in_degree",n,0,n,true);
		_me().inDegrees(g,degrees).post();
		return degrees;
	}

	/**
	 * Creates an array of integer variables representing the out-degree of each node in g
	 * IntVar[i] = k <=> |(j,i) in g| = k
	 * @param g	a directed graph variable
	 * @return an array of integer variables representing the out-degree of each node in g
	 */
	default IntVar[] outDegrees(IDirectedGraphVar g){
		int n = g.getNbMaxNodes();
		IntVar[] degrees = _me().intVarArray("out_degree",n,0,n,true);
		_me().outDegrees(g,degrees).post();
		return degrees;
	}

	//***********************************************************************************
	// UTILS
	//***********************************************************************************

	/**
	 * Creates a set var of initial domain [{},[minInt,maxInt]]
	 * @param name of the set variable
	 * @param minInt minimum value in the set upper bound
	 * @param maxInt maximum value in the set upper bound
	 * @return a set var of initial domain [{},[minInt,maxInt]]
	 */
	default SetVar makeSetVar(String name, int minInt, int maxInt){
		int[] lb = new int[0];
		int[] ub = new int[maxInt-minInt+1];
		for(int i=0;i<ub.length;i++){
			ub[i] = minInt+i;
		}
		return _me().setVar(name,lb,ub);
	}
}