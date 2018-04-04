package org.chocosolver.graphsolver.cstrs.connectivity;


import org.chocosolver.graphsolver.util.ConnectivityFinder;
import org.chocosolver.graphsolver.variables.GraphEventType;
import org.chocosolver.graphsolver.variables.UndirectedGraphVar;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.ESat;

import java.util.*;

/**
 * Propagator ensuring that the size of all the connected components (CC) of a graph variable g is bounded by
 * minNCC (c.f. http://www.emn.fr/x-info/sdemasse/gccat/sec2.2.2.4.html#uid940) and
 * maxNCC (c.f. http://www.emn.fr/x-info/sdemasse/gccat/sec2.2.2.4.html#uid922).
 */
public class PropSizeCC extends Propagator<Variable> {

    /* Variables */

    private UndirectedGraphVar g;
    private int minNCC, maxNCC;
    private ConnectivityFinder GlbCCFinder, GubCCFinder;

    /* Constructor */

    public PropSizeCC(UndirectedGraphVar graph, int minNCC, int maxNCC) {
        super(new Variable[] {graph}, PropagatorPriority.LINEAR, false);
        assert minNCC <= maxNCC;
        this.g = graph;
        this.minNCC = minNCC;
        this.maxNCC = maxNCC;
        this.GlbCCFinder = new ConnectivityFinder(g.getLB());
        this.GubCCFinder = new ConnectivityFinder(g.getUB());
    }

    /* Methods */

    @Override
    public int getPropagationConditions(int vIdx) {
        return GraphEventType.ALL_EVENTS;
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        // Trivial case: empty GUB => there will be no CC.
        if (g.getPotentialNodes().size() == 0) {
            return;
        }

        // Trivial case: minNCC > NbMaxNodes and |GLB| > 0 => fails.
        if (g.getNbMaxNodes() < minNCC && g.getMandatoryNodes().size() > 0) {
            this.fails();
        }

        // Find all GLB CC
        GlbCCFinder.findAllCC();
        int nbGlbCC = GlbCCFinder.getNBCC();

        // Find all GUB CC
        GubCCFinder.findAllCC();
        int nbGubCC = GubCCFinder.getNBCC();

        // Step 1: If the largest GUB CC has less nodes than minNCC and the GLB is not empty => fail.
        int largest = 0;
        for (int cc = 0; cc < nbGubCC; cc++) {
            Set<Integer> currentCCNodes = getGubCCNodes(cc);
            int currentSize = currentCCNodes.size();
            if (currentSize < minNCC) {
                for (int i : currentCCNodes) {
                    g.removeNode(i, this);
                }
            }
            if (currentSize > largest) {
                largest = currentSize;
            }
        }
        if (largest < minNCC && nbGlbCC > 0) {
            fails();
        }

        // Step 2: for each CC of GLB:
        //     - 1. If |CC| < minNCC and CC is isolated in GUB: fail.
        //     - 2. If |CC| < minNCC and CC has only 1 outgoing edge in GUB: force this edge.
        //     - 3. If |CC| > maxNCC: fail.
        //     - 4. If |CC| = maxNCC: forbid every outgoing edge in GUB.
        for (int cc = 0; cc < nbGlbCC; cc++) {
            Set<Integer> ccNodes = getGlbCCNodes(cc);
            if (ccNodes.size() < minNCC) {
                Map<Integer, Set<Integer>> ccPotentialNeighbors = getGlbCCPotentialNeighbors(cc);
                // Case 1
                if (ccPotentialNeighbors.size() == 0) {
                    fails();
                }
                // Case 2
                if (ccPotentialNeighbors.size() == 1) {
                    int i = (int) ccPotentialNeighbors.keySet().toArray()[0];
                    Set<Integer> outNeighbors = ccPotentialNeighbors.get(i);
                    if (outNeighbors.size() == 1) {
                        int j = (int) outNeighbors.toArray()[0];
                        g.enforceArc(i, j, this);
                        //propagate(0);  // I need clarification on this!
                        //break;
                    }
                }
            }
            // Case 3
            if (ccNodes.size() > maxNCC) {
                fails();
            }
            // Case 4
            if (ccNodes.size() == maxNCC) {
                Map<Integer, Set<Integer>> ccPotentialNeighbors = getGlbCCPotentialNeighbors(cc);
                for (int i : ccPotentialNeighbors.keySet()) {
                    for (int j : ccPotentialNeighbors.get(i)) {
                        g.removeArc(i, j, this);
                    }
                }
            }
        }
    }

    /**
     * Retrieve the nodes of a GUB CC.
     * @param cc The GUB CC index.
     * @return The set of nodes of the GUB CC cc.
     */
    private Set<Integer> getGubCCNodes(int cc) {
        Set<Integer> ccNodes = new HashSet<>();
        for (int i = GubCCFinder.getCC_firstNode()[cc]; i >= 0; i = GubCCFinder.getCC_nextNode()[i]) {
            ccNodes.add(i);
        }
        return ccNodes;
    }

    /**
     * Retrieve the nodes of a GLB CC.
     * @param cc The GLB CC index.
     * @return The set of nodes of the GLB CC cc.
     */
    private Set<Integer> getGlbCCNodes(int cc) {
        Set<Integer> ccNodes = new HashSet<>();
        for (int i = GlbCCFinder.getCC_firstNode()[cc]; i >= 0; i = GlbCCFinder.getCC_nextNode()[i]) {
            ccNodes.add(i);
        }
        return ccNodes;
    }

    /**
     * Retrieve the potential CC neighbors (i.e. in GUB) of a GLB CC.
     * @param cc The GLB CC index.
     * @return A map with frontier nodes of the CC as keys (Integer), and their potential neighbors that are
     * outside the CC (Set<Integer>). Only the frontier nodes that have at least one potential neighbor outside the
     * CC are stored in the map.
     * {
     *     frontierNode1: {out-CC potential neighbors},
     *     frontierNode3: {...},
     *     ...
     * }
     */
    private Map<Integer, Set<Integer>> getGlbCCPotentialNeighbors(int cc) {
        Map<Integer, Set<Integer>> ccPotentialNeighbors = new HashMap<>();
        // Retrieve all nodes of CC
        Set<Integer> ccNodes = getGlbCCNodes(cc);
        // Retrieve neighbors of the nodes of CC that are outside the CC
        for (int i : ccNodes) {
            Set<Integer> outNeighbors = new HashSet<>();
            for (int j : g.getPotNeighOf(i)) {
                if (!ccNodes.contains(j)) {
                    outNeighbors.add(j);
                }
            }
            if (outNeighbors.size() > 0) {
                ccPotentialNeighbors.put(i, outNeighbors);
            }
        }
        return ccPotentialNeighbors;
    }


    @Override
    public ESat isEntailed() {
        if (g.isInstantiated()) {
            GlbCCFinder.findAllCC();
            int nbGlbCC = GlbCCFinder.getNBCC();
            for (int cc = 0; cc < nbGlbCC; cc++) {
                Set<Integer> ccNodes = getGlbCCNodes(cc);
                if (ccNodes.size() > maxNCC) {
                    return ESat.FALSE;
                }
                if (ccNodes.size() < minNCC) {
                    return ESat.FALSE;
                }
            }
            return ESat.TRUE;
        }
        return ESat.UNDEFINED;
    }
}
