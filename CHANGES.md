choco-graph
===========

Module to manipulate graph variables

CHANGES


18/01/18:
- code quality review

17/01/18:
- release 4.2.3
- fix nbConnectedComponents constraint (articulation points and isthma) -> bug fix + more filtering
- fix connectivity constraint (articulation points and isthma) -> bug fix + more filtering
- change connectivity constraint definition : graphs with 0 or 1 nodes validate the constraint

01/03/18:
- release 4.2.2
- fix connectivity constraint by enforcing articulation points

02/18:
- release 4.2.1
- Improved connectivity constraint by enforcing articulation points
- update to choco solver 4.0.6
- minor updates

19/07/17:
- Remove interfaces: IGraphVar, IUndirectedGraphVar, IDirectedGraphVar, IGraphDelta, IGraphDeltaMonitor
-> Use class (without I prefix) directly instead
- Rename GraphStrategies into GraphSearch
- Add graphViz export for graph variable domain: g.graphVizExport()

10/07/17:
- Update to handle release script

01/05/17:
- Update to choco 4.0.4

11/10/16:
- Update to choco 4.0.0

05/06/16:
- Update to choco 4.0.0.a

28/01/16:
- Update to choco 3.3.3
- Fix strongly connected constraint (forces the number of SCC to be equal to 1 instead of being unbounded).
- TODO:update doc
