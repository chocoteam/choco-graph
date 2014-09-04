/*
 * Copyright (c) 1999-2012, Ecole des Mines de Nantes
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Ecole des Mines de Nantes nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package solver.cstrs;

import solver.constraints.Constraint;
import solver.constraints.Propagator;
import solver.cstrs.arborescences.PropAntiArborescence;
import solver.cstrs.arborescences.PropArborescence;
import solver.cstrs.basic.*;
import solver.cstrs.channeling.edges.*;
import solver.cstrs.channeling.nodes.PropNodeBoolChannel;
import solver.cstrs.channeling.nodes.PropNodeBoolsChannel;
import solver.cstrs.channeling.nodes.PropNodeSetChannel;
import solver.cstrs.degree.PropNodeDegree_AtLeast_Coarse;
import solver.cstrs.degree.PropNodeDegree_AtLeast_Incr;
import solver.cstrs.degree.PropNodeDegree_AtMost_Incr;
import solver.cstrs.path.PropAllDiffGraphIncremental;
import solver.cstrs.path.PropPathNoCycle;
import solver.cstrs.trees.PropTreeNoSubtour;
import solver.cstrs.tsp.undirected.PropCycleEvalObj;
import solver.cstrs.tsp.undirected.PropCycleNoSubtour;
import solver.cstrs.tsp.undirected.lagrangianRelaxation.PropLagr_OneTree;
import solver.variables.*;
import util.objects.graphs.Orientation;
import util.tools.ArrayUtils;

/**
 * Some usual graph constraints
 *
 * @author Jean-Guillaume Fages
 */
public class GraphConstraintFactory {

	//***********************************************************************************
	// CHANNELING CONSTRAINTS
	//***********************************************************************************

	// Vertices

	public static Constraint nodesChanneling(IGraphVar g, SetVar nodes){
		return new Constraint("nodesSetChanneling",
				new PropNodeSetChannel(nodes,g));
	}

	public static Constraint nodesChanneling(IGraphVar g, BoolVar[] nodes){
		return new Constraint("nodesBoolsChanneling",
				new PropNodeBoolsChannel(nodes,g));
	}

	public static Constraint nodeChanneling(IGraphVar g, BoolVar isIn, int vertex){
		return new Constraint("nodesBoolChanneling",
				new PropNodeBoolChannel(isIn,vertex,g));
	}

	// Arc

	public static Constraint arcChanneling(IDirectedGraphVar g, BoolVar isArc, int from, int to){
		return new Constraint("arcChanneling",
				new PropArcBoolChannel(isArc,from,to,g));
	}

	// Edge

	public static Constraint edgeChanneling(IUndirectedGraphVar g, BoolVar isEdge, int vertex1, int vertex2){
		return new Constraint("arcChanneling",
				new PropArcBoolChannel(isEdge,vertex1,vertex2,g));
	}

	// Neighbors

	public static Constraint neighborsChanneling(IUndirectedGraphVar g, SetVar[] neighbors){
		return new Constraint("neighSetsChanneling",
				new PropNeighSetsChannel1(neighbors,g),new PropNeighSetsChannel2(neighbors,g));

	}

	public static Constraint neighborsChanneling(IUndirectedGraphVar g, BoolVar[][] neighbors){
		return new Constraint("neighBoolsChanneling",
				new PropNeighBoolsChannel1(neighbors,g),new PropNeighBoolsChannel2(neighbors,g));
	}

	public static Constraint neighborsChanneling(IUndirectedGraphVar g, SetVar neighborsOf, int node){
		return new Constraint("neighSetChanneling",
				new PropNeighSetChannel(neighborsOf,node,g,new IncidentSet.SuccOrNeighSet()));
	}

	public static Constraint neighborsChanneling(IUndirectedGraphVar g, BoolVar[] neighborsOf, int node){
		return new Constraint("neighBoolChanneling",
				new PropNeighBoolChannel(neighborsOf,node,g,new IncidentSet.SuccOrNeighSet()));
	}

	// Successors

	public static Constraint successorsChanneling(IDirectedGraphVar g, SetVar[] successors){
		return new Constraint("succSetsChanneling",
				new PropNeighSetsChannel1(successors,g),new PropNeighSetsChannel2(successors,g));
	}

	public static Constraint successorsChanneling(IDirectedGraphVar g, BoolVar[][] successors){
		return new Constraint("succBoolsChanneling",
				new PropNeighBoolsChannel1(successors,g),new PropNeighBoolsChannel2(successors,g));
	}

	public static Constraint successorsChanneling(IDirectedGraphVar g, SetVar successorsOf, int node){
		return new Constraint("succSetChanneling",
				new PropNeighSetChannel(successorsOf,node,g,new IncidentSet.SuccOrNeighSet()));
	}

	public static Constraint successorsChanneling(IDirectedGraphVar g, BoolVar[] successorsOf, int node){
		return new Constraint("succBoolChanneling",
				new PropNeighBoolChannel(successorsOf,node,g,new IncidentSet.SuccOrNeighSet()));
	}

	// Predecessors

	public static Constraint predecessorsChanneling(IDirectedGraphVar g, SetVar predecessorsOf, int node){
		return new Constraint("predSetChanneling",
				new PropNeighSetChannel(predecessorsOf,node,g,new IncidentSet.PredOrNeighSet()));
	}

	public static Constraint predecessorsChanneling(IDirectedGraphVar g, BoolVar[] predecessorsOf, int node){
		return new Constraint("predBoolChanneling",
				new PropNeighBoolChannel(predecessorsOf,node,g,new IncidentSet.PredOrNeighSet()));

	}


	//***********************************************************************************
	// GRAPH CONSTRAINTS
	//***********************************************************************************

	/**
	 * Channeling between a graph variable GRAPH and set variables SETS
	 * representing either node neighbors or node successors
	 * <p/> arc (i,j) in GRAPH <=> j in SETS[i]
	 *
	 * @param SETS  set variables representing nodes neighbors (or successors if directed) in GRAPH
	 * @param GRAPH a graph variable
	 * @return a constraint ensuring that arc (i,j) in GRAPH <=> j in SETS[i]
	 */
	public static Constraint set_channeling(SetVar[] SETS, GraphVar GRAPH) {
		if (GRAPH.isDirected()) {
			return new Constraint("SetUndirectedGraphChannel",new PropSymmetric(SETS, 0),new PropGraphChannel(SETS, GRAPH));
		}else{
			return new Constraint("SetDirectedGraphChannel",new PropGraphChannel(SETS, GRAPH));
		}
	}

	/**
	 * Channeling between a directed graph variable GRAPH and set variables SUCCESSORS and PREDECESSORS
	 * representing node successors and predecessors:
	 * <p/> arc (i,j) in GRAPH <=> j in SUCCESSORS[i] and i in PREDECESSORS[j]
	 *
	 * @param SUCCESSORS   set variables representing nodes' successors in GRAPH
	 * @param PREDECESSORS set variables representing nodes' predecessors in GRAPH
	 * @param GRAPH        a graph variable
	 * @return a constraint ensuring that arc (i,j) in GRAPH <=> j in SUCCESSORS[i] and i in PREDECESSORS[j]
	 */
	public static Constraint graph_channel(SetVar[] SUCCESSORS, SetVar[] PREDECESSORS, DirectedGraphVar GRAPH) {
		return new Constraint("SetPartition",ArrayUtils.append(
				graph_channel(SUCCESSORS, GRAPH).getPropagators(),
				new Propagator[]{new PropInverse(SUCCESSORS, PREDECESSORS, 0, 0)})
		);
	}

	//***********************************************************************************
	// UNDIRECTED GRAPHS
	//***********************************************************************************

	/**
	 * partition a graph variable into nCliques cliques
	 *
	 * @param GRAPHVAR   graph variable partitioned into cliques
	 * @param NB_CLIQUES expected number of cliques
	 * @return a constraint which partitions GRAPHVAR into NB_CLIQUES cliques
	 */
	public static Constraint nCliques(IUndirectedGraphVar GRAPHVAR, IntVar NB_CLIQUES) {
		return new Constraint("NCliques",
				new PropTransitivity(GRAPHVAR),
				new PropKCliques(GRAPHVAR, NB_CLIQUES),
				new PropKCC(GRAPHVAR, NB_CLIQUES)
		);
	}

	/**
	 * Constraint modeling the Traveling Salesman Problem
	 *
	 * @param GRAPHVAR   graph variable representing a Hamiltonian cycle
	 * @param COSTVAR    variable representing the cost of the cycle
	 * @param EDGE_COSTS cost matrix (should be symmetric)
	 * @param HELD_KARP  use the Lagrangian relaxation of the tsp
	 *                   described by Held and Karp
	 *                   {0:noHK,1:HK,2:HK but wait a first solution before running it}
	 * @return a tsp constraint
	 */
	public static Constraint tsp(IUndirectedGraphVar GRAPHVAR, IntVar COSTVAR, int[][] EDGE_COSTS, int HELD_KARP) {
		Propagator[] props = ArrayUtils.append(hamiltonianCycle(GRAPHVAR).getPropagators(),
				new Propagator[]{new PropCycleEvalObj(GRAPHVAR, COSTVAR, EDGE_COSTS)});
		if (HELD_KARP > 0) {
			PropLagr_OneTree hk = new PropLagr_OneTree(GRAPHVAR, COSTVAR, EDGE_COSTS);
			hk.waitFirstSolution(HELD_KARP == 2);
			props = ArrayUtils.append(props,new Propagator[]{hk});
		}
		return new Constraint("Graph_TSP",props);
	}

	/**
	 * GRAPHVAR must form a Hamiltonian cycle
	 * <p/> Filtering algorithms are incremental and run in O(1) per enforced/removed edge.
	 * <p/> Subtour elimination is an undirected adaptation of the
	 * nocycle constraint of Caseau & Laburthe in Solving small TSPs with Constraints.
	 *
	 * @param GRAPHVAR graph variable representing a Hamiltonian cycle
	 * @return a hamiltonian cycle constraint
	 */
	public static Constraint hamiltonianCycle(IUndirectedGraphVar GRAPHVAR) {
		int m = 0;
		int n = GRAPHVAR.getNbMaxNodes();
		for(int i=0;i<n;i++){
			m += GRAPHVAR.getPotNeighOf(i).getSize();
		}
		m /= 2;
		if(m<20*n){
			return new Constraint("Graph_HamiltonianCycle",
					new PropNodeDegree_AtLeast_Incr(GRAPHVAR, 2),
					new PropNodeDegree_AtMost_Incr(GRAPHVAR, 2),
					new PropCycleNoSubtour(GRAPHVAR)
			);
		}else{
			return new Constraint("Graph_HamiltonianCycle",
					new PropNodeDegree_AtLeast_Coarse(GRAPHVAR, 2),
					new PropNodeDegree_AtMost_Incr(GRAPHVAR, 2),
					new PropCycleNoSubtour(GRAPHVAR)
			);
		}
	}

	/**
	 * GRAPHVAR must form a spanning tree, i.e. an acyclic and connected undirected graph spanning every vertex
	 * <p/> Incremental degree constraint, runs in O(1) time per force/removed edge
	 * <p/> Connectivity checker and bridge detection in O(n+m) time (Tarjan's algorithm)
	 * <p/> Subtour elimination in O(n) worst case time per enforced edge
	 *
	 * @param GRAPHVAR graph variable forming a tree
	 * @return a constraint ensuring that GRAPHVAR is a spanning tree
	 */
	public static Constraint spanning_tree(IUndirectedGraphVar GRAPHVAR) {
		IntVar nbNodes = VF.fixed(GRAPHVAR.getNbMaxNodes(),GRAPHVAR.getSolver());
		return new Constraint("Graph_SpanningTree",ArrayUtils.append(
				tree(GRAPHVAR).getPropagators(),
				new Propagator[]{new PropKNodes(GRAPHVAR, nbNodes)}
		));
	}

	/**
	 * GRAPHVAR must form a tree, i.e. an acyclic and connected undirected graph
	 * <p/> Incremental degree constraint, runs in O(1) time per force/removed edge
	 * <p/> Connectivity checker and bridge detection in O(n+m) time (Tarjan's algorithm)
	 * <p/> Subtour elimination in O(n) worst case time per enforced edge
	 *
	 * @param GRAPHVAR graph variable forming a tree
	 * @return a constraint ensuring that GRAPHVAR is a tree
	 */
	public static Constraint tree(IUndirectedGraphVar GRAPHVAR) {
		return new Constraint("Graph_Tree",
				new PropNodeDegree_AtLeast_Coarse(GRAPHVAR, 1),
				new PropTreeNoSubtour(GRAPHVAR),
				new PropConnected(GRAPHVAR)
		);
	}

	//***********************************************************************************
	// DIRECTED GRAPHS
	//***********************************************************************************

	/**
	 * GRAPHVAR must form a Hamiltonian path from ORIGIN to DESTINATION.
	 * <p/> Basic filtering algorithms are incremental and run in O(1) per enforced/removed arc.
	 * <p/> Subtour elimination is the nocycle constraint of Caseau & Laburthe in Solving small TSPs with Constraints.
	 * <p/>
	 * <p/> Assumes that ORIGIN has no predecessor, DESTINATION has no successor and each node is mandatory.
	 *
	 * @param GRAPHVAR      variable representing a path
	 * @param ORIGIN        first node of the path
	 * @param DESTINATION   last node of the path
	 * @param STRONG_FILTER true iff it should be worth to spend time on advanced filtering algorithms (that runs
	 *                      in linear time). If so, then it uses dominator-based and SCCs-based filtering algorithms. This option should
	 *                      be used on small-size.
	 * @return a hamiltonian path constraint
	 */
	public static Constraint hamiltonianPath(IDirectedGraphVar GRAPHVAR, int ORIGIN, int DESTINATION, boolean STRONG_FILTER) {
		int n = GRAPHVAR.getNbMaxNodes();
		int[] succs = new int[n];
		int[] preds = new int[n];
		for (int i = 0; i < n; i++) {
			succs[i] = preds[i] = 1;
		}
		succs[DESTINATION] = preds[ORIGIN] = 0;
		Propagator[] props = new Propagator[]{
				new PropNodeDegree_AtLeast_Coarse(GRAPHVAR, Orientation.SUCCESSORS, succs),
				new PropNodeDegree_AtMost_Incr(GRAPHVAR, Orientation.SUCCESSORS, succs),
				new PropNodeDegree_AtLeast_Coarse(GRAPHVAR, Orientation.PREDECESSORS, preds),
				new PropNodeDegree_AtMost_Incr(GRAPHVAR, Orientation.PREDECESSORS, preds),
				new PropPathNoCycle(GRAPHVAR, ORIGIN, DESTINATION)
		};
		if (STRONG_FILTER) {
			PropArborescence arbo = new PropArborescence(GRAPHVAR, ORIGIN, true);
			PropAntiArborescence aa = new PropAntiArborescence(GRAPHVAR, DESTINATION, true);
			PropAllDiffGraphIncremental ad = new PropAllDiffGraphIncremental(GRAPHVAR, n - 1);
			props = ArrayUtils.append(props,ArrayUtils.toArray(arbo, aa, ad));
		}
		return new Constraint("Graph_HamiltonianPath",props);
	}

	/**
	 * Anti arborescence partitioning constraint
	 * also known as tree constraint (CP'11)
	 * GAC in (almost) linear time : O(alpha.m)
	 * roots are identified by loops
	 * <p/>
	 * BEWARE this implementation supposes that every node is part of the solution graph
	 *
	 * @param GRAPHVAR
	 * @param NB_TREE  number of anti arborescences
	 * @return tree constraint
	 */
	public static Constraint nTrees(IDirectedGraphVar GRAPHVAR, IntVar NB_TREE) {
		return new NTree(GRAPHVAR, NB_TREE);
	}
}
