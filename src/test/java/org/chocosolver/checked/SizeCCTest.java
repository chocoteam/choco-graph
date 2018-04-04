package org.chocosolver.checked;

import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.loop.monitors.IMonitorSolution;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Test class for the SizeCC constraint.
 */
public class SizeCCTest {

    /**
     * Abstract test model, for factorization.
     */
    private class AbstractTestModel {

        // Variables
        public int N, minNCC, maxNCC;
        public int[] GlbNodes, GubNodes;
        public int[][] GlbEdges, GubEdges;
        public GraphModel model;
        public UndirectedGraphVar g;

        // Constructor
        public AbstractTestModel(int N, int minNCC, int maxNCC,
                                 int[] GlbNodes, int[] GubNodes,
                                 int[][] GlbEdges, int[][] GubEdges) {
            this.N = N;
            this.minNCC = minNCC;
            this.maxNCC = maxNCC;
            this.GlbNodes = GlbNodes;
            this.GubNodes = GubNodes;
            this.GlbEdges = GlbEdges;
            this.GubEdges = GubEdges;
            this.model = new GraphModel();
            // Init GLB (graph kernel)
            UndirectedGraph GLB = new UndirectedGraph(model, N, SetType.BIPARTITESET, false);
            Arrays.stream(GlbNodes).forEach(i -> GLB.addNode(i));
            Arrays.stream(GlbEdges).forEach(e -> GLB.addEdge(e[0], e[1]));
            // Init GUB (graph envelope)
            UndirectedGraph GUB = new UndirectedGraph(model, N, SetType.BIPARTITESET, false);
            Arrays.stream(GubNodes).forEach(i -> GUB.addNode(i));
            Arrays.stream(GubEdges).forEach(e -> GUB.addEdge(e[0], e[1]));
            // Create the graph variable
            this.g = model.graphVar("g", GLB, GUB);
            // Post the constraint
            model.sizeConnectedComponents(g, minNCC, maxNCC).post();
        }
    }

    /* --------------- */
    /* Fail test cases */
    /* --------------- */

    /**
     * Fail test case 1: The minimum size of the CC cannot be satisfied.
     * minNCC = 3.
     * maxNCC = 5;
     * graph GLB { 0; }
     * graph GUB {
     *     { 0; 1; 2; 3; 4; 5; 6; }
     *     # CC of size 2
     *     0 -- 1;
     *     # CC of size 2
     *     2 -- 3;
     *     # CC of size 3
     *     4 -- 5;
     *     5 -- 6;
     * }
     */
    @Test
    public void testFailCase1() {
        int N = 7;
        int minNCC = 4;
        int maxNCC = 5;
        int[] GlbNodes = new int[] {0};
        int[][] GlbEdges = new int[][] {};
        int[] GubNodes = new int[] {0, 1, 2, 3, 4, 5, 6};
        int[][] GubEdges = new int[][] {
                {0, 1},
                {2, 3},
                {4, 5}, {5, 6}
        };
        AbstractTestModel test = new AbstractTestModel(N, minNCC, maxNCC, GlbNodes, GubNodes, GlbEdges, GubEdges);
        Assert.assertFalse(test.model.getSolver().solve());
    }

    /**
     * Fail test case 2: The maximum size of the CC cannot be satisfied.
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
    public void testFailCase2() {
        int N = 3;
        int minNCC = 0;
        int maxNCC = 1;
        int[] GlbNodes = new int[] {0, 1};
        int[][] GlbEdges = new int[][] { {0, 1} };
        int[] GubNodes = new int[] {0, 1, 2};
        int[][] GubEdges = new int[][] { {0, 1}, {1, 2} };
        AbstractTestModel test = new AbstractTestModel(N, minNCC, maxNCC, GlbNodes, GubNodes, GlbEdges, GubEdges);
        Assert.assertFalse(test.model.getSolver().solve());
    }

    /**
     * Fail test case 3: minNCC is greater than the number of potential nodes.
     * graph GLB {
     *     { 0; }
     * }
     * graph GUB {
     *     { 0; 1; 2; 3; }
     *     0 -- 1;
     *     1 -- 2;
     *     1 -- 3;
     * }
     */
    @Test
    public void testFailCase3() {
        int N = 4;
        int minNCC = 10;
        int maxNCC = 15;
        int[] GlbNodes = new int[] {0};
        int[][] GlbEdges = new int[][] {};
        int[] GubNodes = new int[] {0, 1, 2, 3};
        int[][] GubEdges = new int[][] { {0, 1}, {1, 2}, {1, 3} };
        AbstractTestModel test = new AbstractTestModel(N, minNCC, maxNCC, GlbNodes, GubNodes, GlbEdges, GubEdges);
        Assert.assertFalse(test.model.getSolver().solve());
    }

    /* ------------------ */
    /* Success test cases */
    /* ------------------ */

    /**
     * Success test case 1: The empty graph is the only solution.
     * graph GLB {}
     * graph GUB {
     *     { 0; 1; 2; 3; 4; }
     *     0 -- 1;
     *     1 -- 2;
     *     2 -- 3;
     *     3 -- 4;
     * }
     */
    @Test
    public void testSuccessCase1() {
        int N = 5;
        int minNCC = 10;
        int maxNCC = 20;
        int[] GlbNodes = new int[] {};
        int[][] GlbEdges = new int[][] {};
        int[] GubNodes = new int[] {0, 1, 2, 3, 4};
        int[][] GubEdges = new int[][] { {0, 1}, {1, 2}, {2, 3}, {3, 4} };
        AbstractTestModel test = new AbstractTestModel(N, minNCC, maxNCC, GlbNodes, GubNodes, GlbEdges, GubEdges);
        boolean solutionFound = test.model.getSolver().solve();
        Assert.assertTrue(solutionFound);
        Assert.assertEquals(test.g.getPotentialNodes().size(), 0); // Assert that the solution is the empty graph
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
        int minNCC = 3;
        int maxNCC = 5;
        int[] GlbNodes = new int[] {0, 1, 2};
        int[][] GlbEdges = new int[][] { {1, 2} };
        int[] GubNodes = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        int[][] GubEdges = new int[][] {
                {2, 1}, {1, 0}, {0, 5}, {5, 4},
                {6, 7}, {7, 10},
                {3, 8}, {8, 9}
        };
        AbstractTestModel test = new AbstractTestModel(N, minNCC, maxNCC, GlbNodes, GubNodes, GlbEdges, GubEdges);
        boolean solutionFound = test.model.getSolver().solve();
        Assert.assertTrue(solutionFound);
        Assert.assertTrue(test.model.getSolver().solve());
    }

    /**
     * Success test case 3. There are exactly 3 solutions:
     *     1. Empty graph.
     *     2. 0 -- 1 -- 2.
     *     3. 1 -- 2 -- 3.
     * graph GLB {}
     * graph GUB {
     *     { 0; 1; 2; 3; }
     *     0 -- 1;
     *     1 -- 2;
     *     2 -- 3;
     * }
     */
    @Test
    public void testSuccessCase3() throws ContradictionException {
        int N = 4;
        int minNCC = 3;
        int maxNCC = 3;
        int[] GlbNodes = new int[] {};
        int[][] GlbEdges = new int[][] {};
        int[] GubNodes = new int[] {0, 1, 2, 3};
        int[][] GubEdges = new int[][] { {0, 1}, {1, 2}, {2, 3} };
        AbstractTestModel test = new AbstractTestModel(N, minNCC, maxNCC, GlbNodes, GubNodes, GlbEdges, GubEdges);
        test.model.getSolver().plugMonitor((IMonitorSolution) () -> {
        });
        List<Solution> solutions = test.model.getSolver().findAllSolutions();
        Assert.assertEquals(solutions.size(), 3);
    }

    /**
     * Success test case 4. A single solution that is not the empty graph, and need the enforcing of edges in GLB:
     * 0    1    2 => 0 -- 1 -- 2.
     * graph GLB {
     *     { 0; 1; 2; }
     * }
     * graph GUB {
     *     { 0; 1; 2; }
     *     0 -- 1;
     *     1 -- 2;
     * }
     */
    @Test
    public void testSuccessCase4() throws ContradictionException {
        int N = 3;
        int minNCC = 3;
        int maxNCC = 3;
        int[] GlbNodes = new int[] {0, 1, 2};
        int[][] GlbEdges = new int[][] {};
        int[] GubNodes = new int[] {0, 1, 2};
        int[][] GubEdges = new int[][] { {0, 1}, {1, 2} };
        AbstractTestModel test = new AbstractTestModel(N, minNCC, maxNCC, GlbNodes, GubNodes, GlbEdges, GubEdges);
        List<Solution> solutions = test.model.getSolver().findAllSolutions();
        Assert.assertEquals(solutions.size(), 1);
    }

    /**
     * Success test case 5. A single solution that is not the empty graph, and need the removal of an edge in GUB:
     * 0 -- 1 -- 2 -- 3 => 0 -- 1 -- 2    3
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
    public void testSuccessCase5() throws ContradictionException {
        int N = 4;
        int minNCC = 3;
        int maxNCC = 3;
        int[] GlbNodes = new int[] {0, 1, 2};
        int[][] GlbEdges = new int[][] { {0, 1}, {1, 2} };
        int[] GubNodes = new int[] {0, 1, 2, 3};
        int[][] GubEdges = new int[][] { {0, 1}, {1, 2}, {2, 3} };
        AbstractTestModel test = new AbstractTestModel(N, minNCC, maxNCC, GlbNodes, GubNodes, GlbEdges, GubEdges);
        List<Solution> solutions = test.model.getSolver().findAllSolutions();
        Assert.assertEquals(solutions.size(), 1);
    }
}
