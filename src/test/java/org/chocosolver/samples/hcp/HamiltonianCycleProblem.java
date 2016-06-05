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

package org.chocosolver.samples.hcp;

import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.graphsolver.search.GraphStrategyFactory;
import org.chocosolver.graphsolver.search.strategy.ArcStrategy;
import org.chocosolver.graphsolver.search.strategy.GraphStrategy;
import org.chocosolver.graphsolver.variables.IUndirectedGraphVar;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.limits.FailCounter;
import org.chocosolver.solver.search.restart.MonotonicRestartStrategy;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetType;

/**
 * Solves the Hamiltonian Cycle Problem
 * <p/>
 * Uses graph variables and a light but fast filtering
 * Parses HCP instances of the TSPLIB:
 * See <a href = "http://comopt.ifi.uni-heidelberg.de/software/TSPLIB95/">TSPLIB</a>
 *
 * @author Jean-Guillaume Fages
 * @since Oct. 2012
 */
public class HamiltonianCycleProblem {

	public static void main(String[] args) {
		// TSPLIB HCP Instance (see http://comopt.ifi.uni-heidelberg.de/software/TSPLIB95/)
		boolean[][] matrix = HCP_Utils.parseTSPLIBInstance("src/test/java/org/chocosolver/samples/hcp/alb1000.hcp");
		int n = matrix.length;

		GraphModel model = new GraphModel("solving the Hamiltonian Cycle Problem");
		// variables (use linked lists because the graph is sparse)
		UndirectedGraph GLB = new UndirectedGraph(model,n,SetType.LINKED_LIST,true);
		UndirectedGraph GUB = new UndirectedGraph(model,n,SetType.LINKED_LIST,true);
		for (int i = 0; i < n; i++) {
			for (int j = i + 1; j < n; j++) {
				if (matrix[i][j]) {
					GUB.addEdge(i, j);
				}
			}
		}
		IUndirectedGraphVar graph = model.graphVar("G", GLB, GUB);
		// constraints
		model.hamiltonianCycle(graph).post();


		Solver solver = model.getSolver();
		solver.setSearch(GraphStrategyFactory.graphStrategy(graph, null,
				new ArcStrategy<IUndirectedGraphVar>(graph){
					@Override
					public boolean computeNextArc() {
						ISet suc;
						to = -1;
						int size = 2*n+2;
						for (int i = 0; i < n; i++) {
							suc = g.getPotNeighOf(i);
							int deltai = g.getPotNeighOf(i).getSize() - g.getMandNeighOf(i).getSize();
							for (int j : suc) {
								if(!g.getMandNeighOf(i).contain(j)){
									int deltaj = g.getPotNeighOf(i).getSize() - g.getMandNeighOf(i).getSize();
									if (deltai+deltaj < size && deltai+deltaj > 0) {
										from = i;
										to = j;
										size = deltai+deltaj;
									}
								}
							}
						}
						return to != -1;
					}
				}, GraphStrategy.NodeArcPriority.ARCS));
		solver.limitTime("10s");
		solver.showStatistics();
		// restart search every 100 fails
		solver.setRestarts(new FailCounter(model,100), new MonotonicRestartStrategy(100), 1000);

		model.getSolver().solve();
	}
}
