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

package org.chocosolver.graphsolver.variables;

import org.chocosolver.graphsolver.variables.delta.GraphDelta;
import org.chocosolver.graphsolver.variables.delta.GraphDeltaMonitor;
import org.chocosolver.graphsolver.variables.delta.IGraphDelta;
import org.chocosolver.graphsolver.variables.delta.IGraphDeltaMonitor;
import org.chocosolver.solver.ICause;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.events.IEventType;
import org.chocosolver.solver.variables.impl.AbstractVariable;
import org.chocosolver.util.objects.graphs.IGraph;
import org.chocosolver.util.objects.setDataStructures.ISet;

import java.lang.reflect.Field;

public abstract class GraphVar<E extends IGraph> extends AbstractVariable implements IGraphVar<E> {

    public static final int GRAPH = 1 << 7; // beware, this relies on choco-solver

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
    public GraphVar(String name, Model solver, E LB, E UB) {
        super(name, solver);
        this.LB = LB;
        this.UB = UB;
        this.n = UB.getNbMaxNodes();
        assert n == LB.getNbMaxNodes();
        Field f = null; //NoSuchFieldException
        try {
            AbstractVariable me = this;
            f = me.getClass().getSuperclass().getSuperclass().getDeclaredField("scheduler");
            f.setAccessible(true);
            f.set(me, new GraphEvtScheduler());
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    @Override
    public boolean isInstantiated() {
        if (getPotentialNodes().size() != getMandatoryNodes().size()) {
            return false;
        }
        ISet suc;
        for (int i :getUB().getNodes()) {
            suc = UB.getSuccOrNeighOf(i);
            if (suc.size() != getLB().getSuccOrNeighOf(i).size()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean removeNode(int x, ICause cause) throws ContradictionException {
        assert cause != null;
        assert (x >= 0 && x < n);
        if (LB.getNodes().contains(x)) {
            this.contradiction(cause, "remove mandatory node");
            return true;
        } else if (!UB.getNodes().contains(x)) {
            return false;
        }
        ISet nei = UB.getSuccOrNeighOf(x);
        for (int i : nei) {
            removeArc(x, i, cause);
        }
        nei = UB.getPredOrNeighOf(x);
        for (int i : nei) {
            removeArc(i, x, cause);
        }
        if (UB.removeNode(x)) {
            if (reactOnModification) {
                delta.add(x, IGraphDelta.NR, cause);
            }
            GraphEventType e = GraphEventType.REMOVE_NODE;
            notifyPropagators(e, cause);
            return true;
        }
        return false;
    }

    @Override
    public boolean enforceNode(int x, ICause cause) throws ContradictionException {
        assert cause != null;
        assert (x >= 0 && x < n);
        if (UB.getNodes().contains(x)) {
            if (LB.addNode(x)) {
                if (reactOnModification) {
                    delta.add(x, IGraphDelta.NE, cause);
                }
                GraphEventType e = GraphEventType.ADD_NODE;
                notifyPropagators(e, cause);
                return true;
            }
            return false;
        }
        this.contradiction(cause,"enforce node which is not in the domain");
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
    public ISet getMandSuccOrNeighOf(int idx) {
        return LB.getSuccOrNeighOf(idx);
    }

    @Override
    public ISet getPotSuccOrNeighOf(int idx) {
        return UB.getSuccOrNeighOf(idx);
    }

    @Override
    public ISet getMandPredOrNeighOf(int idx) {
        return LB.getPredOrNeighOf(idx);
    }

    @Override
    public ISet getPotPredOrNeighOf(int idx) {
        return UB.getPredOrNeighOf(idx);
    }

    @Override
    public int getNbMaxNodes() {
        return n;
    }

    @Override
    public ISet getMandatoryNodes() {
        return LB.getNodes();
    }

    @Override
    public ISet getPotentialNodes() {
        return UB.getNodes();
    }

    @Override
    public abstract boolean isDirected();

    //***********************************************************************************
    // VARIABLE STUFF
    //***********************************************************************************


    @Override
    public IGraphDelta getDelta() {
        return delta;
    }

    @Override
    public int getTypeAndKind() {
        return VAR | GRAPH;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("graph_var ").append(getName());
        if (isInstantiated()) {
            sb.append("\nValue: \n");
            sb.append(UB.toString());
        } else {
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
            delta = new GraphDelta(getEnvironment());
        }
    }

    @Override
    public IGraphDeltaMonitor monitorDelta(ICause propagator) {
        createDelta();
        return new GraphDeltaMonitor(delta, propagator);
    }

    @Override
    public void notifyMonitors(IEventType event) throws ContradictionException {
        for (int i = mIdx - 1; i >= 0; i--) {
            monitors[i].onUpdate(this, event);
        }
    }

    @Override
    public void contradiction(ICause cause, String message) throws ContradictionException {
        assert cause != null;
        model.getSolver().getEngine().fails(cause, this, message);
    }

    //***********************************************************************************
    // SOLUTIONS : STORE AND RESTORE
    //***********************************************************************************

    @Override
    public boolean[][] getValue() {
        int n = getUB().getNbMaxNodes();
        boolean[][] vals = new boolean[n + 1][n];
        for (int i : getLB().getNodes()) {
            for (int j : getLB().getSuccOrNeighOf(i)) {
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
