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

package org.chocosolver.graphsolver.cstrs.cost.tsp;

import org.chocosolver.graphsolver.variables.GraphEventType;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.setDataStructures.ISet;

import java.util.Iterator;

/**
 * Compute the cost of the graph by summing edge costs
 * Supposes that each node must have two neighbors (cycle)
 * - For minimization problem
 */
public class PropCycleCostSimple extends Propagator<Variable> {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    protected UndirectedGraphVar g;
    protected int n;
    protected IntVar sum;
    protected int[][] distMatrix;
    protected int[] replacementCost;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    public PropCycleCostSimple(UndirectedGraphVar graph, IntVar obj, int[][] costMatrix) {
        super(new Variable[]{graph, obj}, PropagatorPriority.LINEAR, false);
        g = graph;
        sum = obj;
        n = g.getNbMaxNodes();
        distMatrix = costMatrix;
        replacementCost = new int[n];
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    @Override
    public int getPropagationConditions(int vIdx) {
        if(vIdx==0) {
            return GraphEventType.REMOVE_ARC.getMask() + GraphEventType.ADD_ARC.getMask();
        } else {
            return IntEventType.boundAndInst();
        }
    }

    @Override
    public ESat isEntailed() {
        int minSum = 0;
        int maxSum = 0;
        for (int i = 0; i < n; i++) {
            ISet env = g.getPotNeighOf(i);
            ISet ker = g.getMandNeighOf(i);
            for (int j : env) {
                if (i <= j) {
                    maxSum += distMatrix[i][j];
                    if (ker.contains(j)) {
                        minSum += distMatrix[i][j];
                    }
                }
            }
        }
        if (maxSum < 0) {
            maxSum = Integer.MAX_VALUE;
        }
        if (minSum > sum.getUB() || maxSum < sum.getLB()) {
            return ESat.FALSE;
        }
        if (maxSum == minSum && sum.isInstantiated()) {
            return ESat.TRUE;
        }
        return ESat.UNDEFINED;
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        int minSum = 0;
        int maxSum = 0;
        for (int i = 0; i < n; i++) {
            minSum += findTwoBest(i);
            maxSum += findTwoWorst(i);
        }
        if (maxSum % 2 != 0) {
            maxSum++;
        }
        if (minSum % 2 != 0) {
            minSum--;
        }
        minSum /= 2;
        maxSum /= 2;
        if (maxSum < 0) {
            maxSum = Integer.MAX_VALUE;
        }
        sum.updateLowerBound(minSum, this);
        sum.updateUpperBound(maxSum, this);
        filter(minSum);
    }

    protected void filter(int minSum) throws ContradictionException {
        ISet succs;
        int delta = sum.getUB() - minSum;
        for (int i = 0; i < n; i++) {
            succs = g.getPotNeighOf(i);
            for (int j : succs) {
                if (i < j && !g.getMandNeighOf(i).contains(j)) {
                    if (replacementCost[i] == -1 || replacementCost[j] == -1) {
                        g.removeArc(i, j, this);
                    }
                    if ((2 * distMatrix[i][j] - replacementCost[i] - replacementCost[j]) / 2 > delta) {
                        g.removeArc(i, j, this);
                    }
                }
            }
        }
    }

    protected int findTwoBest(int i) throws ContradictionException {
        if (g.getMandNeighOf(i).isEmpty()) {
            int mc1 = getBestNot(i, -2);
            int cost = distMatrix[i][getBestNot(i, mc1)];
            replacementCost[i] = cost;
            return distMatrix[i][mc1] + cost;
        }else{
            Iterator<Integer> it = g.getMandNeighOf(i).iterator();
            int mc1 = it.next();
            if (it.hasNext()) {
                int mc2 = it.next();
                replacementCost[i] = -1;
                return distMatrix[i][mc1] + distMatrix[i][mc2];
            }
            int cost = distMatrix[i][getBestNot(i, mc1)];
            replacementCost[i] = cost;
            return distMatrix[i][mc1] + cost;

        }
    }

    protected int getBestNot(int i, int not) throws ContradictionException {
        ISet nei = g.getPotNeighOf(i);
        int cost = -1;
        int idx = -1;
        for (int j : nei) {
            if (j != not && (idx == -1 || cost > distMatrix[i][j])) {
                idx = j;
                cost = distMatrix[i][j];
            }
        }
        if (idx == -1) {
            fails();
        }
        return idx;
    }

    protected int findTwoWorst(int i) throws ContradictionException {
        if (g.getMandNeighOf(i).isEmpty()) {
            int mc1 = getWorstNot(i, -2);
            return distMatrix[i][mc1] + distMatrix[i][getWorstNot(i, mc1)];
        }else{
            Iterator<Integer> it = g.getMandNeighOf(i).iterator();
            int mc1 = it.next();
            if (it.hasNext()) {
                return distMatrix[i][mc1] + distMatrix[i][it.next()];
            }
            return distMatrix[i][mc1] + distMatrix[i][getWorstNot(i, mc1)];
        }
    }

    protected int getWorstNot(int i, int not) throws ContradictionException {
        ISet nei = g.getPotNeighOf(i);
        int cost = -1;
        int idx = -1;
        for (int j : nei) {
            if (j != not && (idx == -1 || cost < distMatrix[i][j])) {
                idx = j;
                cost = distMatrix[i][j];
            }
        }
        if (idx == -1) {
            fails();
        }
        return idx;
    }

}
