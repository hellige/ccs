CCS is, basically, a config file format and library for configuring programs
for the JVM.

Philosophy
----------

The basic idea is that software configuration can be viewed as a tree
annotation problem.

Software contexts form a tree over time, often (although by no means always)
mirroring the execution stack of the program. Existing techniques to associate
tree-structured state with the execution of a program (e.g., thread local
storage with automatic and well-defined fork/join semantics, class properties,
etc.) can be explored to manage this context. Other scenarios are also
possible. For example, one might wish to use code generation, aspect-oriented
tools or dependency injection techniques to establish execution context, and
then of course there may be cases where context should be established by hand.
Finally, some parts of application context may be related to deployment
choices. I believe that all of these scenarios and techniques can comfortably
co-exist in the same software system.

Finally, one might wish to associate context with the static rather than the
dynamic structure of the program. This is better addressed by existing systems,
but CCS could be used there as well.

Requirements: scoped, reliable, tractable, reloadable, expressive,
comprehensible, muliple backing stores, flexible. Some notion of defaults and
overriding is essential.

### Why CSS? ###

CSS finds this balance in an established and well-understood way. Basing a
configuration system on CSS enables many programmers to leverage their existing
skills.

CSS is tractable. It provides flexibility without introducing undecidability
or the possibility of unbounded computation.

### Why not CSS? ###

A number of features of CSS make it less appropriate for software
configuration:

  - The "cascade" is designed to solve the problem of integrating potentially
    conflicting settings from multiple sources. This is less of a problem for
    software configuration in general.
  - It's less suited to basing *everything* on the structure of the tree. In
    part, this is due to the fact that the tree itself specifies sources for
    annotations.
