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

import org.chocosolver.graphsolver.variables.GraphVar;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.solver.variables.delta.ISetDeltaMonitor;
import org.chocosolver.solver.variables.events.SetEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.procedure.IntProcedure;

/**
 * @author Jean-Guillaume Fages
 */
public class PropNeighSetsChannel2 extends Propagator<SetVar> {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private int n, currentSet;
	private ISetDeltaMonitor[] sdm;
	private SetVar[] sets;
	private GraphVar g;
	private IntProcedure elementForced, elementRemoved;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	/**
	 * Channeling between a graph variable and set variables
	 * representing either node neighbors or node successors
	 */
	public PropNeighSetsChannel2(SetVar[] setsV, GraphVar gV) {
		super(setsV, PropagatorPriority.LINEAR, true);
		this.sets = new SetVar[setsV.length];
		System.arraycopy(vars, 0, this.sets, 0, setsV.length);
		n = sets.length;
		this.g = gV;
		assert (n == g.getNbMaxNodes());
		sdm = new ISetDeltaMonitor[n];
		for (int i = 0; i < n; i++) {
			sdm[i] = sets[i].monitorDelta(this);
		}
		elementForced = element -> g.enforceArc(currentSet, element, this);
		elementRemoved = element -> g.removeArc(currentSet, element, this);
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		for (int i = 0; i < n; i++) {
			for (int j : sets[i].getLB()) {
				g.enforceArc(i, j, this);
			}
			ISet tmp = g.getPotSuccOrNeighOf(i);
			for (int j : tmp) {
				if (!sets[i].getUB().contains(j)) {
					g.removeArc(i, j, this);
				}
			}
		}
		for (int i = 0; i < n; i++) {
			sdm[i].unfreeze();
		}
	}

	@Override
	public void propagate(int idxVarInProp, int mask) throws ContradictionException {
		currentSet = idxVarInProp;
		sdm[currentSet].freeze();
		sdm[currentSet].forEach(elementForced, SetEventType.ADD_TO_KER);
		sdm[currentSet].forEach(elementRemoved, SetEventType.REMOVE_FROM_ENVELOPE);
		sdm[currentSet].unfreeze();
	}

	@Override
	public ESat isEntailed() {
		for (int i = 0; i < n; i++) {
			for (int j : sets[i].getLB()) {
				if (!g.getPotSuccOrNeighOf(i).contains(j)) {
					return ESat.FALSE;
				}
			}
			ISet tmp = g.getMandSuccOrNeighOf(i);
			for (int j : tmp) {
				if (!sets[i].getUB().contains(j)) {
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
