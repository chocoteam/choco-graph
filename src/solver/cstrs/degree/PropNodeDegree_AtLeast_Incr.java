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

package solver.cstrs.degree;

import solver.constraints.Propagator;
import solver.constraints.PropagatorPriority;
import solver.variables.IncidentSet;
import solver.exception.ContradictionException;
import solver.variables.EventType;
import solver.variables.IGraphVar;
import solver.variables.delta.IGraphDeltaMonitor;
import solver.variables.IDirectedGraphVar;
import solver.variables.IUndirectedGraphVar;
import util.ESat;
import util.objects.graphs.Orientation;
import util.objects.setDataStructures.ISet;
import util.procedure.IntProcedure;
import util.procedure.PairProcedure;

/**
 * Propagator that ensures that a node has at most N successors/predecessors/neighbors
 *
 * @author Jean-Guillaume Fages
 */
public class PropNodeDegree_AtLeast_Incr extends Propagator<IGraphVar> {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private IGraphVar g;
	private int[] degrees;
	private IncidentSet target;
	private IGraphDeltaMonitor gdm;
	private PairProcedure proc;
	private IntProcedure nodeProc;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropNodeDegree_AtLeast_Incr(IDirectedGraphVar graph, Orientation setType, int degree) {
		this(graph, setType, buildArray(degree, graph.getNbMaxNodes()));
	}

	public PropNodeDegree_AtLeast_Incr(IDirectedGraphVar graph, Orientation setType, int[] degrees) {
		super(new IDirectedGraphVar[]{graph}, PropagatorPriority.BINARY, true);
		g = graph;
		this.degrees = degrees;
		switch (setType) {
			case SUCCESSORS:
				target = new IncidentSet.SuccOrNeighSet();
				proc = new PairProcedure() {
					@Override
					public void execute(int i, int j) throws ContradictionException {
						checkAtLeast(i);
					}
				};
				break;
			case PREDECESSORS:
				target = new IncidentSet.PredOrNeighSet();
				proc = new PairProcedure() {
					@Override
					public void execute(int i, int j) throws ContradictionException {
						checkAtLeast(j);
					}
				};
				break;
			default:
				throw new UnsupportedOperationException();
		}
		nodeProc = new IntProcedure() {
			@Override
			public void execute(int i) throws ContradictionException {
				checkAtLeast(i);
			}
		};
		gdm = g.monitorDelta(this);
	}

	public PropNodeDegree_AtLeast_Incr(IUndirectedGraphVar graph, int degree) {
		this(graph, buildArray(degree, graph.getNbMaxNodes()));
	}

	public PropNodeDegree_AtLeast_Incr(IUndirectedGraphVar graph, int[] degrees) {
		super(new IUndirectedGraphVar[]{graph}, PropagatorPriority.BINARY, true);
		target = new IncidentSet.SuccOrNeighSet();
		g = graph;
		this.degrees = degrees;
		gdm = g.monitorDelta(this);
		proc = new PairProcedure() {
			@Override
			public void execute(int i, int j) throws ContradictionException {
				checkAtLeast(i);
				checkAtLeast(j);
			}
		};
		nodeProc = new IntProcedure() {
			@Override
			public void execute(int i) throws ContradictionException {
				checkAtLeast(i);
			}
		};
	}

	private static int[] buildArray(int degree, int n) {
		int[] degrees = new int[n];
		for (int i = 0; i < n; i++) {
			degrees[i] = degree;
		}
		return degrees;
	}

	//***********************************************************************************
	// PROPAGATIONS
	//***********************************************************************************

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		ISet act = g.getPotentialNodes();
		for (int node = act.getFirstElement(); node >= 0; node = act.getNextElement()) {
			checkAtLeast(node);
		}
		gdm.unfreeze();
	}

	@Override
	public void propagate(int idxVarInProp, int mask) throws ContradictionException {
		gdm.freeze();
		gdm.forEachNode(nodeProc,EventType.REMOVENODE);
		gdm.forEachArc(proc,EventType.REMOVEARC);
		gdm.unfreeze();
	}

	//***********************************************************************************
	// INFO
	//***********************************************************************************

	@Override
	public int getPropagationConditions(int vIdx) {
		return EventType.REMOVEARC.mask + EventType.ENFORCENODE.mask;
	}

	@Override
	public ESat isEntailed() {
		ISet act = g.getMandatoryNodes();
		for (int i = act.getFirstElement(); i >= 0; i = act.getNextElement()) {
			if (target.getPotSet(g, i).getSize() < degrees[i]) {
				return ESat.FALSE;
			}
		}
		if (!g.isInstantiated()) {
			return ESat.UNDEFINED;
		}
		return ESat.TRUE;
	}

	//***********************************************************************************
	// PROCEDURES
	//***********************************************************************************

	private void checkAtLeast(int i) throws ContradictionException {
		ISet nei = target.getPotSet(g, i);
		ISet ker = target.getMandSet(g, i);
		int size = nei.getSize();
		if (size < degrees[i]) {
			g.removeNode(i, aCause);
		} else if (size == degrees[i] && g.getMandatoryNodes().contain(i) && ker.getSize() < size) {
			for (int s = nei.getFirstElement(); s >= 0; s = nei.getNextElement()) {
				target.enforce(g, i, s, aCause);
			}
		}
	}
}