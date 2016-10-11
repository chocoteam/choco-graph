package org.chocosolver.graphsolver.cstrs.symmbreaking;

import org.chocosolver.graphsolver.variables.GraphEventType;
import org.chocosolver.graphsolver.variables.IDirectedGraphVar;
import org.chocosolver.graphsolver.variables.delta.IGraphDeltaMonitor;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.procedure.PairProcedure;
import org.chocosolver.util.tools.ArrayUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Моклев Вячеслав
 */
public class PropIncrementalAdjacencyMatrix extends Propagator<Variable> {

	private IDirectedGraphVar graph;
	private IGraphDeltaMonitor gdm;
	private PairProcedure enforce;
	private PairProcedure remove;
	private int n;
	private BoolVar[] t;

	public PropIncrementalAdjacencyMatrix(IDirectedGraphVar graphVar, BoolVar[] t) {
		super(ArrayUtils.append(new Variable[]{graphVar}, t), PropagatorPriority.LINEAR, true);
		graph = graphVar;
		gdm = graph.monitorDelta(this);
		enforce = (PairProcedure) (from, to) -> {
			t[from + to * n].instantiateTo(1, this);
		};
		remove = (PairProcedure) (from, to) -> {
			t[from + to * n].instantiateTo(0, this);
		};
		n = graphVar.getNbMaxNodes();
		this.t = t;
	}

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		// first, nonincremental propagation
		propagateGraphChanged();
		propagateTChanged();
		// initializing incremental data-structures
		gdm.unfreeze();
	}

	private void propagateGraphChanged() throws ContradictionException {
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				if (t[i + j * n].isInstantiatedTo(1)) {
					graph.enforceArc(i, j, this);
				}
				if (t[i + j * n].isInstantiatedTo(0)) {
					graph.removeArc(i, j, this);
				}
			}
		}
	}

	private void propagateTChanged() throws ContradictionException {
		for (int u = 0; u < n; u++) {
			for (int v: graph.getMandSuccOf(u)) {
				t[u + v * n].instantiateTo(1, this);
			}
		}
		for (int u = 0; u < n; u++) {
			Set<Integer> set = new HashSet<>();
			for (int v: graph.getPotSuccOf(u)) {
				set.add(v);
			}
			for (int v = 0; v < n; v++) {
				if (!set.contains(v)) {
					t[u + v * n].instantiateTo(0, this);
				}
			}
		}
	}

	@Override
	public void propagate(int idxVarInProp, int mask) throws ContradictionException {
		// incremental propagation
		if (idxVarInProp == 0) {
			gdm.freeze();
			gdm.forEachArc(enforce, GraphEventType.ADD_ARC);
			gdm.forEachArc(remove, GraphEventType.REMOVE_ARC);
			gdm.unfreeze();
		} else {
			propagateTChanged();
		}
	}

	@Override
	public int getPropagationConditions(int vIdx) {
		if (vIdx == 0) {
			return GraphEventType.ADD_ARC.getMask() | GraphEventType.REMOVE_ARC.getMask();
		} else {
			return IntEventType.boundAndInst();
		}
	}

	@Override
	public ESat isEntailed() {
		for (int i = 0; i < n; i++) {
			ISet children = graph.getMandSuccOf(i);
			for (int j = 0; j < n; j++) {
				if (t[i + j * n].isInstantiatedTo(0)  && children.contains(j)) {
					return ESat.FALSE;
				}
			}
		}
		for (int i = 0; i < n; i++) {
			ISet children = graph.getPotSuccOf(i);
			for (int j = 0; j < n; j++) {
				if (t[i + j * n].isInstantiatedTo(1) && !children.contains(j)) {
					return ESat.FALSE;
				}
			}
		}
		if (graph.isInstantiated()) {
			return ESat.TRUE;
		}
		return ESat.UNDEFINED;
	}
}

