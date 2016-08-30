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

import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.graphsolver.cstrs.basic.PropNbNodes;
import org.chocosolver.graphsolver.search.strategy.GraphStrategy;
import org.chocosolver.graphsolver.search.strategy.arcs.RandomArc;
import org.chocosolver.graphsolver.search.strategy.nodes.RandomNode;
import org.chocosolver.graphsolver.variables.IDirectedGraphVar;
import org.chocosolver.samples.input.GraphGenerator;
import org.chocosolver.solver.Cause;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.graphs.DirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Random;

public class SubCircuitTest {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private static SetType gt;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	private Solver solve(boolean[][] adjacencyMatrix, long seed) {
		int n = adjacencyMatrix.length;
		// create model
		GraphModel model = new GraphModel();
		DirectedGraph GLB = new DirectedGraph(model, n, SetType.LINKED_LIST, false);
		DirectedGraph GUB = new DirectedGraph(model, n, gt, false);
		for (int i = 0; i < n; i++) {
			GUB.addNode(i);
			if(!adjacencyMatrix[i][i]){
				GLB.addNode(i);
			}
			for (int j = 0; j < n; j++) {
				if (adjacencyMatrix[i][j]) {
					GUB.addArc(i, j);
				}
			}
		}
		IDirectedGraphVar graph = model.digraphVar("G", GLB, GUB);
		IntVar circuitLength = model.intVar("length",2,n,false);
		model.circuit(graph).post();
		new Constraint("SubCircuitLength",new PropNbNodes(graph, circuitLength)).post();

		AbstractStrategy arcs = new GraphStrategy(graph,null,new RandomArc(graph,seed), GraphStrategy.NodeArcPriority.ARCS);
		AbstractStrategy nodes = new GraphStrategy(graph,new RandomNode(graph,seed), null, GraphStrategy.NodeArcPriority.NODES_THEN_ARCS);
		model.getSolver().setSearch(arcs,nodes);

		while (model.getSolver().solve());

		return model.getSolver();
	}

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
//					System.out.println("graph generated");
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
				long s = 1410879673269l;
				System.out.println("n:" + n + " d:" + d + " s:" + s);
				GraphGenerator gg = new GraphGenerator(n, s, GraphGenerator.InitialProperty.HamiltonianCircuit);
				matrix = gg.arcBasedGenerator(d);
				testModels(matrix, s);
				for (int ks = 0; ks < 10; ks++) {
					s = System.currentTimeMillis();
					System.out.println("n:" + n + " d:" + d + " s:" + s);
					gg = new GraphGenerator(n, s, GraphGenerator.InitialProperty.HamiltonianCircuit);
					matrix = gg.arcBasedGenerator(d);
					testModels(matrix, s);
				}
			}
		}
	}

	private static void testModels(boolean[][] m, long seed) {
		Random rd = new Random(seed);
		for(int i=0;i<m.length;i++){
			m[i][i] = rd.nextBoolean();
		}
		long nbSols = referencemodel(m,0);
		if (nbSols == -1) {
			throw new UnsupportedOperationException();
		}
		assert nbSols == referencemodel(m,12);
		System.out.println(nbSols + " sols expected");
		gt = SetType.BITSET;
		Solver resu = new SubCircuitTest().solve(m, seed);
		Assert.assertEquals(nbSols, resu.getSolutionCount(), "nb sol incorrect ");
		System.gc();
		System.gc();
		System.gc();
	}

	private static long referencemodel(boolean[][] matrix, int offset) {
		int n = matrix.length;
		GraphModel model = new GraphModel();
		IntVar[] vars = model.intVarArray("", n, offset, n - 1 + offset, false);
		IntVar length = model.intVar("length",2,n,true);
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
		model.subCircuit(vars, offset, length).post();
		while (model.getSolver().solve());
		return model.getSolver().getSolutionCount();
	}
}
