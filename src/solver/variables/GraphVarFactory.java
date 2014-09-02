package solver.variables;

import solver.Solver;

import java.util.Arrays;

public class GraphVarFactory {

	//*************************************************************************************
	// GRAPH VARIABLES
	//*************************************************************************************

	/**
	 * Builds a non-directed graph variable with an empty domain
	 * but allocates memory to deal with at most NB_NODES nodes.
	 * <p/>
	 * The domain of a graph variable is defined by two graphs:
	 * <p/> The envelope graph denotes nodes and edges that may belong to a solution
	 * <p/> The kernel graph denotes nodes and edges that must belong to any solution
	 *
	 * @param NAME     name of the variable
	 * @param NB_NODES maximal number of nodes
	 * @param SOLVER   solver involving the variable
	 * @return a graph variable with an empty domain
	 */
	public static UndirectedGraphVar undirectedGraph(String NAME, int NB_NODES, Solver SOLVER) {
		return new UndirectedGraphVar(NAME, SOLVER, NB_NODES, false);
	}

	/**
	 * Builds a directed graph variable with an empty domain
	 * but allocates memory to deal with at most NB_NODES nodes.
	 * <p/>
	 * The domain of a graph variable is defined by two graphs:
	 * <p/> The envelope graph denotes nodes and arcs that may belong to a solution
	 * <p/> The kernel graph denotes nodes and arcs that must belong to any solution
	 *
	 * @param NAME     name of the variable
	 * @param NB_NODES maximal number of nodes
	 * @param SOLVER   solver involving the variable
	 * @return a graph variable with an empty domain
	 */
	public static DirectedGraphVar directedGraph(String NAME, int NB_NODES, Solver SOLVER) {
		return new DirectedGraphVar(NAME, SOLVER, NB_NODES, false);
	}

	/**
	 * Iterate over the variable of <code>this</code> and build an array that contains the GraphVar only.
	 * It also contains FIXED variables and VIEWS, if any.
	 *
	 * @return array of SetVars of <code>this</code>
	 */
	public static GraphVar[] retrieveGraphVars(Solver s) {
		int n = s.getNbVars();
		GraphVar[] bvars = new GraphVar[n];
		int k = 0;
		for (int i = 0; i < n; i++) {
			if ((s.getVar(i).getTypeAndKind() & Variable.KIND) == Variable.GRAPH) {
				bvars[k++] = (GraphVar) s.getVar(i);
			}
		}
		return Arrays.copyOf(bvars, k);
	}
}
