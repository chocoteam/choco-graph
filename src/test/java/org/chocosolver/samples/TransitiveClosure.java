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
import org.chocosolver.graphsolver.cstrs.basic.PropNbArcs;
import org.chocosolver.graphsolver.cstrs.basic.PropTransitivity;
import org.chocosolver.graphsolver.search.strategy.GraphStrategy;
import org.chocosolver.graphsolver.variables.DirectedGraphVar;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.graphs.DirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;

/**
 * Computes the transitive closure (TC) of a given directed graphs G
 * by finding the smallest (w.r.t. the number of arcs) transitive supergraph of G
 * @author Jean-Guillaume Fages
 * @since 30/07/13
 */
public class TransitiveClosure {

	public static void main(String[] args){
		// input graph
		int n = 5;
		GraphModel model = new GraphModel();
		DirectedGraph input = new DirectedGraph(n, SetType.BITSET,true);
		input.addArc(0,1);
		input.addArc(1,2);
		input.addArc(2,4);
		input.addArc(3,4);
		input.addArc(4,3);

		// VARIABLE COUNTING THE NUMBER OF ARCS IN THE TC
		IntVar nbArcs = model.intVar("edgeCount", 0, n * n, true);
		// TRANSITIVE CLOSURE VARIABLE : initial domain
		DirectedGraph GLB = new DirectedGraph(model, n, SetType.BITSET, true);
		DirectedGraph GUB = new DirectedGraph(model, n, SetType.BITSET, true);
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
		DirectedGraphVar tc = model.digraphVar("transitive closure", GLB, GUB);

		// CONSTRAINTS
		new Constraint("Graph_TC",new PropTransitivity(tc),new PropNbArcs(tc,nbArcs)).post();
		// tries to find the smallest graph first
		model.getSolver().setSearch(Search.inputOrderLBSearch(nbArcs), new GraphStrategy(tc));

		model.setObjective(Model.MINIMIZE, nbArcs);

		while (model.getSolver().solve()){
			System.out.println("new solution found : "+nbArcs);
			System.out.println(tc);
		}
	}
}
