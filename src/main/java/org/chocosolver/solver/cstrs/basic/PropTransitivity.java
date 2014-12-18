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

package org.chocosolver.solver.cstrs.basic;

import gnu.trove.list.array.TIntArrayList;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.GraphEventType;
import org.chocosolver.solver.variables.IGraphVar;
import org.chocosolver.solver.variables.delta.IGraphDeltaMonitor;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.procedure.PairProcedure;

/**
 * Propagator that ensures that the relation of the graph is transitive : (a,b) + (b,c) => (a,c)
 *
 * @author Jean-Guillaume Fages
 */
public class PropTransitivity<V extends IGraphVar> extends Propagator<V> {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private V g;
	private IGraphDeltaMonitor gdm;
	private PairProcedure arcEnforced, arcRemoved;
	private TIntArrayList eF, eT, rF, rT;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropTransitivity(V graph) {
		super((V[]) new IGraphVar[]{graph}, PropagatorPriority.LINEAR, true);
		g = graph;
		gdm = g.monitorDelta(this);
		int n = g.getNbMaxNodes();
		eF = new TIntArrayList(n);
		eT = new TIntArrayList(n);
		rF = new TIntArrayList(n);
		rT = new TIntArrayList(n);
		arcEnforced = this::_enfArc;
		arcRemoved = this::_remArc;
	}

	//***********************************************************************************
	// PROPAGATIONS
	//***********************************************************************************

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		int n = g.getNbMaxNodes();
		ISet nodes = g.getPotentialNodes();
		for (int i = nodes.getFirstElement(); i>=0; i=nodes.getNextElement()) {
			for (int j = 0; j < n; j++) {
				if (g.getMandSuccOrNeighOf(i).contain(j)) {
					_enfArc(i, j);
				} else if (!g.getPotSuccOrNeighOf(i).contain(j)) {
					_remArc(i, j);
				}
			}
		}
		filter();
		gdm.unfreeze();
	}

	@Override
	public void propagate(int idxVarInProp, int mask) throws ContradictionException {
		gdm.freeze();
		rT.clear();rF.clear();eT.clear();eF.clear();
		gdm.forEachArc(arcEnforced, GraphEventType.ADD_ARC);
		gdm.forEachArc(arcRemoved, GraphEventType.REMOVE_ARC);
		filter();
		gdm.unfreeze();
	}

	private void filter() throws ContradictionException{
		// Fix point
		assert eF.size() == eT.size();
		while(!eF.isEmpty()){
			assert eF.size() == eT.size();
			enfArc(eF.removeAt(eF.size()-1), eT.removeAt(eT.size()-1));
		}
		assert rF.size() == rT.size();
		while(!rF.isEmpty()){
			assert rF.size() == rT.size();
			remArc(rF.removeAt(rF.size()-1), rT.removeAt(rT.size()-1));
		}
		assert eF.size() == eT.size();
		if(!eF.isEmpty()){
			filter();
		}
	}

	//***********************************************************************************
	// INFO
	//***********************************************************************************

	@Override
	public int getPropagationConditions(int vIdx) {
		return GraphEventType.REMOVE_ARC.getMask() + GraphEventType.ADD_ARC.getMask();
	}

	@Override
	public ESat isEntailed() {
		int n = g.getNbMaxNodes();
		for (int i = 0; i < n; i++) {
			ISet m = g.getMandSuccOrNeighOf(i);
			for (int j = m.getFirstElement(); j >= 0; j = m.getNextElement()) {
				if(i!=j){
					ISet m2 = g.getMandSuccOrNeighOf(j);
					for (int j2 = m2.getFirstElement(); j2 >= 0; j2 = m2.getNextElement()) {
						if(j2!=i && !g.getPotSuccOrNeighOf(i).contain(j2)){
							return ESat.FALSE;
						}
					}
				}
			}
		}
		if (g.isInstantiated()) {
			return ESat.TRUE;
		}
		return ESat.UNDEFINED;
	}

	//***********************************************************************************
	// PROCEDURE
	//***********************************************************************************

	private void _enfArc(int x, int y){
		eF.add(x);
		eT.add(y);
	}

	private void _remArc(int x, int y){
		rF.add(x);
		rT.add(y);
	}

	// --- Arc enforcings
	private void enfArc(int from, int to) throws ContradictionException {
		if (from != to) {
			ISet ker = g.getMandSuccOrNeighOf(to);
			ISet env = g.getPotSuccOrNeighOf(to);
			for (int i = env.getFirstElement(); i >= 0; i = env.getNextElement()) {
				if (i != to && i != from) {
					if (ker.contain(i)) {
						if (g.enforceArc(from, i, aCause)) {
							_enfArc(from, i);
						}
					} else if (!g.getPotSuccOrNeighOf(from).contain(i)) {
						if (g.removeArc(to, i, aCause)) {
							_remArc(to, i);
						}
					}
				}
			}
			ker = g.getMandPredOrNeighOf(from);
			env = g.getPotPredOrNeighOf(from);
			for (int i = env.getFirstElement(); i >= 0; i = env.getNextElement()) {
				if(i!=to && i!=from) {
					if (ker.contain(i)) {
						if (g.enforceArc(i, to, aCause)) {
							_enfArc(i, to);
						}
					} else if (!g.getPotSuccOrNeighOf(i).contain(to)) {
						if (g.removeArc(i, from, aCause)) {
							_remArc(i, from);
						}
					}
				}
			}
		}
	}

	// --- Arc removals
	private void remArc(int from, int to) throws ContradictionException {
		if (from != to) {
			ISet nei = g.getMandSuccOrNeighOf(from);
			for (int i = nei.getFirstElement(); i >= 0; i = nei.getNextElement()) {
				if (g.removeArc(i, to, aCause)) {
					_remArc(i, to);
				}
			}
			nei = g.getMandPredOrNeighOf(to);
			for (int i = nei.getFirstElement(); i >= 0; i = nei.getNextElement()) {
				if (g.removeArc(from, i, aCause)) {
					_remArc(from, i);
				}
			}
		}
	}
}
