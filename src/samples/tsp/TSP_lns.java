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
import solver.ICause;
import solver.ResolutionPolicy;
import solver.Solver;
import solver.cstrs.GraphConstraintFactory;
import solver.exception.ContradictionException;
import solver.search.limits.FailCounter;
import solver.search.loop.lns.LargeNeighborhoodSearch;
import solver.search.loop.lns.neighbors.ANeighbor;
import solver.search.loop.lns.neighbors.INeighbor;
import solver.search.loop.monitors.IMonitorSolution;
import solver.search.loop.monitors.SearchMonitorFactory;
import solver.search.strategy.GraphStrategies;
import solver.variables.GraphVarFactory;
import solver.variables.IUndirectedGraphVar;
import solver.variables.IntVar;
import solver.variables.VariableFactory;
import util.objects.graphs.UndirectedGraph;
import util.objects.setDataStructures.ISet;
import util.objects.setDataStructures.SetType;

import java.util.Random;

/**
 * LNS approach to solve the Traveling Salesman Problem
 * Parses TSP instances of the TSPLIB library
 * See <a href = "http://comopt.ifi.uni-heidelberg.de/software/TSPLIB95/">TSPLIB</a>
 * <p/>
 *
 * @author Jean-Guillaume Fages
 * @since Oct. 2012
 */
public class TSP_lns extends AbstractProblem {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	public final static String REPO = "src/samples/tsp";
	public final static String INSTANCE = "bier127";
	public final static int MAX_SIZE = 300;
	public final static int LIMIT = 30; // in seconds

	private int[][] costMatrix; // input cost matrix
	private IUndirectedGraphVar graph; // graph variable representing the cycle
	private IntVar totalCost; // integer variable representing the objective

	//***********************************************************************************
	// MAIN
	//***********************************************************************************

	public static void main(String[] args) {
		int[][] data = TSP_Utils.parseInstance(REPO + "/" + INSTANCE + ".tsp", MAX_SIZE);
		TSP_lns lns = new TSP_lns(data);
		lns.execute();
	}

	//***********************************************************************************
	// CONSTRUCTOR
	//***********************************************************************************

	public TSP_lns(int[][] instance){
		super();
		this.costMatrix = instance;
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
		totalCost = VariableFactory.bounded("obj", 0, 99999999, solver);
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
		solver.post(GraphConstraintFactory.tsp(graph, totalCost, costMatrix, 2));
	}

	@Override
	public void configureSearch() {
		// intuitive heuristic (cheapest edges first)
		final GraphStrategies search = new GraphStrategies(graph, costMatrix);
		search.configure(GraphStrategies.MIN_COST, true);
		solver.set(search);
		SearchMonitorFactory.limitTime(solver, LIMIT+"s");

		// LNS (relaxes consecutive edges)
		INeighbor LNS = new SubpathLNS(graph.getNbMaxNodes(),solver);
		LNS.fastRestart(new FailCounter(30)); // restarts every 30 fails
		solver.plugMonitor(new LargeNeighborhoodSearch(solver,LNS,false));

		// log
		solver.plugMonitor(new IMonitorSolution() {
			@Override
			public void onSolution() {
				search.configure(GraphStrategies.MIN_DELTA_DEGREE, true);
				System.out.println("solution found : " + totalCost);
			}
		});
	}

	@Override
	public void solve() {
		solver.findOptimalSolution(ResolutionPolicy.MINIMIZE, totalCost);
		if(solver.getMeasures().getTimeCount()<LIMIT){
			System.out.println("Optimality proved with LNS");
		}else{
			if(solver.getMeasures().getSolutionCount()>0) {
				System.out.println("Best solution found : " + solver.getMeasures().getBestSolutionValue() +
						" (but no optimality proof");
			}else{
				System.out.println("no solution found");
			}
		}
	}

	@Override
	public void prettyOut() {}

	//***********************************************************************************
	// LNS
	//***********************************************************************************

	/**
	 * Object describing which edges to freeze and which others to relax in the LNS
	 * Relaxes a (sub)path of the previous solution (freezes the rest)
	 */
	private class SubpathLNS extends ANeighbor{

		Random rd = new Random(0);
		int n, nbRL;
		UndirectedGraph solution;
		int nbFreeEdges = 15;

		protected SubpathLNS(int n, Solver mSolver) {
			super(mSolver);
			this.n = n;
			this.solution = new UndirectedGraph(n,SetType.LINKED_LIST,true);
		}

		@Override
		public void recordSolution() {
			// stores a solution in a graph object
			for(int i=0;i<n;i++)solution.getNeighOf(i).clear();
			for(int i=0;i<n;i++){
				ISet nei = graph.getMandNeighOf(i);
				for(int j=nei.getFirstElement();j>=0;j=nei.getNextElement()){
					solution.addEdge(i,j);
				}
			}
		}

		@Override
		public void fixSomeVariables(ICause cause) throws ContradictionException {
			// relaxes a sub-path (a set of consecutive edges in a solution)
			int i1 = rd.nextInt(n);
			ISet nei = solution.getNeighOf(i1);
			int i2 = nei.getFirstElement();
			if(rd.nextBoolean()){
				i2 = nei.getNextElement();
			}
			for(int k=0;k<n-nbFreeEdges;k++){
				graph.enforceArc(i1,i2,cause);
				int i3 = solution.getNeighOf(i2).getFirstElement();
				if(i3==i1){
					i3 = solution.getNeighOf(i2).getNextElement();
				}
				i1 = i2;
				i2 = i3;
			}
		}

		@Override
		public void restrictLess() {
			nbRL++;
			// Eventually increases the size of the relaxes fragment (not necessary)
			if(nbRL>nbFreeEdges){
				nbRL = 0;
				nbFreeEdges += (nbFreeEdges*3)/2;
			}
		}

		@Override
		public boolean isSearchComplete() {
			return nbFreeEdges>=n;
		}
	}
}