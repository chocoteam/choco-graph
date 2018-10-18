package org.chocosolver.checked;

import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Created by ezulkosk on 5/22/15.
 */
public class ConnectedTest {

    @Test(groups = "10s")
    public void testConnectedArticulationX() {
        GraphModel model = new GraphModel();
        int n = 6;
        // build model
        UndirectedGraph GLB = new UndirectedGraph(model,n, SetType.BIPARTITESET,false);
        UndirectedGraph GUB = new UndirectedGraph(model,n,SetType.BIPARTITESET,false);
        for(int i=0;i<n;i++)GUB.addNode(i);
        GLB.addNode(0);
        GLB.addNode(4);
        GUB.addEdge(0,1);
        GUB.addEdge(0,3);
        GUB.addEdge(1,2);
        GUB.addEdge(1,3);
        GUB.addEdge(3,4);
        GUB.addEdge(4,5);
        UndirectedGraphVar graph = model.graphVar("G", GLB, GUB);

        model.connected(graph).post();
        try {
            model.getSolver().propagate();
        } catch (ContradictionException e) {
            e.printStackTrace();
            Assert.assertFalse(true);
        }
        Assert.assertFalse(GLB.getNodes().contains(1));
        Assert.assertTrue(GLB.getNodes().contains(3));
        while (model.getSolver().solve());
    }

    @Test(groups = "10s")
    public void testConnectedArticulation() {
        GraphModel model = new GraphModel();
        int n = 7;
        // build model
        UndirectedGraph GLB = new UndirectedGraph(model,n, SetType.BIPARTITESET,false);
        UndirectedGraph GUB = new UndirectedGraph(model,n,SetType.BIPARTITESET,false);
        for(int i=0;i<n;i++)GUB.addNode(i);
        GLB.addNode(0);
        GLB.addNode(5);
        GUB.addEdge(0,1);
        GUB.addEdge(0,4);
        GUB.addEdge(1,2);
        GUB.addEdge(1,3);
        GUB.addEdge(2,3);
        GUB.addEdge(1,4);
        GUB.addEdge(4,5);
        GUB.addEdge(4,6);
        GUB.addEdge(5,6);
        UndirectedGraphVar graph = model.graphVar("G", GLB, GUB);

        model.connected(graph).post();
        try {
            model.getSolver().propagate();
        } catch (ContradictionException e) {
            e.printStackTrace();
            Assert.assertFalse(true);
        }
        Assert.assertFalse(GLB.getNodes().contains(1));
        Assert.assertTrue(GLB.getNodes().contains(4));
        while (model.getSolver().solve());
    }

    @Test(groups = "10s")
    public void testConnectedArticulation1() {
        GraphModel model = new GraphModel();
        int n = 4;
        // build model
        UndirectedGraph GLB = new UndirectedGraph(model,n, SetType.BIPARTITESET,false);
        UndirectedGraph GUB = new UndirectedGraph(model,n,SetType.BIPARTITESET,false);
        for(int i=0;i<n;i++)GUB.addNode(i);
        GLB.addNode(0);
        GLB.addNode(3);
        GUB.addEdge(0,1);
        GUB.addEdge(1,2);
        GLB.addEdge(0,3);
        GUB.addEdge(0,3);
        UndirectedGraphVar graph = model.graphVar("G", GLB, GUB);

        model.connected(graph).post();
        try {
            model.getSolver().propagate();
        } catch (ContradictionException e) {
            e.printStackTrace();
            Assert.assertFalse(true);
        }
        Assert.assertFalse(GLB.getNodes().contains(1));
    }

    @Test(groups = "10s")
    public void testConnectedArticulation2() {
        GraphModel model = new GraphModel();
        int n = 4;
        // build model
        UndirectedGraph GLB = new UndirectedGraph(model,n, SetType.BIPARTITESET,false);
        UndirectedGraph GUB = new UndirectedGraph(model,n,SetType.BIPARTITESET,false);
        for(int i=0;i<n;i++)GUB.addNode(i);
        GLB.addNode(0);
        GLB.addNode(2);
        GLB.addNode(3);
        GUB.addEdge(0,1);
        GUB.addEdge(1,2);
        GLB.addEdge(0,3);
        GUB.addEdge(0,3);
        UndirectedGraphVar graph = model.graphVar("G", GLB, GUB);

        model.connected(graph).post();

        try {
            model.getSolver().propagate();
        } catch (ContradictionException e) {
            e.printStackTrace();
            Assert.assertFalse(true);
        }
        Assert.assertTrue(GLB.getNodes().contains(1));
    }

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

    @Test(groups = "10s")
    public void testChocoConnected4() {
        GraphModel model = new GraphModel();
        // build model
        UndirectedGraph GLB = new UndirectedGraph(model, 2, SetType.BITSET,false);
        UndirectedGraph GUB = new UndirectedGraph(model, 2,SetType.BITSET,false);

        GUB.addNode(0);
        GUB.addNode(1);

        UndirectedGraphVar graph = model.graphVar("G", GLB, GUB);

        assertEquals(model.connected(graph).isSatisfied(), ESat.UNDEFINED);
    }
}
