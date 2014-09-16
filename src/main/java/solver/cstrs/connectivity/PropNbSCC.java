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

package solver.cstrs.connectivity;

import solver.constraints.Propagator;
import solver.constraints.PropagatorPriority;
import solver.exception.ContradictionException;
import solver.variables.IDirectedGraphVar;
import solver.variables.IntVar;
import solver.variables.Variable;
import util.ESat;
import util.graphOperations.connectivity.StrongConnectivityFinder;
import util.objects.setDataStructures.ISet;

/**
 * Propagator that ensures that the final graph consists in K Strongly Connected Components (SCC)
 * <p/>
 * simple checker and a bit of pruning (runs in linear time)
 *
 * @author Jean-Guillaume Fages
 */
public class PropNbSCC extends Propagator {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private IDirectedGraphVar g;
	private IntVar k;
	private StrongConnectivityFinder env_CC_finder, ker_CC_finder;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropNbSCC(IDirectedGraphVar graph, IntVar k) {
		super(new Variable[]{graph, k}, PropagatorPriority.LINEAR, false);
		this.g = graph;
		this.k = k;
		env_CC_finder = new StrongConnectivityFinder(g.getUB());
		ker_CC_finder = new StrongConnectivityFinder(g.getLB());
	}

	//***********************************************************************************
	// PROPAGATIONS
	//***********************************************************************************

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		// trivial case
		k.updateLowerBound(0,aCause);
		if(g.getPotentialNodes().getSize() == 0){
			k.instantiateTo(0,aCause);
			return;
		}
		if(k.getUB() == 0){
			ISet nodes = g.getPotentialNodes();
			for(int i = nodes.getFirstElement();i>=0;i=nodes.getNextElement()){
				g.removeNode(i,aCause);
			}
			return;
		}

		// bound computation
		int min = minCC();
		int max = maxCC();
		k.updateLowerBound(min, aCause);
		k.updateUpperBound(max,aCause);

		// A bit of pruning (removes unreachable nodes)
		if(k.getUB() == min && min != max){
			int ccs = env_CC_finder.getNbSCC();
			boolean pot = true;
			for (int cc = 0; cc < ccs; cc++) {
				for (int i = env_CC_finder.getSCCFirstNode(cc); i >= 0 && pot; i = env_CC_finder.getNextNode(i)) {
					if (g.getMandatoryNodes().contain(i)) {
						pot = false;
					}
				}
				if(pot){
					for (int i = env_CC_finder.getSCCFirstNode(cc); i >= 0 && pot; i = env_CC_finder.getNextNode(i)) {
						g.removeNode(i,aCause);
					}
				}
			}
		}
	}

	public int minCC() {
		env_CC_finder.findAllSCC();
		int ccs = env_CC_finder.getNbSCC();
		int minCC = 0;
		for (int cc = 0; cc < ccs; cc++) {
			for (int i = env_CC_finder.getSCCFirstNode(cc); i >= 0; i = env_CC_finder.getNextNode(i)) {
				if (g.getMandatoryNodes().contain(i)) {
					minCC++;
					break;
				}
			}
		}
		return minCC;
	}

	public int maxCC() {
		ker_CC_finder.findAllSCC();
		int nbK = ker_CC_finder.getNbSCC();
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
