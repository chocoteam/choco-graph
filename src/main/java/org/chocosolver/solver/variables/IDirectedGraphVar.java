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

package org.chocosolver.solver.variables;

import org.chocosolver.util.objects.graphs.DirectedGraph;
import org.chocosolver.util.objects.setDataStructures.ISet;

public interface IDirectedGraphVar extends IGraphVar<DirectedGraph> {

	/**
	 * Get the set of successors of vertex 'idx' in the lower bound graph
	 * (mandatory outgoing arcs)
	 * @param idx	a vertex
	 * @return The set of successors of 'idx' in LB
	 */
	public ISet getMandSuccOf(int idx);

	/**
	 * Get the set of successors of vertex 'idx'
	 * in the upper bound graph (potential outgoing arcs)
	 * @param idx	a vertex
	 * @return The set of successors of 'idx' in UB
	 */
	public ISet getPotSuccOf(int idx);

	/**
	 * Get the set of predecessors of vertex 'idx' in the lower bound graph
	 * (mandatory ingoing arcs)
	 * @param idx	a vertex
	 * @return The set of predecessors of 'idx' in LB
	 */
	public ISet getMandPredOf(int idx);

	/**
	 * Get the set of predecessors of vertex 'idx'
	 * in the upper bound graph (potential ingoing arcs)
	 * @param idx	a vertex
	 * @return The set of predecessors of 'idx' in UB
	 */
	public ISet getPotPredOf(int idx);
}
