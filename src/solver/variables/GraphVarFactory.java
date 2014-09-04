package solver.variables;

import solver.Solver;
import solver.exception.ContradictionException;
import solver.explanations.Deduction;
import solver.explanations.Explanation;
import util.objects.graphs.DirectedGraph;
import util.objects.graphs.UndirectedGraph;

import java.util.Arrays;

public class GraphVarFactory {

	//*************************************************************************************
	// GRAPH VARIABLES CREATION
	//*************************************************************************************

	/**
	 * Create an undirected graph variable named NAME
	 * and whose domain is the graph interval [LB,UB]
	 * BEWARE: LB and UB graphs must be backtrackable
	 * (use the solver as an argument in their constructor)!
	 *
	 * @param NAME		Name of the variable
	 * @param LB		Undirected graph representing mandatory nodes and edges
	 * @param UB		Undirected graph representing possible nodes and edges
	 * @param SOLVER	Solver of the variable
	 * @return	An undirected graph variable
	 */
	public static IUndirectedGraphVar undirectedGraph(String NAME, UndirectedGraph LB, UndirectedGraph UB, Solver SOLVER) {
		return new UndirectedGraphVar(NAME, SOLVER, LB, UB);
	}


	/**
	 * Create a directed graph variable named NAME
	 * and whose domain is the graph interval [LB,UB]
	 * BEWARE: LB and UB graphs must be backtrackable
	 * (use the solver as an argument in their constructor)!
	 *
	 * @param NAME		Name of the variable
	 * @param LB		Directed graph representing mandatory nodes and edges
	 * @param UB		Directed graph representing possible nodes and edges
	 * @param SOLVER	Solver of the variable
	 * @return	An undirected graph variable
	 */
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
