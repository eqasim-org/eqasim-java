# eqasim-java

![eqasim](docs/top.png "eqasim")

The `eqasim` package is a collection of ready-to-run high-quality scenarios
for the agent- and activity-based transport simulation framework [MATSim](https://matsim.org/).
This repository contains the Java parts of the project which extend MATSim and
provide functionality to generate the simulation data.

Therefore, this repository has two functions: First, to provide the code to run
`eqasim` simulations (and extend them), second, to act as a dependency for the
various pipelines that generate runnable MATSim scenarios from raw data sets,
such as for:

- [Île-de-France and Paris](https://github.com/eqasim-org/ile-de-france)
- [California](https://github.com/eqasim-org/california)
- [Sao Paulo](https://github.com/eqasim-org/sao_paulo)

To understand how to set up a simulation and run it, please refer to the
respective repositories. To cut out smaller parts of existing simulations
check out how to use this repository to [create simulation cut-outs](docs/cutting.md).

## Main reference

The main research reference for the eqasim-java framework:
> Hörl, S. and M. Balac (2021) [Introducing the eqasim pipeline: From raw data to agent-based transport simulation](https://www.researchgate.net/publication/351676356_Introducing_the_eqasim_pipeline_From_raw_data_to_agent-based_transport_simulation), _Procedia Computer Science_, 184, 712-719.

## Versioning and Packging

[![Build Status](https://travis-ci.com/eqasim-org/eqasim-java.svg?branch=develop)](https://travis-ci.com/eqasim-org/eqasim-java)

The current version of `eqasim` is `1.3.1` and is based on MATSim `13.0`. You can access it through the `v1.3.1` tag. The
`develop` branch is kept at version `1.3.1` until the next release is prepared,
but may include additional developments since the last release.

The code is available as a Maven package. To use it, add the following repository
to your `pom.xml`:

```xml
<repository>
    <id>eqasim</id>
    <url>https://packagecloud.io/eth-ivt/eqasim/maven2</url>
</repository>
```

Afterwards, you can add various sub-packages to your project:

```xml
<dependency>
    <groupId>org.eqasim</groupId>
    <artifactId>core</artifactId>
    <version>1.3.1</version>
</dependency>
```

Besides test latest releases based on MATSim 13, legacy versions `1.2.1`, `1.2.0`, `1.0.6`, and `1.0.5` are also available through packagecloud.

## Upstream branch

To keep scenario-based repositories up-to-date (for instance, [ile-de-france](https://github.com/eqasim-org/ile-de-france)), we provide the `upstream` branch, which contains a well-defined `develop` version of `eqasim-java` and is used in the `develop` version of the dependent repository. While this is useful for development purposes, their versioned releases will always depend on versioned releases of `eqasim-java`.
