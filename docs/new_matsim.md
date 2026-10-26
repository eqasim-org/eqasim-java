# MATSim 2026 upgrade notes

This branch moved from the development snapshot `2026.0-2025w33` to the
released MATSim `2026.0`. The main reason was to make MATSim's DSim engine
available through the normal Switzerland runner. The same build and the same
`switzerland.jar` now contain QSim and DSim:

```text
# Existing behavior: optimized QSim
java -Xmx64g -jar switzerland/target/switzerland.jar --config-path config.xml

# DSim
java -Xmx64g -jar switzerland/target/switzerland.jar --config-path config.xml --use-dsim true

# DSim with bicycles simulated on a separate network layer
java -Xmx64g -jar switzerland/target/switzerland.jar --config-path config.xml --use-dsim true --dsim-simulate-bikes true
```

The upgrade applies to every Maven module in the repository. It requires JDK
25. The CI workflows and Maven compiler level were changed accordingly.

Guice 7.0.0's default JAR bundles an older ASM that cannot read Java 25 class
files when formatting injector diagnostics (`Unsupported class file major
version 69`). The parent POM excludes that JAR from MATSim and its contribs,
uses Guice's unshaded `classes` classifier, and supplies ASM 9.9.1 explicitly.
This preserves source-line diagnostics without changing simulation logic or
requiring additional runtime arguments. See the
[upstream Guice issue](https://github.com/google/guice/issues/1926).

## Relevant changes in MATSim 2026

The following upstream changes required work in Eqasim. This is a summary of
the changes that affect this repository rather than a complete MATSim changelog.

| Area | Change in MATSim 2026 | Eqasim consequence |
| --- | --- | --- |
| Java runtime | MATSim 2026 is built for Java 25. | The project, CI and runtime now require JDK 25. |
| DSim | The release contains the partitioned DSim network, scheduler, messages and scoring integration. | DSim can be compiled into the main build instead of using a separate experimental Maven profile. |
| Mobsim lifecycle | Several engine callbacks use `beforeMobsim` and `afterMobsim`. | The SBB transit engine, Eqasim transit engine and VDF engine were adapted to the new lifecycle. |
| Transit engine construction | `TransitQSimEngine` now receives a `TimeInterpretation`. | `SBBTransitQSimEngine` creates it from the scenario configuration. |
| Transit-stop state | `TransitStopHandler` now receives relocating passengers and a `TransitVehicle`, and handlers can be serialized with `toMessage()`. Factories can restore a handler from a message. | The sampled dwell wrapper forwards the new data and restores serialized handlers without applying the sampling factor a second time. |
| SpeedyALT | Landmark preprocessing accepts a thread count. Turn-restricted searches may have several colored copies of the destination node. | The compact router supports the new constructor behavior and selects the cheapest destination-colored node already on the frontier. |
| Routing modules | The freespeed-factor routing factory replaced the older pseudo-transit factory used for this case. | `MultiModeRouter` uses the new factory while retaining its unreachable-path handling. |
| Vehicle lookup | Network routing resolves the vehicle assigned to the person more strictly. | Tests and synthetic scenarios must put mode-to-vehicle IDs in person attributes and register the vehicles. Real loaded scenarios already do this. |
| DRT constraints | The maximum wait constraint is exposed as `maxWaitDuration()`. | `DefaultDrtPredictor` uses the typed API instead of the old getter/reflection fallback. |
| QVehicle API | The copied queue implementation can no longer obtain the current link through the old vehicle method. | Abort and stuck events use the link owned by the queue. |
| DSim serialization | DSim uses Agrona and Fory for messages. Java 25 restricts access to two JDK packages they use. | The executable jar declares the necessary `Add-Opens`. Classpath launches automatically restart once with the same JVM/application arguments when those permissions are missing. |

The MATSim upgrade can also change deterministic regression totals. For
example, the existing Melun emissions test produced 3,303 car departures with
MATSim 2026 where its old expected value is 3,297. Such changes need model-level
validation before updating reference results.

## How the DSim integration is structured

`DsimIntegration2026` is an Eqasim adapter; it does not replace a MATSim class.
It translates the configuration before the scenario and controller are built:

- QSim remains the default. DSim is selected only by `--use-dsim true`.
- QSim start/end time, stuck time, vehicle behavior and thread count are copied
  to the DSim configuration. An explicit `dsim.threads` value takes precedence.
- Car and truck remain network modes.
- Bikes are removed from the network modes and teleported at 4 m/s with a 1.4
  beeline factor by default. With `--dsim-simulate-bikes true`, the integration
  instead copies the bike-eligible links and nodes into a disconnected
  bike-only layer. It keeps only bike-routing attributes and node coordinates,
  omits bike turn restrictions, gives bike vehicle types zero PCU, removes bike
  from the source links, enables access/egress routing, and reroutes bike legs
  onto the copied layer.
- Public transport remains available to SwissRailRaptor and mode choice, but it
  is simulated from timetable departures and arrivals rather than with transit
  vehicles on the road network.
- `SeepageQ` is replaced by FIFO. The optional bike layer provides physical
  separation from road traffic without requiring seepage support in DSim.
- `withHoles` is mapped to DSim's `kinematicWaves` behavior.
- Sampled dwell times, snapshots, PT vehicle count outputs and experienced-plan
  file export are disabled because the necessary QSim/native DSim state is not
  available.
- DRT/DVRP, feeder DRT, VDF, abstract PT access, lanes, time-variant networks,
  non-one-second time steps and `vehicleBehavior=wait` fail explicitly rather
  than silently running with different behavior.

`EqasimDsimModule` then installs two Eqasim engines into DSim:

1. `RoadDelays` supplies DSim with a link traversal time equal to free-flow
   travel time plus the existing Eqasim `CrossingPenalty`. This retains the
   signalized Webster delay and the unsignalized intersection delay. The
   callback is installed only on the downstream owner of a partition-boundary
   link, so a vehicle receives the delay once.
2. `ScheduledTransitEngine` finds the next departure of the chosen service and
   schedules the passenger's arrival using the transit-stop offsets. It handles
   transfers between DSim partitions, has unlimited passenger capacity, and
   emits Eqasim's `PublicTransitEvent` for scoring and experienced-route data.

This PT adapter does not simulate buses or trains as network vehicles. Bus road
load, vehicle capacity, congestion feedback to PT, SBB vehicle behavior and
sampled dwell times are therefore absent in DSim.

## MATSim classes replaced by the DSim integration

The project contains source with the exact same fully qualified name as three
MATSim 2026 classes. The project version therefore replaces the dependency's
class in the compiled and shaded artifacts. Each file is based on the MATSim
2026.0 source and has an upstream URL in its header.

### `org.matsim.dsim.simulation.net.SimLink`

The replacement adds an optional `LinkTravelTime` callback. Without a callback,
the exit-time calculation is the upstream MATSim implementation. Eqasim installs
a callback before mobsim that calculates:

```text
link length / min(link freespeed at time, vehicle maximum speed)
+ Eqasim crossing penalty
```

`LocalLink` and the local half of `SplitInLink` calculate this value. A
`SplitOutLink` is only the upstream representation of a partition boundary and
does not calculate it. This ownership rule prevents an intersection delay from
being applied twice at a partition boundary.

### `org.matsim.dsim.simulation.net.SimNetwork`

Upstream DSim scales a vehicle's PCU by the inverse of QSim's flow-capacity
factor. That scaling influences both flow and storage inside DSim. QSim,
however, permits independent `flowCapFactor` and `storageCapFactor` values.

The replacement leaves the input network unchanged and adjusts only the cell
size passed to DSim queues:

```text
effective cell size = network effective cell size
                      * flowCapFactor / storageCapFactor
```

This compensates DSim's shared PCU scaling and preserves the requested relative
storage capacity. If both factors are equal, which is the common sampled-model
configuration, the effective cell size is unchanged. Both factors are checked
for finite positive values before DSim starts.

This adjustment covers ordinary DSim queue storage and the storage proxy used
on partition boundaries. DSim's own minimum-capacity and kinematic-wave rules
still apply, so this does not make DSim numerically identical to QSim.

### `org.matsim.dsim.scoring.BackpackTransitRouteProvider`

The native provider creates a route recorder for transit vehicles. In this
integration `usingTransitInMobsim=false`, because PT is handled by the timetable
adapter and there are no transit vehicles to board or leave.

The replacement selects `ScheduledTransitBackpackRoute` in that configuration.
The recorder combines person departure/arrival events with Eqasim's
`PublicTransitEvent` and produces an experienced
`DefaultTransitPassengerRoute`, including the actual service, boarding time,
travel time and distance. When native transit simulation is enabled, it still
uses MATSim's normal `BackpackTransitRoute`. Both message types can be restored
after a partition transfer.

`ScheduledTransitBackpackRoute` is an added class rather than an upstream class
replacement. It lives in the MATSim DSim scoring package because it implements
the package-level DSim route/message contract.

## Existing MATSim-package replacements carried forward

This repository already contained several classes in `org.matsim.*` before the
upgrade. They remain replacements and must also be reviewed against upstream
when MATSim changes:

| Area | Replaced or added classes | Purpose in this branch |
| --- | --- | --- |
| QSim network engine | `AbstractQNetsimEngineRunner`, `QueueWithBuffer`, plus `FastQNetsimEngine`, `QSimWorkScheduler`, `TrafficWeightedPartitioner`, `PhaseActivationBuffer`, `FastQSimPartitionCache`, `QVehicleBuffer` and `SeepageVehicleQ` | Avoid repeated task/Future allocation, use adaptive traffic-weighted spatial work blocks, avoid expensive queue copies, and retain seepage behavior. Some names replace upstream classes; the `Fast*` support classes are additions in the MATSim package. |
| Event handling | `ProcessEventThread` | Lower-allocation parallel event processing while retaining event ordering semantics. |
| Routing | `PlanRouter` | Eqasim route replacement rules, including rebuilding missing/invalid routes after DSim changes a mode to teleportation. |
| Traffic analysis | `VolumesAnalyzer`, `LinkStatsControllerListener` | Eqasim flow/bicycle treatment and link-statistics behavior. |
| Discrete mode choice | Classes under `org.matsim.contribs.discrete_mode_choice.model` and selected module/config classes | Eqasim's optimized or modified selector, utility and tour/trip model behavior. |

`ch.sbb.matsim.mobsim.qsim.pt.SBBTransitQSimEngine` is similarly a project copy
of an SBB class. It remains active for QSim and was updated for MATSim 2026's
constructor and lifecycle APIs. DSim does not load it.

Because these replacements depend on package-private details and exact method
contracts, changing `${matsim.version}` should always include a comparison with
the new upstream sources. A successful compile alone is insufficient for these
classes.

## Build and validation

### PT itinerary caching (October 2026)

Switzerland's `RunSimulation` supports the opt-in `--cache-pt-routes true`
wrapper under `org.eqasim.switzerland.ch_cmdp.routing.pt_cache`. It delegates
misses to the existing SwissRailRaptor provider, stores binary itineraries in a
run-local indexed disk cache, and shares a bounded RAM cache across workers and
iterations. Exact routing inputs are keyed; restored itineraries use fresh MATSim
objects. This adds no upstream class replacement. See the
[PT cache documentation](../switzerland/src/main/java/org/eqasim/switzerland/ch_cmdp/routing/pt_cache/README.md)
for configuration, supported routing settings and lifecycle behavior.

Build the common artifact with JDK 25:

```text
mvn -Pstandalone -pl switzerland -am package
```

This creates `switzerland/target/switzerland.jar`, whose manifest contains the
runner main class and DSim's required module permissions.

The MATSim 2026 migration was validated by compiling and packaging all nine
repository modules, by focused regression tests for optimized QSim, SpeedyALT,
network-calibration routing, sampled transit dwell and DSim, and by running
iterations 0 and 1 of the Lausanne scenario through both QSim and DSim using the
same jar. Both runs exited normally with finite scores. DSim remains an
alternative traffic model rather than a result-equivalent QSim implementation.
The optional isolated bike layer was separately exercised for iteration 0 of
the Lausanne scenario and by a two-iteration synthetic DSim controller test.
