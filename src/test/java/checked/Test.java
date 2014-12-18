package checked;

import org.testng.Assert;
import solver.ResolutionPolicy;
import solver.Solver;
import solver.constraints.ICF;
import solver.constraints.SatFactory;
import solver.cstrs.GCF;
import solver.search.GraphStrategyFactory;
import solver.search.loop.monitors.IMonitorSolution;
import solver.search.strategy.ISF;
import solver.search.strategy.SetStrategyFactory;
import solver.variables.*;
import util.ESat;
import util.objects.graphs.UndirectedGraph;
import util.objects.setDataStructures.SetType;
import util.tools.ArrayUtils;

/**
 * @author Jean-Guillaume Fages
 * @since 22/11/14
 * Created by IntelliJ IDEA.
 */
public class Test {

	@org.testng.annotations.Test(groups = "1s")
	public void test() {
		final Solver solver = new Solver();
		BoolVar[] isVertexPresent = VF.boolArray("v_pr", 4, solver);
		BoolVar[] isEdgePresent = VF.boolArray("e_pr", 3, solver);
		IntVar[] actVertW = new IntVar[4];
		IntVar[] actEdgeW = new IntVar[3];
		int[] vw = new int[]{-3, -2, 1, 1};
		int[] ew = new int[]{2, 3, -3};
		int[][] edges = new int[][]{new int[]{0, 2}, new int[]{2, 3}, new int[]{3, 1}};
		for(int i = 0; i < 4; i++){
			actVertW[i] = VF.enumerated("act_v[" + i + "]", new int[]{0, vw[i]}, solver);
			ICF.arithm(actVertW[i], "=", vw[i]).reifyWith(isVertexPresent[i]);
		}
		for(int i = 0; i < 3; i++){
			actEdgeW[i] = VF.enumerated("act_e[" + i + "]", new int[]{0, ew[i]}, solver);
			ICF.arithm(actEdgeW[i], "=", ew[i]).reifyWith(isEdgePresent[i]);
			BoolVar conj = VF.bool("conj", solver);
			SatFactory.addBoolAndArrayEqVar(new BoolVar[]{isVertexPresent[edges[i][0]], isVertexPresent[edges[i][1]]}, conj);
			SatFactory.addBoolOrArrayEqualTrue(new BoolVar[]{isEdgePresent[i].not(), conj});
//			LCF.reification(conj,LCF.and(isVertexPresent[edges[i][0]], isVertexPresent[edges[i][1]]));
//			solver.post(LCF.or(isEdgePresent[i].not(), conj));
		}


		UndirectedGraph UB = new UndirectedGraph(solver, 4, SetType.BIPARTITESET, false);
		UndirectedGraph LB = new UndirectedGraph(solver, 4, SetType.BIPARTITESET, false);
		for (int i = 0; i < 4; i++) {
			UB.addNode(i);
		}
		for(int i = 0; i < 3; i++){
			UB.addEdge(edges[i][0], edges[i][1]);
		}
		final IUndirectedGraphVar sol = GraphVarFactory.undirected_graph_var("solution", LB, UB, solver);
		solver.post(GCF.connected(sol));
		solver.post(GCF.nodes_channeling(sol, isVertexPresent));
		for(int i = 0; i < 3; i++){
			solver.post(GCF.edge_channeling(sol, isEdgePresent[i], edges[i][0], edges[i][1]));
		}
		final IntVar sum = VF.integer("sum", -50, 50, solver);
		solver.post(ICF.sum(ArrayUtils.append(actEdgeW,actVertW), sum));
//		SatFactory.addFalse(isVertexPresent[0]);
//		SatFactory.addFalse(isVertexPresent[1]);

//		solver.post(ICF.arithm(sum,"=",5));

		solver.set(
				GraphStrategyFactory.lexico(sol),
				SetStrategyFactory.force_minDelta_first(solver.retrieveSetVars()),
				ISF.lexico_LB(solver.retrieveIntVars()),
				ISF.lexico_LB(solver.retrieveBoolVars()));

		solver.plugMonitor(new IMonitorSolution() {
			@Override
			public void onSolution() {
				System.out.println("SOLUTION FOUND " + sum);
//				System.out.println(solver.getStrategy());
			}
		});

		solver.findOptimalSolution(ResolutionPolicy.MAXIMIZE, sum);

		Assert.assertEquals(solver.isFeasible(),ESat.TRUE);
		System.out.println(solver.getMeasures());
	}
}