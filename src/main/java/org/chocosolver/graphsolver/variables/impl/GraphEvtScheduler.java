/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2020, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.graphsolver.variables.impl;

import org.chocosolver.graphsolver.variables.GraphEventType;
import org.chocosolver.util.iterators.EvtScheduler;

/**
 * <br/>
 *
 * @author Charles Prud'homme
 * @since 26/11/2020
 */
public class GraphEvtScheduler implements EvtScheduler<GraphEventType> {

    private static final int[] DIS = new int[]{0, 1, -1, // ANY EVT
    };
    private int i = 0;

    @Override
    public void init(int mask) {
        i = 0;
    }

    @Override
    public int select(int mask) {
        if (mask > 0) {
            return 0;
        } else {
            return 1;
        }
    }

    @Override
    public boolean hasNext() {
        return DIS[i] > -1;
    }

    @Override
    public int next() {
        return DIS[i++];
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
