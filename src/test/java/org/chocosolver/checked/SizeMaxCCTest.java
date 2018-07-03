package org.chocosolver.checked;

import org.chocosolver.GraphGenerator;
import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.graphsolver.util.ConnectivityFinder;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.loop.monitors.IMonitorSolution;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/**
 * Test class for the SizeMaxCC constraint.
 */
public class SizeMaxCCTest {

	/**
	 * Abstract test model, for factorization.
	 */
	private class AbstractTestModel {

		// Variables
		public int N;
		public IntVar sizeMaxCC;
		public UndirectedGraph GLB, GUB;
		public GraphModel model;
		public UndirectedGraphVar g;

		// Constructor
		public AbstractTestModel(int N, int minNCC_LB, int minNCC_UB,
								 int[] GLB_Nodes, int[] GUB_Nodes,
								 int[][] GLB_Edges, int[][] GUB_Edges) {
			this.model = new GraphModel();
			this.sizeMaxCC = this.model.intVar(minNCC_LB, minNCC_UB);
			// Init GLB (graph kernel)
			this.GLB = GraphGenerator.makeUndirectedGraph(
					model,
					N,
					SetType.BIPARTITESET,
					GLB_Nodes,
					GLB_Edges
			);
			// Init GUB (graph envelope)
			this.GUB = GraphGenerator.makeUndirectedGraph(
					model,
					N,
					SetType.BIPARTITESET,
					GUB_Nodes,
					GUB_Edges
			);
			// Create the graph variable
			this.g = model.graphVar("g", GLB, GUB);
			// Post the constraint
			model.sizeMaxConnectedComponents(g, sizeMaxCC).post();
		}

		public AbstractTestModel(GraphModel model, int N, int maxNCC_LB, int maxNCC_UB, UndirectedGraph GLB,
								 UndirectedGraph GUB) {
			this.N = N;
			this.model = model;
			this.sizeMaxCC = this.model.intVar(maxNCC_LB, maxNCC_UB);
			// Create the graph variable
			this.g = model.graphVar("g",GLB, GUB);
			// Post the constraint
			model.sizeMaxConnectedComponents(g, sizeMaxCC).post();
		}
	}


	/* --------------- */
	/* Fail test cases */
	/* --------------- */

	/**
	 * Fail test case 1: The maximum size of the CC cannot be satisfied.
	 * graph GLB {
	 *     { 0; 1; }
	 *     0 -- 1;  # CC of size 2.
	 * }
	 * graph GUB {
	 *     { 0; 1; 2; }
	 *     0 -- 1;
	 *     1 -- 2;
	 * }
	 */
	@Test
	public void testFailCase1() {
		int N = 3;
		int maxNCC_LB = 0;
		int maxNCC_UB = 1;
		int[] GlbNodes = new int[] {0, 1};
		int[][] GlbEdges = new int[][] { {0, 1} };
		int[] GubNodes = new int[] {0, 1, 2};
		int[][] GubEdges = new int[][] { {0, 1}, {1, 2} };
		AbstractTestModel test = new AbstractTestModel(N, maxNCC_LB, maxNCC_UB, GlbNodes, GubNodes, GlbEdges, GubEdges);
		Assert.assertFalse(test.model.getSolver().solve());
	}

	/* ------------------ */
	/* Success test cases */
	/* ------------------ */

	/**
	 * Success test case 1: The empty graph is the only solution.
	 * graph GLB {}
	 * graph GUB {}
	 */
	@Test
	public void testSuccessCase1() {
		int N = 5;
		int maxNCC_LB = 0;
		int maxNCC_UB = 20;
		int[] GlbNodes = new int[] {};
		int[][] GlbEdges = new int[][] {};
		int[] GubNodes = new int[] {};
		int[][] GubEdges = new int[][] {};
		AbstractTestModel test = new AbstractTestModel(N, maxNCC_LB, maxNCC_UB, GlbNodes, GubNodes, GlbEdges, GubEdges);
		boolean solutionFound = test.model.getSolver().solve();
		Assert.assertTrue(solutionFound);
		Assert.assertEquals(test.g.getPotentialNodes().size(), 0);
		Assert.assertEquals(test.sizeMaxCC.getValue(), 0);
		Assert.assertFalse(test.model.getSolver().solve()); // Assert that there is no other solution
	}

	/**
	 * Success test case 2. There are several solutions.
	 * graph GLB {
	 *     { 0; 1; 2;}
	 *     1 -- 2;
	 * }
	 * graph GUB {
	 *     { 0; 1; 2; 3; 4; 5; 6; 7; 8; 9; 10 }
	 *     # CC 1
	 *     2 -- 1;
	 *     1 -- 0;
	 *     0 -- 5;
	 *     5 -- 4;
	 *     # CC 2
	 *     6 -- 7;
	 *     7 -- 10;
	 *     # CC 3
	 *     3 -- 8;
	 *     8 -- 9;
	 * }
	 */
	@Test
	public void testSuccessCase2() {
		int N = 11;
		int maxNCC_LB = 0;
		int maxNCC_UB = 5;
		int[] GlbNodes = new int[] {0, 1, 2};
		int[][] GlbEdges = new int[][] { {1, 2} };
		int[] GubNodes = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
		int[][] GubEdges = new int[][] {
				{2, 1}, {1, 0}, {0, 5}, {5, 4},
				{6, 7}, {7, 10},
				{3, 8}, {8, 9}
		};
		AbstractTestModel test = new AbstractTestModel(N, maxNCC_LB, maxNCC_UB, GlbNodes, GubNodes, GlbEdges, GubEdges);
		boolean solutionFound = test.model.getSolver().solve();
		Assert.assertTrue(solutionFound);
		Assert.assertTrue(test.model.getSolver().solve());
	}

	/**
	 * Success test case 3. There are exactly 3 solutions:
	 *     1.      1 -- 2.
	 *     2. 0    1 -- 2.
	 *     3. 0 -- 1 -- 2.
	 * graph GLB {
	 *     { 1; 2; }
	 *     1 -- 2;
	 * }
	 * graph GUB {
	 *     { 0; 1; 2; }
	 *     0 -- 1;
	 *     1 -- 2;
	 * }
	 */
	@Test
	public void testSuccessCase3() throws ContradictionException {
		int N = 4;
		int maxNCC_LB = 2;
		int maxNCC_UB = 3;
		int[] GlbNodes = new int[] {1, 2};
		int[][] GlbEdges = new int[][] { {1, 2} };
		int[] GubNodes = new int[] {0, 1, 2};
		int[][] GubEdges = new int[][] { {0, 1}, {1, 2} };
		AbstractTestModel test = new AbstractTestModel(N, maxNCC_LB, maxNCC_UB, GlbNodes, GubNodes, GlbEdges, GubEdges);
		test.model.getSolver().plugMonitor((IMonitorSolution) () -> {
		});
		List<Solution> solutions = test.model.getSolver().findAllSolutions();
		Assert.assertEquals(solutions.size(), 3);
	}

	/**
	 * Success test case 4. Two solutions that are not the empty graph, and need the removal of an edge in GUB:
	 * 1.  0 -- 1 -- 2 -- 3 => 0 -- 1 -- 2    3
	 * 2.  0 -- 1 -- 2 -- 3 => 0 -- 1 -- 2
	 * graph GLB {
	 *     { 0; 1; 2; }
	 *     0 -- 1;
	 *     1 -- 2;
	 * }
	 * graph GUB {
	 *     { 0; 1; 2; 3; }
	 *     0 -- 1;
	 *     1 -- 2;
	 *     2 -- 3;
	 * }
	 */
	@Test
	public void testSuccessCase4() throws ContradictionException {
		int N = 4;
		int maxNCC_LB = 2;
		int maxNCC_UB = 3;
		int[] GlbNodes = new int[] {0, 1, 2};
		int[][] GlbEdges = new int[][] { {0, 1}, {1, 2} };
		int[] GubNodes = new int[] {0, 1, 2, 3};
		int[][] GubEdges = new int[][] { {0, 1}, {1, 2}, {2, 3} };
		AbstractTestModel test = new AbstractTestModel(N, maxNCC_LB, maxNCC_UB, GlbNodes, GubNodes, GlbEdges, GubEdges);
		List<Solution> solutions = test.model.getSolver().findAllSolutions();
		Assert.assertEquals(solutions.size(), 2);
	}

	@Test
	public void batchTest() {
		int N = 20;
		for (int k : IntStream.range(0, 1).toArray()) {
			GraphModel model = new GraphModel();
			int nbCC1 = ThreadLocalRandom.current().nextInt(3, 6);
			int nbCC2 = ThreadLocalRandom.current().nextInt(3, 6);
			UndirectedGraph GLB = GraphGenerator.makeRandomUndirectedGraphFromNbCC(model, N, SetType.BITSET, nbCC1, 0.3, 10);
			UndirectedGraph GUB = GraphGenerator.makeRandomUndirectedGraphFromNbCC(model, N, SetType.BITSET, nbCC2, 0.1, 5);
			for (int i : GLB.getNodes()) {
				GUB.addNode(i);
				for (int j : GLB.getNeighOf(i)) {
					GUB.addEdge(i, j);
				}
			}
			ConnectivityFinder glbCf = new ConnectivityFinder(GLB);
			glbCf.findAllCC();
			ConnectivityFinder gubCf = new ConnectivityFinder(GUB);
			gubCf.findAllCC();
			AbstractTestModel test = new AbstractTestModel(model, N, 5, 10, GLB, GUB);
			if (test.model.getSolver().findSolution() != null) {
				Assert.assertTrue(test.g.isInstantiated() && test.sizeMaxCC.isInstantiated());
				ConnectivityFinder cFinder = new ConnectivityFinder(test.g.getUB());
				cFinder.findAllCC();
				Assert.assertEquals(cFinder.getSizeMaxCC(), test.sizeMaxCC.getValue());
			}
		}
	}
}
