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

package org.chocosolver.samples.tsp;

import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.graphsolver.search.strategy.GraphSearch;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;

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
public class TSP_exact {

    //***********************************************************************************
    // MAIN
    //***********************************************************************************

    public static void main(String[] args) {
		String REPO = "src/test/java/org/chocosolver/samples/tsp";
		String INSTANCE = "bier127";
		int[][] data = TSP_Utils.parseInstance(REPO+"/"+INSTANCE+".tsp", 300);
		int presolve = TSP_Utils.getOptimum(INSTANCE,REPO+"/bestSols.csv");
        new TSP_exact(data,presolve);
    }

    //***********************************************************************************
    // SOLVER
    //***********************************************************************************

	public TSP_exact(int[][] costMatrix, int initialUB){
		super();
        final int n = costMatrix.length;
		int LIMIT = 30; // in seconds

		GraphModel model = new GraphModel();
        // variables
		IntVar totalCost = model.intVar("obj", 0, initialUB, true);
        // creates a graph containing n nodes
		UndirectedGraph GLB = new UndirectedGraph(model, n, SetType.LINKED_LIST, true);
		UndirectedGraph GUB = new UndirectedGraph(model, n, SetType.BIPARTITESET, true);
        // adds potential edges
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                GUB.addEdge(i, j);
            }
        }
		UndirectedGraphVar graph = model.graphVar("G", GLB, GUB);

        // constraints (TSP basic model + lagrangian relaxation)
		model.tsp(graph, totalCost, costMatrix, 1).post();


		Solver solver = model.getSolver();
		// Fail first principle (requires a very good initial upper bound)
		solver.setSearch(new GraphSearch(graph, costMatrix).configure(GraphSearch.MAX_COST).useLastConflict());
		solver.limitTime(LIMIT+"s");

		model.setObjective(Model.MINIMIZE,totalCost);
		while (solver.solve()){
			System.out.println("solution found : " + totalCost);
		}
		if(solver.getTimeCount()<LIMIT){
			System.out.println("Optimality proved with exact CP approach");
		}else{
			if(solver.getSolutionCount()>0) {
				System.out.println("Best solution found : " + solver.getBestSolutionValue() + " (but no optimality proof");
			}else{
				System.out.println("no solution found");
			}
		}
    }
}
