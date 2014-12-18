/**
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

import org.testng.Assert;
import org.testng.annotations.Test;
import org.chocosolver.samples.AbstractProblem;
import org.chocosolver.samples.input.GraphGenerator;
import org.chocosolver.solver.Cause;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.IntConstraintFactory;
import org.chocosolver.solver.cstrs.GraphConstraintFactory;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.GraphStrategyFactory;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.variables.GraphVarFactory;
import org.chocosolver.solver.variables.IDirectedGraphVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.VariableFactory;
import org.chocosolver.util.objects.graphs.DirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;

public class HamiltonianPathTest2 extends AbstractProblem {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private static SetType gt;

	private int n;
	private IDirectedGraphVar graph;
	private boolean[][] adjacencyMatrix;
	// model parameters
	private long seed;
	private boolean strongFilter;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	private void set(boolean[][] matrix, long s, boolean strong) {
		seed = s;
		n = matrix.length;
		adjacencyMatrix = matrix;
		strongFilter = strong;
	}

	//***********************************************************************************
	// MODEL
	//***********************************************************************************


	@Override
	public void createSolver() {
		level = Level.SILENT;
		solver = new Solver();
	}

	@Override
	public void buildModel() {
		// create model
		DirectedGraph GLB = new DirectedGraph(solver, n, SetType.LINKED_LIST, true);
		DirectedGraph GUB = new DirectedGraph(solver, n, SetType.LINKED_LIST, true);
		for (int i = 0; i < n - 1; i++) {
			for (int j = 1; j < n; j++) {
				if (adjacencyMatrix[i][j]) {
					GUB.addArc(i, j);
				}
			}
		}
		graph = GraphVarFactory.directed_graph_var("G", GLB, GUB, solver);
		solver.post(GraphConstraintFactory.path(graph, 0, n - 1));
		if(strongFilter){
			solver.post(GraphConstraintFactory.reachability(graph,0));
		}
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************


	@Override
	public void configureSearch() {
		AbstractStrategy strategy;
		strategy = GraphStrategyFactory.random(graph, seed);
		solver.set(strategy);
	}

	@Override
	public void solve() {
		solver.findAllSolutions();
	}

	@Override
	public void prettyOut() {}

	//***********************************************************************************
	// TESTS
	//***********************************************************************************


	@Test(groups = "1s")
	public static void test1() {
		int[] sizes = new int[]{15};
		int[] seeds = new int[]{42};
		double[] densities = new double[]{0.2};
		boolean[][] matrix;
		for (int n : sizes) {
			for (double d : densities) {
				for (int s : seeds) {
					System.out.println("n:" + n + " d:" + d + " s:" + s);
					GraphGenerator gg = new GraphGenerator(n, s, GraphGenerator.InitialProperty.HamiltonianCircuit);
					matrix = gg.arcBasedGenerator(d);
					testModels(matrix, s);
				}
			}
		}
	}

	@Test(groups = "1s")
	public static void test2() {
		int n = 8;
		boolean[][] matrix = new boolean[n][n];
		matrix[0][6] = true;
		matrix[1][0] = true;
		matrix[1][7] = true;
		matrix[2][3] = true;
		matrix[2][5] = true;
		matrix[2][7] = true;
		matrix[3][4] = true;
		matrix[3][5] = true;
		matrix[3][6] = true;
		matrix[4][3] = true;
		matrix[4][5] = true;
		matrix[4][6] = true;
		matrix[4][7] = true;
		matrix[5][0] = true;
		matrix[5][4] = true;
		matrix[6][1] = true;
		matrix[6][4] = true;
		matrix[7][0] = true;
		matrix[7][1] = true;
		matrix[7][2] = true;
		matrix[7][3] = true;
		matrix[7][5] = true;
		long nbSols = referencemodel(matrix,0);
		assert nbSols == referencemodel(matrix,12);
//		System.out.println(nbSols);
		testModels(matrix, 0);
	}

	@Test(groups = "1m")
	public static void test3() {
		int[] sizes = new int[]{6, 10};
		int[] seeds = new int[]{0, 10, 42};
		double[] densities = new double[]{0.2, 0.4};
		boolean[][] matrix;
		for (int n : sizes) {
			for (double d : densities) {
				for (int s : seeds) {
					System.out.println("n:" + n + " d:" + d + " s:" + s);
					GraphGenerator gg = new GraphGenerator(n, s, GraphGenerator.InitialProperty.HamiltonianCircuit);
					matrix = gg.arcBasedGenerator(d);
					testModels(matrix, s);
				}
			}
		}
	}

	@Test(groups = "1m")
	public static void test4() {
		int[] sizes = new int[]{8};
		double[] densities = new double[]{0.1, 0.2, 0.5, 0.7};
		boolean[][] matrix;
		for (int n : sizes) {
			for (double d : densities) {
				for (int ks = 0; ks < 10; ks++) {
					long s = System.currentTimeMillis();
					System.out.println("n:" + n + " d:" + d + " s:" + s);
					GraphGenerator gg = new GraphGenerator(n, s, GraphGenerator.InitialProperty.HamiltonianCircuit);
					matrix = gg.arcBasedGenerator(d);
					testModels(matrix, s);
				}
			}
		}
	}

	private static void testModels(boolean[][] m, long seed) {
		long nbSols = referencemodel(m,0);
		if (nbSols == -1) {
			throw new UnsupportedOperationException();
		}
		assert nbSols == referencemodel(m,12);
//		System.out.println(nbSols + " sols expected");
		boolean[][] matrix = transformMatrix(m);
		boolean[] vls = new boolean[]{false, true};
		gt = SetType.LINKED_LIST;
		for (int i = 0; i < 4; i++) {
			for (boolean p : vls) {
				HamiltonianPathTest2 hcp = new HamiltonianPathTest2();
				hcp.set(matrix, seed, p);
				hcp.execute();
				Assert.assertEquals(nbSols, hcp.solver.getMeasures().getSolutionCount(), "nb sol incorrect " + i + " ; " + p + " ; " + gt);
			}
		}
		System.gc();
		System.gc();
		System.gc();
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

	private static long referencemodel(boolean[][] matrix, int offset) {
		int n = matrix.length;
		Solver solver = new Solver();
		IntVar[] vars = VariableFactory.enumeratedArray("", n, offset, n - 1 + offset, solver);
		try {
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					if (!matrix[i][j]) {
						vars[i].removeValue(j+offset, Cause.Null);
					}
				}
			}
		} catch (ContradictionException e) {
			e.printStackTrace();
		}
		solver.post(IntConstraintFactory.circuit(vars, offset));
		long nbsol = solver.findAllSolutions();
		if (nbsol == 0) {
			return -1;
		}
		return solver.getMeasures().getSolutionCount();
	}
}
