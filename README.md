# eqasim-java

![eqasim](docs/top.png "eqasim")

The `eqasim` package is a collection of ready-to-run high-quality scenarios
for the agent- and activity-based transport simulation framework [MATSim](https://matsim.org/).
This repository contains the Java parts of the project which extend MATSim and
provide functionality to generate the simulation data.

Therefore, this repository has to functions: First, to provide the code to run
`eqasim` simulations (and extend them), second, to act as a dependency for the
various pipelines that generate runnable MATSim scenarios from raw data sets,
such as for:

- [Île-de-France and Paris](https://github.com/eqasim-org/ile-de-france)
- [California](https://github.com/eqasim-org/california)
- [Sao Paulo](https://github.com/eqasim-org/california)

To understand how to set up a simulation and run it, please refer to the
respective repositories. To cut out smaller parts of existing simulations
check out how to use this repository to [create simulation cut-outs](docs/cutting.md).

## Versioning and Packging

[![Build Status](https://travis-ci.com/eqasim-org/eqasim-java.svg?branch=develop)](https://travis-ci.com/eqasim-org/eqasim-java)

The current version of `eqasim` is `1.2.0`. You can access it through the `v1.2.0` tag. The
`develop` branch is kept at version `1.2.0` until the next release is prepared,
but may include additional developments since the last release.

The code is available as a Maven package. To use it, add the following repository
to your `pom.xml`:

```xml
<repository>
    <id>matsim-eth</id>
    <url>https://dl.bintray.com/eqasim/eqasim</url>
</repository>
```

Afterwards, you can add various sub-packages to your project:

```xml
<dependency>
    <groupId>org.eqasim</groupId>
    <artifactId>core</artifactId>
    <version>1.2.0</version>
</dependency>
```
