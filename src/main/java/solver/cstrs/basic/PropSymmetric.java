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

package solver.cstrs.basic;

import solver.constraints.Propagator;
import solver.constraints.PropagatorPriority;
import solver.exception.ContradictionException;
import solver.variables.GraphEventType;
import solver.variables.IDirectedGraphVar;
import solver.variables.delta.IGraphDeltaMonitor;
import util.ESat;
import util.objects.setDataStructures.ISet;
import util.procedure.PairProcedure;

/**
 * Ensures that the final graph is symmetric
 * i.e. if G has arc (x,y) then it also has (y,x)
 *
 * Note that it may be preferable to use an undirected graph variable instead!
 *
 * @author Jean-Guillaume Fages
 */
public class PropSymmetric extends Propagator<IDirectedGraphVar> {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

	IDirectedGraphVar g;
    IGraphDeltaMonitor gdm;
    EnfProc enf;
    int n;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    public PropSymmetric(IDirectedGraphVar graph) {
        super(new IDirectedGraphVar[]{graph}, PropagatorPriority.UNARY, true);
        g = graph;
        gdm = g.monitorDelta(this);
        enf = new EnfProc();
        n = g.getNbMaxNodes();
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        ISet ker = g.getMandatoryNodes();
        ISet succ;
        for (int i = ker.getFirstElement(); i >= 0; i = ker.getNextElement()) {
            succ = g.getMandSuccOf(i);
            for (int j = succ.getFirstElement(); j >= 0; j = succ.getNextElement()) {
                g.enforceArc(j, i, aCause);
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
        for (int i = ker.getFirstElement(); i >= 0; i = ker.getNextElement()) {
            succ = g.getMandSuccOf(i);
            for (int j = succ.getFirstElement(); j >= 0; j = succ.getNextElement()) {
                if (!g.getPotSuccOf(j).contain(i)) {
                    return ESat.FALSE;
                }
            }
        }
        if (g.isInstantiated()) {
            return ESat.TRUE;
        }
        return ESat.UNDEFINED;
    }

    //***********************************************************************************
    // PROCEDURES
    //***********************************************************************************

    /**
     * Enable to remove the opposite arc
     */
    private class EnfProc implements PairProcedure {
        @Override
        public void execute(int from, int to) throws ContradictionException {
            if (from != to) {
                g.enforceArc(to, from, aCause);
            }
        }
    }
}
