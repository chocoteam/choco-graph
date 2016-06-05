/*
 * Copyright (c) 1999-2014, Ecole des Mines de Nantes
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Ecole des Mines de Nantes nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
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

package org.chocosolver.graphsolver.cstrs.connectivity;

import org.chocosolver.graphsolver.util.ConnectivityFinder;
import org.chocosolver.graphsolver.variables.GraphEventType;
import org.chocosolver.graphsolver.variables.IUndirectedGraphVar;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.util.ESat;


/**
 * Propagator for enforcing a graph variable to be bi-connected
 *
 * @author Jean-Guillaume Fages
 */
public class PropBiconnected extends Propagator<IUndirectedGraphVar> {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    private IUndirectedGraphVar g;
    private ConnectivityFinder env_CC_finder;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    public PropBiconnected(IUndirectedGraphVar graph) {
        super(new IUndirectedGraphVar[]{graph}, PropagatorPriority.LINEAR, false);
        this.g = graph;
        env_CC_finder = new ConnectivityFinder(g.getUB());
    }

    //***********************************************************************************
    // PROPAGATIONS
    //***********************************************************************************

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        if (g.getPotentialNodes().getSize() == g.getMandatoryNodes().getSize() && !env_CC_finder.isBiconnected()) {
            fails();
        }
    }

    //***********************************************************************************
    // INFO
    //***********************************************************************************

    @Override
    public int getPropagationConditions(int vIdx) {
        return GraphEventType.REMOVE_NODE.getMask() + GraphEventType.REMOVE_ARC.getMask() + GraphEventType.ADD_NODE.getMask();
    }

    @Override
    public ESat isEntailed() {
		if (g.getPotentialNodes().getSize() == g.getMandatoryNodes().getSize()){
			return ESat.UNDEFINED;
		}
        if (!env_CC_finder.isBiconnected()) {
            return ESat.FALSE;
        }
        if (g.isInstantiated()) {
            return ESat.TRUE;
        }
        return ESat.UNDEFINED;
    }
}
