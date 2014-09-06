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

package solver.variables;

import solver.ICause;
import solver.Solver;
import solver.exception.ContradictionException;
import solver.explanations.Explanation;
import solver.explanations.VariableState;
import solver.variables.delta.GraphDelta;
import solver.variables.delta.IGraphDelta;
import solver.variables.delta.IGraphDeltaMonitor;
import solver.variables.delta.GraphDeltaMonitor;
import solver.variables.impl.AbstractVariable;
import util.objects.graphs.IGraph;
import util.objects.setDataStructures.ISet;

public abstract class GraphVar<E extends IGraph> extends AbstractVariable implements IGraphVar<E>{

    //////////////////////////////// GRAPH PART /////////////////////////////////////////
    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    protected E UB, LB;
    protected IGraphDelta delta;
	protected int n;
    ///////////// Attributes related to Variable ////////////
    protected boolean reactOnModification;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    /**
     * Creates a graph variable
     *
     * @param solver
     */
    public GraphVar(String name, Solver solver, E LB, E UB) {
        super(name, solver);
		this.LB = LB;
		this.UB = UB;
		this.n = UB.getNbNodes();
		assert n == LB.getNbNodes();
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    @Override
    public boolean isInstantiated() {
        if (getPotentialNodes().getSize() != getMandatoryNodes().getSize()) {
            return false;
        }
        ISet suc;
        ISet act = getUB().getActiveNodes();
        for (int i = act.getFirstElement(); i >= 0; i = act.getNextElement()) {
            suc = UB.getSuccsOrNeigh(i);
            if (suc.getSize() != getLB().getSuccsOrNeigh(i).getSize()) {
                return false;
            }
        }
        return true;
    }

	@Override
    public boolean removeNode(int x, ICause cause) throws ContradictionException {
        assert cause != null;
		assert (x>=0 && x<n);
        if (LB.getActiveNodes().contain(x)) {
            this.contradiction(cause, EventType.REMOVENODE, "remove mandatory node");
            return true;
        } else if (!UB.getActiveNodes().contain(x)) {
            return false;
        }
        ISet nei = UB.getSuccsOrNeigh(x);
        for (int i = nei.getFirstElement(); i >= 0; i = nei.getNextElement()) {
            removeArc(x, i, cause);
        }
        nei = UB.getPredsOrNeigh(x);
        for (int i = nei.getFirstElement(); i >= 0; i = nei.getNextElement()) {
            removeArc(i, x, cause);
        }
        if (UB.desactivateNode(x)) {
            if (reactOnModification) {
                delta.add(x, IGraphDelta.NR, cause);
            }
            EventType e = EventType.REMOVENODE;
            notifyPropagators(e, cause);
            return true;
        }
        return false;
    }

	@Override
    public boolean enforceNode(int x, ICause cause) throws ContradictionException {
        assert cause != null;
		assert (x>=0 && x<n);
        if (UB.getActiveNodes().contain(x)) {
            if (LB.activateNode(x)) {
                if (reactOnModification) {
                    delta.add(x, IGraphDelta.NE, cause);
                }
                EventType e = EventType.ENFORCENODE;
                notifyPropagators(e, cause);
                return true;
            }
            return false;
        }
        this.contradiction(cause, EventType.ENFORCENODE, "enforce node which is not in the domain");
        return true;
    }

	@Override
    public abstract boolean removeArc(int x, int y, ICause cause) throws ContradictionException;

	@Override
    public abstract boolean enforceArc(int x, int y, ICause cause) throws ContradictionException;

    //***********************************************************************************
    // ACCESSORS
    //***********************************************************************************

	@Override
    public E getLB() {
        return LB;
    }

	@Override
    public E getUB() {
        return UB;
    }

	@Override
	public ISet getMandSuccOrNeighOf(int idx){
		return LB.getSuccsOrNeigh(idx);
	}

	@Override
	public ISet getPotSuccOrNeighOf(int idx){
		return UB.getSuccsOrNeigh(idx);
	}

	@Override
	public ISet getMandPredOrNeighOf(int idx){
		return LB.getPredsOrNeigh(idx);
	}

	@Override
	public ISet getPotPredOrNeighOf(int idx){
		return UB.getPredsOrNeigh(idx);
	}

	@Override
	public int getNbMaxNodes() {
		return n;
	}

	@Override
	public ISet getMandatoryNodes() {
		return LB.getActiveNodes();
	}

	@Override
	public ISet getPotentialNodes() {
		return UB.getActiveNodes();
	}

	@Override
    public abstract boolean isDirected();

    //***********************************************************************************
    // VARIABLE STUFF
    //***********************************************************************************

    @Override
    public void explain(VariableState what, Explanation to) {
        throw new UnsupportedOperationException("GraphVar does not (yet) implement method explain(...)");
    }

    @Override
    public void explain(VariableState what, int val, Explanation to) {
        throw new UnsupportedOperationException("GraphVar does not (yet) implement method explain(...)");
    }

    @Override
    public IGraphDelta getDelta() {
        return delta;
    }

    @Override
    public int getTypeAndKind() {
        return VAR | GRAPH;
    }

    @Override
    public GraphVar duplicate() {
        throw new UnsupportedOperationException("Cannot duplicate GraphVar");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("graph_var " + getName());
		if(isInstantiated()){
			sb.append("\nValue: \n");
			sb.append(UB.toString());
		}else{
			sb.append("\nUpper bound: \n");
			sb.append(UB.toString());
			sb.append("\nLower bound: \n");
			sb.append(LB.toString());
		}
        return sb.toString();
    }

    @Override
    public void createDelta() {
        if (!reactOnModification) {
            reactOnModification = true;
            delta = new GraphDelta(solver.getSearchLoop());
        }
    }

	@Override
    public IGraphDeltaMonitor monitorDelta(ICause propagator) {
        createDelta();
        return new GraphDeltaMonitor(delta, propagator);
    }

	@Override
    public void notifyMonitors(EventType event) throws ContradictionException {
        for (int i = mIdx - 1; i >= 0; i--) {
            monitors[i].onUpdate(this, event);
        }
    }

    @Override
    public void contradiction(ICause cause, EventType event, String message) throws ContradictionException {
        assert cause != null;
        solver.getEngine().fails(cause, this, message);
    }

    //***********************************************************************************
    // SOLUTIONS : STORE AND RESTORE
    //***********************************************************************************

	@Override
    public boolean[][] getValue() {
        int n = getUB().getNbNodes();
        boolean[][] vals = new boolean[n + 1][n];
        ISet kerNodes = getLB().getActiveNodes();
        ISet kerSuccs;
        for (int i = kerNodes.getFirstElement(); i >= 0; i = kerNodes.getNextElement()) {
            kerSuccs = getLB().getSuccsOrNeigh(i);
            for (int j = kerSuccs.getFirstElement(); j >= 0; j = kerSuccs.getNextElement()) {
                vals[i][j] = true; // arc in
            }
            vals[n][i] = true; // node in
        }
        return vals;
    }

	@Override
    public void instantiateTo(boolean[][] value, ICause cause) throws ContradictionException {
        int n = value.length - 1;
        for (int i = 0; i < n; i++) {
            if (value[n][i]) {//nodes
                enforceNode(i, cause);
            } else {
                removeNode(i, cause);
            }
            for (int j = 0; j < n; j++) {
                if (value[i][j]) {//arcs
                    enforceArc(i, j, cause);
                } else {
                    removeArc(i, j, cause);
                }
            }
        }
    }
}
