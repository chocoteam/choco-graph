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

package org.chocosolver.graphsolver.cstrs.cycles;

import org.chocosolver.graphsolver.variables.GraphEventType;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.graphsolver.variables.delta.GraphDeltaMonitor;
import org.chocosolver.memory.IEnvironment;
import org.chocosolver.memory.IStateInt;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.events.PropagatorEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.setDataStructures.ISet;

/**
 * Simple NoSubtour of Caseau-Laburthe adapted to the undirected case
 *
 * @author Jean-Guillaume Fages
 */
public class PropCycle extends Propagator<UndirectedGraphVar> {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private UndirectedGraphVar g;
	private GraphDeltaMonitor gdm;
	private int n;
	private IStateInt[] e1, e2, size;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************


	public PropCycle(UndirectedGraphVar graph) {
		super(new UndirectedGraphVar[]{graph}, PropagatorPriority.LINEAR, true);
		g = graph;
		gdm = g.monitorDelta(this);
		this.n = g.getNbMaxNodes();
		e1 = new IStateInt[n];
		e2 = new IStateInt[n];
		size = new IStateInt[n];
		IEnvironment environment = graph.getEnvironment();
		for (int i = 0; i < n; i++) {
			e1[i] = environment.makeInt(i);
			e2[i] = environment.makeInt(i);
			size[i] = environment.makeInt(1);
		}
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		if (PropagatorEventType.isFullPropagation(evtmask)) {
			for (int i = 0; i < n; i++) {
				e1[i].set(i);
				e2[i].set(i);
				size[i].set(1);
				g.removeArc(i,i,this);
			}
			ISet nei;
			for (int i = 0; i < n; i++) {
				nei = g.getMandNeighOf(i);
				for (int j : nei) {
					if (i < j) {
						enforce(i, j);
					}
				}
			}
		}
		gdm.unfreeze();
	}

	@Override
	public void propagate(int idxVarInProp, int mask) throws ContradictionException {
		gdm.freeze();
		gdm.forEachArc(this::enforce, GraphEventType.ADD_ARC);
		gdm.unfreeze();
	}

	@Override
	public int getPropagationConditions(int vIdx) {
		return GraphEventType.ADD_ARC.getMask();
	}

	@Override
	public ESat isEntailed() {
		ISet nodes = g.getMandatoryNodes();
		for (int i : nodes) {
			if (g.getMandNeighOf(i).size() > 2 || g.getPotNeighOf(i).size() < 2) {
				return ESat.FALSE;
			}
		}
		if (g.isInstantiated()) {
			return ESat.TRUE;
		}
		return ESat.UNDEFINED;
	}

	private void enforce(int i, int j) throws ContradictionException {
		int ext1 = getExt(i);
		int ext2 = getExt(j);
		int t = size[ext1].get() + size[ext2].get();
		setExt(ext1, ext2);
		setExt(ext2, ext1);
		size[ext1].set(t);
		size[ext2].set(t);
		if (t > 2) {
			if (t < g.getMandatoryNodes().size()) {
				g.removeArc(ext1, ext2, this);
			} else if (g.getMandatoryNodes().size() == g.getPotentialNodes().size()) {
				g.enforceArc(ext1, ext2, this);
			}
		}
	}

	private int getExt(int i) {
		return (e1[i].get() == i) ? e2[i].get() : e1[i].get();
	}

	private void setExt(int i, int ext) {
		if (e1[i].get() == i) {
			e2[i].set(ext);
		} else {
			e1[i].set(ext);
		}
	}
}
