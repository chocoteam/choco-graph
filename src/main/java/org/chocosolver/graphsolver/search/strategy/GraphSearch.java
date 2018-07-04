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

/**
 * Created by IntelliJ IDEA.
 * User: Jean-Guillaume Fages
 * Date: 08/08/12
 * Time: 15:27
 */

package org.chocosolver.graphsolver.search.strategy;

import org.chocosolver.graphsolver.search.GraphAssignment;
import org.chocosolver.graphsolver.search.GraphDecision;
import org.chocosolver.graphsolver.variables.GraphVar;
import org.chocosolver.util.objects.setDataStructures.ISet;

public class GraphSearch extends GraphStrategy {


	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	// heuristics
	public static final int LEX = 0;
	public static final int MIN_P_DEGREE = 1;
	public static final int MAX_P_DEGREE = 2;
	public static final int MIN_M_DEGREE = 3;
	public static final int MAX_M_DEGREE = 4;
	public static final int MIN_DELTA_DEGREE = 5;
	public static final int MAX_DELTA_DEGREE = 6;
	public static final int MIN_COST = 7;
	public static final int MAX_COST = 8;

	// variables
	private int n;
	private int mode;
	private int[][] costs;
	private GraphAssignment decisionType;
	private int from, to;
	private int value;
	private boolean useLC;
	private int lastFrom = -1;

	/**
	 * Search strategy for graphs
	 *
	 * @param graphVar varriable to branch on
	 */
	public GraphSearch(GraphVar graphVar) {
		this(graphVar, null);
	}

	/**
	 * Search strategy for graphs
	 *
	 * @param graphVar   varriable to branch on
	 * @param costMatrix can be null
	 */
	public GraphSearch(GraphVar graphVar, int[][] costMatrix) {
		super(graphVar, null, null, NodeArcPriority.ARCS);
		costs = costMatrix;
		n = g.getNbMaxNodes();
	}

	/**
	 * Configures the search
	 *
	 * @param policy way to select arcs
	 */
	public GraphSearch configure(int policy) {
		return configure(policy, true);
	}

	/**
	 * Configures the search
	 *
	 * @param policy  way to select arcs
	 * @param enforce true if a decision is an arc enforcing
	 *                false if a decision is an arc removal
	 */
	public GraphSearch configure(int policy, boolean enforce) {
		if (enforce) {
			decisionType = GraphAssignment.graph_enforcer;
		} else {
			decisionType = GraphAssignment.graph_remover;
		}
		mode = policy;
		return this;
	}

	public GraphSearch useLastConflict() {
		useLC = true;
		return this;
	}

	@Override
	public GraphDecision getDecision() {
		if (g.isInstantiated()) {
			return null;
		}
		GraphDecision dec = pool.getE();
		if (dec == null) {
			dec = new GraphDecision(pool);
		}
		computeNextArc();
		dec.setArc(g, from, to, decisionType);
		lastFrom = from;
		return dec;
	}

	private void computeNextArc() {
		to = -1;
		from = -1;
		if (useLC && lastFrom != -1) {
			evaluateNeighbors(lastFrom);
			if (to != -1) {
				return;
			}
		}
		for (int i = 0; i < n; i++) {
			if (evaluateNeighbors(i)) {
				return;
			}
		}
		if (to == -1) {
			throw new UnsupportedOperationException();
		}
	}

	private boolean evaluateNeighbors(int i) {
		ISet set = g.getPotSuccOrNeighOf(i);
		if (set.size() == g.getMandSuccOrNeighOf(i).size()) {
			return false;
		}
		for (int j : set) {
			if (!g.getMandSuccOrNeighOf(i).contains(j)) {
				int v = -1;
				switch (mode) {
					case LEX:
						from = i;
						to = j;
						return true;
					case MIN_P_DEGREE:
					case MAX_P_DEGREE:
						v = g.getPotSuccOrNeighOf(i).size()
								+ g.getPotPredOrNeighOf(j).size();
						break;
					case MIN_M_DEGREE:
					case MAX_M_DEGREE:
						v = g.getMandSuccOrNeighOf(i).size()
								+ g.getMandPredOrNeighOf(j).size();
						break;
					case MIN_DELTA_DEGREE:
					case MAX_DELTA_DEGREE:
						v = g.getPotSuccOrNeighOf(i).size()
								+ g.getPotPredOrNeighOf(j).size()
								- g.getMandSuccOrNeighOf(i).size()
								- g.getMandPredOrNeighOf(j).size();
						break;
					case MIN_COST:
					case MAX_COST:
						v = costs[i][j];
						break;
					default:
						throw new UnsupportedOperationException("mode " + mode + " does not exist");
				}
				if (select(v)) {
					value = v;
					from = i;
					to = j;
				}
			}
		}
		return false;
	}

	private boolean select(double v) {
		return (from == -1 || (v < value && isMinOrIn(mode)) || (v > value && !isMinOrIn(mode)));
	}

	private static boolean isMinOrIn(int policy) {
		return (policy % 2 == 1);
	}
}
