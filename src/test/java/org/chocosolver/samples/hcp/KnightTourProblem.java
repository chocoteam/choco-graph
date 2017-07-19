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
import org.chocosolver.graphsolver.search.strategy.ArcStrategy;
import org.chocosolver.graphsolver.search.strategy.GraphStrategy;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.Solver;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetType;

/**
 * Solves the Knight's Tour Problem
 * <p/>
 * Uses graph variables (light data structure)
 * better with -Xms1048m -Xmx2048m for memory allocation
 * when solving large instances
 *
 *
 * @author Jean-Guillaume Fages
 * @since Oct. 2012
 */
public class KnightTourProblem {

	public static void main(String[] args) {
		boolean[][] matrix;
		boolean closedTour = true; //Open tour (path instead of cycle)
		int boardLength = 8;
		// This generates the boolean incidence matrix of the chessboard graph
		// It is responsible of the high memory consumption of this example
		// and could be replaced by lighter data structure
		if (closedTour) {
			matrix = HCP_Utils.generateKingTourInstance(boardLength);
		} else {
			matrix = HCP_Utils.generateOpenKingTourInstance(boardLength);
		}
		GraphModel model = new GraphModel("solving the knight's tour problem with a graph variable");
		// variables
		int n = matrix.length;
		// graph representing mandatory nodes and edges
		// (linked list data structure as the expected solution is expected to be sparse,
		// every vertex in [0,n-1] is mandatory)
		UndirectedGraph GLB = new UndirectedGraph(model,n,SetType.LINKED_LIST,true);
		// graph representing potential nodes and edges
		// (linked list data structure as its initial value is sparse,
		UndirectedGraph GUB = new UndirectedGraph(model,n,SetType.LINKED_LIST,true);
		for (int i = 0; i < n; i++) {
			for (int j = i + 1; j < n; j++) {
				if (matrix[i][j]) { // adds possible edge
					GUB.addEdge(i, j);
				}
			}
		}
		// creates the graph variable
		UndirectedGraphVar graph = model.graphVar("G", GLB, GUB);

		// hamiltonian cycle constraint
		model.hamiltonianCycle(graph).post();

		// basically branch on sparse areas of the graph
		Solver solver = model.getSolver();
		solver.setSearch(new GraphStrategy(graph, null, new MinNeigh(graph), GraphStrategy.NodeArcPriority.ARCS));
		solver.limitTime("20s");

		solver.solve();
		solver.printStatistics();
	}

	//***********************************************************************************
	// HEURISTICS
	//***********************************************************************************

	private static class MinNeigh extends ArcStrategy<UndirectedGraphVar> {
		int n;

		public MinNeigh(UndirectedGraphVar graphVar) {
			super(graphVar);
			n = graphVar.getNbMaxNodes();
		}

		@Override
		public boolean computeNextArc() {
			ISet suc;
			int size = n + 1;
			int sizi;
			from = -1;
			for (int i = 0; i < n; i++) {
				sizi = g.getPotNeighOf(i).size() - g.getMandNeighOf(i).size();
				if (sizi < size && sizi > 0) {
					from = i;
					size = sizi;
				}
			}
			if (from == -1) {
				return false;
			}
			suc = g.getPotNeighOf(from);
			for (int j : suc) {
				if (!g.getMandNeighOf(from).contains(j)) {
					to = j;
					return true;
				}
			}
			throw new UnsupportedOperationException();
		}
	}
}
