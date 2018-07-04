/**
 * Copyright (c) 1999-2014, Ecole des Mines de Nantes
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the Ecole des Mines de Nantes nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p>
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

package org.chocosolver.graphsolver.cstrs.channeling.edges;

import org.chocosolver.graphsolver.variables.GraphEventType;
import org.chocosolver.graphsolver.variables.GraphVar;
import org.chocosolver.graphsolver.variables.delta.GraphDeltaMonitor;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.util.ESat;
import org.chocosolver.util.procedure.PairProcedure;

/**
 * Channeling between a graph variable and set variables
 * representing either node neighbors or node successors
 *
 * @author Jean-Guillaume Fages
 */
public class PropNeighBoolsChannel1 extends Propagator<GraphVar> {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private int n;
	private BoolVar[][] matrix;
	private GraphDeltaMonitor gdm;
	private GraphVar g;
	private PairProcedure arcForced, arcRemoved;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropNeighBoolsChannel1(BoolVar[][] adjacencyMatrix, GraphVar gV) {
		super(new GraphVar[]{gV}, PropagatorPriority.LINEAR, true);
		this.matrix = adjacencyMatrix;
		n = matrix.length;
		assert n == matrix[0].length;
		this.g = gV;
		assert (n == g.getNbMaxNodes());
		gdm = g.monitorDelta(this);
		arcForced = (i, j) -> {
			matrix[i][j].setToTrue(this);
			if (!g.isDirected()) {
				matrix[j][i].setToTrue(this);
			}
		};
		arcRemoved = (i, j) -> {
			matrix[i][j].setToFalse(this);
			if (!g.isDirected()) {
				matrix[j][i].setToFalse(this);
			}
		};
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				if (g.getMandSuccOrNeighOf(i).contains(j)) {
					matrix[i][j].setToTrue(this);
				} else if (!g.getPotSuccOrNeighOf(i).contains(j)) {
					matrix[i][j].setToFalse(this);
				}
			}
		}
		gdm.unfreeze();
	}

	@Override
	public void propagate(int idxVarInProp, int mask) throws ContradictionException {
		gdm.freeze();
		gdm.forEachArc(arcForced, GraphEventType.ADD_ARC);
		gdm.forEachArc(arcRemoved, GraphEventType.REMOVE_ARC);
		gdm.unfreeze();
	}

	@Override
	public ESat isEntailed() {
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				if (matrix[i][j].getLB() == 1 && !g.getPotSuccOrNeighOf(i).contains(j)) {
					return ESat.FALSE;
				} else if (matrix[i][j].getUB() == 0 && g.getMandSuccOrNeighOf(i).contains(j)) {
					return ESat.FALSE;
				}
			}
		}
		if (isCompletelyInstantiated()) {
			return ESat.TRUE;
		}
		return ESat.UNDEFINED;
	}
}
