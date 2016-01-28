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

package org.chocosolver.solver.cstrs.inclusion;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.GraphEventType;
import org.chocosolver.solver.variables.IGraphVar;
import org.chocosolver.solver.variables.delta.IGraphDeltaMonitor;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.procedure.IntProcedure;
import org.chocosolver.util.procedure.PairProcedure;

/**
 * @author Jean-Guillaume Fages
 */
public class PropInclusion extends Propagator<IGraphVar> {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private IGraphVar[] g; // g[1] in g[2]
	private IGraphDeltaMonitor[] gdm;
	private IntProcedure[] prNode;
	private PairProcedure[] prArc;
	private GraphEventType[] etNode,etArcs;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropInclusion(IGraphVar g1, IGraphVar g2){
		super(new IGraphVar[]{g1,g2}, PropagatorPriority.LINEAR,true);
		g = new IGraphVar[]{g1,g2};
		gdm = new IGraphDeltaMonitor[]{g1.monitorDelta(this),g2.monitorDelta(this)};
		prNode = new IntProcedure[]{
				i -> g[1].enforceNode(i,this),
				i -> g[0].removeNode(i,this)
		};
		prArc = new PairProcedure[]{
				(i, j) -> g[1].enforceArc(i,j,this),
				(i, j) -> g[0].removeArc(i,j,this)
		};
		etNode = new GraphEventType[]{GraphEventType.ADD_NODE,GraphEventType.REMOVE_NODE};
		etArcs = new GraphEventType[]{GraphEventType.ADD_ARC,GraphEventType.REMOVE_ARC};
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		if(g[0].getNbMaxNodes() != g[1].getNbMaxNodes()){
			for(int i=g[1].getNbMaxNodes();i<g[0].getNbMaxNodes();i++){
				g[0].removeNode(i,this);
			}
		}
		ISet set = g[0].getMandatoryNodes();
		for(int i=set.getFirstElement();i>=0;i=set.getNextElement()){
			g[1].enforceNode(i,this);
			ISet suc = g[0].getMandSuccOrNeighOf(i);
			for(int j=suc.getFirstElement();j>=0;j=suc.getNextElement()){
				g[1].enforceArc(i,j,this);
			}
		}
		set = g[0].getPotentialNodes();
		for(int i=set.getFirstElement();i>=0;i=set.getNextElement()){
			if(!g[1].getPotentialNodes().contain(i)){
				g[0].removeNode(i, this);
			}else {
				ISet suc = g[0].getPotSuccOrNeighOf(i);
				for (int j = suc.getFirstElement(); j >= 0; j = suc.getNextElement()) {
					if(!g[1].getPotSuccOrNeighOf(i).contain(j)) {
						g[1].removeArc(i, j, this);
					}
				}
			}
		}
		gdm[0].unfreeze();
		gdm[1].unfreeze();
	}

	@Override
	public void propagate(int vIdx, int evtmask) throws ContradictionException {
		gdm[vIdx].freeze();
		gdm[vIdx].forEachNode(prNode[vIdx], etNode[vIdx]);
		gdm[vIdx].forEachArc(prArc[vIdx], etArcs[vIdx]);
		gdm[vIdx].unfreeze();
	}

	@Override
	public ESat isEntailed() {
		return null;
	}
}
