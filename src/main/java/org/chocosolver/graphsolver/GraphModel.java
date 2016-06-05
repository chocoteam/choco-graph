/**
 * Copyright (c) 1999-2011, Ecole des Mines de Nantes
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the Ecole des Mines de Nantes nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p>
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

package org.chocosolver.graphsolver;

import org.chocosolver.graphsolver.cstrs.IGraphConstraintFactory;
import org.chocosolver.graphsolver.search.GraphStrategyFactory;
import org.chocosolver.graphsolver.variables.GraphVar;
import org.chocosolver.graphsolver.variables.IGraphVarFactory;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Settings;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.variables.Variable;

import java.util.ArrayList;

/**
 * An extension of Model that handles graph variables
 * @author Jean-Guillaume Fages
 */
public class GraphModel extends Model implements IGraphVarFactory, IGraphConstraintFactory {

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	/**
	 * Creates a model to handle graph variables with Choco Solver
	 *
	 * @param name label of the model
	 */
	public GraphModel(String name) {
		super(name);
		set(new Settings() {
			@Override
			public AbstractStrategy makeDefaultSearch(Model model) {
				// overrides default search strategy to handle graph vars
				AbstractStrategy other = Search.defaultSearch(model);
				GraphVar[] gvs = retrieveGraphVars();
				if(gvs.length==0){
					return other;
				}else{
					AbstractStrategy[] gss = new AbstractStrategy[gvs.length+1];
					for(int i=0; i<gvs.length; i++){
						gss[i] = GraphStrategyFactory.inputOrder(gvs[i]);
					}
					gss[gvs.length] = other;
					return Search.sequencer(gss);
				}
			}
		});
	}

	/**
	 * Creates a model to handle graph variables with Choco Solver
	 */
	public GraphModel() {
		this("Graph Model");
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	/**
	 * Iterate over the variable of <code>this</code> and build an array that contains the GraphVar only.
	 * @return array of GraphVar in <code>this</code> model
	 */
	public GraphVar[] retrieveGraphVars() {
		ArrayList<GraphVar> gvars = new ArrayList<>();
		for (Variable v:getVars()) {
			if ((v.getTypeAndKind() & Variable.KIND) == GraphVar.GRAPH) {
				gvars.add((GraphVar) v);
			}
		}
		return gvars.toArray(new GraphVar[gvars.size()]);
	}

	@Override
	public GraphModel _me() {
		return this;
	}
}
