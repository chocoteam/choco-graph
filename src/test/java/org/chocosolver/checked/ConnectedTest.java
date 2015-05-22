package org.chocosolver.checked;

import org.chocosolver.solver.cstrs.GCF;
import org.chocosolver.solver.variables.IUndirectedGraphVar;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.testng.annotations.Test;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.cstrs.GraphConstraintFactory;
import org.chocosolver.solver.search.GraphStrategyFactory;
import org.chocosolver.solver.variables.GraphVarFactory;
import org.chocosolver.solver.variables.IDirectedGraphVar;
import org.chocosolver.util.objects.graphs.DirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;

/**
 * Created by ezulkosk on 5/22/15.
 */



public class ConnectedTest {

    @Test(groups = "10s")
    public void testChocoConnected() {
        Solver solver = new Solver();
        // build model
        UndirectedGraph GLB = new UndirectedGraph(solver,2, SetType.BITSET,false);
        UndirectedGraph GUB = new UndirectedGraph(solver,2,SetType.BITSET,false);

        GLB.addNode(0);

        GUB.addNode(0);
        GUB.addNode(1);
        GUB.addEdge(0,1);

        IUndirectedGraphVar graph = GraphVarFactory.undirected_graph_var("G", GLB, GUB, solver);
        GCF.connected(graph).reif();

        System.out.println("TEST (should return UNDEFINED): " + GCF.connected(graph).getPropagator(0).isEntailed());

        solver.post(GraphConstraintFactory.connected(graph));
        solver.set(GraphStrategyFactory.lexico(graph));

        if (solver.findSolution()) {
            System.out.println(solver.isSatisfied());
        }

    }

    @Test(groups = "10s")
    public void testChocoConnected2() {
        Solver solver = new Solver();
        // build model
        UndirectedGraph GLB = new UndirectedGraph(solver,2, SetType.BITSET,false);
        UndirectedGraph GUB = new UndirectedGraph(solver,2,SetType.BITSET,false);

        IUndirectedGraphVar graph = GraphVarFactory.undirected_graph_var("G", GLB, GUB, solver);
        GCF.connected(graph).reif();

        System.out.println("TEST (should return UNDEFINED): " + GCF.connected(graph).getPropagator(0).isEntailed());

        solver.post(GraphConstraintFactory.connected(graph));
        solver.set(GraphStrategyFactory.lexico(graph));

        if (solver.findSolution()) {
            System.out.println(solver.isSatisfied());
        }

    }
}
