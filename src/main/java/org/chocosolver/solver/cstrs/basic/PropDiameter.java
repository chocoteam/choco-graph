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

package org.chocosolver.solver.cstrs.basic;

import gnu.trove.list.array.TIntArrayList;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IGraphVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.setDataStructures.ISet;

import java.util.BitSet;

/**
 * Propagator for the diameter constraint
 *
 * @author Jean-Guillaume Fages
 */
public class PropDiameter extends Propagator<IGraphVar> {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private IGraphVar g;
	private IntVar diameter;
	private int n;
	private BitSet visited;
	private TIntArrayList set, nextSet;


	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropDiameter(IGraphVar graph, IntVar maxDiam) {
		super(new IGraphVar[]{graph}, PropagatorPriority.LINEAR, false);
		this.g = graph;
		this.diameter = maxDiam;
		n = g.getNbMaxNodes();
		visited = new BitSet(n);
		set = new TIntArrayList();
		nextSet = new TIntArrayList();
	}

	//***********************************************************************************
	// PROPAGATIONS
	//***********************************************************************************

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		int max = -1;
		for (int i = g.getPotentialNodes().getFirstElement(); i >= 0; i = g.getPotentialNodes().getNextElement()) {
			if(g.getMandatoryNodes().contain(i)) {
				diameter.updateLowerBound(BFS(i, true), this);
			}
			max = Math.max(max,BFS(i,false));
		}
		diameter.updateUpperBound(max,this);
	}

	private int BFS(int i, boolean min) {
		nextSet.clear();
		set.clear();
		visited.clear();
		set.add(i);
		visited.set(i);
		ISet nei;
		int depth = 0;
		int nbMand = g.getMandatoryNodes().getSize();
		int count = 1;
		while (!set.isEmpty()) {
			for (i = set.size() - 1; i >= 0; i--) {
				nei = g.getPotSuccOrNeighOf(set.get(i));
				for (int j = nei.getFirstElement(); j >= 0; j = nei.getNextElement()) {
					if (!visited.get(j)) {
						visited.set(j);
						nextSet.add(j);
						if(min) {
							if (g.getMandatoryNodes().contain(j)) {
								count++;
								if (count == nbMand) {
									return depth + 1;
								}
							}
						}
					}
				}
			}
			depth++;
			TIntArrayList tmp = nextSet;
			nextSet = set;
			set = tmp;
			nextSet.clear();
		}
		return depth;
	}

	//***********************************************************************************
	// INFO
	//***********************************************************************************

	@Override
	public ESat isEntailed() {
		throw new UnsupportedOperationException("isEntail() not implemented yet");
	}
}
