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
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.graphsolver.variables.delta.GraphDeltaMonitor;
import org.chocosolver.memory.IEnvironment;
import org.chocosolver.memory.IStateInt;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.procedure.PairProcedure;

/**
 * Compute the cost of the graph by summing edge costs
 * - For minimization problem
 */
public class PropTreeCostSimple extends Propagator<UndirectedGraphVar> {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	protected UndirectedGraphVar g;
	private GraphDeltaMonitor gdm;
	private PairProcedure edgeEnf, edgeRem;
	protected int n;
	protected IntVar sum;
	protected int[][] distMatrix;
	private IStateInt minSum, maxSum;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropTreeCostSimple(UndirectedGraphVar graph, IntVar obj, int[][] costMatrix) {
		super(new UndirectedGraphVar[]{graph}, PropagatorPriority.LINEAR, true);
		g = graph;
		sum = obj;
		n = g.getNbMaxNodes();
		distMatrix = costMatrix;
		IEnvironment environment = graph.getEnvironment();
		minSum = environment.makeInt(0);
		maxSum = environment.makeInt(0);
		gdm = g.monitorDelta(this);
		edgeEnf = (i, j) -> minSum.add(distMatrix[i][j]);
		edgeRem = (i, j) -> maxSum.add(-distMatrix[i][j]);
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		int min = 0;
		int max = 0;
		for (int i = 0; i < n; i++) {
			ISet nei = g.getPotNeighOf(i);
			for (int j : nei) {
				if (i <= j) {
					max += distMatrix[i][j];
					if (g.getMandNeighOf(i).contains(j)) {
						min += distMatrix[i][j];
					}
				}
			}
		}
		gdm.unfreeze();
		minSum.set(min);
		maxSum.set(max);
		sum.updateLowerBound(min, this);
		sum.updateUpperBound(max, this);
	}

	@Override
	public void propagate(int idxVarInProp, int mask) throws ContradictionException {
		gdm.freeze();
		gdm.forEachArc(edgeEnf, GraphEventType.ADD_ARC);
		gdm.forEachArc(edgeRem, GraphEventType.REMOVE_ARC);
		gdm.unfreeze();
		sum.updateLowerBound(minSum.get(), this);
		sum.updateUpperBound(maxSum.get(), this);
	}

	@Override
	public int getPropagationConditions(int vIdx) {
		return GraphEventType.REMOVE_ARC.getMask() + GraphEventType.ADD_ARC.getMask();
	}

	@Override
	public ESat isEntailed() {
		int min = 0;
		int max = 0;
		for (int i = 0; i < n; i++) {
			ISet nei = g.getPotNeighOf(i);
			for (int j : nei) {
				if (i <= j) {
					max += distMatrix[i][j];
					if (g.getMandNeighOf(i).contains(j)) {
						min += distMatrix[i][j];
					}
				}
			}
		}
		if (min > sum.getUB() || max < sum.getLB()) {
			return ESat.FALSE;
		}
		if (min == max) {
			return ESat.TRUE;
		} else {
			return ESat.UNDEFINED;
		}
	}
}
