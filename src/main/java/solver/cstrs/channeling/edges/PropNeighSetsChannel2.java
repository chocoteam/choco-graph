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

package solver.cstrs.channeling.edges;

import solver.constraints.Propagator;
import solver.constraints.PropagatorPriority;
import solver.exception.ContradictionException;
import solver.variables.IGraphVar;
import solver.variables.SetVar;
import solver.variables.delta.ISetDeltaMonitor;
import solver.variables.events.SetEventType;
import util.ESat;
import util.objects.setDataStructures.ISet;
import util.procedure.IntProcedure;

/**
 * @author Jean-Guillaume Fages
 */
public class PropNeighSetsChannel2 extends Propagator<SetVar> {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    private int n, currentSet;
    private ISetDeltaMonitor[] sdm;
    private SetVar[] sets;
    private IGraphVar g;
    private IntProcedure elementForced, elementRemoved;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    /**
     * Channeling between a graph variable and set variables
     * representing either node neighbors or node successors
     *
     * @param setsV
     * @param gV
     */
    public PropNeighSetsChannel2(SetVar[] setsV, IGraphVar gV) {
        super(setsV, PropagatorPriority.LINEAR, true);
        this.sets = new SetVar[setsV.length];
        for (int i = 0; i < setsV.length; i++) {
            this.sets[i] = vars[i];
        }
        n = sets.length;
        this.g = gV;
        assert (n == g.getNbMaxNodes());
        sdm = new ISetDeltaMonitor[n];
        for (int i = 0; i < n; i++) {
            sdm[i] = sets[i].monitorDelta(this);
        }
        elementForced = new IntProcedure() {
            @Override
            public void execute(int element) throws ContradictionException {
                g.enforceArc(currentSet, element, aCause);
            }
        };
        elementRemoved = new IntProcedure() {
            @Override
            public void execute(int element) throws ContradictionException {
                g.removeArc(currentSet, element, aCause);
            }
        };
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        for (int i = 0; i < n; i++) {
            for (int j=sets[i].getKernelFirst(); j!=SetVar.END; j=sets[i].getKernelNext()) {
                g.enforceArc(i, j, aCause);
            }
            ISet tmp = g.getPotSuccOrNeighOf(i);
            for (int j = tmp.getFirstElement(); j >= 0; j = tmp.getNextElement()) {
                if (!sets[i].envelopeContains(j)) {
                    g.removeArc(i, j, aCause);
                }
            }
        }
        for (int i = 0; i < n; i++) {
            sdm[i].unfreeze();
        }
    }

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
		currentSet = idxVarInProp;
		sdm[currentSet].freeze();
		sdm[currentSet].forEach(elementForced, SetEventType.ADD_TO_KER);
		sdm[currentSet].forEach(elementRemoved, SetEventType.REMOVE_FROM_ENVELOPE);
		sdm[currentSet].unfreeze();
    }

    @Override
    public ESat isEntailed() {
        for (int i = 0; i < n; i++) {
            for (int j=sets[i].getKernelFirst(); j!=SetVar.END; j=sets[i].getKernelNext()) {
                if (!g.getPotSuccOrNeighOf(i).contain(j)) {
                    return ESat.FALSE;
                }
            }
            ISet tmp = g.getMandSuccOrNeighOf(i);
            for (int j = tmp.getFirstElement(); j >= 0; j = tmp.getNextElement()) {
                if (!sets[i].envelopeContains(j)) {
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