package solver.variables;

import solver.Solver;
import util.objects.graphs.DirectedGraph;
import util.objects.graphs.UndirectedGraph;

import java.util.Arrays;

public class GraphVarFactory {

	//*************************************************************************************
	// GRAPH VARIABLES CREATION
	//*************************************************************************************

	public static IUndirectedGraphVar undirectedGraph(String NAME, UndirectedGraph LB, UndirectedGraph UB, Solver SOLVER) {
		return new UndirectedGraphVar(NAME, SOLVER, LB, UB);
	}

	public static IDirectedGraphVar directedGraph(String NAME, DirectedGraph LB, DirectedGraph UB, Solver SOLVER) {
		return new DirectedGraphVar(NAME, SOLVER, LB, UB);
	}

	//*************************************************************************************
	// OTHER
	//*************************************************************************************

	/**
	 * Iterate over the variable of <code>this</code> and build an array that contains the GraphVar only.
	 * It also contains FIXED variables and VIEWS, if any.
	 *
	 * @return array of SetVars of <code>this</code>
	 */
	public static IGraphVar[] retrieveGraphVars(Solver s) {
		int n = s.getNbVars();
		IGraphVar[] bvars = new IGraphVar[n];
		int k = 0;
		for (int i = 0; i < n; i++) {
			if ((s.getVar(i).getTypeAndKind() & Variable.KIND) == Variable.GRAPH) {
				bvars[k++] = (IGraphVar) s.getVar(i);
			}
		}
		return Arrays.copyOf(bvars, k);
	}
}
