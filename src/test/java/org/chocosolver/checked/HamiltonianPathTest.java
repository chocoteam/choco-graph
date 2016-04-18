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

package org.chocosolver.checked;

import gnu.trove.list.array.TIntArrayList;
import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.solver.search.strategy.SearchStrategyFactory;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.chocosolver.samples.input.GraphGenerator;
import org.chocosolver.graphsolver.search.GraphStrategyFactory;

import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.graphsolver.variables.IDirectedGraphVar;
import org.chocosolver.util.objects.graphs.DirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;

/**
 * @author Jean-Guillaume Fages
 */
public class HamiltonianPathTest {

	private final static long TIME_LIMIT = 3000;

	@Test(groups = "1m")
	public static void test() {
		int[] sizes = new int[]{20, 50};
		long s;
		int[] nbVoisins = new int[]{3, 5, 10};
		boolean[][] matrix;
		long time = System.currentTimeMillis();
		for (int n : sizes) {
			for (int nb : nbVoisins) {
				for (int ks = 0; ks < 2; ks++) {
					s = System.currentTimeMillis();
					System.out.println("n:" + n + " nbVoisins:" + nb + " s:" + s);
					GraphGenerator gg = new GraphGenerator(n, s, GraphGenerator.InitialProperty.HamiltonianCircuit);
					matrix = transformMatrix(gg.neighborBasedGenerator(nb));
					testProblem(matrix, s, true, false);
					testProblem(matrix, s, false, false);
					testProblem(matrix, s, true, true);
					testProblem(matrix, s, false, true);
					testInt(matrix, s, true, false);
					testInt(matrix, s, false, false);
					testInt(matrix, s, true, true);
					testInt(matrix, s, false, true);
				}
			}
		}
		System.out.println("it took "+(System.currentTimeMillis()-time)+" ms");
	}

	private static void testProblem(boolean[][] matrix, long s, boolean rd, boolean strongFilter) {
		GraphModel model = new GraphModel();
		int n = matrix.length;
		// build model
		DirectedGraph GLB = new DirectedGraph(n,SetType.LINKED_LIST,true);
		DirectedGraph GUB = new DirectedGraph(n,SetType.BITSET,true);
		for (int i = 0; i < n - 1; i++) {
			for (int j = 1; j < n; j++) {
				if (matrix[i][j]) {
					GUB.addArc(i, j);
				}
			}
		}
		IDirectedGraphVar graph = model.digraphVar("G", GLB, GUB);
		model.path(graph, 0, n - 1).post();
		if(strongFilter){
			// could add alldiff as well
			model.reachability(graph,0).post();
		}

		// configure solver
		if (rd) {
			model.getSolver().set(GraphStrategyFactory.random(graph, s));
		} else {
			model.getSolver().set(GraphStrategyFactory.inputOrder(graph));
		}
		model.getSolver().limitTime(TIME_LIMIT);
		model.solve();

		// the problem has at least one solution
		Assert.assertFalse(model.getSolver().getSolutionCount() == 0 && model.getSolver().getTimeCount() < TIME_LIMIT/1000);
	}

	private static void testInt(boolean[][] matrix, long seed, boolean rd, boolean enumerated) {
		GraphModel model = new GraphModel();
		int n = matrix.length;
		// build model
		IntVar[] succ = new IntVar[n];
		int offset = -5;
		TIntArrayList l = new TIntArrayList();
		for (int i = 0; i < n-1; i++) {
			l.clear();
			for (int j = 0; j < n; j++) {
				if(matrix[i][j]){
					l.add(j+offset);
				}
			}
			if(l.isEmpty())throw new UnsupportedOperationException();
			if(enumerated){
				succ[i] = model.intVar("suc",l.toArray());
			}else{
				succ[i] = model.intVar("suc",offset,n+offset,true);
				model.member(succ[i],l.toArray()).post();
			}
		}
		succ[n-1] = model.intVar(n+offset);
		model.path(succ,model.intVar(offset),model.intVar(n-1+offset),offset).post();
		// configure solver
		if (rd) {
			model.getSolver().set(SearchStrategyFactory.randomSearch(succ,seed));
		} else {
			model.getSolver().set(SearchStrategyFactory.inputOrderLBSearch(succ));
		}
		model.getSolver().limitTime(TIME_LIMIT);
		model.solve();
		// the problem has at least one solution
		Assert.assertFalse(model.getSolver().getSolutionCount() == 0 && model.getSolver().getTimeCount() < TIME_LIMIT/1000);
	}

	private static boolean[][] transformMatrix(boolean[][] m) {
		int n = m.length + 1;
		boolean[][] matrix = new boolean[n][n];
		for (int i = 0; i < n - 1; i++) {
			System.arraycopy(m[i], 1, matrix[i], 1, n - 1 - 1);
			matrix[i][n - 1] = m[i][0];
		}
		return matrix;
	}
//
//	// constructive heuristic, can be useful to debug
//	private static class ConstructorHeur extends ArcStrategy<IDirectedGraphVar> {
//		int source, n;
//
//		public ConstructorHeur(IDirectedGraphVar graphVar, int s) {
//			super(graphVar);
//			source = s;
//			n = graphVar.getNbMaxNodes();
//		}
//
//		@Override
//		public boolean computeNextArc() {
//			int x = source;
//			int y = g.getMandSuccOf(x).getFirstElement();
//			int nb = 1;
//			while (y != -1) {
//				x = y;
//				y = g.getMandSuccOf(x).getFirstElement();
//				nb++;
//			}
//			y = g.getPotSuccOf(x).getFirstElement();
//			if (y == -1) {
//				if (x != n - 1 || nb != n) {
//					for (int i = 0; i < n; i++) {
//						if (g.getPotSuccOf(i).getSize() > 1) {
//							this.from = i;
//							this.to = g.getPotSuccOf(i).getFirstElement();
//							return true;
//						}
//					}
//					throw new UnsupportedOperationException();
//				}
//				return false;
//			}
//			this.from = x;
//			this.to = y;
//			return true;
//		}
//	}
//
//	private static class ConstructorIntHeur extends AbstractStrategy<IntVar> {
//		int n, offset;
//		PoolManager<IntDecision> pool;
//
//		public ConstructorIntHeur(IntVar[] v, int off) {
//			super(v);
//			offset = off;
//			n = v.length;
//			pool = new PoolManager<>();
//		}
//
//		@Override
//		public boolean init() {
//			return true;
//		}
//
//		@Override
//		public Decision<IntVar> getDecision() {
//			int x = 0;
//			while (vars[x].isInstantiated()) {
//				x = vars[x].getValue()-offset;
//				if(x==vars.length){
//					return null;
//				}
//			}
//			IntDecision d = pool.getE();
//			if(d==null)d=new IntDecision(pool);
//			d.set(vars[x], vars[x].getLB(), DecisionOperator.int_eq);
//			return d;
//		}
//	}
}
