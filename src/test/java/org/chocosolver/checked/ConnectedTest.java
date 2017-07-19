package org.chocosolver.checked;

import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Created by ezulkosk on 5/22/15.
 */
public class ConnectedTest {

    @Test(groups = "10s")
    public void testChocoConnected() {
        GraphModel model = new GraphModel();
        // build model
        UndirectedGraph GLB = new UndirectedGraph(model,2, SetType.BITSET,false);
        UndirectedGraph GUB = new UndirectedGraph(model,2,SetType.BITSET,false);

        GLB.addNode(0);

        GUB.addNode(0);
        GUB.addNode(1);
        GUB.addEdge(0,1);

        UndirectedGraphVar graph = model.graphVar("G", GLB, GUB);

        assertEquals(model.connected(graph).isSatisfied(), ESat.UNDEFINED);

        model.connected(graph).post();

        while (model.getSolver().solve()){
            System.out.println(graph);
        }
    }

    @Test(groups = "10s")
    public void testChocoConnected2() {
        GraphModel model = new GraphModel();
        // build model
        UndirectedGraph GLB = new UndirectedGraph(model, 2, SetType.BITSET,false);
        UndirectedGraph GUB = new UndirectedGraph(model, 2,SetType.BITSET,false);

        //empty graphs are traditionally *not* connected.
        UndirectedGraphVar graph = model.graphVar("G", GLB, GUB);

        assertEquals(model.connected(graph).isSatisfied(), ESat.FALSE);

        model.connected(graph).getOpposite().post();

        while (model.getSolver().solve()){
            System.out.println(graph);
        }
    }

    @Test(groups = "10s")
    public void testChocoConnected3() {
        GraphModel m = new GraphModel();
        UndirectedGraph LB = new UndirectedGraph(m, 2, SetType.BITSET, false);
        UndirectedGraph UB = new UndirectedGraph(m, 2, SetType.BITSET, false);
        UB.addNode(0);
        UB.addNode(1);
        UB.addEdge(0, 1);
        UndirectedGraphVar g = m.graphVar("g", LB, UB);

        m.connected(g).post();

        while (m.getSolver().solve()){
            System.out.println(g);
        }
    }
}
