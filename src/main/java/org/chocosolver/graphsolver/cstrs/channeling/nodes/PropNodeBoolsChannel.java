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

package org.chocosolver.graphsolver.cstrs.channeling.nodes;

import org.chocosolver.graphsolver.variables.GraphEventType;
import org.chocosolver.graphsolver.variables.IGraphVar;
import org.chocosolver.graphsolver.variables.delta.IGraphDeltaMonitor;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.procedure.IntProcedure;
import org.chocosolver.util.tools.ArrayUtils;

/**
 * Channeling between a graph variable and set variables
 * representing either node neighbors or node successors
 *
 * @author Jean-Guillaume Fages
 */
public class PropNodeBoolsChannel extends Propagator<Variable> {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private BoolVar[] bools;
	private IGraphVar g;
	private IGraphDeltaMonitor gdm;
	private IntProcedure remG, forceG;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropNodeBoolsChannel(BoolVar[] vertices, IGraphVar gV) {
		super(ArrayUtils.append(vertices,new Variable[]{gV}), PropagatorPriority.LINEAR, true);
		this.bools = vertices;
		this.g = gV;
		gdm = g.monitorDelta(this);
		forceG = element -> bools[element].setToTrue(this);
		remG = element -> bools[element].setToFalse(this);
		super.linkVariables();
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	@Override
	public int getPropagationConditions(int vIdx) {
		if (vIdx == bools.length) {
			return GraphEventType.ADD_NODE.getMask() + GraphEventType.REMOVE_NODE.getMask();
		}else{
			return IntEventType.all();
		}
	}

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		for(int i=0;i<bools.length;i++){
			if(!g.getPotentialNodes().contain(i)){
				bools[i].setToFalse(this);
			}else if(g.getMandatoryNodes().contain(i)){
				bools[i].setToTrue(this);
			}
		}
		for(int i: g.getPotentialNodes()){
			if(!bools[i].contains(1)){
				g.removeNode(i,this);
			}else if(bools[i].getLB()==1){
				g.enforceNode(i,this);
			}
		}
		gdm.unfreeze();
	}

	@Override
	public void propagate(int idxVarInProp, int mask) throws ContradictionException {
		if (idxVarInProp < bools.length) {
			if(bools[idxVarInProp].getValue()==1){
				g.enforceNode(idxVarInProp,this);
			}else{
				g.removeNode(idxVarInProp,this);
			}
		} else {
			gdm.freeze();
			gdm.forEachNode(forceG, GraphEventType.ADD_NODE);
			gdm.forEachNode(remG, GraphEventType.REMOVE_NODE);
			gdm.unfreeze();
		}
	}

	@Override
	public ESat isEntailed() {
		for(int i=0;i<bools.length;i++){
			if(bools[i].getLB()==1 && !g.getPotentialNodes().contain(i)){
				return ESat.FALSE;
			}
			if(bools[i].getUB()==0 && g.getMandatoryNodes().contain(i)){
				return ESat.FALSE;
			}
		}
		if(isCompletelyInstantiated()){
			return ESat.TRUE;
		}
		return ESat.UNDEFINED;
	}
}
