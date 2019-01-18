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
import org.chocosolver.solver.ICause;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.util.objects.graphs.DirectedGraph;
import org.chocosolver.util.objects.setDataStructures.ISet;

public class DirectedGraphVar extends GraphVar<DirectedGraph> {

	////////////////////////////////// GRAPH PART ///////////////////////////////////////

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	/**
	 * Creates a graph variable
	 *
	 * @param name
	 * @param solver
	 * @param LB
	 * @param UB
	 */
	public DirectedGraphVar(String name, Model solver, DirectedGraph LB, DirectedGraph UB) {
		super(name, solver, LB, UB);
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	@Override
	public boolean removeArc(int x, int y, ICause cause) throws ContradictionException {
		assert cause != null;
		if (LB.arcExists(x, y)) {
			this.contradiction(cause, "remove mandatory arc " + x + "->" + y);
			return false;
		}
		if (UB.removeArc(x, y)) {
			if (reactOnModification) {
				delta.add(x, GraphDelta.AR_TAIL, cause);
				delta.add(y, GraphDelta.AR_HEAD, cause);
			}
			GraphEventType e = GraphEventType.REMOVE_ARC;
			notifyPropagators(e, cause);
			return true;
		}
		return false;
	}

	@Override
	public boolean enforceArc(int x, int y, ICause cause) throws ContradictionException {
		assert cause != null;
		enforceNode(x, cause);
		enforceNode(y, cause);
		if (UB.arcExists(x, y)) {
			if (LB.addArc(x, y)) {
				if (reactOnModification) {
					delta.add(x, GraphDelta.AE_TAIL, cause);
					delta.add(y, GraphDelta.AE_HEAD, cause);
				}
				GraphEventType e = GraphEventType.ADD_ARC;
				notifyPropagators(e, cause);
				return true;
			}
			return false;
		}
		this.contradiction(cause, "enforce arc which is not in the domain");
		return false;
	}

	/**
	 * Get the set of successors of vertex 'idx' in the lower bound graph
	 * (mandatory outgoing arcs)
	 *
	 * @param idx a vertex
	 * @return The set of successors of 'idx' in LB
	 */
	public ISet getMandSuccOf(int idx) {
		return getMandSuccOrNeighOf(idx);
	}

	/**
	 * Get the set of predecessors of vertex 'idx' in the lower bound graph
	 * (mandatory ingoing arcs)
	 *
	 * @param idx a vertex
	 * @return The set of predecessors of 'idx' in LB
	 */
	public ISet getMandPredOf(int idx) {
		return getMandPredOrNeighOf(idx);
	}

	/**
	 * Get the set of predecessors of vertex 'idx'
	 * in the upper bound graph (potential ingoing arcs)
	 *
	 * @param idx a vertex
	 * @return The set of predecessors of 'idx' in UB
	 */
	public ISet getPotPredOf(int idx) {
		return getPotPredOrNeighOf(idx);
	}

	/**
	 * Get the set of successors of vertex 'idx'
	 * in the upper bound graph (potential outgoing arcs)
	 *
	 * @param idx a vertex
	 * @return The set of successors of 'idx' in UB
	 */
	public ISet getPotSuccOf(int idx) {
		return getPotSuccOrNeighOf(idx);
	}

	@Override
	public boolean isDirected() {
		return true;
	}
}
