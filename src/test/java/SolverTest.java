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

package test.java;

import org.testng.Assert;
import org.testng.annotations.Test;
import solver.Solver;
import solver.variables.*;

/**
 * <br/>
 *
 * @author Charles Prud'homme
 * @since 23 juil. 2010
 */
public class SolverTest {

    @Test(groups = "1s")
    public void testFH1() {
        Solver solver = new Solver();
        BoolVar b = VF.bool("b", solver);
        IntVar i = VF.bounded("i", VF.MIN_INT_BOUND, VF.MAX_INT_BOUND, solver);
        SetVar s = VF.set("s", 2, 3, solver);
        RealVar r = VF.real("r", 1.0, 2.2, 0.01, solver);
		IGraphVar g = GraphVarFactory.directedGraph("g", null, null, solver);


        BoolVar[] bvars = solver.retrieveBoolVars();
        Assert.assertEquals(bvars, new BoolVar[]{solver.ZERO, solver.ONE, b});

        IntVar[] ivars = solver.retrieveIntVars();
        Assert.assertEquals(ivars, new IntVar[]{i});

        SetVar[] svars = solver.retrieveSetVars();
        Assert.assertEquals(svars, new SetVar[]{s});

        RealVar[] rvars = solver.retrieveRealVars();
        Assert.assertEquals(rvars, new RealVar[]{r});

		IGraphVar[] gvars = GraphVarFactory.retrieveGraphVars(solver);
        Assert.assertEquals(gvars, new IGraphVar[]{g});
    }

}
