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

package org.chocosolver.graphsolver.cstrs.channeling.nodes;

import org.chocosolver.graphsolver.variables.GraphEventType;
import org.chocosolver.graphsolver.variables.GraphVar;
import org.chocosolver.graphsolver.variables.delta.GraphDeltaMonitor;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.variables.delta.ISetDeltaMonitor;
import org.chocosolver.solver.variables.events.SetEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.procedure.IntProcedure;

/**
 * Channeling between a graph variable and set variables
 * representing either node neighbors or node successors
 *
 * @author Jean-Guillaume Fages
 */
public class PropNodeSetChannel extends Propagator<Variable> {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private SetVar set;
	private GraphVar g;
	private ISetDeltaMonitor sdm;
	private GraphDeltaMonitor gdm;
	private IntProcedure forceG, forceS, remG, remS;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropNodeSetChannel(SetVar vertexSet, GraphVar gV) {
		super(new Variable[]{vertexSet, gV}, PropagatorPriority.LINEAR, true);
		this.set = vertexSet;
		this.g = gV;
		sdm = set.monitorDelta(this);
		gdm = g.monitorDelta(this);
		forceS = element -> g.enforceNode(element, this);
		remS = element -> g.removeNode(element, this);
		forceG = element -> set.force(element, this);
		remG = element -> set.remove(element, this);
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	@Override
	public int getPropagationConditions(int vIdx) {
		if (vIdx == 0) {
			return SetEventType.ADD_TO_KER.getMask() + SetEventType.REMOVE_FROM_ENVELOPE.getMask();
		} else {
			return GraphEventType.ADD_NODE.getMask() + GraphEventType.REMOVE_NODE.getMask();
		}
	}

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		for (int i : set.getUB()) {
			if (g.getMandatoryNodes().contains(i)) {
				set.force(i, this);
			} else if (!g.getPotentialNodes().contains(i)) {
				set.remove(i, this);
			}
		}
		for (int i : g.getPotentialNodes()) {
			if (set.getLB().contains(i)) {
				g.enforceNode(i, this);
			} else if (!set.getUB().contains(i)) {
				g.removeNode(i, this);
			}
		}
		gdm.unfreeze();
		sdm.unfreeze();
	}

	@Override
	public void propagate(int idxVarInProp, int mask) throws ContradictionException {
		if (idxVarInProp == 0) {
			sdm.freeze();
			sdm.forEach(forceS, SetEventType.ADD_TO_KER);
			sdm.forEach(remS, SetEventType.REMOVE_FROM_ENVELOPE);
			sdm.unfreeze();
		} else {
			gdm.freeze();
			gdm.forEachNode(forceG, GraphEventType.ADD_NODE);
			gdm.forEachNode(remG, GraphEventType.REMOVE_NODE);
			gdm.unfreeze();
		}
	}

	@Override
	public ESat isEntailed() {
		for (int i : set.getLB()) {
			if (!g.getPotentialNodes().contains(i)) {
				return ESat.FALSE;
			}
		}
		for (int i : g.getMandatoryNodes()) {
			if (!set.getUB().contains(i)) {
				return ESat.FALSE;
			}
		}
		int n = g.getMandatoryNodes().size();
		if (n == g.getPotentialNodes().size() && n == set.getUB().size() && n == set.getLB().size()) {
			return ESat.TRUE;
		}
		return ESat.UNDEFINED;
	}
}
