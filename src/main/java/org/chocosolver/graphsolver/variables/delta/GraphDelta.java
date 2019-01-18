/**
 * Copyright (c) 1999-2014, Ecole des Mines de Nantes
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

package org.chocosolver.graphsolver.variables.delta;

import org.chocosolver.memory.IEnvironment;
import org.chocosolver.solver.ICause;
import org.chocosolver.solver.search.loop.TimeStampedObject;
import org.chocosolver.solver.variables.delta.EnumDelta;
import org.chocosolver.solver.variables.delta.IDelta;
import org.chocosolver.solver.variables.delta.IEnumDelta;

public class GraphDelta extends TimeStampedObject implements IDelta {

	//NR NE AR AE : NodeRemoved NodeEnforced ArcRemoved ArcEnforced
	public final static int NR = 0;
	public final static int NE = 1;
	public final static int AR_TAIL = 2;
	public final static int AR_HEAD = 3;
	public final static int AE_TAIL = 4;
	public final static int AE_HEAD = 5;
	public final static int NB = 6;

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private IEnumDelta[] deltaOfType;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public GraphDelta(IEnvironment environment) {
		super(environment);
		deltaOfType = new IEnumDelta[NB];
		for (int i = 0; i < NB; i++) {
			deltaOfType[i] = new EnumDelta(environment);
		}
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	public int getSize(int i) {
		return deltaOfType[i].size();
	}

	public void add(int element, int type, ICause cause) {
		lazyClear();
		deltaOfType[type].add(element, cause);
	}

	public void lazyClear() {
		if (needReset()) {
			for (int i = 0; i < NB; i++) {
				deltaOfType[i].lazyClear();
			}
			resetStamp();
		}
	}

	public int get(int index, int type) {
		return deltaOfType[type].get(index);
	}

	public ICause getCause(int index, int type) {
		return deltaOfType[type].getCause(index);
	}
}
