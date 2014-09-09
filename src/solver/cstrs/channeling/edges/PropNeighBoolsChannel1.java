/**
 *  Copyright (c) 1999-2014, Ecole des Mines de Nantes
 *  All rights reserved.
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *      * Neither the name of the Ecole des Mines de Nantes nor the
 *        names of its contributors may be used to endorse or promote products
 *        derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package solver.cstrs.channeling.edges;

import solver.constraints.Propagator;
import solver.constraints.PropagatorPriority;
import solver.exception.ContradictionException;
import solver.variables.BoolVar;
import solver.variables.EventType;
import solver.variables.IGraphVar;
import solver.variables.delta.IGraphDeltaMonitor;
import util.ESat;
import util.procedure.PairProcedure;

/**
 * Channeling between a graph variable and set variables
 * representing either node neighbors or node successors
 *
 * @author Jean-Guillaume Fages
 */
public class PropNeighBoolsChannel1 extends Propagator<IGraphVar> {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private int n;
	private BoolVar[][] matrix;
	private IGraphDeltaMonitor gdm;
	private IGraphVar g;
	private PairProcedure arcForced, arcRemoved;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropNeighBoolsChannel1(BoolVar[][] adjacencyMatrix, IGraphVar gV) {
		super(new IGraphVar[]{gV}, PropagatorPriority.LINEAR, true);
		this.matrix = adjacencyMatrix;
		n = matrix.length;
		assert n == matrix[0].length;
		this.g = gV;
		assert (n == g.getNbMaxNodes());
		gdm = g.monitorDelta(this);
		arcForced = new PairProcedure() {
			@Override
			public void execute(int i, int j) throws ContradictionException {
				matrix[i][j].setToTrue(aCause);
				if(!g.isDirected()){
					matrix[j][i].setToTrue(aCause);
				}
			}
		};
		arcRemoved = new PairProcedure() {
			@Override
			public void execute(int i, int j) throws ContradictionException {
				matrix[i][j].setToFalse(aCause);
				if(!g.isDirected()){
					matrix[j][i].setToFalse(aCause);
				}
			}
		};
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		for (int i = 0; i < n; i++) {
			for (int j=0;j<n;j++) {
				if(g.getMandSuccOrNeighOf(i).contain(j)){
					matrix[i][j].setToTrue(aCause);
				}else if (!g.getPotSuccOrNeighOf(i).contain(j)) {
					matrix[i][j].setToFalse(aCause);
				}
			}
		}
		gdm.unfreeze();
	}

	@Override
	public void propagate(int idxVarInProp, int mask) throws ContradictionException {
		gdm.freeze();
		gdm.forEachArc(arcForced, EventType.ENFORCEARC);
		gdm.forEachArc(arcRemoved, EventType.REMOVEARC);
		gdm.unfreeze();
	}

	@Override
	public ESat isEntailed() {
		for (int i = 0; i < n; i++) {
			for(int j=0;j<n;j++){
				if(matrix[i][j].getLB()==1 && !g.getPotSuccOrNeighOf(i).contain(j)) {
					return ESat.FALSE;
				} else if(matrix[i][j].getUB()==0 && g.getMandSuccOrNeighOf(i).contain(j)) {
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
