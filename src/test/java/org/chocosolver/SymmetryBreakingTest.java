package org.chocosolver;

import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.graphsolver.cstrs.symmbreaking.Pair;
import org.chocosolver.graphsolver.cstrs.symmbreaking.PropGirth;
import org.chocosolver.graphsolver.search.GraphStrategyFactory;
import org.chocosolver.graphsolver.variables.IUndirectedGraphVar;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;

/**
 * Tests for {@code GraphConstraintFactory#postSymmetryBreaking(IUndirectedGraphVar, Solver) postSymmetryBreaking},
 * {@code GraphConstraintFactory#symmetryBreaking2(IUndirectedGraphVar, Solver) symmetryBreaking2} and
 * {@code GraphConstraintFactory#symmetryBreaking3(IUndirectedGraphVar, Solver) symmetryBreaking3}.
 * Symmetry breaking is using in next problem: given n, m and l – integers.
 * <br/>
 * Find whether exists undirected connected graph with n nodes, m edges
 * and with girth equals to l.
 * <br/>
 * Tests contain checking correctness of answers found and equivalence of existance of solution
 * with symmetry breaking predicates and without them.
 *
 * @author Моклев Вячеслав
 */
public class SymmetryBreakingTest {
    private static PrintStream oldOut;

    /**
     * Calculates a girth of a given graph.
     *
     * @param graph given graph
     * @return girth of {@code graph}
     */
    private static int getGraphGirth(IUndirectedGraphVar graph) {
        int n = graph.getNbMaxNodes();
        int g = n + 1;
        for (int i = 0; i < n; i++) {
            int pg = getGraphVertexGirth(graph, i);
            if (pg < g) {
                g = pg;
            }
        }
        return g;
    }

    private static int getGraphVertexGirth(IUndirectedGraphVar graph, int vertex) {
        int n = graph.getNbMaxNodes();
        HashSet<Pair<Integer, Integer>> reachable = new HashSet<>();
        reachable.add(new Pair<>(vertex, -1));
        for (int i = 1; i <= n; i++) {
            HashSet<Pair<Integer, Integer>> set = new HashSet<>();
            for (Pair<Integer, Integer> u: reachable) {
                for (int v: graph.getMandNeighOf(u.getA())) {
                    if (v != u.getB()) {
                        if (v == vertex) {
                            return i;
                        }
                        set.add(new Pair<>(v, u.getA()));
                    }
                }
            }
            reachable = set;
        }
        return n + 1;
    }

    /**
     * Tries to find a solution of the given problem and check it for correctness.
     *
     * @param n count of nodes
     * @param m count of edges
     * @param l required girth
     * @param addSymmetryBreaking enable symmetry breaking predicates or not
     * @throws AssertionError if solution found is not correct
     * @return true, if solution exists and false otherwise
     */
    private static boolean solutionExists(int n, int m, int l, boolean addSymmetryBreaking) {
        GraphModel model = new GraphModel();
        UndirectedGraph GLB = new UndirectedGraph(model, n, SetType.BITSET, true);
        UndirectedGraph GUB = new UndirectedGraph(model, n, SetType.BITSET, true);
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                GUB.addEdge(i, j);
            }
        }
        IUndirectedGraphVar graph = model.graphVar("G", GLB, GUB);
        // graph mush contains n nodes, m edges and have girth exactly l
        model.getSolver().setSearch(GraphStrategyFactory.inputOrder(graph));
        model.nbEdges(graph, model.intVar(m)).post();
        model.connected(graph).post(); // GCF.postSymmetryBreaking is sb predicate only for connected undirected graphs
        new Constraint("GirthConstraint", new PropGirth(graph, model.intVar(l))).post();
        // add symmetry breaking constraint if necessary
        if (addSymmetryBreaking) {
            // choose one to test
            model.postSymmetryBreaking(graph);
//            model.symmetryBreaking2(graph, solver));
//            model.symmetryBreaking3(graph, solver));
        }
        boolean result = model.getSolver().solve();
        if (result) { // check correctness of found answer
            int count = 0;
            for (int u = 0; u < n; u++) {
                count += graph.getMandNeighOf(u).getSize();
            }
            Assert.assertEquals(count, 2 * m, "correct number of edges");
            Assert.assertEquals(getGraphGirth(graph), l, "correct girth");
        }
        return result;
    }

    @BeforeMethod
    public void setUp() throws Exception {
        oldOut = System.out;
        System.setOut(new PrintStream(new OutputStream() {
            @Override public void write(int b) throws IOException {}
        }));
    }

    @AfterMethod
    public void tearDown() throws Exception {
        System.setOut(oldOut);
    }

    /**
     * Checks equivalence of existance of solution
     * with and without symmetry breaking.
     *
     * @param n count of nodes
     * @param m count of edges
     * @param l required girth
     */
    public static void test(int n, int m, int l) {
        Assert.assertEquals(
                solutionExists(n, m, l, true),
                solutionExists(n, m, l, false),
                "symmetry breaking: " + n + ", " + m + ", " + l
        );
    }

    @Test
    public static void testSimple1() {
        test(1, 1, 1);
    }

    @Test
    public static void testSimple2() {
        test(5, 4, 2);
    }

    @Test
    public static void testSimple3() {
        test(3, 5, 4);
    }

    @Test
    public static void testSimple4() {
        test(2, 1, 3);
    }

    @Test
    public static void testSimple5() {
        test(3, 2, 3);
    }

    @Test
    public static void testMedium1() {
        test(4, 3, 3);
    }

    @Test
    public static void testAllSmall() {
        for (int n = 1; n <= 6; n++) {
            for (int m = 1; m <= 6; m++) {
                for (int l = 1; l <= 6; l++) {
                    test(n, m, l);
                }
            }
        }
    }

    // OEIS, A006856
    private static final int[] a = new int[] {0, 0, 1, 2, 3, 5, 6, 8, 10, 12, 15, 16, 18, 21, 23, 36, 28, 31};

    @Test
    public static void testCorrectness() {
        for (int n = 5; n <= 7; n++) {
            Assert.assertEquals(solutionExists(n, a[n], 5, true), true);
            Assert.assertEquals(solutionExists(n, a[n] + 1, 5, true), false);
        }
    }

    @Test
    public static void testCorrectness1() {
        int n = 10;
        Assert.assertEquals(solutionExists(n, a[n], 5, true), true);
    }

    @Test
    public static void testCorrectness2() {
        int n = 8;
        Assert.assertEquals(solutionExists(n, a[n] + 1, 5, true), false);
    }

    @Test
    public static void testHardNoSolution() {
        Assert.assertEquals(
                solutionExists(8, 10, 6, true),
                false // it's preprocessed value of solutionExists(8, 10, 6, false), 70 seconds @ AMD FX-8150 (3.6 GHz, 8 Gb RAM)
        );
    }

    @Test
    public static void testHardSolutionExists() {
        Assert.assertEquals(
                solutionExists(10, 10, 9, true),
                true // it's preprocessed value of solutionExists(10, 10, 9, false), 225 seconds @ AMD FX-8150 (3.6 GHz, 8 Gb RAM)
        );
    }

}