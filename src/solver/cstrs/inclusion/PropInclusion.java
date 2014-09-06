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


/**
 * @author Jean-Guillaume Fages
 * @since 06/09/14
 * Created by IntelliJ IDEA.
 */
package solver.cstrs.inclusion;

import solver.constraints.Propagator;
import solver.constraints.PropagatorPriority;
import solver.exception.ContradictionException;
import solver.variables.EventType;
import solver.variables.IGraphVar;
import solver.variables.delta.IGraphDeltaMonitor;
import util.ESat;
import util.objects.setDataStructures.ISet;
import util.procedure.IntProcedure;
import util.procedure.PairProcedure;

public class PropInclusion extends Propagator<IGraphVar> {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private IGraphVar[] g; // g[1] in g[2]
	private IGraphDeltaMonitor[] gdm;
	private IntProcedure[] prNode;
	private PairProcedure[] prArc;
	private EventType[] etNode,etArcs;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropInclusion(IGraphVar g1, IGraphVar g2){
		super(new IGraphVar[]{g1,g2}, PropagatorPriority.LINEAR,true);
		g = new IGraphVar[]{g1,g2};
		gdm = new IGraphDeltaMonitor[]{g1.monitorDelta(this),g2.monitorDelta(this)};
		prNode = new IntProcedure[]{
				new IntProcedure() {
					@Override
					public void execute(int i) throws ContradictionException {
						g[1].enforceNode(i,aCause);
					}
				},
				new IntProcedure() {
					@Override
					public void execute(int i) throws ContradictionException {
						g[0].removeNode(i,aCause);
					}
				}
		};
		prArc = new PairProcedure[]{
				new PairProcedure() {
					@Override
					public void execute(int i, int j) throws ContradictionException {
						g[1].enforceArc(i,j,aCause);
					}
				},
				new PairProcedure() {
					@Override
					public void execute(int i, int j) throws ContradictionException {
						g[0].removeArc(i,j,aCause);
					}
				}
		};
		etNode = new EventType[]{EventType.ENFORCENODE,EventType.REMOVENODE};
		etArcs = new EventType[]{EventType.ENFORCEARC,EventType.REMOVEARC};
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		if(g[0].getNbMaxNodes() != g[1].getNbMaxNodes()){
			for(int i=g[1].getNbMaxNodes();i<g[0].getNbMaxNodes();i++){
				g[0].removeNode(i,aCause);
			}
		}
		ISet set = g[0].getMandatoryNodes();
		for(int i=set.getFirstElement();i>=0;i=set.getNextElement()){
			g[1].enforceNode(i,aCause);
			ISet suc = g[0].getMandSuccOrNeighOf(i);
			for(int j=suc.getFirstElement();j>=0;j=suc.getNextElement()){
				g[1].enforceArc(i,j,aCause);
			}
		}
		set = g[0].getPotentialNodes();
		for(int i=set.getFirstElement();i>=0;i=set.getNextElement()){
			if(!g[1].getPotentialNodes().contain(i)){
				g[0].removeNode(i, aCause);
			}else {
				ISet suc = g[0].getPotSuccOrNeighOf(i);
				for (int j = suc.getFirstElement(); j >= 0; j = suc.getNextElement()) {
					if(!g[1].getPotSuccOrNeighOf(i).contain(j)) {
						g[1].removeArc(i, j, aCause);
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
