/**
 *  Copyright (c) 1999-2011, Ecole des Mines de Nantes
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

/**
 * Created by IntelliJ IDEA.
 * User: Jean-Guillaume Fages
 * Date: 14/01/13
 * Time: 16:36
 */

package solver.cstrs.channeling.edges;

import solver.constraints.Propagator;
import solver.constraints.PropagatorPriority;
import solver.exception.ContradictionException;
import solver.variables.*;
import solver.variables.delta.IGraphDeltaMonitor;
import util.ESat;
import util.objects.setDataStructures.ISet;
import util.procedure.PairProcedure;

public class PropSuccIntsChannel1 extends Propagator<IDirectedGraphVar> {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private int n;
	private IntVar[] succs;
	private IGraphDeltaMonitor gdm;
	private IDirectedGraphVar g;
	private PairProcedure arcForced, arcRemoved;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropSuccIntsChannel1(final IntVar[] succs, IDirectedGraphVar gV) {
		super(new IDirectedGraphVar[]{gV}, PropagatorPriority.LINEAR, true);
		this.succs = succs;
		n = succs.length;
		this.g = gV;
		assert (n == g.getNbMaxNodes());
		gdm = g.monitorDelta(this);
		for(int i=0;i<n;i++){
			assert succs[i].hasEnumeratedDomain():"channeling variables should be enumerated";
		}
		arcForced = new PairProcedure() {
			@Override
			public void execute(int i, int j) throws ContradictionException {
				succs[i].instantiateTo(j, aCause);
			}
		};
		arcRemoved = new PairProcedure() {
			@Override
			public void execute(int i, int j) throws ContradictionException {
				succs[i].removeValue(j, aCause);
			}
		};
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		for (int i = 0; i < n; i++) {
			ISet tmp = g.getMandSuccOrNeighOf(i);
			for (int j = tmp.getFirstElement(); j >= 0; j = tmp.getNextElement()) {
				succs[i].instantiateTo(j, aCause);
			}
			for (int j=succs[i].getLB(); j<=succs[i].getUB(); j=succs[i].nextValue(j)) {
				if (!g.getPotSuccOrNeighOf(i).contain(j)) {
					succs[i].removeValue(j, aCause);
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
			if(succs[i].isInstantiated()){
				if (!g.getPotSuccOrNeighOf(i).contain(succs[i].getValue())) {
					return ESat.FALSE;
				}
			}
			ISet tmp = g.getMandSuccOrNeighOf(i);
			for (int j = tmp.getFirstElement(); j >= 0; j = tmp.getNextElement()) {
				if (!succs[i].contains(j)) {
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
