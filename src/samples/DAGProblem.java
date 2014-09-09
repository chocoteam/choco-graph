/**
 *  Copyright (c) 1999-2014, Ecole des Mines de Nantes
 *  All rights reserved.
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *      * Neither the name of the Ecole des Mines de Nantes nor the
 *        names of its contributors may be used to endorse or promote products
 *        derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package samples;

import solver.ResolutionPolicy;
import solver.Solver;
import solver.cstrs.GraphConstraintFactory;
import solver.search.GraphStrategyFactory;
import solver.variables.GraphVarFactory;
import solver.variables.IDirectedGraphVar;
import solver.variables.IntVar;
import solver.variables.VF;
import util.objects.graphs.DirectedGraph;
import util.objects.setDataStructures.SetType;

public class DAGProblem extends AbstractProblem{

	IDirectedGraphVar dag;
	IntVar nbArcs;

	@Override
	public void createSolver() {
		solver = new Solver("DAG sample");
	}

	@Override
	public void buildModel() {
		// input graph
		int n = 5;

		// VARIABLE COUNTING THE NUMBER OF ARCS
		nbArcs = VF.bounded("arcCount", 0, n * n, solver);
		// GRAPH VARIABLE : initial domain (every node belongs to the solution)
		DirectedGraph GLB = new DirectedGraph(solver, n, SetType.BITSET, true);
		DirectedGraph GUB = new DirectedGraph(solver, n, SetType.BITSET, true);
		GLB.addArc(0,1); // some arbitrary mandatory arcs
		GLB.addArc(1,2);
		GLB.addArc(3,1);
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				GUB.addArc(i, j);		// potential edge
			}
		}

		dag = GraphVarFactory.directedGraph("dag", GLB, GUB, solver);

		// CONSTRAINTS
		solver.post(GraphConstraintFactory.no_circuit(dag));
		solver.post(GraphConstraintFactory.nb_arcs(dag, nbArcs));
	}

	@Override
	public void configureSearch() {
		// tries to find the largest graph first
		solver.set(GraphStrategyFactory.lexico(dag));
	}

	@Override
	public void solve() {
		solver.findOptimalSolution(ResolutionPolicy.MAXIMIZE,nbArcs);
	}

	@Override
	public void prettyOut() {}

	public static void main(String[] args){
		new DAGProblem().execute(args);
	}
}
