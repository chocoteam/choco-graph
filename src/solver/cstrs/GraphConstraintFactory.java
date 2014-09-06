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
import solver.constraints.PropagatorPriority;
import solver.cstrs.cycles.PropACyclic;
import solver.cstrs.basic.*;
import solver.cstrs.connectivity.PropConnected;
import solver.cstrs.connectivity.PropKCC;
import solver.cstrs.connectivity.PropKSCC;
import solver.cstrs.inclusion.PropInclusion;
import solver.cstrs.channeling.edges.*;
import solver.cstrs.channeling.nodes.PropNodeBoolChannel;
import solver.cstrs.channeling.nodes.PropNodeBoolsChannel;
import solver.cstrs.channeling.nodes.PropNodeSetChannel;
import solver.cstrs.degree.*;
import solver.cstrs.toCheck.path.PropPathNoCycle;
import solver.cstrs.toCheck.tsp.undirected.PropCycleEvalObj;
import solver.cstrs.toCheck.tsp.undirected.PropCycleNoSubtour;
import solver.cstrs.toCheck.tsp.undirected.lagrangianRelaxation.PropLagr_OneTree;
import solver.cstrs.tree.PropArborescences;
import solver.exception.ContradictionException;
import solver.variables.*;
import util.ESat;
import util.objects.graphs.Orientation;
import util.objects.setDataStructures.ISet;
import util.tools.ArrayUtils;

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
		return new Constraint("nb_nodes", new PropKNodes(g,nb));
	}

	/**
	 * Create a constraint to force the number of arcs in g to be equal to nb
	 * @param g	a directed graph variable
	 * @param nb	an integer variable indicating the expected number of arcs in g
	 * @return A constraint to force the number of arcs in g to be equal to nb
	 */
	public static Constraint nb_arcs(IDirectedGraphVar g, IntVar nb){
		return new Constraint("nb_arcs", new PropKArcs(g,nb));
	}

	/**
	 * Create a constraint to force the number of edges in g to be equal to nb
	 * @param g	an undirected graph variable
	 * @param nb an integer variable indicating the expected number of edges in g
	 * @return A constraint to force the number of edges in g to be equal to nb
	 */
	public static Constraint nb_edges(IUndirectedGraphVar g, IntVar nb){
		return new Constraint("nb_edges", new PropKArcs(g,nb));
	}

	// loops

	/**
	 * Create a constraint which makes sure every node has a loop
	 * i.e. vertex i in g => arc (i,i) in g
	 * @param g	a graph variable
	 * @return A constraint which makes sure every node has a loop
	 */
	public static Constraint each_node_has_loop(IGraphVar g){
		return new Constraint("each_node_has_loop", new PropEachNodeHasLoop(g));
	}

	/**
	 * Create a constraint which ensures g has nb loops
	 * |(i,i) in g| = nb
	 * @param g	a graph variable
	 * @param nb an integer variable counting the number of loops in g
	 * @return A constraint which ensures g has nb loops
	 */
	public static Constraint nb_loops(IGraphVar g, IntVar nb){
		return new Constraint("nb_loops", new PropKLoops(g,nb));
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
	 * @param g			directed graph var
	 * @param degrees	integer array giving the degree of each node
	 * @return an outer degree constraint
	 */
	public static Constraint out_degrees(IDirectedGraphVar g, IntVar[] degrees){
		return new Constraint("out_degrees",new PropNodeDegree_Var(g, Orientation.SUCCESSORS, degrees));
	}




	//***********************************************************************************
	// ACYCLICITY CONSTRAINTS
	//***********************************************************************************




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
		return new Constraint("NbCC",new PropKCC(g,nb));
	}
	/**
	 * Creates a strong connectedness constraint which ensures that g is strongly connected
	 * @param g	a directed graph variable
	 * @return A strong connectedness constraint which ensures that g is strongly connected
	 */
	public static Constraint strongly_connected(IDirectedGraphVar g){
		return nb_strongly_connected_components(g, VF.bounded("nbSCC", 0, g.getNbMaxNodes(), g.getSolver()));
	}

	/**
	 * Creates a strong connectedness constraint which ensures that g has nb strongly connected components
	 * @param g	a directed graph variable
	 * @param nb an integer variable indicating the expected number of connected components in g
	 * @return A strong connectedness constraint which ensures that g has nb strongly connected components
	 */
	public static Constraint nb_strongly_connected_components(IDirectedGraphVar g, IntVar nb){
		return new Constraint("NbSCC",new PropKSCC(g,nb));
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
	 * @return a directed tree constraint
	 */
	public static Constraint directed_tree(IDirectedGraphVar g, IntVar root){
		return new Constraint("directed_tree",
				new PropArborescences(g),
				new PropNodeDegree_AtMost_Coarse(g, Orientation.PREDECESSORS, 1),
				// ad hoc propagator to get the root (naive filtering)
				new Propagator<Variable>(new Variable[]{g,root}, PropagatorPriority.BINARY, false) {
					@Override
					public void propagate(int evtmask) throws ContradictionException {
						IntVar root = (IntVar) vars[1];
						IDirectedGraphVar g = (IDirectedGraphVar) vars[0];
						int n = g.getNbMaxNodes();
						root.updateLowerBound(0,aCause);
						root.updateUpperBound(n-1,aCause);
						int pos = -1; // identify impossible roots
						for(int i=root.getLB();i<=root.getUB();i=root.nextValue(i)){
							if(g.getMandPredOf(i).getSize() > 0
									|| !g.getPotentialNodes().contain(i)){
								root.removeValue(i,aCause);
							}else{
								if(pos == -1){
									pos = i;
								}else{
									pos = -2;
								}
							}
						}
						if(pos>=0){ // unique root found
							g.enforceNode(pos, aCause);
							ISet preds = g.getPotPredOf(pos);
							for(int p=preds.getFirstElement();p>=0;p=preds.getNextElement()){
								g.removeArc(p,pos,aCause);
							}
						}
						for(int i=g.getPotentialNodes().getFirstElement();i>=0;i=g.getPotentialNodes().getNextElement()){
							ISet preds = g.getPotPredOf(i);
							if(!root.contains(i)){
								// non-root nodes must have exactly one predecessor
								if(g.getMandatoryNodes().contain(i)){
									if(preds.getSize()==1) {
										g.enforceArc(preds.getFirstElement(), i, aCause);
									}
								}else if(preds.getSize() == 0){
									g.removeNode(i,aCause);
								}
							}
							if(preds.getSize() == 0 && g.getMandatoryNodes().contain(i)) {
								root.instantiateTo(i, aCause);
							}
						}
					}

					@Override
					public ESat isEntailed() {
						IntVar root = (IntVar) vars[1];
						IDirectedGraphVar g = (IDirectedGraphVar) vars[0];
						int n = g.getNbMaxNodes();
						if(root.getUB()<0 || root.getLB()>=n){
							return ESat.FALSE;
						}
						int pos = -1; // identify impossible roots
						for(int i=root.getLB();i<=root.getUB();i=root.nextValue(i)){
							if(!(g.getMandPredOf(i).getSize() > 0
									|| !g.getPotentialNodes().contain(i))){
								pos = i;
							}
						}
						if(pos == -1){
							return ESat.FALSE;
						}
						pos = -1;
						for(int i=g.getMandatoryNodes().getFirstElement();i>=0;i=g.getMandatoryNodes().getNextElement()){
							ISet preds = g.getPotPredOf(i);
							if(preds.getSize()==0){
								if(pos == -1){
									pos = i;
								}else{
									return ESat.FALSE; // two roots;
								}
							}
						}
						if(isCompletelyInstantiated()){
							return ESat.TRUE;
						}
						return ESat.UNDEFINED;
					}
				}
		);
	}

	/**
	 * Creates a directed forest constraint :
	 * g form is composed of several disjoint (potentially singleton) arborescences
	 * @param g	a directed graph variable
	 * @return a directed forest constraint
	 */
	public static Constraint directed_forest(IDirectedGraphVar g){
		return new Constraint("directed_forest",new PropArborescences(g));
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
				new PropKCC(g, nb),
				new PropNbCliques(g, nb) // redundant propagator
		);
	}




	//***********************************************************************************
	// CYCLES AND PATHS CONSTRAINTS
	//***********************************************************************************





	/** TODO cycle no subtour
	 * g must form a Hamiltonian cycle
	 *
	 * @param g graph variable representing a Hamiltonian cycle
	 * @return a hamiltonian cycle constraint
	 */
	public static Constraint hamiltonianCycle(IUndirectedGraphVar g) {
		int m = 0;
		int n = g.getNbMaxNodes();
		for(int i=0;i<n;i++){
			m += g.getPotNeighOf(i).getSize();
		}
		m /= 2;
		if(m<20*n){
			return new Constraint("Graph_HamiltonianCycle",
					new PropNodeDegree_AtLeast_Incr(g, 2),
					new PropNodeDegree_AtMost_Incr(g, 2),
					new PropCycleNoSubtour(g)
			);
		}else{
			return new Constraint("Graph_HamiltonianCycle",
					new PropNodeDegree_AtLeast_Coarse(g, 2),
					new PropNodeDegree_AtMost_Incr(g, 2),
					new PropCycleNoSubtour(g)
			);
		}
	}
	// ---



	// ----

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
//		if (STRONG_FILTER) {
//			PropArborescence arbo = new PropArborescence(GRAPHVAR, ORIGIN, true);
//			PropAntiArborescence aa = new PropAntiArborescence(GRAPHVAR, DESTINATION, true);
//			PropAllDiffGraphIncremental ad = new PropAllDiffGraphIncremental(GRAPHVAR, n - 1);
//			props = ArrayUtils.append(props,ArrayUtils.toArray(arbo, aa, ad));
//		}
		return new Constraint("Graph_HamiltonianPath",props);
	}
}
