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
import solver.exception.ContradictionException;
import solver.variables.EventType;
import solver.variables.IGraphVar;
import solver.variables.SetVar;
import solver.variables.delta.IGraphDeltaMonitor;
import util.ESat;
import util.objects.setDataStructures.ISet;
import util.procedure.PairProcedure;

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
     *
     * @param setsV
     * @param gV
     */
    public PropNeighSetsChannel1(SetVar[] setsV, IGraphVar gV) {
        super(new IGraphVar[]{gV}, PropagatorPriority.LINEAR, true);
        this.sets = new SetVar[setsV.length];
        for (int i = 0; i < setsV.length; i++) {
            this.sets[i] = (SetVar) vars[i];
        }
        n = sets.length;
        this.g = gV;
        assert (n == g.getNbMaxNodes());
        gdm = g.monitorDelta(this);
        arcForced = new PairProcedure() {
            @Override
            public void execute(int i, int j) throws ContradictionException {
                sets[i].addToKernel(j, aCause);
				if(!g.isDirected()){
					sets[j].addToKernel(i,aCause);
				}
            }
        };
        arcRemoved = new PairProcedure() {
            @Override
            public void execute(int i, int j) throws ContradictionException {
                sets[i].removeFromEnvelope(j, aCause);
				if(!g.isDirected()){
					sets[j].removeFromEnvelope(i,aCause);
				}
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
            for (int j = tmp.getFirstElement(); j >= 0; j = tmp.getNextElement()) {
                sets[i].addToKernel(j, aCause);
				if(!g.isDirected()){
					sets[j].addToKernel(i,aCause);
				}
            }
            for (int j=sets[i].getEnvelopeFirst(); j!=SetVar.END; j=sets[i].getEnvelopeNext()) {
                if (!g.getPotSuccOrNeighOf(i).contain(j)) {
                    sets[i].removeFromEnvelope(j, aCause);
					if(!g.isDirected()){
						sets[j].removeFromEnvelope(i,aCause);
					}
                }
            }
        }
        gdm.unfreeze();
    }

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
		gdm.freeze();
		gdm.forEachArc(arcForced, EventType.ENFORCEARC);
		gdm.forEachArc(arcRemoved, EventType.REMOVEARC);
		gdm.unfreeze();
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
