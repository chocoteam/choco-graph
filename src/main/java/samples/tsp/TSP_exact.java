/*
 * Copyright (c) 1999-2014, Ecole des Mines de Nantes
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Ecole des Mines de Nantes nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package samples.tsp;

import samples.AbstractProblem;
import solver.ResolutionPolicy;
import solver.Solver;
import solver.cstrs.GraphConstraintFactory;
import solver.search.loop.monitors.IMonitorSolution;
import solver.search.loop.monitors.SearchMonitorFactory;
import solver.search.strategy.GraphStrategies;
import solver.variables.GraphVarFactory;
import solver.variables.IntVar;
import solver.variables.VariableFactory;
import solver.variables.IUndirectedGraphVar;
import util.objects.graphs.UndirectedGraph;
import util.objects.setDataStructures.SetType;

/**
 * Solves the Traveling Salesman Problem
 * Parses TSP instances of the TSPLIB library
 * See <a href = "http://comopt.ifi.uni-heidelberg.de/software/TSPLIB95/">TSPLIB</a>
 * <p/>
 *
 * This is an exact approach dedicated to prove optimality of a solution.
 * It is assumed that a local search (e.g. LKH) algorithm has been performed
 * as a pre-processing step
 *
 * @author Jean-Guillaume Fages
 * @since Oct. 2012
 */
public class TSP_exact extends AbstractProblem {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

	public final static String REPO = "src/main/java/samples/tsp";
	public final static String INSTANCE = "bier127";
	public final static int MAX_SIZE = 300;
	public final static int LIMIT = 30; // in seconds

	private int initialUB;	// initial upper bound
    private int[][] costMatrix; // input cost matrix
    private IUndirectedGraphVar graph; // graph variable representing the cycle
    private IntVar totalCost; // integer variable representing the objective

    //***********************************************************************************
    // MAIN
    //***********************************************************************************

    public static void main(String[] args) {
		int[][] data = TSP_Utils.parseInstance(REPO+"/"+INSTANCE+".tsp", MAX_SIZE);
		int presolve = TSP_Utils.getOptimum(INSTANCE,REPO+"/bestSols.csv");
        new TSP_exact(data,presolve).execute(args);
    }

    //***********************************************************************************
    // CONSTRUCTOR
    //***********************************************************************************

	public TSP_exact(int[][] instance, int upperBound){
		super();
		this.costMatrix = instance;
		this.initialUB = upperBound;
	}

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    @Override
    public void createSolver() {
        this.solver = new Solver("solving the Traveling Salesman Problem");
		this.level = Level.QUIET;
    }

    @Override
    public void buildModel() {
        final int n = costMatrix.length;
        solver = new Solver();
        // variables
        totalCost = VariableFactory.bounded("obj", 0, initialUB, solver);
        // creates a graph containing n nodes
		UndirectedGraph GLB = new UndirectedGraph(solver, n, SetType.LINKED_LIST, true);
		UndirectedGraph GUB = new UndirectedGraph(solver, n, SetType.BIPARTITESET, true);
        // adds potential edges
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                GUB.addEdge(i, j);
            }
        }
        graph = GraphVarFactory.undirected_graph_var("G", GLB, GUB, solver);

        // constraints (TSP basic model + lagrangian relaxation)
		solver.post(GraphConstraintFactory.tsp(graph, totalCost, costMatrix, 1));
    }

    @Override
    public void configureSearch() {
		// Fail first principle (requires a very good initial upper bound)
		GraphStrategies strategy = new GraphStrategies(graph, costMatrix);
		strategy.configure(GraphStrategies.MAX_COST, true);
		strategy.useLastConflict();
		solver.set(strategy);
        SearchMonitorFactory.limitTime(solver, LIMIT+"s");
		solver.plugMonitor((IMonitorSolution) () -> System.out.println("solution found : " + totalCost));
    }

    @Override
    public void solve() {
        solver.findOptimalSolution(ResolutionPolicy.MINIMIZE, totalCost);
		if(solver.getMeasures().getTimeCount()<LIMIT){
			System.out.println("Optimality proved with exact CP approach");
		}else{
			if(solver.getMeasures().getSolutionCount()>0) {
				System.out.println("Best solution found : " + solver.getMeasures().getBestSolutionValue() + " (but no optimality proof");
			}else{
				System.out.println("no solution found");
			}
		}
    }

    @Override
    public void prettyOut() {}
}