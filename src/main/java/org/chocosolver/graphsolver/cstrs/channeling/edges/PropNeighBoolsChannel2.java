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

package org.chocosolver.graphsolver.cstrs.channeling.edges;

import org.chocosolver.graphsolver.variables.IGraphVar;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.util.ESat;
import org.chocosolver.util.tools.ArrayUtils;

/**
 * @author Jean-Guillaume Fages
 */
public class PropNeighBoolsChannel2 extends Propagator<BoolVar> {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    private int n;
    private BoolVar[][] matrix;
    private IGraphVar g;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    public PropNeighBoolsChannel2(BoolVar[][] adjacencyMatrix, IGraphVar gV) {
        super(ArrayUtils.flatten(adjacencyMatrix), PropagatorPriority.LINEAR, false);
        this.matrix = adjacencyMatrix;
        n = adjacencyMatrix.length;
		assert n == adjacencyMatrix[0].length;
        this.g = gV;
        assert (n == g.getNbMaxNodes());
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        for (int i = 0; i < n; i++) {
			for(int j=0;j<n;j++){
				if(matrix[i][j].getLB()==1){
					g.enforceArc(i,j,this);
				}else if(matrix[i][j].getUB()==0){
					g.removeArc(i,j,this);
				}
			}
        }
    }

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
		int i = idxVarInProp/n;
		int j = idxVarInProp%n;
		if(matrix[i][j].getLB()==1){
			g.enforceArc(i,j,this);
		}else{
			g.removeArc(i,j,this);
		}
    }

    @Override
    public ESat isEntailed() {
        for (int i = 0; i < n; i++) {
			for(int j=0;j<n;j++){
				if(matrix[i][j].getLB()==1 && !g.getPotSuccOrNeighOf(i).contains(j)){
					return ESat.FALSE;
				}else if(matrix[i][j].getUB()==0 && g.getMandSuccOrNeighOf(i).contains(j)){
					return ESat.FALSE;
				}
			}
        }
        if (isCompletelyInstantiated()) {
            return ESat.TRUE;
        }
        return ESat.UNDEFINED;
    }
}
