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

package org.chocosolver.graphsolver.variables;

import org.chocosolver.graphsolver.variables.delta.GraphDelta;
import org.chocosolver.graphsolver.variables.delta.GraphDeltaMonitor;
import org.chocosolver.solver.ICause;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.variables.events.IEventType;
import org.chocosolver.solver.variables.impl.AbstractVariable;
import org.chocosolver.util.objects.graphs.IGraph;
import org.chocosolver.util.objects.setDataStructures.ISet;

import java.lang.reflect.Field;

public abstract class GraphVar<E extends IGraph> extends AbstractVariable implements Variable {

	public static final int GRAPH = 1 << 7; // beware, this relies on choco-solver

	//////////////////////////////// GRAPH PART /////////////////////////////////////////
	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	protected E UB, LB;
	protected GraphDelta delta;
	protected int n;
	///////////// Attributes related to Variable ////////////
	protected boolean reactOnModification;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	/**
	 * Creates a graph variable
	 *
	 * @param solver
	 */
	protected GraphVar(String name, Model solver, E LB, E UB) {
		super(name, solver);
		this.LB = LB;
		this.UB = UB;
		this.n = UB.getNbMaxNodes();
		assert n == LB.getNbMaxNodes();
		Field f = null; //NoSuchFieldException
		try {
			AbstractVariable me = this;
			f = me.getClass().getSuperclass().getSuperclass().getDeclaredField("scheduler");
			f.setAccessible(true);
			f.set(me, new GraphEvtScheduler());
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	@Override
	public boolean isInstantiated() {
		if (getPotentialNodes().size() != getMandatoryNodes().size()) {
			return false;
		}
		ISet suc;
		for (int i : getUB().getNodes()) {
			suc = UB.getSuccOrNeighOf(i);
			if (suc.size() != getLB().getSuccOrNeighOf(i).size()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Remove node x from the domain
	 * Removes x from the upper bound graph
	 *
	 * @param x     node's index
	 * @param cause algorithm which is related to the removal
	 * @return true iff the removal has an effect
	 */
	public boolean removeNode(int x, ICause cause) throws ContradictionException {
		assert cause != null;
		assert (x >= 0 && x < n);
		if (LB.getNodes().contains(x)) {
			this.contradiction(cause, "remove mandatory node");
			return true;
		} else if (!UB.getNodes().contains(x)) {
			return false;
		}
		ISet nei = UB.getSuccOrNeighOf(x);
		for (int i : nei) {
			removeArc(x, i, cause);
		}
		nei = UB.getPredOrNeighOf(x);
		for (int i : nei) {
			removeArc(i, x, cause);
		}
		if (UB.removeNode(x)) {
			if (reactOnModification) {
				delta.add(x, GraphDelta.NR, cause);
			}
			GraphEventType e = GraphEventType.REMOVE_NODE;
			notifyPropagators(e, cause);
			return true;
		}
		return false;
	}

	/**
	 * Enforce the node x to belong to any solution
	 * Adds x to the lower bound graph
	 *
	 * @param x     node's index
	 * @param cause algorithm which is related to the modification
	 * @return true iff the enforcing has an effect
	 */
	public boolean enforceNode(int x, ICause cause) throws ContradictionException {
		assert cause != null;
		assert (x >= 0 && x < n);
		if (UB.getNodes().contains(x)) {
			if (LB.addNode(x)) {
				if (reactOnModification) {
					delta.add(x, GraphDelta.NE, cause);
				}
				GraphEventType e = GraphEventType.ADD_NODE;
				notifyPropagators(e, cause);
				return true;
			}
			return false;
		}
		this.contradiction(cause, "enforce node which is not in the domain");
		return true;
	}

	/**
	 * Remove arc (or edge in case of undirected graph variable) (x,y) from the domain
	 * Removes (x,y) from the upper bound graph
	 *
	 * @param x     node's index
	 * @param y     node's index
	 * @param cause algorithm which is related to the removal
	 * @return true iff the removal has an effect
	 * @throws ContradictionException if the arc was mandatory
	 */
	public abstract boolean removeArc(int x, int y, ICause cause) throws ContradictionException;

	/**
	 * Enforces arc (or edge in case of undirected graph variable) (x,y) to belong to any solution
	 * Adds (x,y) to the lower bound graph
	 *
	 * @param x     node's index
	 * @param y     node's index
	 * @param cause algorithm which is related to the removal
	 * @return true iff the enforcing has an effect
	 */
	public abstract boolean enforceArc(int x, int y, ICause cause) throws ContradictionException;

	//***********************************************************************************
	// ACCESSORS
	//***********************************************************************************

	/**
	 * @return the lower bound graph (having mandatory nodes and arcs)
	 */
	public E getLB() {
		return LB;
	}

	/**
	 * @return the upper bound graph (having possible nodes and arcs)
	 */
	public E getUB() {
		return UB;
	}

	/**
	 * Get the set of successors (if directed) or neighbors (if undirected) of vertex 'idx'
	 * in the lower bound graph (mandatory outgoing arcs)
	 *
	 * @param idx a vertex
	 * @return The set of successors (if directed) or neighbors (if undirected) of 'idx' in LB
	 */
	public ISet getMandSuccOrNeighOf(int idx) {
		return LB.getSuccOrNeighOf(idx);
	}

	/**
	 * Get the set of successors (if directed) or neighbors (if undirected) of vertex 'idx'
	 * in the upper bound graph (potential outgoing arcs)
	 *
	 * @param idx a vertex
	 * @return The set of successors (if directed) or neighbors (if undirected) of 'idx' in UB
	 */
	public ISet getPotSuccOrNeighOf(int idx) {
		return UB.getSuccOrNeighOf(idx);
	}

	/**
	 * Get the set of predecessors (if directed) or neighbors (if undirected) of vertex 'idx'
	 * in the lower bound graph (mandatory ingoing arcs)
	 *
	 * @param idx a vertex
	 * @return The set of predecessors (if directed) or neighbors (if undirected) of 'idx' in LB
	 */
	public ISet getMandPredOrNeighOf(int idx) {
		return LB.getPredOrNeighOf(idx);
	}

	/**
	 * Get the set of predecessors (if directed) or neighbors (if undirected) of vertex 'idx'
	 * in the upper bound graph (potential ingoing arcs)
	 *
	 * @param idx a vertex
	 * @return The set of predecessors (if directed) or neighbors (if undirected) of 'idx' in UB
	 */
	public ISet getPotPredOrNeighOf(int idx) {
		return UB.getPredOrNeighOf(idx);
	}

	/**
	 * @return the maximum number of node the graph variable may have.
	 * Nodes are comprised in the interval [0,getNbMaxNodes()]
	 * Therefore, any vertex should be strictly lower than getNbMaxNodes()
	 */
	public int getNbMaxNodes() {
		return n;
	}

	/**
	 * @return the node set of the lower bound graph,
	 * i.e. nodes that belong to every solution
	 */
	public ISet getMandatoryNodes() {
		return LB.getNodes();
	}

	/**
	 * @return the node set of the upper bound graph,
	 * i.e. nodes that may belong to one solution
	 */
	public ISet getPotentialNodes() {
		return UB.getNodes();
	}

	/**
	 * @return true iff the graph is directed. It is undirected otherwise.
	 */
	public abstract boolean isDirected();


	//***********************************************************************************
	// VARIABLE STUFF
	//***********************************************************************************


	@Override
	public GraphDelta getDelta() {
		return delta;
	}

	@Override
	public int getTypeAndKind() {
		return VAR | GRAPH;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("graph_var ").append(getName());
		if (isInstantiated()) {
			sb.append("\nValue: \n");
			sb.append(UB.toString());
		} else {
			sb.append("\nUpper bound: \n");
			sb.append(UB.toString());
			sb.append("\nLower bound: \n");
			sb.append(LB.toString());
		}
		return sb.toString();
	}

	@Override
	public void createDelta() {
		if (!reactOnModification) {
			reactOnModification = true;
			delta = new GraphDelta(getEnvironment());
		}
	}

	/**
	 * Make the propagator 'prop' have an incremental filtering w.r.t. this graph variable
	 *
	 * @param propagator A propagator involving this graph variable
	 * @return A new instance of GraphDeltaMonitor to make incremental propagators
	 */
	public GraphDeltaMonitor monitorDelta(ICause propagator) {
		createDelta();
		return new GraphDeltaMonitor(delta, propagator);
	}

	@Override
	public void notifyMonitors(IEventType event) throws ContradictionException {
		for (int i = mIdx - 1; i >= 0; i--) {
			monitors[i].onUpdate(this, event);
		}
	}

	//***********************************************************************************
	// SOLUTIONS : STORE AND RESTORE
	//***********************************************************************************

	/**
	 * @return the value of the graph variable represented through an adjacency matrix
	 * plus a set of nodes (last row of the matrix).
	 * This method is not supposed to be used except for restoring solutions.
	 */
	public boolean[][] getValue() {
		int n = getUB().getNbMaxNodes();
		boolean[][] vals = new boolean[n + 1][n];
		for (int i : getLB().getNodes()) {
			for (int j : getLB().getSuccOrNeighOf(i)) {
				vals[i][j] = true; // arc in
			}
			vals[n][i] = true; // node in
		}
		return vals;
	}

	/**
	 * Instantiates <code>this</code> to value which represents an adjacency
	 * matrix plus a set of nodes (last row of the matrix).
	 * This method is not supposed to be used except for restoring solutions.
	 *
	 * @param value value of <code>this</code>
	 * @param cause
	 * @throws ContradictionException if the arc was mandatory
	 */
	public void instantiateTo(boolean[][] value, ICause cause) throws ContradictionException {
		int n = value.length - 1;
		for (int i = 0; i < n; i++) {
			if (value[n][i]) {//nodes
				enforceNode(i, cause);
			} else {
				removeNode(i, cause);
			}
			for (int j = 0; j < n; j++) {
				if (value[i][j]) {//arcs
					enforceArc(i, j, cause);
				} else {
					removeArc(i, j, cause);
				}
			}
		}
	}

	//***********************************************************************************
	// GraphViz
	//***********************************************************************************

	/**
	 * Export domain to graphviz format, see http://www.webgraphviz.com/
	 * Mandatory elements are in red whereas potential elements are in black
	 *
	 * @return a String encoding the domain of the variable
	 */
	public String graphVizExport() {
		boolean directed = isDirected();
		String arc = directed ? " -> " : " -- ";
		StringBuilder sb = new StringBuilder();
		sb.append(directed ? "digraph " : "graph ").append(getName() + "{\n");
		sb.append("node [color = red, fontcolor=red]; ");
		for (int i : getMandatoryNodes()) sb.append(i + " ");
		sb.append(";\n");
		for (int i : getMandatoryNodes()) {
			for (int j : getMandSuccOrNeighOf(i)) {
				if (directed || i < j) sb.append(i + arc + j + " [color=red] ;\n");
			}
		}
		if (getMandatoryNodes().size() < getPotentialNodes().size()) {
			sb.append("node [color = black, fontcolor=black]; ");
			for (int i : getPotentialNodes()) if (!getMandatoryNodes().contains(i)) sb.append(i + " ");
			sb.append(";\n");
		}
		for (int i : getPotentialNodes()) {
			for (int j : getPotSuccOrNeighOf(i)) {
				if ((directed || i < j) && !getMandSuccOrNeighOf(i).contains(j)) sb.append(i + arc + j + " ;\n");
			}
		}
		sb.append("}");
		return sb.toString();
	}
}
