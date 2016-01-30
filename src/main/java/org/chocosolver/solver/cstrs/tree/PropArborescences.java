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

package org.chocosolver.solver.cstrs.tree;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IDirectedGraphVar;
import org.chocosolver.util.ESat;
import org.chocosolver.util.graphOperations.dominance.AbstractLengauerTarjanDominatorsFinder;
import org.chocosolver.util.graphOperations.dominance.AlphaDominatorsFinder;
import org.chocosolver.util.graphOperations.dominance.SimpleDominatorsFinder;
import org.chocosolver.util.objects.graphs.DirectedGraph;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetType;

import java.util.BitSet;
import java.util.logging.Logger;

/**
 * Arborescences constraint (simplification from tree constraint) based on dominators
 * CONSIDERS THAT EACH NODE WITH NO PREDECESSOR IS A ROOT (needs at least one such node)
 * @author Jean-Guillaume Fages
 */
public class PropArborescences extends Propagator<IDirectedGraphVar> {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	// flow graph
	protected IDirectedGraphVar g;
	protected DirectedGraph connectedGraph;
	// number of nodes
	protected int n;
	// dominators finder that contains the dominator tree
	protected AbstractLengauerTarjanDominatorsFinder domFinder;
	protected ISet[] successors;
	protected BitSet mandVert;

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
		mandVert = new BitSet(n);
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
		// reset data structures
		reset();
		// reach all nodes from root
		if (domFinder.findDominators()) {
			// remove backward arcs
			remBackArcs();
			// enforce dominators and arc-dominators
			enforceDominators();
		} else {
			contradiction(g, "the source cannot reach all nodes");
		}
	}

	protected void reset(){
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
	}

	protected void remBackArcs() throws ContradictionException {
		// remove backward arcs
		ISet potNodes = g.getPotentialNodes();
		for (int x = potNodes.getFirstElement(); x>=0; x=potNodes.getNextElement()) {
			g.removeArc(x, x, this); // no loop
			ISet nei = g.getPotSuccOf(x);
			for (int y = nei.getFirstElement(); y >= 0; y = nei.getNextElement()) {
				if (domFinder.isDomminatedBy(x, y)) {
					g.removeArc(x, y, this);
				}
			}
		}
	}

	protected void enforceDominators() throws ContradictionException {
		// enforce dominator nodes and arcs
		mandVert.clear();
		ISet mn = g.getMandatoryNodes();
		for (int x = mn.getFirstElement(); x>=0; x=mn.getNextElement()) {
			mandVert.set(x);
		}
		while(mandVert.nextSetBit(0)>=0){
			enforceDominatorsFrom(mandVert.nextSetBit(0));
		}
	}

	protected void enforceDominatorsFrom(int j) throws ContradictionException {
		mandVert.clear(j);
		int i = domFinder.getImmediateDominatorsOf(j);
		if(i!=n) {
			if (!domFinder.isDomminatedBy(j, i)) {
				throw new UnsupportedOperationException();
			}
			// DOMINATOR enforcing
			if(g.enforceNode(i, this)){
				mandVert.set(i);
			}
			// ARC-DOMINATOR enforcing
			ISet pred = g.getPotPredOf(j);
			if(pred.contain(i) && !g.getMandPredOf(j).contain(i)) {
				boolean arcDom = true;
				for (int p = pred.getFirstElement(); p >= 0; p = pred.getNextElement()) {
					if (p != i && !domFinder.isDomminatedBy(p, j)) {
						arcDom = false;
					}
				}
				if (arcDom) {
					g.enforceArc(i, j, this);
				}
			}
		}
	}

	@Override
	public ESat isEntailed() {
		System.out.println("[WARNING] "+this.getClass().getSimpleName()+".isEntail() is not implemented yet " +
				"and returns true by default. Please do not reify this constraint ");
		return ESat.TRUE;
	}
}
