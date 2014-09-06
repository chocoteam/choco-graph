/**
 *  Copyright (c) 1999-2011, Ecole des Mines de Nantes
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
import solver.constraints.Constraint;
import solver.cstrs.basic.PropKArcs;
import solver.cstrs.basic.PropTransitivity;
import solver.search.GraphStrategyFactory;
import solver.search.strategy.ISF;
import solver.variables.*;
import util.objects.graphs.DirectedGraph;
import util.objects.setDataStructures.SetType;

/**
 * Computes the transitive closure (TC) of a given directed graphs G
 * by finding the smallest (w.r.t. the number of arcs) transitive supergraph of G
 * @author Jean-Guillaume Fages
 * @since 30/07/13
 */
public class TransitiveClosure extends AbstractProblem{

	IDirectedGraphVar tc;
	IntVar nbArcs;

	@Override
	public void createSolver() {
		solver = new Solver("transitive closure sample");
	}

	@Override
	public void buildModel() {
		// input graph
		int n = 5;
		DirectedGraph input = new DirectedGraph(n, SetType.BITSET,true);
		input.addArc(0,1);
		input.addArc(1,2);
		input.addArc(2,0);
		input.addArc(3,4);
		input.addArc(4,3);

		// VARIABLE COUNTING THE NUMBER OF ARCS IN THE TC
		nbArcs = VF.bounded("edgeCount", 0, n * n, solver);
		// TRANSITIVE CLOSURE VARIABLE : initial domain
		DirectedGraph GLB = new DirectedGraph(solver, n, SetType.BITSET, true);
		DirectedGraph GUB = new DirectedGraph(solver, n, SetType.BITSET, true);
		for (int i = 0; i < n; i++) {
			GUB.addArc(i, i);			// potential loop
			GLB.addArc(i, i);			// mandatory loop
			for (int j = 0; j < n; j++) {
				GUB.addArc(i, j);		// potential edge
				if (input.arcExists(i,j)) {
					GLB.addArc(i, j);	// mandatory edge
				}
			}
		}
		tc = GraphVarFactory.directedGraph("transitive closure", GLB, GUB, solver);

		// CONSTRAINTS
		solver.post(new Constraint("Graph_TC",new PropTransitivity(tc),new PropKArcs(tc,nbArcs)));
	}

	@Override
	public void configureSearch() {
		// tries to find the smallest graph first
		solver.set(ISF.lexico_LB(new IntVar[]{nbArcs}),
				GraphStrategyFactory.lexico(tc));
	}

	@Override
	public void solve() {
		solver.findOptimalSolution(ResolutionPolicy.MINIMIZE,nbArcs);
	}

	@Override
	public void prettyOut() {}

	public static void main(String[] args){
		new TransitiveClosure().execute(args);
	}
}
