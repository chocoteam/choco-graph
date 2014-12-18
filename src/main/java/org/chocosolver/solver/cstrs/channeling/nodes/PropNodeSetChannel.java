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

package org.chocosolver.solver.cstrs.channeling.nodes;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.GraphEventType;
import org.chocosolver.solver.variables.IGraphVar;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.variables.delta.IGraphDeltaMonitor;
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
	private IGraphVar g;
	private ISetDeltaMonitor sdm;
	private IGraphDeltaMonitor gdm;
	private IntProcedure forceG, forceS, remG, remS;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropNodeSetChannel(SetVar vertexSet, IGraphVar gV) {
		super(new Variable[]{vertexSet,gV}, PropagatorPriority.LINEAR, true);
		this.set = vertexSet;
		this.g = gV;
		sdm = set.monitorDelta(this);
		gdm = g.monitorDelta(this);
		forceS = element -> g.enforceNode(element, aCause);
		remS = element -> g.removeNode(element, aCause);
		forceG = element -> set.addToKernel(element, aCause);
		remG = element -> set.removeFromEnvelope(element, aCause);
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
		for(int i=set.getEnvelopeFirst();i!=SetVar.END;i=set.getEnvelopeNext()){
			if(g.getMandatoryNodes().contain(i)){
				set.addToKernel(i,aCause);
			}else if(!g.getPotentialNodes().contain(i)){
				set.removeFromEnvelope(i,aCause);
			}
		}
		for(int i=g.getPotentialNodes().getFirstElement();i>=0;i=g.getPotentialNodes().getNextElement()){
			if(set.kernelContains(i)) {
				g.enforceNode(i, aCause);
			} else if(!set.envelopeContains(i)){
				g.removeNode(i,aCause);
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
		for(int i=set.getKernelFirst();i!=SetVar.END;i=set.getKernelNext()){
			if(!g.getPotentialNodes().contain(i)){
				return ESat.FALSE;
			}
		}
		for(int i=g.getMandatoryNodes().getFirstElement();i>=0;i=g.getMandatoryNodes().getNextElement()){
			if(!set.envelopeContains(i)){
				return ESat.FALSE;
			}
		}
		int n = g.getMandatoryNodes().getSize();
		if(n == g.getPotentialNodes().getSize() && n == set.getEnvelopeSize() && n == set.getKernelSize()){
			return ESat.TRUE;
		}
		return ESat.UNDEFINED;
	}
}
