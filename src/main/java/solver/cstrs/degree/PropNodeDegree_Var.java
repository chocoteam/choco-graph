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

package solver.cstrs.degree;

import solver.constraints.Propagator;
import solver.constraints.PropagatorPriority;
import solver.variables.IncidentSet;
import solver.exception.ContradictionException;
import solver.variables.*;
import util.ESat;
import util.objects.graphs.Orientation;
import util.objects.setDataStructures.ISet;
import util.tools.ArrayUtils;

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
	private IGraphVar g;
	private IntVar[] degrees;
	private IncidentSet target;
	private BitSet toDo;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropNodeDegree_Var(IDirectedGraphVar graph, Orientation setType, IntVar[] degrees) {
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

	public PropNodeDegree_Var(IUndirectedGraphVar graph, IntVar[] degrees) {
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
			if(!g.getPotentialNodes().contain(i)){
				degrees[i].instantiateTo(0,aCause);
			}else if(degrees[i].getLB()>0){
				g.enforceNode(i,aCause);
			}
			ISet env = target.getPotSet(g, i);
			ISet ker = target.getMandSet(g, i);
			degrees[i].updateLowerBound(ker.getSize(),aCause);
			degrees[i].updateUpperBound(env.getSize(),aCause);
			if(ker.getSize() < env.getSize() && degrees[i].isInstantiated()){
				int d = degrees[i].getValue();
				if(env.getSize() == d){
					for (int s = env.getFirstElement(); s >= 0; s = env.getNextElement()) {
						target.enforce(g, i, s, aCause);
					}
				}else if(ker.getSize() == d){
					for (int s = env.getFirstElement(); s >= 0; s = env.getNextElement()) {
						if(!ker.contain(s)) {
							target.remove(g, i, s, aCause);
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
			if(!g.getPotentialNodes().contain(i)){
				degrees[i].instantiateTo(0,aCause);
			}else if(degrees[i].getLB()>0){
				g.enforceNode(i,aCause);
			}
			ISet env = target.getPotSet(g, i);
			ISet ker = target.getMandSet(g, i);
			degrees[i].updateLowerBound(ker.getSize(),aCause);
			degrees[i].updateUpperBound(env.getSize(),aCause);
			if(ker.getSize() < env.getSize() && degrees[i].isInstantiated()){
				int d = degrees[i].getValue();
				if(env.getSize() == d){
					for (int s = env.getFirstElement(); s >= 0; s = env.getNextElement()) {
						if(target.enforce(g, i, s, aCause)){
							toDo.set(s);
						}
					}
				}else if(ker.getSize() == d){
					for (int s = env.getFirstElement(); s >= 0; s = env.getNextElement()) {
						if(!ker.contain(s)) {
							if (target.remove(g, i, s, aCause)) {
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
			if((!degrees[i].contains(0)) && !g.getPotentialNodes().contain(i)){
				return ESat.FALSE;
			}
			ISet env = target.getPotSet(g, i);
			ISet ker = target.getMandSet(g, i);
			if(degrees[i].getLB()>env.getSize()
					|| degrees[i].getUB()<ker.getSize()){
				return ESat.FALSE;
			}
			if(env.getSize() != ker.getSize() || !degrees[i].isInstantiated()){
				done = false;
			}
		}
		if (!done) {
			return ESat.UNDEFINED;
		}
		return ESat.TRUE;
	}
}