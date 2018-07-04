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

package org.chocosolver.graphsolver.cstrs.tree;

import org.chocosolver.graphsolver.variables.DirectedGraphVar;
import org.chocosolver.solver.exception.ContradictionException;

import java.util.BitSet;

/**
 * Arborescence constraint (simplification from tree constraint) based on dominators
 *
 * @author Jean-Guillaume Fages
 */
public class PropArborescence extends PropArborescences {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	protected final int root;
	protected final BitSet visited;
	protected final int[] fifo;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************


	public PropArborescence(DirectedGraphVar graph, int root) {
		this(graph, root, false);
	}

	public PropArborescence(DirectedGraphVar graph, int root, boolean simple) {
		super(graph, simple);
		this.root = root;
		this.visited = new BitSet(n);
		this.fifo = new int[n];
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	@Override
	public void propagate(int evt) throws ContradictionException {
		g.enforceNode(root, this);
		// explore the graph from the root
		explore();
		// remove unreachable nodes
		for (int o = visited.nextClearBit(0); o < n; o = visited.nextClearBit(o + 1)) {
			g.removeNode(o, this);
		}
		super.propagate(evt);
	}

	@Override
	protected void reset() {
		// reset data structure
		for (int i = 0; i < n + 1; i++) {
			connectedGraph.getSuccOf(i).clear();
			connectedGraph.getPredOf(i).clear();
		}
		for (int i = 0; i < n; i++) {
			for (int y : g.getPotPredOf(i)) {
				connectedGraph.addArc(y, i);
			}
			if (!g.getPotentialNodes().contains(i)) {
				connectedGraph.addArc(n, i);
			}
		}
		connectedGraph.addArc(n, root);
	}

	protected void explore() {
		visited.clear();
		int first = 0;
		int last = 0;
		int i = root;
		fifo[last++] = i;
		visited.set(i);
		while (first < last) {
			i = fifo[first++];
			for (int j : g.getPotSuccOf(i)) {
				if (!visited.get(j)) {
					visited.set(j);
					fifo[last++] = j;
				}
			}
		}
	}
}
