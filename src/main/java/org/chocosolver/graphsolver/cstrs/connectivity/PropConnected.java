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
import org.chocosolver.graphsolver.variables.GraphEventType;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.setDataStructures.ISet;

import java.util.BitSet;

/**
 * Propagator checking that the graph is connected
 * (Allows graphs with 0 or 1 nodes)
 * Complete Filtering
 *
 * @author Jean-Guillaume Fages
 */
public class PropConnected extends Propagator<UndirectedGraphVar> {


	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private final int n;
	private final UndirectedGraphVar g;
	private final BitSet visited;
	private final UGVarConnectivityHelper helper;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropConnected(UndirectedGraphVar graph) {
		super(new UndirectedGraphVar[]{graph}, PropagatorPriority.LINEAR, false);
		this.g = graph;
		this.n = graph.getNbMaxNodes();
		this.visited = new BitSet(n);
		this.helper = new UGVarConnectivityHelper(g);
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	@Override
	public int getPropagationConditions(int vIdx) {
		return GraphEventType.REMOVE_ARC.getMask() + GraphEventType.ADD_NODE.getMask() + GraphEventType.REMOVE_NODE.getMask();
	}

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		// 0-node or 1-node graphs are accepted
		if (g.getPotentialNodes().size() <= 1) {
			setPassive();
			return;
		}
		// cannot filter if no mandatory node
		if (g.getMandatoryNodes().size() > 0) {

			// 1 --- explore the graph from the first mandatory node and
			// remove unreachable nodes (fail if mandatory node is not reached)
			visited.clear();
			int root = g.getMandatoryNodes().iterator().next();
			helper.exploreFrom(root, visited);
			for (int o = visited.nextClearBit(0); o < n; o = visited.nextClearBit(o + 1)) {
				g.removeNode(o, this);
			}

			if (g.getMandatoryNodes().size() > 1) {

				helper.findMandatoryArticulationPointsAndBridges();

				// 2 --- enforce articulation points that link two mandatory nodes
				for(int ap:helper.getArticulationPoints()){
					g.enforceNode(ap, this);
				}

				// 3 --- enforce isthma that link two mandatory nodes (current version is bugged)
				ISet mNodes = g.getMandatoryNodes();
				TIntArrayList brI = helper.getBridgeFrom();
				TIntArrayList brJ = helper.getBridgeTo();
				for(int k=0; k<brI.size(); k++){
					int i = brI.get(k);
					int j = brJ.get(k);
					if(mNodes.contains(i) && mNodes.contains(j)){
						g.enforceArc(i, j, this);
					}
				}
			}
		}
	}

	@Override
	public ESat isEntailed() {
		// 0-node or 1-node graphs are accepted
		if (g.getPotentialNodes().size() <= 1) {
			return ESat.TRUE;
		}
		// cannot conclude if less than 2 mandatory nodes
		if (g.getMandatoryNodes().size() < 2) {
			return ESat.UNDEFINED;
		}
		// BFS from a mandatory node
		visited.clear();
		int root = g.getMandatoryNodes().iterator().next();
		helper.exploreFrom(root, visited);
		// every mandatory node is reached?
		for (int i : g.getMandatoryNodes()) {
			if (!visited.get(i)) {
				return ESat.FALSE;
			}
		}
		if (g.isInstantiated()) {
			return ESat.TRUE;
		}
		return ESat.UNDEFINED;
	}
}
