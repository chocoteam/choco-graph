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
import org.chocosolver.solver.variables.IGraphVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.delta.IIntDeltaMonitor;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.procedure.IntProcedure;

/**
 * @author Jean-Guillaume Fages
 */
public class PropNeighIntsChannel2 extends Propagator<IntVar> {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    private int n, currentSet;
    private IIntDeltaMonitor[] idm;
    private IntVar[] succs;
    private IGraphVar g;
    private IntProcedure elementRemoved;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    public PropNeighIntsChannel2(IntVar[] succs, IGraphVar gV) {
        super(succs, PropagatorPriority.LINEAR, true);
        this.succs = succs;
        n = succs.length;
        g = gV;
        assert (n == g.getNbMaxNodes());
		idm = new IIntDeltaMonitor[n];
        for (int i = 0; i < n; i++) {
			idm[i] = succs[i].monitorDelta(this);
        }
        elementRemoved = element -> g.removeArc(currentSet, element, this);
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        for (int i = 0; i < n; i++) {
			g.enforceNode(i,this);
			if(succs[i].isInstantiated()){
				g.enforceArc(i,succs[i].getValue(),this);
			}
            ISet tmp = g.getPotSuccOrNeighOf(i);
            for (int j = tmp.getFirstElement(); j >= 0; j = tmp.getNextElement()) {
                if (!succs[i].contains(j)) {
                    g.removeArc(i, j, this);
                }
            }
        }
        for (int i = 0; i < n; i++) {
			idm[i].unfreeze();
        }
    }

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
		currentSet = idxVarInProp;
		idm[currentSet].freeze();
		if(vars[idxVarInProp].isInstantiated()){
			g.enforceArc(idxVarInProp,vars[idxVarInProp].getValue(),this);
		}else {
			idm[currentSet].forEachRemVal(elementRemoved);
		}
		idm[currentSet].unfreeze();
    }

    @Override
    public ESat isEntailed() {
        for (int i = 0; i < n; i++) {
			if(succs[i].isInstantiated()){
                if (!g.getPotSuccOrNeighOf(i).contain(succs[i].getValue())) {
                    return ESat.FALSE;
                }
            }
            ISet tmp = g.getMandSuccOrNeighOf(i);
            for (int j = tmp.getFirstElement(); j >= 0; j = tmp.getNextElement()) {
                if (!succs[i].contains(j)) {
					if(g.isDirected() || !succs[j].contains(i)) {
						return ESat.FALSE;
					}
                }
            }
        }
        if (isCompletelyInstantiated()) {
            return ESat.TRUE;
        }
        return ESat.UNDEFINED;
    }
}
