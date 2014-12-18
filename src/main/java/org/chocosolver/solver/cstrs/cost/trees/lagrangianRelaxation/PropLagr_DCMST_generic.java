package org.chocosolver.solver.cstrs.cost.trees.lagrangianRelaxation;
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

import gnu.trove.list.array.TIntArrayList;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.cstrs.cost.GraphLagrangianRelaxation;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IUndirectedGraphVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetType;

/**
 * Lagrangian relaxation of the DCMST problem
 */
public class PropLagr_DCMST_generic extends Propagator implements GraphLagrangianRelaxation {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	protected IUndirectedGraphVar gV;
	protected UndirectedGraph g;
	protected IntVar obj;
	protected int n;
	protected int[][] originalCosts;
	protected double[][] costs;
	protected UndirectedGraph mst;
	protected TIntArrayList mandatoryArcsList;
	protected AbstractTreeFinder HKfilter, HK;
	protected long nbRem;
	protected boolean waitFirstSol;
	protected int nbSprints;
	protected IntVar[] D;
	protected int[] Dmax;
	protected int[] Dmin;
	protected double[] lambdaMin, lambdaMax;
	protected double C;
	protected double K;
	protected boolean firstPropag = true;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	/**
	 * Propagator performing the Lagrangian relaxation of the Degree Constrained Minimum Spanning Tree Problem
	 */
	public PropLagr_DCMST_generic(IUndirectedGraphVar graph, IntVar cost, IntVar[] degrees, int[][] costMatrix, boolean waitFirstSol) {
		super(new Variable[]{graph, cost}, PropagatorPriority.CUBIC, false);
		gV = graph;
		n = gV.getNbMaxNodes();
		obj = cost;
		originalCosts = costMatrix;
		costs = new double[n][n];
		lambdaMin = new double[n];
		lambdaMax = new double[n];
		mandatoryArcsList = new TIntArrayList();
		nbRem = 0;
		nbSprints = 30;
		this.D = degrees;
		this.Dmin = new int[n];
		this.Dmax = new int[n];
		HK = new PrimMSTFinder(n, this);
		HKfilter = new KruskalMST_GAC(n, this);
		this.waitFirstSol = waitFirstSol;
		g = new UndirectedGraph(n, SetType.BITSET, true);
		for (int i = 0; i < n; i++) {
			for (int j = i+1; j < n; j++) {
				g.addEdge(i, j);
			}
		}
	}

	//***********************************************************************************
	// HK Algorithm(s)
	//***********************************************************************************

	protected long nbSols = 0;
	protected int objUB = -1;

	protected void lagrangianRelaxation() throws ContradictionException {
		int lb = obj.getLB();
		nbSprints = 30;
		if (nbSols != solver.getMeasures().getSolutionCount()
				|| obj.getUB() < objUB
				|| (firstPropag && !waitFirstSol)) {
			nbSols = solver.getMeasures().getSolutionCount();
			objUB = obj.getUB();
			convergeAndFilter();
			firstPropag = false;
			g = gV.getUB();
		} else {
			fastRun(2);
		}
		if (lb < obj.getLB()) {
			lagrangianRelaxation();
		}
	}

	protected void fastRun(double coef) throws ContradictionException {
		convergeFast(coef);
		HKfilter.computeMST(costs, g);
		double hkb = HKfilter.getBound() - C;
		mst = HKfilter.getMST();
		if (hkb - Math.floor(hkb) < 0.001) {
			hkb = Math.floor(hkb);
		}
		obj.updateLowerBound((int) Math.ceil(hkb), aCause);
		HKfilter.performPruning((double) (obj.getUB()) + C + 0.001);
	}

	protected void convergeAndFilter() throws ContradictionException {
		double hkb;
		double alpha = 2;
		double beta = 0.5;
		double besthkb = -9999998;
		double oldhkb = -9999999;
		while (oldhkb + 0.001 < besthkb || alpha > 0.01) {
			oldhkb = besthkb;
			convergeFast(alpha);
			HKfilter.computeMST(costs, g);
			hkb = HKfilter.getBound() - C;
			if (hkb > besthkb) {
				besthkb = hkb;
			}
			mst = HKfilter.getMST();
			if (hkb - Math.floor(hkb) < 0.00001) {
				hkb = Math.floor(hkb);
			}
			obj.updateLowerBound((int) Math.ceil(hkb), aCause);
			HKfilter.performPruning((double) (obj.getUB()) + C + 0.001);
			alpha *= beta;
		}
	}

	protected void convergeFast(double alpha) throws ContradictionException {
		double besthkb = 0;
		double oldhkb = -20;
		while (oldhkb + 0.1 < besthkb) {
			oldhkb = besthkb;
			for (int i = 0; i < nbSprints; i++) {
				HK.computeMST(costs, g);
				mst = HK.getMST();
				double hkb = HK.getBound() - C;
				if (hkb - Math.floor(hkb) < 0.001) {
					hkb = Math.floor(hkb);
				}
				if (hkb > besthkb) {
					besthkb = hkb;
				}
				obj.updateLowerBound((int) Math.ceil(hkb), aCause);
				if (updateStep(hkb, alpha)) return;
			}
		}
	}

	protected boolean updateStep(double hkb, double alpha) throws ContradictionException {
		double nb2viol = 0;
		double target = obj.getUB();
		assert (target - hkb >= 0);
		if (target - hkb < 0.001) {
			target = hkb + 0.001;
		}
		int deg;
		for (int i = 0; i < n; i++) {
			deg = mst.getNeighOf(i).getSize();
			if (deg > Dmax[i] || lambdaMax[i] != 0) {
				nb2viol += (Dmax[i] - deg) * (Dmax[i] - deg);
			}
			if (deg < Dmin[i] || lambdaMin[i] != 0) {
				nb2viol += (Dmin[i] - deg) * (Dmin[i] - deg);
			}
		}
		if (nb2viol == 0) {
			return true;
		} else {
			K = alpha * (target - hkb) / nb2viol;
		}
		if (K < 0.0001) {
			return true;
		}
		double maxPen = 2 * obj.getUB();
		for (int i = 0; i < n; i++) {
			deg = mst.getNeighOf(i).getSize();
			lambdaMin[i] += (deg - Dmin[i]) * K;
			lambdaMax[i] += (deg - Dmax[i]) * K;
			if(lambdaMin[i]>0){
				lambdaMin[i] = 0;
			}
			lambdaMin[i] = 0;
			if(lambdaMax[i]<0){
				lambdaMax[i] = 0;
			}
			if (gV.getPotNeighOf(i).getSize() <= Dmax[i]) {
				lambdaMax[i] = 0;
			}
			if (gV.getMandNeighOf(i).getSize() >= Dmin[i] || Dmin[i]<=1) {
				lambdaMin[i] = 0;
			}
			if (lambdaMin[i] < -maxPen) {
				lambdaMin[i] = -maxPen;
			}
			if (lambdaMax[i] > maxPen) {
				lambdaMax[i] = maxPen;
			}
			assert !(lambdaMax[i] > Double.MAX_VALUE / (n - 1) || lambdaMax[i] < 0);
			assert !(lambdaMin[i] < -Double.MAX_VALUE / (n - 1) || lambdaMin[i] > 0);
		}
		updateCosts();
		return false;
	}

	protected void updateCosts() {
		C = 0;
		for (int i = 0; i < n; i++) {
			C += Dmax[i] * lambdaMax[i];
			C += Dmin[i] * lambdaMin[i];
			ISet nei = g.getNeighOf(i);
			for (int j = nei.getFirstElement(); j >= 0; j = nei.getNextElement()) {
				if (i < j) {
					costs[j][i] = costs[i][j] = originalCosts[i][j] + lambdaMin[i] + lambdaMin[j] + lambdaMax[i] + lambdaMax[j];
					assert costs[j][i] >= 0;
				}
			}
		}
		assert C > -Double.MAX_VALUE / (n - 1) && C < Double.MAX_VALUE / (n - 1);
	}

	//***********************************************************************************
	// INFERENCE
	//***********************************************************************************

	@Override
	public void remove(int from, int to) throws ContradictionException {
		gV.removeArc(from, to, aCause);
		if (firstPropag) {
			g.removeEdge(from, to);
		}
		nbRem++;
	}

	@Override
	public void enforce(int from, int to) throws ContradictionException {
		gV.enforceArc(from, to, aCause);
	}

	@Override
	public void contradiction() throws ContradictionException {
		contradiction(gV, "mst failure");
	}

	//***********************************************************************************
	// PROP METHODS
	//***********************************************************************************

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		if (waitFirstSol && solver.getMeasures().getSolutionCount() == 0) {
			return;//the UB does not allow to prune
		}
		// initialisation
		mandatoryArcsList.clear();
		for (int i = 0; i < n; i++) {
			Dmin[i] = D[i].getLB();
			Dmax[i] = D[i].getUB();
		}
		for (int i = 0; i < n; i++) {
			ISet nei = gV.getMandNeighOf(i);
			for (int j = nei.getFirstElement(); j >= 0; j = nei.getNextElement()) {
				if (i < j) {
					mandatoryArcsList.add(i * n + j);
				}
			}
		}
		updateCosts();
		lagrangianRelaxation();
	}

	@Override
	public ESat isEntailed() {
		return ESat.TRUE;
	}

	@Override
	public double getMinArcVal() {
		return Integer.MIN_VALUE/10;
	}

	@Override
	public TIntArrayList getMandatoryArcsList() {
		return mandatoryArcsList;
	}

	@Override
	public boolean isMandatory(int i, int j) {
		return gV.getMandNeighOf(i).contain(j);
	}

	@Override
	public void waitFirstSolution(boolean b) {
		waitFirstSol = b;
	}

	@Override
	public boolean contains(int i, int j) {
		return mst == null || mst.edgeExists(i, j);
	}

	@Override
	public UndirectedGraph getSupport() {
		return mst;
	}

	@Override
	public double getReplacementCost(int from, int to) {
		return HKfilter.getRepCost(from, to);
	}

	@Override
	public double getMarginalCost(int from, int to) {
		return HKfilter.getRepCost(from, to);
	}
}
