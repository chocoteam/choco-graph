package samples.dcmstp;
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

import samples.AbstractProblem;
import solver.ResolutionPolicy;
import solver.Solver;
import solver.constraints.Constraint;
import solver.cstrs.GraphConstraintFactory;
import solver.cstrs.cost.trees.PropMaxDegTree;
import solver.cstrs.cost.trees.PropTreeCostSimple;
import solver.cstrs.cost.trees.lagrangianRelaxation.PropLagr_DCMST;
import solver.objective.ObjectiveStrategy;
import solver.objective.OptimizationPolicy;
import solver.search.loop.monitors.IMonitorSolution;
import solver.search.loop.monitors.SearchMonitorFactory;
import solver.search.strategy.GraphStrategies;
import solver.variables.*;
import util.objects.graphs.UndirectedGraph;
import util.objects.setDataStructures.SetType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * Solves the Degree Constrained Minimum Spanning Tree Problem
 *
 * @author Jean-Guillaume Fages
 * @since Oct. 2012
 */
public class DCMST extends AbstractProblem {

	//***********************************************************************************
	// BENCHMARK
	//***********************************************************************************

	public static void main(String[] args) {
		String dir = "src/samples/dcmstp";
		String inst = "r123_100_1";
		new DCMST(new File(dir + "/" + inst)).execute();
	}

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	// input
	private int n;
	private int[] dMax;
	private int[][] dist;
	private int lb, ub;
	// model
	private IntVar totalCost;
	private IUndirectedGraphVar graph;
	// parameters
	public static long TIMELIMIT = 60000;

	//***********************************************************************************
	// CONSTRUCTOR
	//***********************************************************************************

	public DCMST(File input) {
		parse_T_DE_DR(input);
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	@Override
	public void createSolver() {
		this.level = Level.QUIET;
		solver = new Solver("DCMSTP");
	}

	@Override
	public void buildModel() {
		totalCost = VariableFactory.bounded("obj", lb, ub, solver);
		// graph var domain
		UndirectedGraph GLB = new UndirectedGraph(solver,n,SetType.LINKED_LIST,true);
		UndirectedGraph GUB = new UndirectedGraph(solver,n,SetType.SWAP_ARRAY,true);
		for (int i = 0; i < n; i++) {
			for (int j = i + 1; j < n; j++) {
				if (dist[i][j] != -1 && !(dMax[i] == 1 && dMax[j] == 1)) {
					GUB.addEdge(i, j); // possible edge
				}
			}
		}
		graph = GraphVarFactory.undirected_graph_var("G", GLB, GUB, solver);

		// tree constraint
		solver.post(GraphConstraintFactory.tree(graph));
		// max degree constraint
		solver.post(GraphConstraintFactory.max_degrees(graph,dMax));
		// redundant ad hoc propagator (filters connectivity + degrees)
		solver.post(new Constraint("Graph_deg", new PropMaxDegTree(graph, dMax)));
		// cost constraint
		solver.post(new Constraint("Graph_cost",
				new PropTreeCostSimple(graph, totalCost, dist)
				// (redundant) lagrangian relaxation based propagator
				,new PropLagr_DCMST(graph, totalCost, dMax, dist, true)
		));
	}

	@Override
	public void configureSearch() {
		final GraphStrategies mainSearch = new GraphStrategies(graph, dist);
		// find the first solution by selecting cheap edges
		mainSearch.configure(GraphStrategies.MIN_COST, true);
		// then select the most expensive ones (fail first principle, with last conflict)
		solver.plugMonitor(new IMonitorSolution() {
			@Override
			public void onSolution() {
				mainSearch.useLastConflict();
				mainSearch.configure(GraphStrategies.MAX_COST, true);
			}
		});
		// bottom-up optimization : find a first solution then reach the global minimum from below
		solver.set(new ObjectiveStrategy(totalCost, OptimizationPolicy.BOTTOM_UP), mainSearch);
		SearchMonitorFactory.limitSolution(solver, 2); // therefore there is at most two solutions
		SearchMonitorFactory.limitTime(solver, TIMELIMIT); // time limit
	}

	@Override
	public void solve() {
		// find optimum
		solver.findOptimalSolution(ResolutionPolicy.MINIMIZE, totalCost);
		if (solver.getMeasures().getSolutionCount() == 0
				&& solver.getMeasures().getTimeCount() < TIMELIMIT) {
			throw new UnsupportedOperationException("Provided instances are feasible!");
		}
	}

	@Override
	public void prettyOut() {}

	//***********************************************************************************
	// PARSING
	//***********************************************************************************

	public boolean parse_T_DE_DR(File file) {
		try {
			BufferedReader buf = new BufferedReader(new FileReader(file));
			String line = buf.readLine();
			String[] numbers;
			n = Integer.parseInt(line);
			dist = new int[n][n];
			dMax = new int[n];
			for (int i = 0; i < n; i++) {
				line = buf.readLine();
				numbers = line.split(" ");
				if (Integer.parseInt(numbers[0]) != i + 1) {
					throw new UnsupportedOperationException();
				}
				dMax[i] = Integer.parseInt(numbers[1]);
				for (int j = 0; j < n; j++) {
					dist[i][j] = -1;
				}
			}
			line = buf.readLine();
			int from, to, cost;
			int min = 1000000;
			int max = 0;
			while (line != null) {
				numbers = line.split(" ");
				from = Integer.parseInt(numbers[0]) - 1;
				to = Integer.parseInt(numbers[1]) - 1;
				cost = Integer.parseInt(numbers[2]);
				min = Math.min(min, cost);
				max = Math.max(max, cost);
				if (dist[from][to] != -1) {
					throw new UnsupportedOperationException();
				}
				dist[from][to] = dist[to][from] = cost;
				line = buf.readLine();
			}
			lb = (n - 1) * min;
			ub = (n - 1) * max;
			//            setUB(dirOpt, s);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		throw new UnsupportedOperationException();
	}
}