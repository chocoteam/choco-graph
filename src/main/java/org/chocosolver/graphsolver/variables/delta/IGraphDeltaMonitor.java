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
package org.chocosolver.graphsolver.variables.delta;

import org.chocosolver.graphsolver.variables.GraphEventType;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.delta.IDeltaMonitor;
import org.chocosolver.util.procedure.IntProcedure;
import org.chocosolver.util.procedure.PairProcedure;

public interface IGraphDeltaMonitor extends IDeltaMonitor {

	/**
	 * Applies proc to every vertex which has just been removed or enforced, depending on evt.
	 * @param proc    an incremental procedure over vertices
	 * @param evt    either ENFORCENODE or REMOVENODE
	 * @throws ContradictionException if a failure occurs
	 */
    void forEachNode(IntProcedure proc, GraphEventType evt) throws ContradictionException;

	/**
	 * Applies proc to every arc which has just been removed or enforced, depending on evt.
	 * @param proc    an incremental procedure over arcs
	 * @param evt    either ENFORCEARC or REMOVEARC
	 * @throws ContradictionException if a failure occurs
	 */
    void forEachArc(PairProcedure proc, GraphEventType evt) throws ContradictionException;
}
