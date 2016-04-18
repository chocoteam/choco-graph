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

package org.chocosolver.samples;

import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.solver.ResolutionPolicy;
import org.chocosolver.graphsolver.variables.IDirectedGraphVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.graphs.DirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;

public class DAGProblem {

	public static void main(String[] args){
		// input graph
		int n = 5;
		// VARIABLE COUNTING THE NUMBER OF ARCS
		GraphModel model = new GraphModel();
		IntVar nbArcs = model.intVar("arcCount", 0, n * n, true);
		// GRAPH VARIABLE : initial domain (every node belongs to the solution)
		DirectedGraph GLB = new DirectedGraph(model, n, SetType.BITSET, true);
		DirectedGraph GUB = new DirectedGraph(model, n, SetType.BITSET, true);
		GLB.addArc(0,1); // some arbitrary mandatory arcs
		GLB.addArc(1,2);
		GLB.addArc(3,1);
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				GUB.addArc(i, j);		// potential edge
			}
		}

		IDirectedGraphVar dag = model.digraphVar("dag", GLB, GUB);

		// CONSTRAINTS
		model.no_circuit(dag).post();
		model.nb_arcs(dag, nbArcs).post();

		model.setObjective(ResolutionPolicy.MAXIMIZE,nbArcs);
		while (model.solve()){
			System.out.println("new solution found : "+nbArcs);
			System.out.println(dag);
		}
	}
}
