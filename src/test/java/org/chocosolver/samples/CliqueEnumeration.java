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
import org.chocosolver.graphsolver.variables.IUndirectedGraphVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.SetType;

/**
 * This sample illustrates how to use a graph variable to
 * enumerate all cliques that respect certain conditions
 * In this example, we enumerates cliques which contain edge (1,2)
 * by using a graph variable and a clique partitioning constraint
 *
 * @author Jean-Guillaume Fages
 */
public class CliqueEnumeration {

	public static void main(String[] args) {
		// input data
		int n = 5;
		boolean[][] link = new boolean[n][n];
		link[1][2] = true;
		link[2][3] = true;
		link[2][4] = true;
		link[1][3] = true;
		link[1][4] = true;
		link[3][4] = true;

		GraphModel model = new GraphModel("clique enumeration");

		// graph variable domain
		UndirectedGraph GLB = new UndirectedGraph(model,n, SetType.BIPARTITESET,false);
		UndirectedGraph GUB = new UndirectedGraph(model,n, SetType.BITSET,false);
		for (int i = 0; i < n; i++) {
			GUB.addNode(i);			// potential node
			GUB.addEdge(i, i);				// potential loop
			for (int j = i + 1; j < n; j++) {
				if (link[i][j]) {
					GUB.addEdge(i, j);		// potential edge
				}
			}
		}
		GLB.addNode(1);				// 1 and 2 must belong to the solution
		GLB.addNode(2);
		GLB.addEdge(1,2);					// 1 and 2 must belong to the same clique
		// graph variable
		IUndirectedGraphVar graphvar = model.undirected_graph_var("G", GLB, GUB);

		final SetVar vertices = model.nodes_set(graphvar);
		final IntVar card = model.intVar(3);
		model.cardinality(vertices, card).post();

		// constraint : the graph must be a clique
		model.nb_cliques(graphvar, model.ONE()).post();

		// solution enumeration
		while (model.solve()){
			System.out.println("solution found : ");
			System.out.println(graphvar);
		}
	}
}
