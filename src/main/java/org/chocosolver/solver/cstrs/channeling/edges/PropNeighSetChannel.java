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

package org.chocosolver.solver.cstrs.channeling.edges;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.*;
import org.chocosolver.solver.variables.delta.ISetDeltaMonitor;
import org.chocosolver.solver.variables.events.SetEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.procedure.IntProcedure;

/**
 * @author Jean-Guillaume Fages
 */
public class PropNeighSetChannel extends Propagator<Variable> {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private SetVar set;
	private IGraphVar g;
	private int vertex;
	private IncidentSet inc;
	private ISetDeltaMonitor sdm;
	private IntProcedure forceS, remS;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropNeighSetChannel(SetVar neigh, final int vertex, IGraphVar gV, IncidentSet incSet) {
		super(new Variable[]{neigh,gV}, PropagatorPriority.LINEAR, true);
		this.vertex = vertex;
		this.set = neigh;
		this.g = gV;
		this.inc = incSet;
		sdm = set.monitorDelta(this);
		forceS = element -> inc.enforce(g,vertex,element,aCause);
		remS = element -> inc.remove(g,vertex,element, aCause);
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	@Override
	public int getPropagationConditions(int vIdx) {
		if (vIdx == 0) {
			return SetEventType.ADD_TO_KER.getMask() + SetEventType.REMOVE_FROM_ENVELOPE.getMask();
		} else {
			return GraphEventType.ADD_ARC.getMask() + GraphEventType.REMOVE_ARC.getMask();
		}
	}

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		for(int i=inc.getPotSet(g,vertex).getFirstElement();i>=0;i=inc.getPotSet(g,vertex).getNextElement()){
			if(!set.envelopeContains(i)){
				inc.remove(g,vertex,i,aCause);
			}else if(set.kernelContains(i)){
				inc.enforce(g,vertex,i,aCause);
			}
		}
		for(int i=set.getEnvelopeFirst();i!=SetVar.END;i=set.getEnvelopeNext()){
			if(!inc.getPotSet(g,vertex).contain(i)){
				set.removeFromEnvelope(i,aCause);
			}else if(inc.getMandSet(g,vertex).contain(i)){
				set.addToKernel(i,aCause);
			}
		}
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
			for(int i=set.getEnvelopeFirst();i!=SetVar.END;i=set.getEnvelopeNext()){
				if(!inc.getPotSet(g,vertex).contain(i)){
					set.removeFromEnvelope(i,aCause);
				}else if(inc.getMandSet(g,vertex).contain(i)){
					set.addToKernel(i,aCause);
				}
			}
		}
	}

	@Override
	public ESat isEntailed() {
		for(int i=set.getKernelFirst();i!=SetVar.END;i=set.getKernelNext()){
			if(!inc.getPotSet(g,vertex).contain(i)){
				return ESat.FALSE;
			}
		}
		for(int i=inc.getMandSet(g,vertex).getFirstElement();i>=0;i=inc.getMandSet(g,vertex).getNextElement()){
			if(!set.envelopeContains(i)){
				return ESat.FALSE;
			}
		}
		if(isCompletelyInstantiated()){
			return ESat.TRUE;
		}
		return ESat.UNDEFINED;
	}
}
