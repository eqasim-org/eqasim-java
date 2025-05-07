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
respective repositories. 

Additional topics:
- How to [cut out smaller parts of existing simulations](docs/cutting.md).
- How to [run a simulation with on-demand mobility services](docs/on_demand_mobility.md) (as a main mode and as a transit feeder).
- How to [run the discrete mode choice model as a standalone](docs/standalone_mode_choice.md)
- How to [use Volume Delay Functions for the network simulation](docs/vdf.md)

## Main reference

The main research reference for the eqasim-java framework:
> Hörl, S. and M. Balac (2021) [Introducing the eqasim pipeline: From raw data to agent-based transport simulation](https://www.researchgate.net/publication/351676356_Introducing_the_eqasim_pipeline_From_raw_data_to_agent-based_transport_simulation), _Procedia Computer Science_, 184, 712-719.

## Versioning and Packging

<!-- x-release-please-start-version -->
The current version of `eqasim-java` is `1.5.0`. New versions are created with every Github release and can be used as a Maven dependency. First, you need to add our [packagecloud repository](https://packagecloud.io/eth-ivt/eqasim) to your `pom.xml`:
<!-- x-release-please-end -->

```xml
<repository>
    <id>eqasim</id>
    <url>https://packagecloud.io/eth-ivt/eqasim/maven2</url>
</repository>
```

Afterwards, you can add various sub-packages to your project:

<!-- x-release-please-start-version -->
```xml
<dependency>
    <groupId>org.eqasim</groupId>
    <artifactId>core</artifactId>
    <version>1.5.0</version>
</dependency>
```
<!-- x-release-please-end -->
