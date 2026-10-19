# DSim in the Switzerland runner

See also [MATSim 2026 upgrade notes](new_matsim.md) for the upstream API
changes, the classes replaced by this integration, and the storage-capacity
adjustment.

The main Switzerland build uses MATSim 2026.0 and Java 25. One standalone jar
contains both engines. `org.eqasim.switzerland.ch_cmdp.RunSimulation` uses the
existing optimized QSim by default and selects DSim only when
`--use-dsim true` is supplied.

## Build and run

With `JAVA_HOME` pointing to JDK 25:

```text
mvn -Pstandalone -pl switzerland -am package
```

The shaded artifact is `switzerland/target/switzerland.jar`. It can run
either engine:

```text
java -Xmx16g -jar switzerland/target/switzerland.jar --config-path /path/to/config.xml --config:controller.outputDirectory /path/to/output_qsim

java -Xmx16g -jar switzerland/target/switzerland.jar --config-path /path/to/config.xml --use-dsim true --config:dsim.threads 12 --config:controller.outputDirectory /path/to/output_dsim

# DSim with bicycles simulated on an isolated bike network
java -Xmx16g -jar switzerland/target/switzerland.jar --config-path /path/to/config.xml --use-dsim true --dsim-simulate-bikes true --config:dsim.threads 12 --config:controller.outputDirectory /path/to/output_dsim_bikes
```

The traditional classpath command also works. When DSim is requested, the
runner detects whether Agrona's required Java module access is present and, if
necessary, starts the simulation in a child Java process with the same heap,
system properties, classpath and application arguments. This keeps
`--use-dsim true` as the only required DSim switch. The executable-jar manifest
already supplies those module permissions, so `java -jar` does not relaunch.
For IDE debugging or a classpath launch that must remain in one JVM, add:

```text
--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED
--add-opens=java.base/java.lang.invoke=ALL-UNNAMED
```

DSim partitions run in one JVM. Multi-machine execution has not been integrated
with Eqasim's shared datasets and listeners. The DSim thread count defaults to
the QSim thread count unless a positive `dsim.threads` value is supplied.
QSim scheduler settings and `eventsManager.numberOfThreads` do not configure
DSim's scheduler.

## Automatic adaptations

| Component | Behavior with DSim |
| --- | --- |
| Car/truck network traffic | DSim queue network using the configured road modes and vehicle speeds. |
| Bikes | Teleported at 4 m/s with beeline factor 1.4 by default, matching `RunAdaptConfig`. With `--dsim-simulate-bikes true`, the bike-eligible network is copied into a disconnected bike-only layer and bikes are simulated by DSim on that layer. Existing bike routes are rebuilt in both cases. |
| SeepageQ | Replaced by FIFO. With network bikes, physical separation of the copied layer prevents bicycles and road vehicles from blocking each other. |
| PT routing | SwissRailRaptor and the timetable remain available to routing and mode choice. |
| PT simulation | Unlimited-capacity timetable teleportation. The next available departure is selected and scheduled stop offsets determine arrival. |
| Transit vehicles | Buses and trains are not simulated on the road network. Consequently there is no capacity constraint, road load, congestion feedback, SBB vehicle engine or sampled dwell time. |
| Intersection delays | The existing `CrossingPenalty` is applied once on link traversal, including signalized and unsignalized delays. It contributes to measured travel time and is not added again by routing. |
| Routing and travel-time storage | The optimized SpeedyALT router and float travel-time storage remain active. |
| Sampling factors | Native DSim PCU flow scaling is used; the adapter adjusts storage geometry for the separate QSim storage factor. |
| `withHoles` | Mapped to DSim kinematic waves, with a warning. |
| Stuck vehicles | Native DSim pushes vehicles downstream; QSim's removal setting is disabled with a warning. |
| Outputs | PT vehicle passenger/link counts, snapshots and experienced-plan file export are disabled. Normal plans, scoring, road statistics and configured events remain available. |

The adapted configuration is written to `run_config.xml`; scenario inputs are
not modified. Unsupported QSim extensions (DRT/DVRP, feeder DRT, abstract PT
access and VDF), lanes, time-variant networks, non-one-second time steps and
`vehicleBehavior=wait` fail with an explicit message. Chained/interlined PT
routes within one leg are not supported; ordinary transfers represented as
separate legs are supported.

The network-bike option removes `bike` from every original link, duplicates
each bike-eligible link and its endpoints with reserved IDs, and assigns only
`bike` to the copies. Bike turn restrictions are deliberately omitted. The
copies retain the link geometry and operating values plus only the attributes
used by bike routing: `osm:way:highway`, `osm:way:name`, `traffic_light` and
`crossingPenalty`. Node X/Y/Z coordinates are retained and node attributes are
discarded. Original links are retained even when bike was their only mode so
that activity and other scenario references remain valid. MATSim inserts access
and egress stages between activity links and the disconnected bike layer. The
adapter enables the required access/egress routing when a configuration has it
disabled. Re-reading an already adapted output scenario cleans, reuses and
validates the marked bike layer instead of duplicating it again.
DSim adds its `partition` attribute to copied nodes and links while constructing
the distributed network; this is required engine metadata. Capacity diagnostic
attributes created during queue construction are removed from bike links.

This option increases memory use by the size of the bike-eligible subnetwork.
Bicycle vehicle types use PCU `0.0`, so bikes consume neither flow nor storage
capacity. They remain ordered as agents on the bike layer, but capacity never
holds them back and they never share links or nodes with cars and trucks. DSim
uses FIFO dynamics there; it does not reproduce QSim's `SeepageQ` ordering.
DSim traversal uses the copied free speed, vehicle maximum speed and the
configured Eqasim intersection penalty.

DSim is an alternative traffic simulation rather than a result-preserving QSim
optimization. Its tick boundaries, partitions, flow/storage handling and node
processing differ, and teleporting bikes and PT changes road demand. Validate
counts, travel times and mode shares before using it for production results.

This integration adapts three MATSim 2026.0 classes: `SimLink` for the traversal
callback, `SimNetwork` for independent storage scaling, and
`BackpackTransitRouteProvider` for experienced teleported PT routes. Their
upstream provenance is recorded in their source headers and should be reviewed
when MATSim is upgraded again.

## Validation

On 2026-10-19, the same `switzerland.jar` completed iterations 0 and 1 of the
Lausanne test scenario with default QSim and with `--use-dsim true`, using four
workers and a 6 GB heap. QSim loaded `FastQNetsimEngine` and
`SBBTransitQSimEngine`; DSim created four partitions and retained the float
travel-time collector and intersection-delay adapter. Both runs exited normally
with finite scores. This is a functional smoke test rather than an equivalence
or performance claim.

The isolated bicycle layer also has a focused two-iteration controller test.
It verifies that a bike finishes its network leg in DSim, enters only copied
bike links, has zero PCU, and that the source road links no longer permit bike.
A structural test verifies the minimal attributes, preserved Z coordinates,
absence of bike turn restrictions and idempotent reuse.
The Lausanne scenario additionally completed iteration 0 with
`--dsim-simulate-bikes true`: 22,115 bike links and 10,216 bike nodes were
copied, 3,181 bike legs were rerouted, four DSim workers ran to the configured
end time, and the controller shut down normally.
