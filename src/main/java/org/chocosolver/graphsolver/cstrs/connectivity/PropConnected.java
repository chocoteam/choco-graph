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

import org.chocosolver.graphsolver.util.ConnectivityFinder;
import org.chocosolver.graphsolver.variables.GraphEventType;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.ISetIterator;

import java.util.BitSet;

/**
 * Propagator checking that the graph is connected
 * can filter by forcing bridges
 * <p>
 * In case not all vertices are mandatory, the filtering could be improved
 *
 * @author Jean-Guillaume Fages
 */
public class PropConnected extends Propagator<UndirectedGraphVar> {


	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private int n;
	private BitSet visited;
	private int[] fifo;
	private UndirectedGraphVar g;
	private ConnectivityFinder env_CC_finder;
	private boolean checkerOnly;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropConnected(UndirectedGraphVar graph) {
		this(graph, false);
	}

	public PropConnected(UndirectedGraphVar graph, boolean checkerOnly) {
		super(new UndirectedGraphVar[]{graph}, PropagatorPriority.LINEAR, false);
		this.g = graph;
		this.n = graph.getNbMaxNodes();
		this.visited = new BitSet(n);
		this.fifo = new int[n];
		this.env_CC_finder = new ConnectivityFinder(g.getUB());
		this.checkerOnly = checkerOnly;
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
		if (g.getPotentialNodes().size() == 0) {
			fails();
		}
		if (g.getMandatoryNodes().size() > 1) {
			// explore the graph from the first mandatory node
			explore();
			// remove unreachable nodes
			for (int o = visited.nextClearBit(0); o < n; o = visited.nextClearBit(o + 1)) {
				g.removeNode(o, this);
			}
			// force articulation points
			if (g.getLB().getNodes().size() < g.getUB().getNodes().size()) {
				forceArticulationPoints();
			}

			// force isthma in case vertices are fixed
			if (g.getMandatoryNodes().size() == g.getPotentialNodes().size() && !checkerOnly) {
				if (!env_CC_finder.isConnectedAndFindIsthma()) {
					throw new UnsupportedOperationException("connectivity has been checked");
				}
				int nbIsma = env_CC_finder.isthmusFrom.size();
				for (int i = 0; i < nbIsma; i++) {
					g.enforceArc(env_CC_finder.isthmusFrom.get(i), env_CC_finder.isthmusTo.get(i), this);
				}
			}
		}
	}

	@Override
	public ESat isEntailed() {
		if (g.getPotentialNodes().size() == 1) {
			return ESat.TRUE;
		}
		//Graphs with zero nodes are not connected.
		if (g.getPotentialNodes().size() == 0) {
			return ESat.FALSE;
		}
		explore();
		for (int i : g.getMandatoryNodes()) {
			if (!visited.get(i)) {
				return ESat.FALSE;
			}
		}
		if (!g.isInstantiated()) {
			return ESat.UNDEFINED;
		}

		return ESat.TRUE;
	}

	private void explore() {
		visited.clear();
		int first = 0;
		int last = 0;
		if (g.getMandatoryNodes().size() <= 0) {
			return; // empty graph
		}
		int i = g.getMandatoryNodes().iterator().next();
		fifo[last++] = i;
		visited.set(i);
		while (first < last) {
			i = fifo[first++];
			for (int j : g.getPotNeighOf(i)) {
				if (!visited.get(j)) {
					visited.set(j);
					fifo[last++] = j;
				}
			}
		}
	}

	int[] numOfNode;
	int[] nodeOfNum;
	int[] inf, p;
	ISetIterator[] iterators;
	boolean[] mandInSub;

	public void forceArticulationPoints() throws ContradictionException {
		if (inf == null) {
			nodeOfNum = new int[n];
			numOfNode = new int[n];
			inf = new int[n];
			p = new int[n];
			iterators = new ISetIterator[n];
			mandInSub = new boolean[n];
		}
		int start = -1;
		int nNodes = g.getUB().getNodes().size();
		ISet mandNodes = g.getLB().getNodes();
		ISet act = g.getUB().getNodes();
		ISetIterator iter = act.iterator();
		while (iter.hasNext()) {
			int i = iter.next();
			inf[i] = Integer.MAX_VALUE;
			p[i] = -1;
			iterators[i] = g.getUB().getSuccOrNeighOf(i).iterator();
			mandInSub[i] = false;
			if (start == -1 && mandNodes.contains(i)) start = i;
		}
		if (start == -1) return;
		//algo
		int i = start;
		int k = 0;
		numOfNode[start] = k;
		nodeOfNum[k] = start;
		p[start] = start;
		int j, q;
		while (true) {
			if (iterators[i].hasNext()) {
				j = iterators[i].next();
				if (p[j] == -1) { // no need to know if root is articulation (already mandatory node)
					p[j] = i;
					i = j;
					k++;
					numOfNode[i] = k;
					nodeOfNum[k] = i;
					inf[i] = numOfNode[i];
					mandInSub[i] = mandNodes.contains(i);
				} else if (p[i] != j) {
					inf[i] = Math.min(inf[i], numOfNode[j]);
					mandInSub[i] |= mandNodes.contains(j);
				}
			} else {
				if (i == start) {
					if (k < nNodes - 1) {
						throw new UnsupportedOperationException("disconnected graph");
					}
					return;
				}
				q = inf[i];
				boolean mis = mandInSub[i];
				i = p[i];
				mandInSub[i] |= mis;
				inf[i] = Math.min(q, inf[i]);
				if (q >= numOfNode[i] && i != start) {
					if (mandInSub[i] && !mandNodes.contains(i)) // must contain a mandatory node in subtree
						g.enforceNode(i, this); // ARTICULATION POINT DETECTED
				}
			}
		}
	}
}
