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

import gnu.trove.list.array.TIntArrayList;
import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.graphsolver.search.strategy.GraphSearch;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.limits.FailCounter;
import org.chocosolver.solver.search.loop.lns.neighbors.INeighbor;
import org.chocosolver.solver.search.strategy.decision.Decision;
import org.chocosolver.solver.search.strategy.decision.DecisionPath;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetType;

import java.util.Iterator;
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
public class TSP_lns {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private int bestSolutionValue = -1;
	private boolean opt = false;

	//***********************************************************************************
	// MAIN
	//***********************************************************************************

	public static void main(String[] args) {
		String REPO = "src/test/java/org/chocosolver/samples/tsp";
		String INSTANCE = "bier127";
		int[][] data = TSP_Utils.parseInstance(REPO + "/" + INSTANCE + ".tsp", 300);
		new TSP_lns(data);
	}

	//***********************************************************************************
	// CONSTRUCTOR
	//***********************************************************************************

	public TSP_lns(int[][] costMatrix){
		super();
		int LIMIT = 30; // in seconds

		final int n = costMatrix.length;
		// variables
		GraphModel model = new GraphModel();
		IntVar totalCost = model.intVar("obj", 0, 99999999, true);
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
		model.tsp(graph, totalCost, costMatrix, 2).post();

		// intuitive heuristic (cheapest edges first)
		final GraphSearch search = new GraphSearch(graph, costMatrix).configure(GraphSearch.MIN_COST);
		Solver solver = model.getSolver();
		solver.setSearch(search);
		solver.limitTime(LIMIT+"s");

		// LNS (relaxes consecutive edges)
		INeighbor LNS = new SubpathLNS(graph);
		solver.setLNS(LNS,new FailCounter(model,30));

		model.setObjective(Model.MINIMIZE, totalCost);

		while (solver.solve()){
			search.configure(GraphSearch.MIN_DELTA_DEGREE);
			System.out.println("solution found : " + totalCost);
			bestSolutionValue = totalCost.getValue();
		}

		if(solver.getTimeCount()<LIMIT){
			opt = true;
			System.out.println("Optimality proved with LNS");
		}else{
			opt = false;
			if(solver.getSolutionCount()>0) {
				System.out.println("Best solution found : " +solver.getBestSolutionValue()+" (but no optimality proof");
			}else{
				System.out.println("no solution found");
			}
		}
	}

	public int getBestSolutionValue() {
		return bestSolutionValue;
	}

	public boolean optimalityProved() {
		return opt;
	}

	//***********************************************************************************
	// LNS
	//***********************************************************************************

	/**
	 * Object describing which edges to freeze and which others to relax in the LNS
	 * Relaxes a (sub)path of the previous solution (freezes the rest)
	 */
	private class SubpathLNS implements INeighbor{

		Random rd = new Random(0);
		int n, nbRL;
		UndirectedGraph solution;
		int nbFreeEdges = 15;
		LNSDecision metaDec = new LNSDecision();
		UndirectedGraphVar graph;

		protected SubpathLNS(UndirectedGraphVar graph) {
			this.graph = graph;
			this.n = graph.getNbMaxNodes();
			this.solution = new UndirectedGraph(n,SetType.LINKED_LIST,true);
		}

		@Override
		public void init() {}

		@Override
		public void recordSolution() {
			// stores a solution in a graph object
			for(int i=0;i<n;i++)solution.getNeighOf(i).clear();
			for(int i=0;i<n;i++){
				ISet nei = graph.getMandNeighOf(i);
				for(int j:nei){
					solution.addEdge(i,j);
				}
			}
		}

		@Override
		public void fixSomeVariables(DecisionPath decisionPath) {
			metaDec.free();
			// relaxes a sub-path (a set of consecutive edges in a solution)
			int i1 = rd.nextInt(n);
			ISet nei = solution.getNeighOf(i1);
			Iterator<Integer> iter = nei.iterator();
			int i2 = iter.next();
			if(rd.nextBoolean()){
				i2 = iter.next();
			}
			for(int k=0;k<n-nbFreeEdges;k++){
				metaDec.add(i1,i2);
				int i3 = -1;
				for(int z : solution.getNeighOf(i2)) {
					if (z != i1) {
						i3 = z;
						break;
					}
				}
				assert i3>=0;
				i1 = i2;
				i2 = i3;
			}
			metaDec.setRefutable(false);
			decisionPath.pushDecision(metaDec);
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

		@Override
		public void loadFromSolution(Solution solution) {
			throw new UnsupportedOperationException("not implemented");
		}

		private class LNSDecision extends Decision{
			TIntArrayList from = new TIntArrayList();
			TIntArrayList to = new TIntArrayList();

			public LNSDecision() {
				super(1);
			}

			public void add(int i, int j){
				from.add(i);
				to.add(j);
			}

			@Override
			public void apply() throws ContradictionException {
				for(int k = 0;k<from.size();k++){
					graph.enforceArc(from.get(k),to.get(k),this);
				}
			}

			@Override
			public Object getDecisionValue() {
				return null;
			}

			@Override
			public void free() {
				from.clear();
				to.clear();
			}
		}
	}
}
