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

package org.chocosolver.graphsolver.cstrs.basic;

import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.constraints.nary.nValue.amnv.mis.F;
import org.chocosolver.solver.constraints.nary.nValue.amnv.mis.MDRk;
import org.chocosolver.solver.constraints.nary.nValue.amnv.rules.R;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetType;

import java.util.BitSet;

/**
 * Propagator for the number of cliques in a graph
 *
 * @author Jean-Guillaume Fages
 */
public class PropNbCliques extends Propagator<Variable> {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private UndirectedGraphVar g;
	private UndirectedGraph support;
	private IntVar[] nb;
	private R[] rules;
	private F heur;
	private int delta;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropNbCliques(UndirectedGraphVar g, IntVar nb){
		super(new Variable[]{g,nb}, PropagatorPriority.QUADRATIC,false);
		this.g = g;
		this.support = new UndirectedGraph(g.getNbMaxNodes(), SetType.BITSET,false);
		this.nb = new IntVar[]{nb};
		this.rules = new R[]{new Rcustom()};
		this.heur = new MDRk(support,30);
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		// reset
		int n = g.getNbMaxNodes();
		support.getNodes().clear();
		for(int i=0;i<n;i++){
			support.getNeighOf(i).clear();
		}
		ISet nodes = g.getMandatoryNodes();
		for(int i: nodes){
			support.addNode(i);
		}
		for(int i: nodes){
			ISet nei = g.getPotNeighOf(i);
			for(int j:nei){
				if(i<j){
					support.addEdge(i,j);
				}
			}
		}
		delta = n-g.getMandatoryNodes().size();
		// algorithm
		heur.prepare();
		do{
			heur.computeMIS();
			for(R rule:rules){
				rule.filter(nb,support,heur,this);
			}
		}while(heur.hasNextMIS());
	}

	@Override
	public ESat isEntailed() {
		return ESat.TRUE; // redundant propagator (in addition to transitivity and nbConnectedComponents
	}

	class Rcustom implements R{
		@Override
		public void filter(IntVar[] nbCliques, UndirectedGraph graph, F heur, Propagator aCause) throws ContradictionException{
			assert nbCliques.length == 1;
			int n = graph.getNbMaxNodes();
			BitSet mis = heur.getMIS();
			int LB = heur.getMIS().cardinality()-delta;
			nbCliques[0].updateLowerBound(LB, aCause);
			if(LB==nbCliques[0].getUB()){
				ISet nei;
				for (int i = mis.nextClearBit(0); i>=0 && i < n; i = mis.nextClearBit(i + 1)) {
					int mate = -1;
					int last = 0;
					nei = graph.getNeighOf(i);
					for (int j : nei) {
						if (mis.get(j)) {
							if (mate == -1) {
								mate = j;
							} else if (mate >= 0) {
								mate = -2;
							}
							if (mate == -2 && last == 0) break;
						}
					}
					if (mate >= 0) {
						g.enforceArc(i, mate, aCause);
					}
				}
			}
		}
	}
}
