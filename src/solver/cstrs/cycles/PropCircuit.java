/*
 * Copyright (c) 1999-2012, Ecole des Mines de Nantes
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

/**
 * Created by IntelliJ IDEA.
 * User: Jean-Guillaume Fages
 * Date: 03/10/11
 * Time: 19:56
 */

package solver.cstrs.cycles;

import memory.IEnvironment;
import memory.IStateInt;
import solver.constraints.Propagator;
import solver.constraints.PropagatorPriority;
import solver.exception.ContradictionException;
import solver.variables.EventType;
import solver.variables.IDirectedGraphVar;
import solver.variables.delta.IGraphDeltaMonitor;
import util.ESat;
import util.objects.setDataStructures.ISet;
import util.procedure.PairProcedure;

/**
 * Simple NoSubtour of Caseau-Laburthe adapted to the undirected case
 */
public class PropCircuit extends Propagator<IDirectedGraphVar> {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private IDirectedGraphVar g;
	private IGraphDeltaMonitor gdm;
	private int n;
	private PairProcedure arcEnforced;
	private IStateInt[] e1, e2, size;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************


	public PropCircuit(IDirectedGraphVar graph){
		super(new IDirectedGraphVar[]{graph}, PropagatorPriority.LINEAR, true);
		g = graph;
		gdm = g.monitorDelta(this);
		this.n = g.getNbMaxNodes();
		arcEnforced = new EnfArc();
		e1 = new IStateInt[n];
		e2 = new IStateInt[n];
		size = new IStateInt[n];
		IEnvironment environment = solver.getEnvironment();
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
		if((evtmask & EventType.FULL_PROPAGATION.mask)!=0) {
			for (int i = 0; i < n; i++) {
				e1[i].set(i);
				e2[i].set(i);
				size[i].set(1);
			}
			ISet nei;
			for (int i = 0; i < n; i++) {
				nei = g.getMandSuccOf(i);
				for (int j = nei.getFirstElement(); j >= 0; j = nei.getNextElement()) {
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
		gdm.forEachArc(arcEnforced, EventType.ENFORCEARC);
		gdm.unfreeze();
	}

	@Override
	public int getPropagationConditions(int vIdx) {
		return EventType.ENFORCEARC.mask;
	}

	@Override
	public ESat isEntailed() {
		ISet nodes = g.getMandatoryNodes();
		for (int i = nodes.getFirstElement(); i >= 0; i = nodes.getNextElement()) {
			if (g.getMandSuccOf(i).getSize() > 2 || g.getPotSuccOf(i).getSize() < 2) {
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
		if (t > 2 && t <= n && t < n && t<g.getMandatoryNodes().getSize()) {
			g.removeArc(ext1, ext2, aCause);
			g.removeArc(ext2, ext1, aCause);
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

	//***********************************************************************************
	// PROCEDURES
	//***********************************************************************************

	protected class EnfArc implements PairProcedure {
		@Override
		public void execute(int i, int j) throws ContradictionException {
			enforce(i, j);
		}
	}
}
