[![Build Status](https://travis-ci.org/hellige/ccs.svg?branch=master)](https://travis-ci.org/hellige/ccs)
[![GitHub release](https://img.shields.io/github/v/release/hellige/ccs?include_prereleases&sort=semver)](https://github.com/hellige/ccs/releases)
[![GitHub license](https://img.shields.io/github/license/hellige/ccs)](https://github.com/hellige/ccs/blob/master/LICENSE)


CCS for Java
===========

This is the Java implementation of [CCS][1].

CCS is a language for config files, and libraries to read those files and
configure applications. The documentation is currently quite poor, but the
Java and [C++][1] implementations are mature and have been used in production
for many years.

There's a presentation about the language [here][2], but it's a little sketchy
without someone talking along with it. A syntax reference is [here][3].


[1]: http://github.com/hellige/ccs-cpp
[2]: http://hellige.github.io/ccs
[3]: https://github.com/hellige/ccs-cpp#syntax-quick-reference


Including CCS in your project
-----------------------------

Release artifacts are available in Maven:

```xml
<dependency>
    <groupId>net.immute</groupId>
    <artifactId>ccs</artifactId>
    <version>...</version>
</dependency>
```

