package org.chocosolver.checked;

import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CycleTest {

	@Test
	public void test() throws ContradictionException {
		GraphModel m = new GraphModel();
		int n = 4;
		UndirectedGraph GLB = new UndirectedGraph(m, n, SetType.BITSET, false);
		UndirectedGraph GUB = new UndirectedGraph(m, n, SetType.BITSET, false);
		for (int i = 0; i < n; i++) {
			GUB.addNode(i);
		}
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				GUB.addEdge(i, j);
			}
		}
		UndirectedGraphVar g = m.graphVar("g", GLB, GUB);
		m.cycle(g).post();
		m.nbNodes(g, m.intVar(n)).post();

		Solver s = m.getSolver();
		s.propagate();

		while (s.solve()){
			System.out.println("sol ---");
			System.out.println(g.graphVizExport());
		}
		s.printStatistics();
		Assert.assertEquals(s.getSolutionCount(), 3);
	}

	@Test
	public void testPropag() throws ContradictionException {
		GraphModel m = new GraphModel();
		int n = 4;
		UndirectedGraph GLB = new UndirectedGraph(m, n, SetType.LINKED_LIST, false);
		UndirectedGraph GUB = new UndirectedGraph(m, n, SetType.LINKED_LIST, false);
		for (int i = 0; i < n; i++) {
			GUB.addNode(i);
		}
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				GUB.addEdge(i, j);
			}
		}
		GLB.addEdge(0, 3);
		GLB.addEdge(0, 2);
		UndirectedGraphVar g = m.graphVar("g", GLB, GUB);
		m.cycle(g).post();
		m.nbNodes(g, m.intVar(n)).post();

		Solver s = m.getSolver();
		s.propagate();

		Assert.assertTrue(g.isInstantiated());

		while (s.solve()){
			System.out.println("sol ---");
			System.out.println(g.graphVizExport());
		}
		s.printStatistics();
		Assert.assertEquals(s.getSolutionCount(), 1);
	}
}
