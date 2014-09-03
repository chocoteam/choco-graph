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

package solver.variables;

import solver.ICause;
import solver.constraints.Propagator;
import solver.exception.ContradictionException;
import solver.variables.delta.IGraphDeltaMonitor;
import util.objects.graphs.IGraph;
import util.objects.setDataStructures.ISet;

public interface IGraphVar<E extends IGraph> extends Variable {

	//////////////////////////////// GRAPH PART /////////////////////////////////////////
	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	/**
	 * Remove node x from the maximal partial subgraph
	 *
	 * @param x     node's index
	 * @param cause algorithm which is related to the removal
	 * @return true iff the removal has an effect
	 */
	public boolean removeNode(int x, ICause cause) throws ContradictionException ;

	/**
	 * Enforce the node x to belong to any partial subgraph
	 *
	 * @param x     node's index
	 * @param cause algorithm which is related to the modification
	 * @return true iff the node is effectively added to the mandatory structure
	 */
	public boolean enforceNode(int x, ICause cause) throws ContradictionException ;

	/**
	 * Remove node y from the neighborhood of node x from the maximal partial subgraph
	 *
	 * @param x     node's index
	 * @param y     node's index
	 * @param cause algorithm which is related to the removal
	 * @return true iff the removal has an effect
	 * @throws solver.exception.ContradictionException
	 */
	public abstract boolean removeArc(int x, int y, ICause cause) throws ContradictionException;

	/**
	 * Enforce the node y into the neighborhood of node x in any partial subgraph
	 *
	 * @param x     node's index
	 * @param y     node's index
	 * @param cause algorithm which is related to the removal
	 * @return true iff the node y is effectively added in the neighborhooh of node x
	 */
	public abstract boolean enforceArc(int x, int y, ICause cause) throws ContradictionException;

	//***********************************************************************************
	// ACCESSORS
	//***********************************************************************************

	public int getNbMaxNodes();

	public ISet getMandatoryNodes();

	public ISet getPotentialNodes();

	public ISet getMandNeighOf(int idx);

	public ISet getPotNeighOf(int idx);

	public ISet getMandSuccOf(int idx);

	public ISet getPotSuccOf(int idx);

	public ISet getMandPredOf(int idx);

	public ISet getPotPredOf(int idx);

	public ISet getMandSuccOrNeighOf(int idx);

	public ISet getPotSuccOrNeighOf(int idx);

	public ISet getMandPredOrNeighOf(int idx);

	public ISet getPotPredOrNeighOf(int idx);

//    /**
//     * @return the graph representing the domain of the variable graph
//     */
//    public E getKernelGraph() ;
//
//    /**
//     * @return the graph representing the instantiated values (nodes and edges) of the variable graph
//     */
//    public E getEnvelopGraph() ;

	/**
	 * @return true iff the graph is directed
	 */
	public abstract boolean isDirected();

	/**
	 * @return true iff the graph variable domain is represented by graphs (otherwise it can be a view over set variables)
	 */
	public abstract boolean isExplicit();

	//***********************************************************************************
	// SOLUTIONS : STORE AND RESTORE
	//***********************************************************************************

	/**
	 * @return the value of the graph variable represented through an adjacency matrix
	 *         plus a set of nodes (last line of value).
	 *         This method is not supposed to be used except for restoring solutions.
	 */
	public boolean[][] getValue() ;

	/**
	 * Instantiates <code>this</code> to value which represents an adjacency
	 * matrix plus a set of nodes (last line of value).
	 * This method is not supposed to be used except for restoring solutions.
	 *
	 * @param value value of <code>this</code>
	 * @param cause
	 * @throws solver.exception.ContradictionException
	 */
	public void instantiateTo(boolean[][] value, ICause cause) throws ContradictionException ;

	IGraphDeltaMonitor monitorDelta(Propagator prop);
}
