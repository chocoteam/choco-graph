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

package solver.cstrs.tree;

import solver.constraints.Propagator;
import solver.constraints.PropagatorPriority;
import solver.exception.ContradictionException;
import solver.variables.GraphEventType;
import solver.variables.IDirectedGraphVar;
import util.ESat;
import util.graphOperations.dominance.AbstractLengauerTarjanDominatorsFinder;
import util.graphOperations.dominance.AlphaDominatorsFinder;
import util.graphOperations.dominance.SimpleDominatorsFinder;
import util.objects.graphs.DirectedGraph;
import util.objects.setDataStructures.ISet;
import util.objects.setDataStructures.SetType;

/**
 * Arborescences constraint (simplification from tree constraint) based on dominators
 * CONSIDERS THAT EACH NODE WITH NO PREDECESSOR IS A ROOT (needs at least one such node)
 * Uses simple LT algorithm which runs in O(m.log(n)) worst case time
 * but very efficient in practice
 * @author Jean-Guillaume Fages
 */
public class PropArborescences extends Propagator<IDirectedGraphVar> {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    // flow graph
    IDirectedGraphVar g;
    DirectedGraph connectedGraph;
    // number of nodes
    int n;
    // dominators finder that contains the dominator tree
    AbstractLengauerTarjanDominatorsFinder domFinder;
    ISet[] successors;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************


	public PropArborescences(IDirectedGraphVar graph) {
		this(graph,false);
	}

    public PropArborescences(IDirectedGraphVar graph, boolean simple) {
        super(new IDirectedGraphVar[]{graph}, PropagatorPriority.QUADRATIC, false);
        g = graph;
        n = g.getNbMaxNodes();
        successors = new ISet[n];
        connectedGraph = new DirectedGraph(n + 1, SetType.BITSET, true);
        if (simple) {
            domFinder = new SimpleDominatorsFinder(n, connectedGraph);
        } else {
            domFinder = new AlphaDominatorsFinder(n, connectedGraph);
        }
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        // reset data structure
		for (int i = 0; i < n + 1; i++) {
            connectedGraph.getSuccOf(i).clear();
            connectedGraph.getPredOf(i).clear();
        }
        ISet nei;
        for (int i = 0; i < n; i++) {
            nei = g.getPotPredOf(i);
			for (int y = nei.getFirstElement(); y >= 0; y = nei.getNextElement()) {
				connectedGraph.addArc(y, i);
			}
			nei = g.getMandPredOf(i);
			if (nei.isEmpty()) {
				connectedGraph.addArc(n, i);
			}
        }
		// reach all nodes and filter from dominators
        if (domFinder.findDominators()) {
			ISet potNodes = g.getPotentialNodes();
            for (int x = potNodes.getFirstElement(); x>=0; x=potNodes.getNextElement()) {
                nei = g.getPotSuccOf(x);
				if(g.getMandatoryNodes().contain(x) && g.getMandatoryNodes().getSize()!=g.getPotentialNodes().getSize()){
					for(int y=0;y<n;y++){
						if (domFinder.isDomminatedBy(x, y)){
							g.enforceNode(y,aCause);
						}
					}
				}
                for (int y = nei.getFirstElement(); y >= 0; y = nei.getNextElement()) {
                    //--- STANDART PRUNING
                    if (domFinder.isDomminatedBy(x, y)) {
                        g.removeArc(x, y, aCause);
                    }
                }
            }
        } else {
            contradiction(g, "the source cannot reach all nodes");
        }
    }

    @Override
    public int getPropagationConditions(int vIdx) {
        return GraphEventType.REMOVE_ARC.getMask()+GraphEventType.ADD_NODE.getMask();
    }

    @Override
    public ESat isEntailed() {
        throw new UnsupportedOperationException("tree constraint isEntail() is not implemented");
    }
}
