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
import org.chocosolver.graphsolver.variables.IUndirectedGraphVar;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.util.ESat;

import java.util.BitSet;

/**
 * Propagator checking that the graph is connected
 * can filter by forcing bridges
 *
 * In case not all vertices are mandatory, the filtering could be improved
 *
 * @author Jean-Guillaume Fages
 */
public class PropConnected extends Propagator<IUndirectedGraphVar> {


	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private int n;
	private BitSet visited;
	private int[] fifo;
	private IUndirectedGraphVar g;
	private ConnectivityFinder env_CC_finder;
	private boolean checkerOnly;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropConnected(IUndirectedGraphVar graph) {
		this(graph,false);
	}

	public PropConnected(IUndirectedGraphVar graph, boolean checkerOnly) {
		super(new IUndirectedGraphVar[]{graph}, PropagatorPriority.LINEAR, false);
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
		if(g.getMandatoryNodes().size()>1) {
			// explore the graph from the first mandatory node
			explore();
			// remove unreachable nodes
			for (int o = visited.nextClearBit(0); o < n; o = visited.nextClearBit(o + 1)) {
				g.removeNode(o,this);
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
			// one could force nodes which are necessary to connect mandatory nodes together (dominator algo)
		}
	}

	@Override
	public ESat isEntailed() {
		if(g.getPotentialNodes().size()==1){
			return ESat.TRUE;
		}
		//Graphs with zero nodes are not connected.
		if(g.getMandatoryNodes().size() == 0){
			return ESat.FALSE;
		}
		explore();
		for(int i: g.getMandatoryNodes()){
			if(!visited.get(i)){
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
		if(g.getMandatoryNodes().size()<=0){
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
}
