/*
 * Copyright (c) 1999-2012, Ecole des Mines de Nantes
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

package solver.variables;

import solver.ICause;
import solver.Solver;
import solver.exception.ContradictionException;
import solver.explanations.Explanation;
import solver.explanations.VariableState;
import solver.variables.delta.GraphDelta;
import solver.variables.delta.GraphDeltaMonitor;
import solver.variables.delta.IGraphDelta;
import solver.variables.delta.IGraphDeltaMonitor;
import solver.variables.impl.AbstractVariable;
import solver.variables.view.IView;
import util.objects.graphs.IGraph;
import util.objects.setDataStructures.ISet;

/**
 * Created by IntelliJ IDEA.
 * User: chameau, Jean-Guillaume Fages
 * Date: 7 feb. 2011
 */
public abstract class SetBasedGraphVar<E extends IGraph> extends AbstractVariable implements IGraphVar<E>, IView{

	//////////////////////////////// GRAPH PART /////////////////////////////////////////
	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	protected SetVar nodes;
	protected SetVar[] succs, preds;
	protected IGraphDelta delta;
	protected int n;
	///////////// Attributes related to Variable ////////////
	protected boolean reactOnModification;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	/**
	 * Creates a graph variable
	 *
	 * @param solver
	 */
	public SetBasedGraphVar(String name, Solver solver, SetVar nodes, SetVar[] succs, SetVar[] preds) {
		super(name, solver);
		this.nodes = nodes;
		this.succs = succs;
		this.preds = preds;
		this.n = succs.length;
		assert n==preds.length;
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	@Override
	public boolean isInstantiated() {
		int idx = 0;
		boolean inst = nodes.isInstantiated();
		while(idx<n && inst){
			inst &= (succs[idx].isInstantiated() && preds[idx].isInstantiated());
			idx++;
		}
		return inst;
	}

	/**
	 * Remove node x from the maximal partial subgraph
	 *
	 * @param x     node's index
	 * @param cause algorithm which is related to the removal
	 * @return true iff the removal has an effect
	 */
	public boolean removeNode(int x, ICause cause) throws ContradictionException {
		assert cause != null;
		assert (x>=0 && x<getEnvelopGraph().getNbNodes());
		boolean hasChanged = false;
		while(succs[x].getEnvelopeSize()>0){
			hasChanged = true;
			succs[x].removeFromEnvelope(succs[x].getEnvelopeFirst(),this);
		}
		while(preds[x].getEnvelopeSize()>0){
			hasChanged = true;
			preds[x].removeFromEnvelope(preds[x].getEnvelopeFirst(),this);
		}
		hasChanged |= nodes.removeFromEnvelope(x,this);
		return hasChanged;
	}

	/**
	 * Enforce the node x to belong to any partial subgraph
	 *
	 * @param x     node's index
	 * @param cause algorithm which is related to the modification
	 * @return true iff the node is effectively added to the mandatory structure
	 */
	public boolean enforceNode(int x, ICause cause) throws ContradictionException {
		assert cause != null;
		assert (x>=0 && x<getEnvelopGraph().getNbNodes());
		return nodes.addToKernel(x, this);
	}

	/**
	 * Remove node y from the neighborhood of node x from the maximal partial subgraph
	 *
	 * @param x     node's index
	 * @param y     node's index
	 * @param cause algorithm which is related to the removal
	 * @return true iff the removal has an effect
	 * @throws solver.exception.ContradictionException
	 */
	public boolean removeArc(int x, int y, ICause cause) throws ContradictionException{
		return succs[x].removeFromEnvelope(y,this);
	}

	/**
	 * Enforce the node y into the neighborhood of node x in any partial subgraph
	 *
	 * @param x     node's index
	 * @param y     node's index
	 * @param cause algorithm which is related to the removal
	 * @return true iff the node y is effectively added in the neighborhooh of node x
	 */
	public boolean enforceArc(int x, int y, ICause cause) throws ContradictionException{
		boolean hasChanged = false;
		hasChanged |= nodes.addToKernel(x,this);
		hasChanged |= nodes.addToKernel(y,this);
		hasChanged |= succs[x].addToKernel(y,this);
		return hasChanged;
	}

	//***********************************************************************************
	// ACCESSORS
	//***********************************************************************************

	/**
	 * @return the graph representing the domain of the variable graph
	 */
	public E getKernelGraph() {
		return kernel;
	}

	/**
	 * @return the graph representing the instantiated values (nodes and edges) of the variable graph
	 */
	public E getEnvelopGraph() {
		return envelop;
	}

	@Override
	public ISet getMandSuccOrNeighOf(int idx){
		return kernel.getSuccsOrNeigh(idx);
	}

	@Override
	public ISet getPotSuccOrNeighOf(int idx){
		return envelop.getSuccsOrNeigh(idx);
	}

	@Override
	public ISet getMandPredOrNeighOf(int idx){
		return kernel.getPredsOrNeigh(idx);
	}

	@Override
	public ISet getPotPredOrNeighOf(int idx){
		return envelop.getPredsOrNeigh(idx);
	}

	/**
	 * @return true iff the graph is directed
	 */
	public abstract boolean isDirected();

	@Override
	public boolean isExplicit(){
		return false;
	}

	//***********************************************************************************
	// VARIABLE STUFF
	//***********************************************************************************

	@Override
	public void explain(VariableState what, Explanation to) {
		throw new UnsupportedOperationException("GraphVar does not (yet) implement method explain(...)");
	}

	@Override
	public void explain(VariableState what, int val, Explanation to) {
		throw new UnsupportedOperationException("GraphVar does not (yet) implement method explain(...)");
	}

	@Override
	public IGraphDelta getDelta() {
		return delta;
	}

	@Override
	public int getTypeAndKind() {
		return VIEW | GRAPH;
	}

	@Override
	public SetBasedGraphVar duplicate() {
		throw new UnsupportedOperationException("Cannot duplicate GraphVar");
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("graph_var " + getName());
		if(isInstantiated()){
			sb.append("\nnodes: \n");
			sb.append(nodes.toString());
		}else{
			for(int i=0;i<n;i++) {
				sb.append("\nSuccOrNeighOf node "+i+": \n");
				sb.append(succs[i].toString());
			}
		}
		return sb.toString();
	}

	@Override
	public void createDelta() {
		if (!reactOnModification) {
			reactOnModification = true;
			delta = new GraphDelta(solver.getSearchLoop());
		}
	}

	public IGraphDeltaMonitor monitorDelta(ICause propagator) {
		createDelta();
		return new GraphDeltaMonitor(delta, propagator);
	}

	@Override
	public void notifyMonitors(EventType event) throws ContradictionException {
		for (int i = mIdx - 1; i >= 0; i--) {
			monitors[i].onUpdate(this, event);
		}
	}

	@Override
	public void contradiction(ICause cause, EventType event, String message) throws ContradictionException {
		assert cause != null;
		solver.getEngine().fails(cause, this, message);
	}

	//***********************************************************************************
	// SOLUTIONS : STORE AND RESTORE
	//***********************************************************************************

	/**
	 * @return the value of the graph variable represented through an adjacency matrix
	 *         plus a set of nodes (last line of value).
	 *         This method is not supposed to be used except for restoring solutions.
	 */
	public boolean[][] getValue() {
		int n = getEnvelopGraph().getNbNodes();
		boolean[][] vals = new boolean[n + 1][n];
		ISet kerNodes = getKernelGraph().getActiveNodes();
		ISet kerSuccs;
		for (int i = kerNodes.getFirstElement(); i >= 0; i = kerNodes.getNextElement()) {
			kerSuccs = getKernelGraph().getSuccsOrNeigh(i);
			for (int j = kerSuccs.getFirstElement(); j >= 0; j = kerSuccs.getNextElement()) {
				vals[i][j] = true; // arc in
			}
			vals[n][i] = true; // node in
		}
		return vals;
	}

	/**
	 * Instantiates <code>this</code> to value which represents an adjacency
	 * matrix plus a set of nodes (last line of value).
	 * This method is not supposed to be used except for restoring solutions.
	 *
	 * @param value value of <code>this</code>
	 * @param cause
	 * @throws solver.exception.ContradictionException
	 */
	public void instantiateTo(boolean[][] value, ICause cause) throws ContradictionException {
		int n = value.length - 1;
		for (int i = 0; i < n; i++) {
			if (value[n][i]) {//nodes
				enforceNode(i, cause);
			} else {
				removeNode(i, cause);
			}
			for (int j = 0; j < n; j++) {
				if (value[i][j]) {//arcs
					enforceArc(i, j, cause);
				} else {
					removeArc(i, j, cause);
				}
			}
		}
	}
}
