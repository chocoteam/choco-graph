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
import org.chocosolver.graphsolver.variables.IDirectedGraphVar;
import org.chocosolver.graphsolver.variables.delta.IGraphDeltaMonitor;
import org.chocosolver.memory.IEnvironment;
import org.chocosolver.memory.IStateInt;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.util.ESat;
import org.chocosolver.util.procedure.PairProcedure;

/**
 * Simple no-circuit constraint (from noCycle of Caseaux/Laburthe)
 *
 * @author Jean-Guillaume Fages
 */
public class PropPathNoCircuit extends Propagator<IDirectedGraphVar> {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private IDirectedGraphVar g;
	private IGraphDeltaMonitor gdm;
	private int n;
	private PairProcedure arcEnforced;
	private IStateInt[] origin, end, size;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	/**
	 * Ensures that graph has no circuit, with Caseaux/Laburthe/Pesant algorithm
	 * runs in O(1) per instantiation event
	 *
	 * @param graph
	 */
	public PropPathNoCircuit(IDirectedGraphVar graph) {
		super(new IDirectedGraphVar[]{graph}, PropagatorPriority.LINEAR, true);
		g = graph;
		gdm = g.monitorDelta(this);
		this.n = g.getNbMaxNodes();
		arcEnforced = new EnfArc();
		origin = new IStateInt[n];
		size = new IStateInt[n];
		end = new IStateInt[n];
		IEnvironment environment = graph.getEnvironment();
		for (int i = 0; i < n; i++) {
			origin[i] = environment.makeInt(i);
			size[i] = environment.makeInt(1);
			end[i] = environment.makeInt(i);
		}
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		int j;
		for (int i = 0; i < n; i++) {
			end[i].set(i);
			origin[i].set(i);
			size[i].set(1);
		}
		for (int i = 0; i < n; i++) {
			j = g.getMandSuccOf(i).iterator().next();
			if (j != -1) {
				enforce(i, j);
			}
		}
		gdm.unfreeze();
	}

	@Override
	public void propagate(int idxVarInProp, int mask) throws ContradictionException {
		gdm.freeze();
		gdm.forEachArc(arcEnforced, GraphEventType.ADD_ARC);
		gdm.unfreeze();
	}

	@Override
	public int getPropagationConditions(int vIdx) {
		return GraphEventType.ADD_ARC.getMask();
	}

	@Override
	public ESat isEntailed() {
		System.out.println("[WARNING] "+this.getClass().getSimpleName()+".isEntail() is not implemented yet " +
				"and returns true by default. Please do not reify this constraint ");
		return ESat.TRUE;
	}

	private void enforce(int i, int j) throws ContradictionException {
		int last = end[j].get();
		int start = origin[i].get();
		if (origin[j].get() != j) {
			fails();
		}
		g.removeArc(last, start, this);
		origin[last].set(start);
		end[start].set(last);
		size[start].add(size[j].get());
	}

	//***********************************************************************************
	// PROCEDURES
	//***********************************************************************************

	private class EnfArc implements PairProcedure {
		@Override
		public void execute(int i, int j) throws ContradictionException {
			enforce(i, j);
		}
	}
}
