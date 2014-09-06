/*
 * Copyright (c) 1999-2012, Ecole des Mines de Nantes
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
import solver.variables.EventType;
import solver.variables.IGraphVar;
import util.ESat;
import util.objects.setDataStructures.ISet;

/**
 * Propagator that ensures that each node of the given subset of nodes has a loop
 *
 * @author Jean-Guillaume Fages
 */
public class PropEachNodeHasLoop extends Propagator<IGraphVar> {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    private IGraphVar g;
    private ISet concernedNodes;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    public PropEachNodeHasLoop(IGraphVar graph, ISet concernedNodes) {
        super(new IGraphVar[]{graph}, PropagatorPriority.LINEAR, false);
        this.g = graph;
        this.concernedNodes = concernedNodes;
    }

    public PropEachNodeHasLoop(IGraphVar graph) {
        this(graph, graph.getPotentialNodes());
    }

    //***********************************************************************************
    // PROPAGATIONS
    //***********************************************************************************

    @Override
    public void propagate(int evtmask) throws ContradictionException {
		for(int i=concernedNodes.getFirstElement();i>=0;i=concernedNodes.getNextElement()){
			if(g.getMandatoryNodes().contain(i)){
				g.enforceArc(i,i,aCause);
			}else if(!g.getPotSuccOrNeighOf(i).contain(i)){
				g.removeNode(i,aCause);
			}
		}
    }

    //***********************************************************************************
    // INFO
    //***********************************************************************************

    @Override
    public int getPropagationConditions(int vIdx) {
        return EventType.ENFORCENODE.mask + EventType.REMOVEARC.mask;
    }

    @Override
    public ESat isEntailed() {
		boolean sure = true;
		for(int i=concernedNodes.getFirstElement();i>=0;i=concernedNodes.getNextElement()){
			if(g.getMandatoryNodes().contain(i) && !g.getPotSuccOrNeighOf(i).contain(i)){
				return ESat.FALSE;
			}else if(g.getPotentialNodes().contain(i) && !g.getPotSuccOrNeighOf(i).contain(i)){
				sure = false;
			}
		}
        if (sure) {
            return ESat.TRUE;
        }
        return ESat.UNDEFINED;
    }
}
