/*
 * Copyright (c) 1999-2014, Ecole des Mines de Nantes
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
package org.chocosolver.solver.cstrs;

import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.ICF;
import org.chocosolver.solver.constraints.LCF;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.cstrs.basic.*;
import org.chocosolver.solver.cstrs.channeling.edges.*;
import org.chocosolver.solver.cstrs.channeling.nodes.PropNodeBoolChannel;
import org.chocosolver.solver.cstrs.channeling.nodes.PropNodeBoolsChannel;
import org.chocosolver.solver.cstrs.channeling.nodes.PropNodeSetChannel;
import org.chocosolver.solver.cstrs.connectivity.PropConnected;
import org.chocosolver.solver.cstrs.connectivity.PropNbCC;
import org.chocosolver.solver.cstrs.connectivity.PropNbSCC;
import org.chocosolver.solver.cstrs.cost.trees.PropMaxDegVarTree;
import org.chocosolver.solver.cstrs.cost.trees.PropTreeCostSimple;
import org.chocosolver.solver.cstrs.cost.trees.lagrangianRelaxation.PropLagr_DCMST_generic;
import org.chocosolver.solver.cstrs.cost.tsp.PropCycleCostSimple;
import org.chocosolver.solver.cstrs.cost.tsp.lagrangianRelaxation.PropLagr_OneTree;
import org.chocosolver.solver.cstrs.cycles.*;
import org.chocosolver.solver.cstrs.degree.*;
import org.chocosolver.solver.cstrs.inclusion.PropInclusion;
import org.chocosolver.solver.cstrs.symmbreaking.PropIncrementalAdjacencyMatrix;
import org.chocosolver.solver.cstrs.symmbreaking.PropIncrementalAdjacencyUndirectedMatrix;
import org.chocosolver.solver.cstrs.symmbreaking.PropSymmetryBreaking;
import org.chocosolver.solver.cstrs.symmbreaking.PropSymmetryBreakingEx;
import org.chocosolver.solver.cstrs.tree.PropArborescence;
import org.chocosolver.solver.cstrs.tree.PropArborescences;
import org.chocosolver.solver.cstrs.tree.PropReachability;
import org.chocosolver.solver.variables.*;
import org.chocosolver.util.objects.graphs.Orientation;
import org.chocosolver.util.tools.ArrayUtils;

/**
 * Some usual graph constraints
 *
 * @author Jean-Guillaume Fages
 */
public class GraphConstraintFactory {




	//***********************************************************************************
	// BASIC CONSTRAINTS
	//***********************************************************************************


	// counting

	/**
	 * Create a constraint to force the number of nodes in g to be equal to nb
	 * @param g	a graph variable
	 * @param nb	an integer variable indicating the expected number of nodes in g
	 * @return A constraint to force the number of nodes in g to be equal to nb
	 */
	public static Constraint nb_nodes(IGraphVar g, IntVar nb){
		return new Constraint("nb_nodes", new PropNbNodes(g,nb));
	}

	/**
	 * Create a constraint to force the number of arcs in g to be equal to nb
	 * @param g	a directed graph variable
	 * @param nb	an integer variable indicating the expected number of arcs in g
	 * @return A constraint to force the number of arcs in g to be equal to nb
	 */
	public static Constraint nb_arcs(IDirectedGraphVar g, IntVar nb){
		return new Constraint("nb_arcs", new PropNbArcs(g,nb));
	}

	/**
	 * Create a constraint to force the number of edges in g to be equal to nb
	 * @param g	an undirected graph variable
	 * @param nb an integer variable indicating the expected number of edges in g
	 * @return A constraint to force the number of edges in g to be equal to nb
	 */
	public static Constraint nb_edges(IUndirectedGraphVar g, IntVar nb){
		return new Constraint("nb_edges", new PropNbArcs(g,nb));
	}

	// loops

	/**
	 * Create a constraint which ensures that 'loops' denotes the set
	 * of vertices in g which have a loop, i.e. an arc of the form f(i,i)
	 * i.e. vertex i in g => arc (i,i) in g
	 * @param g	a graph variable
	 * @return A constraint which makes sure every node has a loop
	 */
	public static Constraint loop_set(IGraphVar g, SetVar loops){
		return new Constraint("loop_set", new PropLoopSet(g,loops));
	}

	/**
	 * Create a constraint which ensures g has nb loops
	 * |(i,i) in g| = nb
	 * @param g	a graph variable
	 * @param nb an integer variable counting the number of loops in g
	 * @return A constraint which ensures g has nb loops
	 */
	public static Constraint nb_loops(IGraphVar g, IntVar nb){
		return new Constraint("nb_loops", new PropNbLoops(g,nb));
	}


	//***********************************************************************************
	// SIMPLE PROPERTY CONSTRAINTS
	//***********************************************************************************


	// symmetry

	/**
	 * Creates a constraint which ensures that g is a symmetric directed graph
	 * This means (i,j) in g <=> (j,i) in g
	 * Note that it may be preferable to use an undirected graph variable instead!
	 * @param g	a directed graph variable
	 * @return A constraint which ensures that g is a symmetric directed graph
	 */
	public static Constraint symmetric(IDirectedGraphVar g){
		return new Constraint("symmetric", new PropSymmetric(g));
	}

	/**
	 * Creates a constraint which ensures that g is an antisymmetric directed graph
	 * This means (i,j) in g => (j,i) notin g
	 * @param g	a directed graph variable
	 * @return A constraint which ensures that g is an antisymmetric directed graph
	 */
	public static Constraint antisymmetric(IDirectedGraphVar g){
		return new Constraint("antisymmetric", new PropAntiSymmetric(g));
	}

	// Transitivity

	/**
	 * Create a transitivity constraint
	 * (i,j) in g and (j,k) in g => (i,k) in g
	 * Does not consider loops
	 * Enables to make cliques
	 * @param g An undirected graph variable
	 * @return A transitivity constraint
	 */
	public static Constraint transitivity(IUndirectedGraphVar g){
		return new Constraint("transitivity",new PropTransitivity(g));
	}

	/**
	 * Create a transitivity constraint
	 * (i,j) in g and (j,k) in g => (i,k) in g
	 * Does not consider loops
	 * Enables to make cliques and transitive closures
	 * @param g A directed graph variable
	 * @return A transitivity constraint
	 */
	public static Constraint transitivity(IDirectedGraphVar g){
		return new Constraint("transitivity",new PropTransitivity(g));
	}


	//***********************************************************************************
	// INCLUSION CONSTRAINTS
	//***********************************************************************************


	/**
	 * Create an inclusion constraint between g1 and g2 such that
	 * g1 is a subgraph of g2
	 * Note that node are labelled with their indexes :
	 * the vertex 0 in g1 corresponds to the vertex 0 in g2
	 * @param g1 An undirected graph variable
	 * @param g2 An undirected graph variable
	 * @return a constraint which ensures that g1 is a subgraph of g2
	 */
	public static Constraint subgraph(IUndirectedGraphVar g1, IUndirectedGraphVar g2){
		return new Constraint("subgraph",new PropInclusion(g1,g2));
	}

	/**
	 * Create an inclusion constraint between g1 and g2 such that
	 * g1 is a subgraph of g2
	 * Note that node are labelled with their indexes :
	 * the vertex 0 in g1 corresponds to the vertex 0 in g2
	 * @param g1 A directed graph variable
	 * @param g2 A directed graph variable
	 * @return a constraint which ensures that g1 is a subgraph of g2
	 */
	public static Constraint subgraph(IDirectedGraphVar g1, IDirectedGraphVar g2){
		return new Constraint("subgraph",new PropInclusion(g1,g2));
	}


	//***********************************************************************************
	// CHANNELING CONSTRAINTS
	//***********************************************************************************

	// Vertices

	/**
	 * Channeling constraint :
	 * int i in nodes <=> vertex i in g
	 * @param g
	 * @param nodes
	 */
	public static Constraint nodes_channeling(IGraphVar g, SetVar nodes){
		return new Constraint("nodesSetChanneling",
				new PropNodeSetChannel(nodes,g));
	}

	/**
	 * Channeling constraint :
	 * nodes[i] = 1 <=> vertex i in g
	 * @param g
	 * @param nodes
	 */
	public static Constraint nodes_channeling(IGraphVar g, BoolVar[] nodes){
		return new Constraint("nodesBoolsChanneling",
				new PropNodeBoolsChannel(nodes,g));
	}

	/**
	 * Channeling constraint :
	 * isIn = 1 <=> vertex 'vertex' in g
	 * @param g
	 * @param isIn
	 * @param vertex
	 */
	public static Constraint node_channeling(IGraphVar g, BoolVar isIn, int vertex){
		return new Constraint("nodesBoolChanneling",
				new PropNodeBoolChannel(isIn,vertex,g));
	}

	// Arc

	/**
	 * Channeling constraint :
	 * isArc = 1 <=> arc (from,to) in g
	 * @param g
	 * @param isArc
	 * @param from
	 * @param to
	 */
	public static Constraint arc_channeling(IDirectedGraphVar g, BoolVar isArc, int from, int to){
		return new Constraint("arcChanneling",
				new PropArcBoolChannel(isArc,from,to,g));
	}

	// Edge

	/**
	 * Channeling constraint:
	 * isEdge = 1 <=> edge (i,j) in g
	 * @param g
	 * @param isEdge
	 * @param i
	 * @param j
	 */
	public static Constraint edge_channeling(IUndirectedGraphVar g, BoolVar isEdge, int i, int j){
		return new Constraint("arcChanneling",
				new PropArcBoolChannel(isEdge,i,j,g));
	}

	// Neighbors

	/**
	 * Channeling constraint:
	 * successors[i] = j OR successors[j] = i <=> edge (i,j) in g
	 * @param g
	 * @param successors
	 */
	public static Constraint neighbors_channeling(IUndirectedGraphVar g, IntVar[] successors){
		return new Constraint("neighIntsChanneling",
				new PropNeighIntsChannel1(successors,g),new PropNeighIntsChannel2(successors,g));
	}

	/**
	 * Channeling constraint:
	 * int j in neighbors[i] <=> edge (i,j) in g
	 * @param g
	 * @param neighbors
	 */
	public static Constraint neighbors_channeling(IUndirectedGraphVar g, SetVar[] neighbors){
		return new Constraint("neighSetsChanneling",
				new PropNeighSetsChannel1(neighbors,g),new PropNeighSetsChannel2(neighbors,g));

	}

	/**
	 * Channeling constraint:
	 * neighbors[i][j] = 1 <=> edge (i,j) in g
	 * @param g
	 * @param neighbors
	 */
	public static Constraint neighbors_channeling(IUndirectedGraphVar g, BoolVar[][] neighbors){
		return new Constraint("neighBoolsChanneling",
				new PropNeighBoolsChannel1(neighbors,g),new PropNeighBoolsChannel2(neighbors,g));
	}

	/**
	 * Channeling constraint:
	 * int j in neighborsOf <=> edge (node,j) in g
	 * @param g
	 * @param neighborsOf
	 * @param node
	 */
	public static Constraint neighbors_channeling(IUndirectedGraphVar g, SetVar neighborsOf, int node){
		return new Constraint("neighSetChanneling",
				new PropNeighSetChannel(neighborsOf,node,g,new IncidentSet.SuccOrNeighSet()));
	}

	/**
	 * Channeling constraint:
	 * neighborsOf[j] = 1 <=> edge (node,j) in g
	 * @param g
	 * @param neighborsOf
	 * @param node
	 */
	public static Constraint neighbors_channeling(IUndirectedGraphVar g, BoolVar[] neighborsOf, int node){
		return new Constraint("neighBoolChanneling",
				new PropNeighBoolChannel(neighborsOf,node,g,new IncidentSet.SuccOrNeighSet()));
	}

	// Successors

	/**
	 * Channeling constraint:
	 * successors[i] = j <=> arc (i,j) in g
	 * @param g
	 * @param successors
	 */
	public static Constraint successors_channeling(IDirectedGraphVar g, IntVar[] successors){
		return new Constraint("succIntsChanneling",
				new PropSuccIntsChannel1(successors,g),new PropNeighIntsChannel2(successors,g));
	}

	/**
	 * Channeling constraint:
	 * int j in successors[i] <=> arc (i,j) in g
	 * @param g
	 * @param successors
	 */
	public static Constraint successors_channeling(IDirectedGraphVar g, SetVar[] successors){
		return new Constraint("succSetsChanneling",
				new PropNeighSetsChannel1(successors,g),new PropNeighSetsChannel2(successors,g));
	}

	/**
	 * Channeling constraint:
	 * successors[i][j] <=> arc (i,j) in g
	 * @param g
	 * @param successors
	 */
	public static Constraint successors_channeling(IDirectedGraphVar g, BoolVar[][] successors){
		return new Constraint("succBoolsChanneling",
				new PropNeighBoolsChannel1(successors,g),new PropNeighBoolsChannel2(successors,g));
	}

	/**
	 * Channeling constraint:
	 * int j in successorsOf <=> arc (node,j) in g
	 * @param g
	 * @param successorsOf
	 * @param node
	 */
	public static Constraint successors_channeling(IDirectedGraphVar g, SetVar successorsOf, int node){
		return new Constraint("succSetChanneling",
				new PropNeighSetChannel(successorsOf,node,g,new IncidentSet.SuccOrNeighSet()));
	}

	/**
	 * Channeling constraint:
	 * successorsOf[j] = 1 <=> arc (node,j) in g
	 * @param g
	 * @param successorsOf
	 * @param node
	 */
	public static Constraint successors_channeling(IDirectedGraphVar g, BoolVar[] successorsOf, int node){
		return new Constraint("succBoolChanneling",
				new PropNeighBoolChannel(successorsOf,node,g,new IncidentSet.SuccOrNeighSet()));
	}

	// Predecessors

	/**
	 * Channeling constraint:
	 * int j in predecessorsOf <=> arc (j,node) in g
	 * @param g
	 * @param predecessorsOf
	 * @param node
	 */
	public static Constraint predecessors_channeling(IDirectedGraphVar g, SetVar predecessorsOf, int node){
		return new Constraint("predSetChanneling",
				new PropNeighSetChannel(predecessorsOf,node,g,new IncidentSet.PredOrNeighSet()));
	}

	/**
	 * Channeling constraint:
	 * predecessorsOf[j] = 1 <=> arc (j,node) in g
	 * @param g
	 * @param predecessorsOf
	 * @param node
	 */
	public static Constraint predecessors_channeling(IDirectedGraphVar g, BoolVar[] predecessorsOf, int node){
		return new Constraint("predBoolChanneling",
				new PropNeighBoolChannel(predecessorsOf,node,g,new IncidentSet.PredOrNeighSet()));

	}




	//***********************************************************************************
	// DEGREE CONSTRAINTS
	//***********************************************************************************




	// degrees

	/**
	 * Minimum degree constraint
	 * for any vertex i in g, |(i,j)| >= minDegree
	 * This constraint only holds on vertices that are mandatory
	 * @param g			undirected graph var
	 * @param minDegree	integer minimum degree of every node
	 * @return a minimum degree constraint
	 */
	public static Constraint min_degrees(IUndirectedGraphVar g, int minDegree){
		return new Constraint("min_degrees",new PropNodeDegree_AtLeast_Incr(g, minDegree));
	}

	/**
	 * Minimum degree constraint
	 * for any vertex i in g, |(i,j)| >= minDegree[i]
	 * This constraint only holds on vertices that are mandatory
	 * @param g				undirected graph var
	 * @param minDegrees	integer array giving the minimum degree of each node
	 * @return a minimum degree constraint
	 */
	public static Constraint min_degrees(IUndirectedGraphVar g, int[] minDegrees){
		return new Constraint("min_degrees",new PropNodeDegree_AtLeast_Incr(g, minDegrees));
	}

	/**
	 * Maximum degree constraint
	 * for any vertex i in g, |(i,j)| <= maxDegree
	 * This constraint only holds on vertices that are mandatory
	 * @param g			undirected graph var
	 * @param maxDegree	integer maximum degree
	 * @return a maximum degree constraint
	 */
	public static Constraint max_degrees(IUndirectedGraphVar g, int maxDegree){
		return new Constraint("max_degrees",new PropNodeDegree_AtMost_Coarse(g, maxDegree));
	}

	/**
	 * Maximum degree constraint
	 * for any vertex i in g, |(i,j)| <= maxDegrees[i]
	 * This constraint only holds on vertices that are mandatory
	 * @param g				undirected graph var
	 * @param maxDegrees	integer array giving the maximum degree of each node
	 * @return a maximum degree constraint
	 */
	public static Constraint max_degrees(IUndirectedGraphVar g, int[] maxDegrees){
		return new Constraint("max_degrees",new PropNodeDegree_AtMost_Coarse(g, maxDegrees));
	}

	/**
	 * Degree constraint
	 * for any vertex i in g, |(i,j)| = degrees[i]
	 * A vertex which has been removed has a degree equal to 0
	 * ENSURES EVERY VERTEX i FOR WHICH DEGREE[i]>0 IS MANDATORY
	 * @param g			undirected graph var
	 * @param degrees	integer array giving the degree of each node
	 * @return a degree constraint
	 */
	public static Constraint degrees(IUndirectedGraphVar g, IntVar[] degrees){
		return new Constraint("degrees",new PropNodeDegree_Var(g, degrees));
	}

	// in_degrees

	/**
	 * Minimum inner degree constraint
	 * for any vertex i in g, |(j,i)| >= minDegree
	 * This constraint only holds on vertices that are mandatory
	 * @param g			directed graph var
	 * @param minDegree	integer minimum degree of every node
	 * @return a minimum inner degree constraint
	 */
	public static Constraint min_in_degrees(IDirectedGraphVar g, int minDegree){
		return new Constraint("min_in_degrees",new PropNodeDegree_AtLeast_Incr(g, Orientation.PREDECESSORS, minDegree));
	}

	/**
	 * Minimum inner degree constraint
	 * for any vertex i in g, |(j,i)| >= minDegree[i]
	 * This constraint only holds on vertices that are mandatory
	 * @param g				directed graph var
	 * @param minDegrees	integer array giving the minimum degree of each node
	 * @return a minimum inner degree constraint
	 */
	public static Constraint min_in_degrees(IDirectedGraphVar g, int[] minDegrees){
		return new Constraint("min_in_degrees",new PropNodeDegree_AtLeast_Incr(g, Orientation.PREDECESSORS, minDegrees));
	}

	/**
	 * Maximum inner degree constraint
	 * for any vertex i in g, |(j,i)| <= maxDegree
	 * This constraint only holds on vertices that are mandatory
	 * @param g			directed graph var
	 * @param maxDegree	integer maximum degree
	 * @return a maximum inner degree constraint
	 */
	public static Constraint max_in_degrees(IDirectedGraphVar g, int maxDegree){
		return new Constraint("max_in_degrees",new PropNodeDegree_AtMost_Coarse(g, Orientation.PREDECESSORS, maxDegree));
	}

	/**
	 * Maximum inner degree constraint
	 * for any vertex i in g, |(j,i)| <= maxDegrees[i]
	 * This constraint only holds on vertices that are mandatory
	 * @param g				directed graph var
	 * @param maxDegrees	integer array giving the maximum degree of each node
	 * @return a maximum inner degree constraint
	 */
	public static Constraint max_in_degrees(IDirectedGraphVar g, int[] maxDegrees){
		return new Constraint("max_in_degrees",new PropNodeDegree_AtMost_Coarse(g, Orientation.PREDECESSORS, maxDegrees));
	}

	/**
	 * Degree inner constraint
	 * for any vertex i in g, |(j,i)| = degrees[i]
	 * A vertex which has been removed has a degree equal to 0
	 * ENSURES EVERY VERTEX i FOR WHICH DEGREE[i]>0 IS MANDATORY
	 * @param g			directed graph var
	 * @param degrees	integer array giving the degree of each node
	 * @return a degree inner constraint
	 */
	public static Constraint in_degrees(IDirectedGraphVar g, IntVar[] degrees){
		return new Constraint("in_degrees",new PropNodeDegree_Var(g, Orientation.PREDECESSORS, degrees));
	}

	// out-degrees

	/**
	 * Minimum outer degree constraint
	 * for any vertex i in g, |(i,j)| >= minDegree
	 * This constraint only holds on vertices that are mandatory
	 * @param g			directed graph var
	 * @param minDegree	integer minimum degree of every node
	 * @return a minimum outer degree constraint
	 */
	public static Constraint min_out_degrees(IDirectedGraphVar g, int minDegree){
		return new Constraint("min_out_degrees",new PropNodeDegree_AtLeast_Incr(g, Orientation.SUCCESSORS, minDegree));
	}

	/**
	 * Minimum outer degree constraint
	 * for any vertex i in g, |(i,j)| >= minDegree[i]
	 * This constraint only holds on vertices that are mandatory
	 * @param g				directed graph var
	 * @param minDegrees	integer array giving the minimum degree of each node
	 * @return a minimum outer degree constraint
	 */
	public static Constraint min_out_degrees(IDirectedGraphVar g, int[] minDegrees){
		return new Constraint("min_out_degrees",new PropNodeDegree_AtLeast_Incr(g, Orientation.SUCCESSORS, minDegrees));
	}

	/**
	 * Maximum outer degree constraint
	 * for any vertex i in g, |(i,j)| <= maxDegree
	 * This constraint only holds on vertices that are mandatory
	 * @param g			directed graph var
	 * @param maxDegree	integer maximum degree
	 * @return a maximum outer degree constraint
	 */
	public static Constraint max_out_degrees(IDirectedGraphVar g, int maxDegree){
		return new Constraint("max_out_degrees",new PropNodeDegree_AtMost_Coarse(g, Orientation.SUCCESSORS, maxDegree));
	}

	/**
	 * Maximum outer degree constraint
	 * for any vertex i in g, |(i,j)| <= maxDegrees[i]
	 * This constraint only holds on vertices that are mandatory
	 * @param g				directed graph var
	 * @param maxDegrees	integer array giving the maximum outer degree of each node
	 * @return a outer maximum degree constraint
	 */
	public static Constraint max_out_degrees(IDirectedGraphVar g, int[] maxDegrees){
		return new Constraint("max_out_degrees",new PropNodeDegree_AtMost_Coarse(g, Orientation.SUCCESSORS, maxDegrees));
	}

	/**
	 * Outer degree constraint
	 * for any vertex i in g, |(i,j)| = degrees[i]
	 * A vertex which has been removed has a degree equal to 0
	 * ENSURES EVERY VERTEX i FOR WHICH DEGREE[i]>0 IS MANDATORY
	 * @param g			directed graph var
	 * @param degrees	integer array giving the degree of each node
	 * @return an outer degree constraint
	 */
	public static Constraint out_degrees(IDirectedGraphVar g, IntVar[] degrees){
		return new Constraint("out_degrees",new PropNodeDegree_Var(g, Orientation.SUCCESSORS, degrees));
	}




	//***********************************************************************************
	// CYCLE CONSTRAINTS
	//***********************************************************************************




	/**
	 * g must form a Hamiltonian cycle
	 * Implies that every vertex in [0,g.getNbMaxNodes()-1] is mandatory
	 *
	 * @param g an undirected graph variable
	 * @return a hamiltonian cycle constraint
	 */
	public static Constraint hamiltonian_cycle(IUndirectedGraphVar g) {
		int m = 0;
		int n = g.getNbMaxNodes();
		for(int i=0;i<n;i++){
			m += g.getPotNeighOf(i).getSize();
		}
		m /= 2;
		Propagator pMaxDeg = (m<20*n)?new PropNodeDegree_AtMost_Incr(g, 2):new PropNodeDegree_AtMost_Incr(g, 2);
		return new Constraint("hamiltonian_cycle",
				new PropNodeDegree_AtLeast_Incr(g, 2),
				pMaxDeg,
				new PropHamiltonianCycle(g)
		);
	}

	/**
	 * g must form a cycle
	 *
	 * @param g an undirected graph variable
	 * @return a cycle constraint
	 */
	public static Constraint cycle(IUndirectedGraphVar g) {
		if(g.getMandatoryNodes().getSize() == g.getNbMaxNodes()){
			return hamiltonian_cycle(g);
		}
		int m = 0;
		int n = g.getNbMaxNodes();
		for(int i=0;i<n;i++){
			m += g.getPotNeighOf(i).getSize();
		}
		m /= 2;
		Propagator pMaxDeg = (m<20*n)?new PropNodeDegree_AtMost_Incr(g, 2):new PropNodeDegree_AtMost_Incr(g, 2);
		return new Constraint("cycle",
				new PropNodeDegree_AtLeast_Incr(g, 2),
				pMaxDeg,
				new PropConnected(g),
				new PropCycle(g)
		);
	}

	/**
	 * g must form a Hamiltonian circuit
	 * Implies that every vertex in [0,g.getNbMaxNodes()-1] is mandatory
	 * This constraint cannot be reified
	 *
	 * @param g a directed graph variable
	 * @return a circuit constraint
	 */
	public static Constraint hamiltonian_circuit(IDirectedGraphVar g) {
		IntVar[] gint = GVF.succ_int_array(g);
		return ICF.circuit(gint,0);
	}

	/**
	 * g must form a circuit
	 *
	 * @param g a directed graph variable
	 * @return a circuit constraint
	 */
	public static Constraint circuit(IDirectedGraphVar g) {
		if(g.getMandatoryNodes().getSize() == g.getNbMaxNodes()){
			return hamiltonian_circuit(g);
		}
		return new Constraint("circuit",
				new PropNodeDegree_AtLeast_Incr(g, Orientation.SUCCESSORS, 1),
				new PropNodeDegree_AtLeast_Incr(g, Orientation.PREDECESSORS, 1),
				new PropNodeDegree_AtMost_Incr(g, Orientation.SUCCESSORS, 1),
				new PropNodeDegree_AtMost_Incr(g, Orientation.PREDECESSORS, 1),
				new PropNbSCC(g,g.getSolver().ONE()),
				new PropCircuit(g)
		);
	}

	/**
	 * Cycle elimination constraint
	 * Prevent the graph from containing circuits
	 * e.g. an edge set of the form {(i1,i2),(i2,i3),(i3,i1)}
	 * @param g	an undirected graph variable
	 * @return A cycle elimination constraint
	 */
	public static Constraint no_cycle(IUndirectedGraphVar g){
		return new Constraint("no_cycle",new PropACyclic(g));
	}

	/**
	 * Circuit elimination constraint
	 * Prevent the graph from containing circuits
	 * e.g. an arc set of the form {(i1,i2),(i2,i3),(i3,i1)}
	 * However, it allows to have (i1,i2)(i2,i3)(i1,i3).
	 * @param g	a directed graph variable
	 * @return A circuit elimination constraint
	 */
	public static Constraint no_circuit(IDirectedGraphVar g){
		return new Constraint("no_circuit",new PropACyclic(g));
	}




	//***********************************************************************************
	// CONNECTIVITY CONSTRAINTS
	//***********************************************************************************




	/**
	 * Creates a connectedness constraint which ensures that g is connected
	 * @param g	an undirected graph variable
	 * @return A connectedness constraint which ensures that g is connected
	 */
	public static Constraint connected(IUndirectedGraphVar g){
		return new Constraint("connected",new PropConnected(g));
	}

	/**
	 * Creates a connectedness constraint which ensures that g has nb connected components
	 * @param g	an undirected graph variable
	 * @param nb an integer variable indicating the expected number of connected components in g
	 * @return A connectedness constraint which ensures that g has nb connected components
	 */
	public static Constraint nb_connected_components(IUndirectedGraphVar g, IntVar nb){
		return new Constraint("NbCC",new PropNbCC(g,nb));
	}
	/**
	 * Creates a strong connectedness constraint which ensures that g has exactly one strongly connected component
	 * @param g	a directed graph variable
	 * @return A strong connectedness constraint which ensures that g is strongly connected
	 */
	public static Constraint strongly_connected(IDirectedGraphVar g){
		return nb_strongly_connected_components(g, g.getSolver().ONE());
	}

	/**
	 * Creates a strong connectedness constraint which ensures that g has nb strongly connected components
	 * @param g	a directed graph variable
	 * @param nb an integer variable indicating the expected number of connected components in g
	 * @return A strong connectedness constraint which ensures that g has nb strongly connected components
	 */
	public static Constraint nb_strongly_connected_components(IDirectedGraphVar g, IntVar nb){
		return new Constraint("NbSCC",new PropNbSCC(g,nb));
	}




	//***********************************************************************************
	// TREE CONSTRAINTS
	//***********************************************************************************




	/**
	 * Creates a tree constraint : g is connected and has no cycle
	 * @param g	an undirected graph variable
	 * @return a tree constraint
	 */
	public static Constraint tree(IUndirectedGraphVar g){
		return new Constraint("tree", new PropACyclic(g), new PropConnected(g));
	}

	/**
	 * Creates a forest constraint : g has no cycle but may have several connected components
	 * @param g	an undirected graph variable
	 * @return a forest constraint
	 */
	public static Constraint forest(IUndirectedGraphVar g){
		return new Constraint("forest",new PropACyclic(g));
	}

	/**
	 * Creates a directed tree constraint :
	 * g forms an arborescence rooted in vertex 'root'
	 * i.e. g has no circuit and a path exists from the root to every node
	 * @param g	a directed graph variable
	 * @param root the (fixed) root of the tree
	 * @return a directed tree constraint
	 */
	public static Constraint directed_tree(IDirectedGraphVar g, int root){
		int n = g.getNbMaxNodes();
		int[] nbPreds = new int[n];
		for(int i=0;i<n;i++){
			nbPreds[i] = 1;
		}
		nbPreds[root] = 0;
		return new Constraint("directed_tree"
				,new PropArborescence(g,root)
				,new PropNodeDegree_AtMost_Coarse(g, Orientation.PREDECESSORS, nbPreds)
				,new PropNodeDegree_AtLeast_Incr(g, Orientation.PREDECESSORS, nbPreds)
		);
	}

	/**
	 * Creates a directed forest constraint :
	 * g form is composed of several disjoint (potentially singleton) arborescences
	 * @param g	a directed graph variable
	 * @return a directed forest constraint
	 */
	public static Constraint directed_forest(IDirectedGraphVar g){
		return new Constraint("directed_forest",new PropArborescences(g)
				,new PropNodeDegree_AtMost_Coarse(g, Orientation.PREDECESSORS, 1)
		);
	}



	//***********************************************************************************
	// PATH and REACHABILITY
	//***********************************************************************************

	// reachability

	/**
	 * Creates a constraint which ensures that every vertex in g is reachable by a simple path from the root.
	 * @param g	a directed graph variable
	 * @param root	a vertex reaching every node
	 * @return A constraint which ensures that every vertex in g is reachable by a simple path from the root
	 */
	public static Constraint reachability(IDirectedGraphVar g, int root){
		return new Constraint("reachability_from_"+root,new PropReachability(g,root));
	}

	// directed path

	/**
	 * Creates a path constraint : g forms a path from node 'from' to node 'to'
	 * Basic but fast propagation
	 * @param g a directed graph variable
	 * @param from an integer variable
	 * @param to an integer variable
	 * @return a path constraint
	 */
	public static Constraint path(IDirectedGraphVar g, int from, int to) {
		int n = g.getNbMaxNodes();
		int[] succs = new int[n];
		int[] preds = new int[n];
		for (int i = 0; i < n; i++) {
			succs[i] = preds[i] = 1;
		}
		succs[to] = preds[from] = 0;
		Propagator[] props = new Propagator[]{
				new PropNodeDegree_AtLeast_Coarse(g, Orientation.SUCCESSORS, succs)
				,new PropNodeDegree_AtMost_Incr(g, Orientation.SUCCESSORS, succs)
				,new PropNodeDegree_AtLeast_Coarse(g, Orientation.PREDECESSORS, preds)
				,new PropNodeDegree_AtMost_Incr(g, Orientation.PREDECESSORS, preds)
				,new PropPathNoCircuit(g)
		};
		return new Constraint("path",props);
	}




	//***********************************************************************************
	// CLIQUES
	//***********************************************************************************




	/**
	 * partition a graph variable into nb cliques
	 *
	 * @param g   a graph variable
	 * @param nb expected number of cliques in g
	 * @return a constraint which partitions g into nb cliques
	 */
	public static Constraint nb_cliques(IUndirectedGraphVar g, IntVar nb) {
		return new Constraint("NbCliques",
				new PropTransitivity(g),
				new PropNbCC(g, nb),
				new PropNbCliques(g, nb) // redundant propagator
		);
	}




	//***********************************************************************************
	// DIAMETER
	//***********************************************************************************




	/**
	 * Creates a constraint which states that d is the diameter of g
	 * i.e. d is the length (number of edges) of the largest shortest path among any pair of nodes
	 * This constraint implies that g is connected
	 *
	 * @param g an undirected graph variable
	 * @param d	an integer variable
	 * @return a constraint which states that d is the diameter of g
	 */
	public static Constraint diameter(IUndirectedGraphVar g, IntVar d) {
		return new Constraint("NbCliques",
				new PropConnected(g),
				new PropDiameter(g, d)
		);
	}




	/**
	 * Creates a constraint which states that d is the diameter of g
	 * i.e. d is the length (number of arcs) of the largest shortest path among any pair of nodes
	 * This constraint implies that g is strongly connected
	 *
	 * @param g a directed graph variable
	 * @param d	an integer variable
	 * @return a constraint which states that d is the diameter of g
	 */
	public static Constraint diameter(IDirectedGraphVar g, IntVar d) {
		return new Constraint("NbCliques",
				new PropNbSCC(g,g.getSolver().ONE()),
				new PropDiameter(g, d)
		);
	}




	//***********************************************************************************
	// OPTIMIZATION CONSTRAINTS
	//***********************************************************************************


	/**
	 * Constraint modeling the Traveling Salesman Problem
	 *
	 * @param GRAPHVAR   graph variable representing a Hamiltonian cycle
	 * @param COSTVAR    variable representing the cost of the cycle
	 * @param EDGE_COSTS cost matrix (should be symmetric)
	 * @param LAGR_MODE  use the Lagrangian relaxation of the tsp
	 *                   described by Held and Karp
	 *                   {0:no Lagrangian relaxation,
	 *                   1:Lagrangian relaxation (since root node),
	 *                   2:Lagrangian relaxation but wait a first solution before running it}
	 * @return a tsp constraint
	 */
	public static Constraint tsp(IUndirectedGraphVar GRAPHVAR, IntVar COSTVAR, int[][] EDGE_COSTS, int LAGR_MODE) {
		Propagator[] props = ArrayUtils.append(hamiltonian_cycle(GRAPHVAR).getPropagators(),
				new Propagator[]{new PropCycleCostSimple(GRAPHVAR, COSTVAR, EDGE_COSTS)});
		if (LAGR_MODE > 0) {
			PropLagr_OneTree hk = new PropLagr_OneTree(GRAPHVAR, COSTVAR, EDGE_COSTS);
			hk.waitFirstSolution(LAGR_MODE == 2);
			props = ArrayUtils.append(props,new Propagator[]{hk});
		}
		return new Constraint("TSP",props);
	}

	/**
	 * Creates a degree-constrained minimum spanning tree constraint :
	 * GRAPH is a spanning tree of cost COSTVAR and each vertex degree is constrained
	 *
	 * BEWARE : assumes the channeling between GRAPH and DEGREES is already done
	 *
	 * @param GRAPH		an undirected graph variable
	 * @param DEGREES	the degree of every vertex
	 * @param COSTVAR    variable representing the cost of the mst
	 * @param EDGE_COSTS cost matrix (should be symmetric)
	 * @param LAGR_MODE  use the Lagrangian relaxation of the dcmst
	 *                   {0:no Lagrangian relaxation,
	 *                   1:Lagrangian relaxation (since root node),
	 *                   2:Lagrangian relaxation but wait a first solution before running it}
	 * @return a degree-constrained minimum spanning tree constraint
	 */
	public static Constraint dcmst(IUndirectedGraphVar GRAPH, IntVar[] DEGREES,
													 IntVar COSTVAR, int[][] EDGE_COSTS,
													 int LAGR_MODE){
		Propagator[] props = ArrayUtils.append(
				tree(GRAPH).getPropagators()
				,new Propagator[]{
						new PropTreeCostSimple(GRAPH, COSTVAR, EDGE_COSTS)
						,new PropMaxDegVarTree(GRAPH, DEGREES)
				}
		);
		if (LAGR_MODE > 0) {
			PropLagr_DCMST_generic hk = new PropLagr_DCMST_generic(GRAPH, COSTVAR, DEGREES, EDGE_COSTS, LAGR_MODE == 2);
			props = ArrayUtils.append(props,new Propagator[]{hk});
		}
		return new Constraint("dcmst",props);
	}

	//***********************************************************************************
	// SYMMETRY BREAKING CONSTRAINTS
	//***********************************************************************************

	/**
	 * Post a symmetry breaking constraint. This constraint is a symmetry breaking for
	 * class of directed graphs which contain a directed tree with root in node 0.
	 * (All nodes must be reachable from node 0)
	 * Note, that this method post this constraint directly, so it cannot be reified.
	 * If you need to get a single {@link Constraint} use {@link #symmetryBreaking(IDirectedGraphVar, Solver)}.
	 *
	 * This symmetry breaking method based on paper:
	 *     Ulyantsev V., Zakirzyanov I., Shalyto A.
	 * 	   BFS-Based Symmetry Breaking Predicates for DFA Identification
	 *     //Language and Automata Theory and Applications. – Springer International Publishing, 2015. – С. 611-622.
	 *
	 *
	 * @param graph graph to be constrainted
	 * @param solver solver to post constraint
	 */
	public static void postSymmetryBreaking(IDirectedGraphVar graph, Solver solver) {
		// ---------------------- variables ------------------------
		int n = graph.getNbMaxNodes();
		// t[i, j]
		BoolVar[] t = VF.boolArray("T[]", n * n, solver);
		// p[i]
		IntVar[] p = new IntVar[n];
		p[0] = VF.fixed("P[0]", 0, solver);
		for (int i = 1; i < n; i++) {
			p[i] = VF.integer("P[" + i + "]", 0, i - 1, solver);
		}
		// ---------------------- constraints -----------------------
		// t[i, j] <-> G
		solver.post(new Constraint("AdjacencyMatrix", new PropIncrementalAdjacencyMatrix(graph, t)));

		// (p[j] == i) ⇔ t[i, j] and AND(!t[k, j], 0 ≤ k < j)
		for (int i = 0; i < n - 1; i++) {
			IntVar I = VF.fixed(i, solver);
			for (int j = 1; j < n; j++) {
				BoolVar[] clause = new BoolVar[i + 1];
				clause[i] = t[i + j * n];
				for (int k = 0; k < i; k++) {
					clause[k] = t[k + j * n].not();
				}
				Constraint c = LCF.and(clause);
				Constraint pij = ICF.arithm(p[j], "=", I);
				LCF.ifThen(pij, c);
				LCF.ifThen(c, pij);
			}
		}

		// p[i] ≤ p[i + 1]
		for (int i = 1; i < n - 1; i++) {
			solver.post(ICF.arithm(p[i], "<=", p[i + 1]));
		}
	}

	/**
	 * Post a symmetry breaking constraint. This constraint is a symmetry breaking for
	 * class of undirected connected graphs.
	 * Note, that this method post this constraint directly, so it cannot be reified.
	 * If you need to get a single {@link Constraint} use {@link #symmetryBreaking(IUndirectedGraphVar, Solver)}.
	 *
	 * This symmetry breaking method based on paper:
	 *     Ulyantsev V., Zakirzyanov I., Shalyto A.
	 * 	   BFS-Based Symmetry Breaking Predicates for DFA Identification
	 *     //Language and Automata Theory and Applications. – Springer International Publishing, 2015. – С. 611-622.
	 *
	 * @param graph graph to be constrainted
	 * @param solver solver to post constraint
	 */
	public static void postSymmetryBreaking(IUndirectedGraphVar graph, Solver solver) {
		// ---------------------- variables ------------------------
		int n = graph.getNbMaxNodes();

		// t[i, j]
		BoolVar[] t = VF.boolArray("T[]", n * n, solver);

		// p[i]
		IntVar[] p = new IntVar[n];
		p[0] = VF.fixed("P[0]", 0, solver);
		for (int i = 1; i < n; i++) {
			p[i] = VF.integer("P[" + i + "]", 0, i - 1, solver);
		}
		// ---------------------- constraints -----------------------
		// t[i, j] <-> G
		solver.post(new Constraint("AdjacencyMatrix", new PropIncrementalAdjacencyUndirectedMatrix(graph, t)));

		// (p[j] == i) ⇔ t[i, j] and AND(!t[k, j], 0 ≤ k < j)
		for (int i = 0; i < n - 1; i++) {
			IntVar I = VF.fixed(i, solver);
			for (int j = 1; j < n; j++) {
				BoolVar[] clause = new BoolVar[i + 1];
				clause[i] = t[i + j * n];
				for (int k = 0; k < i; k++) {
					clause[k] = t[k + j * n].not();
				}
				Constraint c = LCF.and(clause);
				Constraint pij = ICF.arithm(p[j], "=", I);
				LCF.ifThen(pij, c);
				LCF.ifThen(c, pij);
			}
		}

		// p[i] ≤ p[i + 1]
		for (int i = 1; i < n - 1; i++) {
			solver.post(ICF.arithm(p[i], "<=", p[i + 1]));
		}
	}

	/**
	 * Does the same as {@link #postSymmetryBreaking(IDirectedGraphVar, Solver)}, but
	 * post nothing and return single {@link Constraint}.
	 * It can be reified, but beware: it should be posted manually and it can be slower.
	 *
	 * @param graph graph to be constrainted
	 * @param solver solver, connected to graph
	 */
	public static Constraint symmetryBreaking(IUndirectedGraphVar graph, Solver solver) {
		// ---------------------- variables ------------------------
		int n = graph.getNbMaxNodes();

		// t[i, j]
		BoolVar[] t = VF.boolArray("T[]", n * n, solver);

		// p[i]
		IntVar[] p = new IntVar[n];
		p[0] = VF.fixed("P[0]", 0, solver);
		for (int i = 1; i < n; i++) {
			p[i] = VF.integer("P[" + i + "]", 0, i - 1, solver);
		}
		// ---------------------- constraints -----------------------
		// t[i, j] <-> G
		Constraint first = new Constraint("AdjacencyMatrix", new PropIncrementalAdjacencyUndirectedMatrix(graph, t));

		// (p[j] == i) ⇔ t[i, j] and AND(!t[k, j], 0 ≤ k < j)
		Constraint[] secondA = new Constraint[(n - 1) * (n - 1)];
		Constraint[] secondB = new Constraint[(n - 1) * (n - 1)];
		for (int i = 0; i < n - 1; i++) {
			IntVar I = VF.fixed(i, solver);
			for (int j = 1; j < n; j++) {
				BoolVar[] clause = new BoolVar[i + 1];
				clause[i] = t[i + j * n];
				for (int k = 0; k < i; k++) {
					clause[k] = t[k + j * n].not();
				}
				Constraint c = LCF.and(clause);
				Constraint pij = ICF.arithm(p[j], "=", I);
				secondA[i + (j - 1) * (n - 1)] = LCF.ifThen_reifiable(pij, c);
				secondB[i + (j - 1) * (n - 1)] = LCF.ifThen_reifiable(c, pij);
			}
		}

		// p[i] ≤ p[i + 1]
		Constraint[] third = new Constraint[n - 1];
		for (int i = 1; i < n - 1; i++) {
			third[i - 1] = ICF.arithm(p[i], "<=", p[i + 1]);
		}
		Constraint[] all = ArrayUtils.append(new Constraint[] {first}, secondA, secondB, third);
		return LCF.and(all);
	}

	/**
	 * Does the same as {@link #postSymmetryBreaking(IUndirectedGraphVar, Solver)}, but
	 * post nothing and return single {@link Constraint}.
	 * It can be reified, but beware: it should be posted manually and it can be slower.
	 *
	 * @param graph graph to be constrainted
	 * @param solver solver, connected to graph
	 */
	public static Constraint symmetryBreaking(IDirectedGraphVar graph, Solver solver) {
		// ---------------------- variables ------------------------
		int n = graph.getNbMaxNodes();

		// t[i, j]
		BoolVar[] t = VF.boolArray("T[]", n * n, solver);

		// p[i]
		IntVar[] p = new IntVar[n];
		p[0] = VF.fixed("P[0]", 0, solver);
		for (int i = 1; i < n; i++) {
			p[i] = VF.integer("P[" + i + "]", 0, i - 1, solver);
		}
		// ---------------------- constraints -----------------------
		// t[i, j] <-> G
		Constraint first = new Constraint("AdjacencyMatrix", new PropIncrementalAdjacencyMatrix(graph, t));

		// (p[j] == i) ⇔ t[i, j] and AND(!t[k, j], 0 ≤ k < j)
		Constraint[] secondA = new Constraint[(n - 1) * (n - 1)];
		Constraint[] secondB = new Constraint[(n - 1) * (n - 1)];
		for (int i = 0; i < n - 1; i++) {
			IntVar I = VF.fixed(i, solver);
			for (int j = 1; j < n; j++) {
				BoolVar[] clause = new BoolVar[i + 1];
				clause[i] = t[i + j * n];
				for (int k = 0; k < i; k++) {
					clause[k] = t[k + j * n].not();
				}
				Constraint c = LCF.and(clause);
				Constraint pij = ICF.arithm(p[j], "=", I);
				secondA[i + (j - 1) * (n - 1)] = LCF.ifThen_reifiable(pij, c);
				secondB[i + (j - 1) * (n - 1)] = LCF.ifThen_reifiable(c, pij);
			}
		}

		// p[i] ≤ p[i + 1]
		Constraint[] third = new Constraint[n - 1];
		for (int i = 1; i < n - 1; i++) {
			third[i - 1] = ICF.arithm(p[i], "<=", p[i + 1]);
		}
		Constraint[] all = ArrayUtils.append(new Constraint[] {first}, secondA, secondB, third);
		return LCF.and(all);
	}

	/**
	 * Creates a symmetry breaking constraint. This constraint is a symmetry breaking for
	 * class of undirected connected graphs.
	 *
	 * This symmetry breaking method based on paper:
	 *     Codish M. et al.
	 *     Breaking Symmetries in Graph Representation
	 *     //IJCAI. – 2013. – С. 3-9.
	 *
	 * @param graph graph to be constrainted
	 * @param solver solver to associate variables with
	 */
	public static Constraint symmetryBreaking2(IUndirectedGraphVar graph, Solver solver) {
		int n = graph.getNbMaxNodes();
		BoolVar[] t = VF.boolArray("T[]", n * n, solver);
		return new Constraint("symmBreak",
				new PropIncrementalAdjacencyUndirectedMatrix(graph, t),
				new PropSymmetryBreaking(t)
		);
	}

	/**
	 * Creates a symmetry breaking constraint. This constraint is a symmetry breaking for
	 * class of undirected connected graphs.
	 *
	 * This symmetry breaking method based on paper:
	 *     Codish M. et al.
	 *     Breaking Symmetries in Graph Representation
	 *     //IJCAI. – 2013. – С. 3-9.
	 *
	 * @param graph graph to be constrainted
	 * @param solver solver to associate variables with
	 */
	public static Constraint symmetryBreaking3(IUndirectedGraphVar graph, Solver solver) {
		int n = graph.getNbMaxNodes();
		BoolVar[] t = VF.boolArray("T[]", n * n, solver);
		return new Constraint("symmBreakEx",
				new PropIncrementalAdjacencyUndirectedMatrix(graph, t),
				new PropSymmetryBreakingEx(t)
		);
	}

}
