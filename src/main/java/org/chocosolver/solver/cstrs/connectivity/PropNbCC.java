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

package org.chocosolver.solver.cstrs.connectivity;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IUndirectedGraphVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.ESat;
import org.chocosolver.util.graphOperations.connectivity.ConnectivityFinder;
import org.chocosolver.util.objects.setDataStructures.ISet;

/**
 * Propagator that ensures that the final graph consists in K Connected Components (CC)
 * <p/>
 * simple checker and a bit of pruning (runs in linear time)
 *
 * @author Jean-Guillaume Fages
 */
public class PropNbCC extends Propagator {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private IUndirectedGraphVar g;
	private IntVar k;
	private ConnectivityFinder env_CC_finder, ker_CC_finder;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropNbCC(IUndirectedGraphVar graph, IntVar k) {
		super(new Variable[]{graph, k}, PropagatorPriority.LINEAR, false);
		this.g = graph;
		this.k = k;
		env_CC_finder = new ConnectivityFinder(g.getUB());
		ker_CC_finder = new ConnectivityFinder(g.getLB());
	}

	//***********************************************************************************
	// PROPAGATIONS
	//***********************************************************************************

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		// trivial case
		k.updateLowerBound(0,this);
		if(g.getPotentialNodes().getSize() == 0){
			k.instantiateTo(0,this);
			return;
		}
		if(k.getUB() == 0){
			ISet nodes = g.getPotentialNodes();
			for(int i = nodes.getFirstElement();i>=0;i=nodes.getNextElement()){
				g.removeNode(i,this);
			}
			return;
		}

		// bound computation
		int min = minCC();
		int max = maxCC();
		k.updateLowerBound(min, this);
		k.updateUpperBound(max,this);

		// A bit of pruning (removes unreachable nodes)
		if(k.getUB() == min && min != max){
			int ccs = env_CC_finder.getNBCC();
			boolean pot = true;
			for (int cc = 0; cc < ccs; cc++) {
				for (int i = env_CC_finder.getCC_firstNode()[cc]; i >= 0 && pot; i = env_CC_finder.getCC_nextNode()[i]) {
					if (g.getMandatoryNodes().contain(i)) {
						pot = false;
					}
				}
				if(pot){
					for (int i = env_CC_finder.getCC_firstNode()[cc]; i >= 0 && pot; i = env_CC_finder.getCC_nextNode()[i]) {
						g.removeNode(i,this);
					}
				}
			}
		}

		// Force isthma in case of 1 CC and if vertices are fixed
		if(k.isInstantiatedTo(1) && g.getMandatoryNodes().getSize()==g.getPotentialNodes().getSize()){
			if (!env_CC_finder.isConnectedAndFindIsthma()) {
				throw new UnsupportedOperationException("connectivity has been checked");
			}
			int nbIsma = env_CC_finder.isthmusFrom.size();
			for (int i = 0; i < nbIsma; i++) {
				g.enforceArc(env_CC_finder.isthmusFrom.get(i), env_CC_finder.isthmusTo.get(i), this);
			}
		}
	}

	public int minCC() {
		env_CC_finder.findAllCC();
		int ccs = env_CC_finder.getNBCC();
		int minCC = 0;
		for (int cc = 0; cc < ccs; cc++) {
			for (int i = env_CC_finder.getCC_firstNode()[cc]; i >= 0; i = env_CC_finder.getCC_nextNode()[i]) {
				if (g.getMandatoryNodes().contain(i)) {
					minCC++;
					break;
				}
			}
		}
		return minCC;
	}

	public int maxCC() {
		ker_CC_finder.findAllCC();
		int nbK = ker_CC_finder.getNBCC();
		int delta = g.getPotentialNodes().getSize()-g.getMandatoryNodes().getSize();
		return nbK+delta;
	}

	//***********************************************************************************
	// INFO
	//***********************************************************************************

	@Override
	public ESat isEntailed() {
		if (k.getUB() < minCC() || k.getLB()>maxCC()) {
			return ESat.FALSE;
		}
		if (isCompletelyInstantiated()) {
			return ESat.TRUE;
		}
		return ESat.UNDEFINED;
	}
}
