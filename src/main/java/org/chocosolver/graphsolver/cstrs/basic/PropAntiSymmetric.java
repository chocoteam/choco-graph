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

package org.chocosolver.graphsolver.cstrs.basic;

import org.chocosolver.graphsolver.variables.GraphEventType;
import org.chocosolver.graphsolver.variables.IDirectedGraphVar;
import org.chocosolver.graphsolver.variables.delta.IGraphDeltaMonitor;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.procedure.PairProcedure;

/**
 * Ensures that the final graph is antisymmetric
 * i.e. if G has arc (x,y) then it does not have (y,x)
 * Except for loops : (x,x) is allowed
 *
 * @author Jean-Guillaume Fages
 */
public class PropAntiSymmetric extends Propagator<IDirectedGraphVar> {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

	private IDirectedGraphVar g;
    private IGraphDeltaMonitor gdm;
    private PairProcedure enf;
    private int n;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    public PropAntiSymmetric(IDirectedGraphVar graph) {
        super(graph);
        g = graph;
        gdm = g.monitorDelta(this);
        enf = (from, to) -> {
			if (from != to) {
				g.removeArc(to, from, PropAntiSymmetric.this);
			}
		};
        n = g.getNbMaxNodes();
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        ISet ker = g.getMandatoryNodes();
        ISet succ;
        for (int i : ker) {
            succ = g.getMandSuccOf(i);
            for (int j : succ) {
                g.removeArc(j, i, this);
            }
        }
        gdm.unfreeze();
    }

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
        gdm.freeze();
        gdm.forEachArc(enf, GraphEventType.ADD_ARC);
        gdm.unfreeze();
    }

    @Override
    public int getPropagationConditions(int vIdx) {
        return GraphEventType.ADD_ARC.getMask();
    }

    @Override
    public ESat isEntailed() {
        ISet ker = g.getMandatoryNodes();
        ISet succ;
        for (int i : ker) {
            succ = g.getMandSuccOf(i);
            for (int j : succ) {
                if (g.getMandSuccOf(j).contain(i)) {
                    return ESat.FALSE;
                }
            }
        }
        if (g.isInstantiated()) {
            return ESat.TRUE;
        }
        return ESat.UNDEFINED;
    }
}
