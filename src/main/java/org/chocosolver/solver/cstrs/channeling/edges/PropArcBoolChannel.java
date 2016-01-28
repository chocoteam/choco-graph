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
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;

/**
 * @author Jean-Guillaume Fages
 */
public class PropArcBoolChannel extends Propagator<Variable> {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private BoolVar bool;
	private int from,to;
	private IGraphVar g;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropArcBoolChannel(BoolVar isIn, int from, int to, IGraphVar gV) {
		super(new Variable[]{isIn,gV}, PropagatorPriority.UNARY, false);
		this.bool = isIn;
		this.from = from;
		this.to = to;
		this.g = gV;
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	@Override
	public int getPropagationConditions(int vIdx) {
		if (vIdx == 1) {
			return GraphEventType.ADD_ARC.getMask() + GraphEventType.REMOVE_ARC.getMask();
		}else{
			return IntEventType.all();
		}
	}

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		if(from<0 || to<0 || from>=g.getNbMaxNodes() || to>=g.getNbMaxNodes()
				|| !g.getPotSuccOrNeighOf(from).contain(to)){
			bool.setToFalse(this);
		}else if(g.getMandSuccOrNeighOf(from).contain(to)){
			bool.setToTrue(this);
		}else if(bool.getLB()==1){
			g.enforceArc(from,to,this);
		}else if(bool.getUB()==0){
			g.removeArc(from,to,this);
		}
	}

	@Override
	public ESat isEntailed() {
		if((from<0 || from>=g.getNbMaxNodes() || to<0 || to>=g.getNbMaxNodes())
				|| (bool.getLB()==1 && !g.getPotSuccOrNeighOf(from).contain(to))
				|| (bool.getUB()==0 && g.getMandSuccOrNeighOf(from).contain(to))
				){
			return ESat.FALSE;
		}
		if(bool.isInstantiated()
				&& g.getMandSuccOrNeighOf(from).contain(to)==g.getPotSuccOrNeighOf(from).contain(to)){
			return ESat.TRUE;
		}
		return ESat.UNDEFINED;
	}
}
