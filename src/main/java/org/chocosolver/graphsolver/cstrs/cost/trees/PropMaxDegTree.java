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

package org.chocosolver.graphsolver.cstrs.cost.trees;

import org.chocosolver.graphsolver.variables.GraphEventType;
import org.chocosolver.graphsolver.variables.IUndirectedGraphVar;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.setDataStructures.ISet;

import java.util.BitSet;

/**
 * Redundant filtering for a tree for which the max degree of each vertex is restricted:
 * if dMax(i) = dMax(j) = 1, then edge (i,j) is infeasible
 * if dMax(k) = 2 and (i,k) is already forced, then (k,j) is infeasible
 * ...
 *
 * @author Jean-Guillaume Fages
 */
public class PropMaxDegTree extends Propagator<IUndirectedGraphVar> {


	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	protected int n;
	protected int[] dMax;
	private int[] counter;
	private BitSet oneNode;
	private int[] list;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropMaxDegTree(IUndirectedGraphVar g, int[] maxDegrees) {
		super(new IUndirectedGraphVar[]{g}, PropagatorPriority.LINEAR, false);
		n = maxDegrees.length;
		oneNode = new BitSet(n);
		counter = new int[n];
		dMax = maxDegrees;
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	@Override
	public int getPropagationConditions(int vIdx) {
		return GraphEventType.ADD_ARC.getMask();
	}

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		preprocessOneNodes();
		IUndirectedGraphVar g = vars[0];
		if (oneNode.cardinality() < n) {
			for (int i = 0; i < n; i++) {
				ISet nei = g.getPotNeighOf(i);
				if (oneNode.get(i)) {
					for (int j : nei) {
						if (oneNode.get(j)) {
							if (!g.getMandNeighOf(i).contains(j)) {
								g.removeArc(i, j, this);
							}
						}
					}
				}
			}
		}
	}

	private void preprocessOneNodes() throws ContradictionException {
		ISet nei;
		oneNode.clear();
		for (int i = 0; i < n; i++) {
			counter[i] = 0;
		}
		IUndirectedGraphVar g = vars[0];
		int[] maxDegree = dMax;
		if (list == null) {
			list = new int[n];
		}
		int first = 0;
		int last = 0;
		for (int i = 0; i < n; i++) {
			if (maxDegree[i] == 1) {
				list[last++] = i;
				oneNode.set(i);
			}
		}
		while (first < last) {
			int k = list[first++];
			nei = g.getMandNeighOf(k);
			for (int s : nei) {
				if (!oneNode.get(s)) {
					counter[s]++;
					if (counter[s] > maxDegree[s]) {
						fails();
					} else if (counter[s] == maxDegree[s] - 1) {
						oneNode.set(s);
						list[last++] = s;
					}
				}
			}
		}
	}


	@Override // redundant propagator
	public ESat isEntailed() {
		return ESat.TRUE;
	}
}
