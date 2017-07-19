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

import org.chocosolver.graphsolver.variables.delta.IGraphDeltaMonitor;
import org.chocosolver.solver.ICause;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.objects.graphs.IGraph;
import org.chocosolver.util.objects.setDataStructures.ISet;

public interface IGraphVar<E extends IGraph> extends Variable {

	//////////////////////////////// GRAPH PART /////////////////////////////////////////
	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	/**
	 * Remove node x from the domain
	 * Removes x from the upper bound graph
	 *
	 * @param x     node's index
	 * @param cause algorithm which is related to the removal
	 * @return true iff the removal has an effect
	 */
	boolean removeNode(int x, ICause cause) throws ContradictionException ;

	/**
	 * Enforce the node x to belong to any solution
	 * Adds x to the lower bound graph
	 *
	 * @param x     node's index
	 * @param cause algorithm which is related to the modification
	 * @return true iff the enforcing has an effect
	 */
	boolean enforceNode(int x, ICause cause) throws ContradictionException ;

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
	boolean removeArc(int x, int y, ICause cause) throws ContradictionException;

	/**
	 * Enforces arc (or edge in case of undirected graph variable) (x,y) to belong to any solution
	 * Adds (x,y) to the lower bound graph
	 *
	 * @param x     node's index
	 * @param y     node's index
	 * @param cause algorithm which is related to the removal
	 * @return true iff the enforcing has an effect
	 */
	boolean enforceArc(int x, int y, ICause cause) throws ContradictionException;

	//***********************************************************************************
	// INCREMENTALITY
	//***********************************************************************************

	/**
	 * Make the propagator 'prop' have an incremental filtering w.r.t. this graph variable
	 * @param prop A propagator involving this graph variable
	 * @return A new instance of IGraphDeltaMonitor to make incremental propagators
	 */
	IGraphDeltaMonitor monitorDelta(ICause prop);

	//***********************************************************************************
	// ACCESSORS
	//***********************************************************************************

	/**
	 * @return the maximum number of node the graph variable may have.
	 * Nodes are comprised in the interval [0,getNbMaxNodes()]
	 * Therefore, any vertex should be strictly lower than getNbMaxNodes()
	 */
	int getNbMaxNodes();

	/**
	 * @return the node set of the lower bound graph,
	 * i.e. nodes that belong to every solution
	 */
	ISet getMandatoryNodes();

	/**
	 * @return the node set of the upper bound graph,
	 * i.e. nodes that may belong to one solution
	 */
	ISet getPotentialNodes();

	/**
	 * Get the set of successors (if directed) or neighbors (if undirected) of vertex 'idx'
	 * in the lower bound graph (mandatory outgoing arcs)
	 * @param idx	a vertex
	 * @return The set of successors (if directed) or neighbors (if undirected) of 'idx' in LB
	 */
	ISet getMandSuccOrNeighOf(int idx);

	/**
	 * Get the set of successors (if directed) or neighbors (if undirected) of vertex 'idx'
	 * in the upper bound graph (potential outgoing arcs)
	 * @param idx	a vertex
	 * @return The set of successors (if directed) or neighbors (if undirected) of 'idx' in UB
	 */
	ISet getPotSuccOrNeighOf(int idx);

	/**
	 * Get the set of predecessors (if directed) or neighbors (if undirected) of vertex 'idx'
	 * in the lower bound graph (mandatory ingoing arcs)
	 * @param idx	a vertex
	 * @return The set of predecessors (if directed) or neighbors (if undirected) of 'idx' in LB
	 */
	ISet getMandPredOrNeighOf(int idx);

	/**
	 * Get the set of predecessors (if directed) or neighbors (if undirected) of vertex 'idx'
	 * in the upper bound graph (potential ingoing arcs)
	 * @param idx	a vertex
	 * @return The set of predecessors (if directed) or neighbors (if undirected) of 'idx' in UB
	 */
	ISet getPotPredOrNeighOf(int idx);

	/**
	 * @return the lower bound graph (having mandatory nodes and arcs)
	 */
    E getLB() ;

	/**
	 * @return the upper bound graph (having possible nodes and arcs)
	 */
    E getUB() ;

	/**
	 * @return true iff the graph is directed. It is undirected otherwise.
	 */
	boolean isDirected();

	//***********************************************************************************
	// SOLUTIONS : STORE AND RESTORE
	//***********************************************************************************

	/**
	 * @return the value of the graph variable represented through an adjacency matrix
	 *         plus a set of nodes (last row of the matrix).
	 *         This method is not supposed to be used except for restoring solutions.
	 */
	boolean[][] getValue() ;

	/**
	 * Instantiates <code>this</code> to value which represents an adjacency
	 * matrix plus a set of nodes (last row of the matrix).
	 * This method is not supposed to be used except for restoring solutions.
	 *
	 * @param value value of <code>this</code>
	 * @param cause
	 * @throws ContradictionException if the arc was mandatory
	 */
	void instantiateTo(boolean[][] value, ICause cause) throws ContradictionException ;
}
