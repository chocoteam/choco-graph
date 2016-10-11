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

package org.chocosolver.graphsolver.cstrs.channeling.edges;

import org.chocosolver.graphsolver.variables.GraphEventType;
import org.chocosolver.graphsolver.variables.IGraphVar;
import org.chocosolver.graphsolver.variables.delta.IGraphDeltaMonitor;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.procedure.PairProcedure;

/**
 * Channeling between a graph variable and set variables
 * representing either node neighbors or node successors
 *
 * @author Jean-Guillaume Fages
 */
public class PropNeighSetsChannel1 extends Propagator<IGraphVar> {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    private int n;
    private SetVar[] sets;
    private IGraphDeltaMonitor gdm;
    private IGraphVar g;
    private PairProcedure arcForced, arcRemoved;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    /**
     * Channeling between a graph variable and set variables
     * representing either node neighbors or node successors
     */
    public PropNeighSetsChannel1(SetVar[] setsV, IGraphVar gV) {
        super(new IGraphVar[]{gV}, PropagatorPriority.LINEAR, true);
        this.sets = setsV;
        n = sets.length;
        this.g = gV;
        assert (n == g.getNbMaxNodes());
        gdm = g.monitorDelta(this);
        arcForced = (i, j) -> {
            sets[i].force(j, this);
            if(!g.isDirected()){
                sets[j].force(i,this);
            }
        };
        arcRemoved = (i, j) -> {
            sets[i].remove(j, this);
            if(!g.isDirected()){
                sets[j].remove(i,this);
            }
        };
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        for (int i = 0; i < n; i++) {
            ISet tmp = g.getMandSuccOrNeighOf(i);
            for (int j : tmp) {
                sets[i].force(j, this);
				if(!g.isDirected()){
					sets[j].force(i,this);
				}
            }
            for (int j: sets[i].getUB()) {
                if (!g.getPotSuccOrNeighOf(i).contain(j)) {
                    sets[i].remove(j, this);
					if(!g.isDirected()){
						sets[j].remove(i,this);
					}
                }
            }
        }
        gdm.unfreeze();
    }

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
		gdm.freeze();
		gdm.forEachArc(arcForced, GraphEventType.ADD_ARC);
		gdm.forEachArc(arcRemoved, GraphEventType.REMOVE_ARC);
		gdm.unfreeze();
    }

    @Override
    public ESat isEntailed() {
        for (int i = 0; i < n; i++) {
            for (int j:sets[i].getLB()) {
                if (!g.getPotSuccOrNeighOf(i).contain(j)) {
                    return ESat.FALSE;
                }
            }
            ISet tmp = g.getMandSuccOrNeighOf(i);
            for (int j : tmp) {
                if (!sets[i].getUB().contain(j)) {
                    return ESat.FALSE;
                }
            }
        }
        if (isCompletelyInstantiated()) {
            return ESat.TRUE;
        }
        return ESat.UNDEFINED;
    }
}
