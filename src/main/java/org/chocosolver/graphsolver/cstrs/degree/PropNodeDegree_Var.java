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

package org.chocosolver.graphsolver.cstrs.degree;

import org.chocosolver.graphsolver.variables.DirectedGraphVar;
import org.chocosolver.graphsolver.variables.GraphVar;
import org.chocosolver.graphsolver.variables.IncidentSet;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.graphs.Orientation;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.tools.ArrayUtils;

import java.util.BitSet;

/**
 * Propagator that ensures that a node has at most N successors/predecessors/neighbors
 * ENSURES EVERY VERTEX i FOR WHICH DEGREE[i]>0 IS MANDATORY
 *
 * @author Jean-Guillaume Fages
 */
public class PropNodeDegree_Var extends Propagator<Variable> {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private int n;
	private GraphVar g;
	private IntVar[] degrees;
	private IncidentSet target;
	private BitSet toDo;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropNodeDegree_Var(DirectedGraphVar graph, Orientation setType, IntVar[] degrees) {
		super(ArrayUtils.append(degrees,new Variable[]{graph}), PropagatorPriority.BINARY, false);
		this.g = graph;
		this.n = g.getNbMaxNodes();
		this.degrees = degrees;
		if(setType == Orientation.PREDECESSORS){
			this.target = new IncidentSet.PredOrNeighSet();
		}else{
			this.target = new IncidentSet.SuccOrNeighSet();
		}
	}

	public PropNodeDegree_Var(UndirectedGraphVar graph, IntVar[] degrees) {
		super(ArrayUtils.append(degrees,new Variable[]{graph}), PropagatorPriority.BINARY, false);
		this.target = new IncidentSet.SuccOrNeighSet();
		this.g = graph;
		this.n = g.getNbMaxNodes();
		this.degrees = degrees;
		this.toDo = new BitSet(n);
	}

	//***********************************************************************************
	// PROPAGATIONS
	//***********************************************************************************

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		if(g.isDirected()){
			propagateDirected();
		}else{
			propagateUndirected();
		}
	}

	public void propagateDirected() throws ContradictionException {
		for(int i=0;i<n;i++){
			if(!g.getPotentialNodes().contains(i)){
				degrees[i].instantiateTo(0,this);
			}else if(degrees[i].getLB()>0){
				g.enforceNode(i,this);
			}
			ISet env = target.getPotSet(g, i);
			ISet ker = target.getMandSet(g, i);
			degrees[i].updateLowerBound(ker.size(),this);
			degrees[i].updateUpperBound(env.size(),this);
			if(ker.size() < env.size() && degrees[i].isInstantiated()){
				int d = degrees[i].getValue();
				if(env.size() == d){
					for (int s : env) {
						target.enforce(g, i, s, this);
					}
				}else if(ker.size() == d){
					for (int s : env) {
						if(!ker.contains(s)) {
							target.remove(g, i, s, this);
						}
					}
				}
			}
		}
	}

	public void propagateUndirected() throws ContradictionException {
		assert !g.isDirected();
		toDo.clear();
		for(int i=0;i<n;i++){
			toDo.set(i);
		}
		int i = toDo.nextSetBit(0);
		do{
			toDo.clear(i);
			if(!g.getPotentialNodes().contains(i)){
				degrees[i].instantiateTo(0,this);
			}else if(degrees[i].getLB()>0){
				g.enforceNode(i,this);
			}
			ISet env = target.getPotSet(g, i);
			ISet ker = target.getMandSet(g, i);
			degrees[i].updateLowerBound(ker.size(),this);
			degrees[i].updateUpperBound(env.size(),this);
			if(ker.size() < env.size() && degrees[i].isInstantiated()){
				int d = degrees[i].getValue();
				if(env.size() == d){
					for (int s : env) {
						if(target.enforce(g, i, s, this)){
							toDo.set(s);
						}
					}
				}else if(ker.size() == d){
					for (int s : env) {
						if(!ker.contains(s)) {
							if (target.remove(g, i, s, this)) {
								toDo.set(s);
							}
						}
					}
				}
			}
			i = toDo.nextSetBit(0);
		}while(i>=0);
	}

	//***********************************************************************************
	// INFO
	//***********************************************************************************

	@Override
	public ESat isEntailed() {
		boolean done = true;
		for(int i=0;i<n;i++){
			if((!degrees[i].contains(0)) && !g.getPotentialNodes().contains(i)){
				return ESat.FALSE;
			}
			ISet env = target.getPotSet(g, i);
			ISet ker = target.getMandSet(g, i);
			if(degrees[i].getLB()>env.size()
					|| degrees[i].getUB()<ker.size()){
				return ESat.FALSE;
			}
			if(env.size() != ker.size() || !degrees[i].isInstantiated()){
				done = false;
			}
		}
		if (!done) {
			return ESat.UNDEFINED;
		}
		return ESat.TRUE;
	}
}
