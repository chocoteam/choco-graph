package org.chocosolver.checked;


import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.graphsolver.variables.IUndirectedGraphVar;
import org.chocosolver.solver.ResolutionPolicy;
import org.chocosolver.solver.variables.*;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.chocosolver.util.tools.ArrayUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Jean-Guillaume Fages
 * @since 22/11/14
 * Created by IntelliJ IDEA.
 */
public class GraphTest {

    @Test(groups = "1s")
    public void test() {
        final GraphModel model = new GraphModel();
        BoolVar[] isVertexPresent = model.boolVarArray("v_pr", 4);
        BoolVar[] isEdgePresent = model.boolVarArray("e_pr", 3);
        IntVar[] actVertW = new IntVar[4];
        IntVar[] actEdgeW = new IntVar[3];
        int[] vw = new int[]{-3, -2, 1, 1};
        int[] ew = new int[]{2, 3, -3};
        int[][] edges = new int[][]{new int[]{0, 2}, new int[]{2, 3}, new int[]{3, 1}};
        for (int i = 0; i < 4; i++) {
            actVertW[i] = model.intVar("act_v[" + i + "]", new int[]{0, vw[i]});
            model.arithm(actVertW[i], "=", vw[i]).reifyWith(isVertexPresent[i]);
        }
        for (int i = 0; i < 3; i++) {
            actEdgeW[i] = model.intVar("act_e[" + i + "]", new int[]{0, ew[i]});
            model.arithm(actEdgeW[i], "=", ew[i]).reifyWith(isEdgePresent[i]);
            BoolVar conj = model.boolVar("conj");
            model.addClausesBoolAndArrayEqVar(new BoolVar[]{isVertexPresent[edges[i][0]], isVertexPresent[edges[i][1]]}, conj);
            model.addClausesBoolOrArrayEqualTrue(new BoolVar[]{isEdgePresent[i].not(), conj});
        }


        UndirectedGraph UB = new UndirectedGraph(model, 4, SetType.BIPARTITESET, false);
        UndirectedGraph LB = new UndirectedGraph(model, 4, SetType.BIPARTITESET, false);
        for (int i = 0; i < 4; i++) {
            UB.addNode(i);
        }
        for (int i = 0; i < 3; i++) {
            UB.addEdge(edges[i][0], edges[i][1]);
        }
        final IUndirectedGraphVar sol = model.graphVar("solution", LB, UB);
        model.connected(sol).post();
        model.nodes_channeling(sol, isVertexPresent).post();
        for (int i = 0; i < 3; i++) {
            model.edge_channeling(sol, isEdgePresent[i], edges[i][0], edges[i][1]).post();
        }
        final IntVar sum = model.intVar("sum", -50, 50);
        model.sum(ArrayUtils.append(actEdgeW, actVertW), "=", sum).post();

        model.setObjective(ResolutionPolicy.MAXIMIZE,sum);

        while (model.solve()){
            System.out.println("SOLUTION FOUND " + sum);
        }

        Assert.assertEquals(model.getSolver().isFeasible(), ESat.TRUE);
        System.out.println(model.getSolver().toOneLineString());
    }
}
