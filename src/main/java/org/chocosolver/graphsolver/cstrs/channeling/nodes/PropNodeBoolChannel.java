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
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;

/**
 * Channeling between a graph variable and set variables
 * representing either node neighbors or node successors
 *
 * @author Jean-Guillaume Fages
 */
public class PropNodeBoolChannel extends Propagator<Variable> {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private BoolVar bool;
	private int vertex;
	private GraphVar g;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropNodeBoolChannel(BoolVar isIn, int vertex, GraphVar gV) {
		super(new Variable[]{isIn, gV}, PropagatorPriority.UNARY, false);
		this.bool = isIn;
		this.vertex = vertex;
		this.g = gV;
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	@Override
	public int getPropagationConditions(int vIdx) {
		if (vIdx == 1) {
			return GraphEventType.ADD_NODE.getMask() + GraphEventType.REMOVE_NODE.getMask();
		} else {
			return IntEventType.all();
		}
	}

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		if (vertex < 0 || vertex >= g.getNbMaxNodes() || !g.getPotentialNodes().contains(vertex)) {
			bool.setToFalse(this);
		} else if (g.getMandatoryNodes().contains(vertex)) {
			bool.setToTrue(this);
		} else if (bool.getLB() == 1) {
			g.enforceNode(vertex, this);
		} else if (bool.getUB() == 0) {
			g.removeNode(vertex, this);
		}
	}

	@Override
	public ESat isEntailed() {
		if ((vertex < 0 || vertex >= g.getNbMaxNodes())
				|| (bool.getLB() == 1 && !g.getPotentialNodes().contains(vertex))
				|| (bool.getUB() == 0 && g.getMandatoryNodes().contains(vertex))
				) {
			return ESat.FALSE;
		}
		if (bool.isInstantiated()
				&& g.getMandatoryNodes().contains(vertex) == g.getPotentialNodes().contains(vertex)) {
			return ESat.TRUE;
		}
		return ESat.UNDEFINED;
	}
}
