[![Codacy Badge](https://api.codacy.com/project/badge/Grade/3f09a253fa3349938d9310c0f52b4c46)](https://www.codacy.com/app/jg-fages/choco-graph?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=chocoteam/choco-graph&amp;utm_campaign=Badge_Grade)
[![Build Status](https://travis-ci.org/chocoteam/choco-graph.svg?branch=master)](https://travis-ci.org/chocoteam/choco-graph)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.choco-solver/choco-graph/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.choco-graph/choco-solver)

Choco-graph is an open-source Java library for Constraint Programming over Graph Variables. 
This extension of Choco Solver enables you to search for a graph that satisfies a graph properties such as 
forming a tree, a hamiltonian cycle, having 3 strongly connected components, etc.

## Documentation

A user guide may be found in the doc folder. Feel free to contribute to it. 

## License

This software is licensed under BSD license. 

## Requirement ##

This extension of Choco Solver works with choco-solver-4.0.6 and associated dependencies. It requires JDK 1.8+

## Main authors

Please meet our team of cho-coders : 

- [@jgFages](https://github.com/jgFages) (Jean-Guillaume Fages)
- [@cprudhom](https://github.com/cprudhom) (Charles Prud'homme)


### Building from sources ###

The source of the released versions are directly available in the `Tag` section.
You can also download them using github features.
Once downloaded, move to the source directory then execute the following maven command
to make the jar:

    $ mvn clean package -DskipTests

If the build succeeded, the resulting jar will be automatically
installed in your local maven repository and available in the `target` sub-folders.

## Issues

Use the [issue tracker](https://github.com/chocoteam/choco-graph/issues) here on GitHub to report issues.
As far as possible, provide a [Minimal Working Example](https://en.wikipedia.org/wiki/Minimal_Working_Example).


===================
The Choco Solver dev team.
