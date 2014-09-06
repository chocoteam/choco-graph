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
import solver.variables.IncidentSet;
import solver.exception.ContradictionException;
import solver.variables.*;
import util.ESat;
import util.tools.ArrayUtils;

public class PropNeighBoolChannel extends Propagator<Variable> {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private BoolVar[] bools;
	private IGraphVar g;
	private int vertex;
	private IncidentSet inc;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropNeighBoolChannel(BoolVar[] neigh, final int vertex, IGraphVar gV, IncidentSet incSet) {
		super(ArrayUtils.append(neigh,new Variable[]{gV}), PropagatorPriority.LINEAR, true);
		this.vertex = vertex;
		this.bools = neigh;
		this.g = gV;
		this.inc = incSet;
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	@Override
	public int getPropagationConditions(int vIdx) {
		if (vIdx == bools.length) {
			return EventType.ENFORCEARC.mask + EventType.REMOVEARC.mask;
		}else{
			return EventType.INT_ALL_MASK();
		}
	}

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		for(int i=inc.getPotSet(g,vertex).getFirstElement();i>=0;i=inc.getPotSet(g, vertex).getNextElement()){
			if(bools[i].getUB()==0){
				inc.remove(g,vertex,i,aCause);
			}else if(bools[i].getLB()==1){
				inc.enforce(g,vertex,i,aCause);
			}
		}
		for(int i=0;i<bools.length;i++){
			if(!inc.getPotSet(g,vertex).contain(i)){
				bools[i].setToFalse(aCause);
			}else if(inc.getMandSet(g,vertex).contain(i)){
				bools[i].setToTrue(aCause);
			}
		}
	}

	@Override
	public void propagate(int idxVarInProp, int mask) throws ContradictionException {
		if (idxVarInProp < bools.length) {
			if(bools[idxVarInProp].getLB()==1){
				inc.enforce(g,vertex,idxVarInProp,aCause);
			}else{
				inc.remove(g,vertex,idxVarInProp,aCause);
			}
		} else {
			for(int i=0;i<bools.length;i++){
				if(!inc.getPotSet(g,vertex).contain(i)){
					bools[i].setToFalse(aCause);
				}else if(inc.getMandSet(g,vertex).contain(i)){
					bools[i].setToTrue(aCause);
				}
			}
		}
	}

	@Override
	public ESat isEntailed() {
		for(int i=0;i<bools.length;i++){
			if(bools[i].getLB()==1 && !inc.getPotSet(g, vertex).contain(i)){
				return ESat.FALSE;
			}
		}
		for(int i=inc.getMandSet(g, vertex).getFirstElement();i>=0;i=inc.getMandSet(g, vertex).getNextElement()){
			if(bools[i].getUB()==0){
				return ESat.FALSE;
			}
		}
		if(isCompletelyInstantiated()){
			return ESat.TRUE;
		}
		return ESat.UNDEFINED;
	}
}
