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

import gnu.trove.list.array.TIntArrayList;
import org.chocosolver.graphsolver.util.UGVarConnectivityHelper;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.setDataStructures.ISet;

import java.util.BitSet;

/**
 * Propagator that ensures that the final graph consists in K Connected Components (CC)
 * <p/>
 * complete filtering in linear time
 *
 * @author Jean-Guillaume Fages
 */
public class PropNbCC extends Propagator<Variable> {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private final UndirectedGraphVar g;
	private final IntVar k;
	private final UGVarConnectivityHelper helper;
	private final BitSet visitedMin, visitedMax;
	private final int[] fifo, ccOf;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropNbCC(UndirectedGraphVar graph, IntVar k) {
		super(new Variable[]{graph, k}, PropagatorPriority.LINEAR, false);
		this.g = graph;
		this.k = k;
		this.helper = new UGVarConnectivityHelper(g);
		this.visitedMin = new BitSet(g.getNbMaxNodes());
		this.visitedMax = new BitSet(g.getNbMaxNodes());
		this.fifo = new int[g.getNbMaxNodes()];
		this.ccOf = new int[g.getNbMaxNodes()];
	}

	//***********************************************************************************
	// PROPAGATIONS
	//***********************************************************************************

	@Override
	public void propagate(int evtmask) throws ContradictionException {

		// trivial case
		k.updateBounds(0, g.getPotentialNodes().size(), this);
		if (k.getUB() == 0) {
			for (int i : g.getPotentialNodes()) g.removeNode(i, this);
			return;
		}

		// bound computation
		int min = minCC();
		int max = maxCC();
		k.updateLowerBound(min, this);
		k.updateUpperBound(max, this);

		// The number of CC cannot increase :
		// - remove unreachable nodes
		// - force articulation points and bridges
		if(min != max) {
			if (k.getUB() == min) {

				// 1 --- remove unreachable nodes
				int n = g.getNbMaxNodes();
				for (int o = visitedMin.nextClearBit(0); o < n; o = visitedMin.nextClearBit(o + 1)) {
					g.removeNode(o, this);
				}

				ISet mNodes = g.getMandatoryNodes();
				if (mNodes.size() >= 2) {

					helper.findMandatoryArticulationPointsAndBridges();

					// 2 --- enforce articulation points that link two mandatory nodes
					for (int ap : helper.getArticulationPoints()) {
						g.enforceNode(ap, this);
					}

					// 3 --- enforce isthma that link two mandatory nodes (current version is bugged)
					TIntArrayList brI = helper.getBridgeFrom();
					TIntArrayList brJ = helper.getBridgeTo();
					for (int k = 0; k < brI.size(); k++) {
						int i = brI.get(k);
						int j = brJ.get(k);
						if (mNodes.contains(i) && mNodes.contains(j)) {
							g.enforceArc(i, j, this);
						}
					}
				}
			}
			// a maximal number of CC is required : remaining nodes will be singleton
			else if(k.getLB() == max){
				// --- transform every potential node into a mandatory isolated node
				ISet mNodes = g.getMandatoryNodes();
				for(int i:g.getPotentialNodes()){
					if(!mNodes.contains(i)){
						for(int j:g.getPotNeighOf(i)){
							g.removeArc(i,j,this);
						}
						g.enforceNode(i,this);
					}
				}
				// --- remove edges between mandatory nodes that would merge 2 CC
				// note that it can happen that 2 mandatory node already belong to the same CC
				// if so the edge should not be filtered
				for(int i:g.getPotentialNodes()){
					for(int j:g.getPotNeighOf(i)){
						if(ccOf[i] != ccOf[j]) {
							g.removeArc(i,j,this);
						}
					}
				}
			}
		}
	}

	private int minCC() {
		int min = 0;
		visitedMin.clear();
		for (int i : g.getMandatoryNodes().toArray()) {
			if (!visitedMin.get(i)) {
				helper.exploreFrom(i, visitedMin);
				min++;
			}
		}
		return min;
	}

	private int maxCC() {
		int nbK = 0;
		visitedMax.clear();
		for(int i:g.getMandatoryNodes().toArray()) {
			if(!visitedMax.get(i)) {
				exploreLBFrom(i, visitedMax);
				nbK++;
			}
		}
		int delta = g.getPotentialNodes().size() - g.getMandatoryNodes().size();
		return nbK + delta;
	}

	private void exploreLBFrom(int root, BitSet visited) {
		int first = 0;
		int last = 0;
		int i = root;
		fifo[last++] = i;
		visited.set(i);
		ccOf[i] = root; // mark cc of explored node
		while (first < last) {
			i = fifo[first++];
			for (int j : g.getMandNeighOf(i)) { // mandatory edges only
				if (!visited.get(j)) {
					visited.set(j);
					ccOf[j] = root; // mark cc of explored node
					fifo[last++] = j;
				}
			}
		}
	}

	//***********************************************************************************
	// INFO
	//***********************************************************************************

	@Override
	public ESat isEntailed() {
		if (k.getUB() < minCC() || k.getLB() > maxCC()) {
			return ESat.FALSE;
		}
		if (isCompletelyInstantiated()) {
			return ESat.TRUE;
		}
		return ESat.UNDEFINED;
	}
}
