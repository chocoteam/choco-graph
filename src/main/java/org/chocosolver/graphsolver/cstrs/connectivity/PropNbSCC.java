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

import org.chocosolver.graphsolver.util.StrongConnectivityFinder;
import org.chocosolver.graphsolver.variables.DirectedGraphVar;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.ESat;

/**
 * Propagator that ensures that the final graph consists in K Strongly Connected Components (SCC)
 * <p/>
 * simple checker and a bit of pruning (runs in linear time)
 *
 * @author Jean-Guillaume Fages
 */
public class PropNbSCC extends Propagator<Variable> {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private DirectedGraphVar g;
	private IntVar k;
	private StrongConnectivityFinder envCCFinder, kerCCFinder;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropNbSCC(DirectedGraphVar graph, IntVar k) {
		super(new Variable[]{graph, k}, PropagatorPriority.LINEAR, false);
		this.g = graph;
		this.k = k;
		envCCFinder = new StrongConnectivityFinder(g.getUB());
		kerCCFinder = new StrongConnectivityFinder(g.getLB());
	}

	//***********************************************************************************
	// PROPAGATIONS
	//***********************************************************************************

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		// trivial case
		k.updateLowerBound(0, this);
		if (g.getPotentialNodes().size() == 0) {
			k.instantiateTo(0, this);
			return;
		}
		if (k.getUB() == 0) {
			for (int i : g.getPotentialNodes()) {
				g.removeNode(i, this);
			}
			return;
		}

		// bound computation
		int min = minCC();
		int max = maxCC();
		k.updateLowerBound(min, this);
		k.updateUpperBound(max, this);

		// A bit of pruning (removes unreachable nodes)
		if (k.getUB() == min && min != max) {
			int ccs = envCCFinder.getNbSCC();
			boolean pot = true;
			for (int cc = 0; cc < ccs; cc++) {
				for (int i = envCCFinder.getSCCFirstNode(cc); i >= 0 && pot; i = envCCFinder.getNextNode(i)) {
					if (g.getMandatoryNodes().contains(i)) {
						pot = false;
					}
				}
				if (pot) {
					for (int i = envCCFinder.getSCCFirstNode(cc); i >= 0; i = envCCFinder.getNextNode(i)) {
						g.removeNode(i, this);
					}
				}
			}
		}
	}

	public int minCC() {
		envCCFinder.findAllSCC();
		int ccs = envCCFinder.getNbSCC();
		int minCC = 0;
		for (int cc = 0; cc < ccs; cc++) {
			for (int i = envCCFinder.getSCCFirstNode(cc); i >= 0; i = envCCFinder.getNextNode(i)) {
				if (g.getMandatoryNodes().contains(i)) {
					minCC++;
					break;
				}
			}
		}
		return minCC;
	}

	public int maxCC() {
		kerCCFinder.findAllSCC();
		int nbK = kerCCFinder.getNbSCC();
		int delta = g.getPotentialNodes().size() - g.getMandatoryNodes().size();
		return nbK + delta;
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
