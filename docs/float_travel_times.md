# Shared float travel times

`org.eqasim.switzerland.ch_cmdp.RunSimulation` uses a shared float observed
travel-time collector by default. `--float-travel-times false` selects the
original MATSim observed collector. This switch is independent of
`--memory-efficient-routing`; the float times work with either router.

The implementation is in `org.eqasim.core.components.travel_time`, adapted from
the earlier MATSim 2026.0-2025w33 event collector and used with MATSim 2026.0. It does not shadow any
MATSim classes. The public `TravelTime` API still returns doubles; the stored
link/time-bin averages are floats. Routing costs, accumulated arrival times,
landmarks, event timestamps, and simulation clocks retain their existing precision.

## Storage and sharing

MATSim's current observed collector uses a packed `long[]`, combining a mean
travel time at millisecond resolution and an observation count in eight bytes
per bin. It was already shared across routing workers.

The replacement uses four-byte float averages and temporary four-byte integer
counts. `AfterMobsim` consolidates congestion spillover, fills missing bins on
observed links, releases all count arrays and pending event references, and
prepares the tables before replanning starts. Thus retained bin payload during
routing is four bytes instead of eight. During event collection both values
and counts are needed; array headers and collector metadata are additional.

Every routing worker receives the same read-only `TravelTime` view. Queries do
not allocate rows, cache free-flow values, update observation counts, or perform
lazy consolidation. Unobserved links use free speed without allocating a table.
Routers still need separate mutable search state.

This follows MATSim's normal phase separation: events/reset update the collector
during mobsim, then routers read prepared values during replanning. It is not a
separate immutable previous-iteration snapshot for simultaneous within-day
routing and event collection. The event manager must serialize delivery to each
collector, as required for the original collector's per-bin aggregation too.

When `separateModes=false`, all observed modes share one collector and view.
When true, each analyzed mode keeps its own observations, shared across its
routing workers. Distinct mode data is not merged. Collectors are lazy so later
custom bike/VDF providers do not allocate unused observed tables. The float
module is installed before those overrides in the Switzerland runner.

Both average-bin lookup and linear interpolation, mode filtering, vehicle maximum
speed, overflow bins, PT stop events, aborts and congestion spillover are handled.
The congestion correction rounds upward when necessary to avoid violating its
minimum after float rounding. Small numerical and resulting route-choice changes
relative to the original packed collector are expected.

## Optional VDF mode

Standalone server event-derived tables also use shared float arrays; see
[Shared routing in calibration and servers](routing_servers.md) for their
snapshot behavior, binary compatibility and worker ownership.

`VDFTravelTime` also stores its link/bin table as `float[]`. Its singleton provider
was already shared. VDF routing bindings now share one read-only view which uses
`estimateCrossingPenalty`, avoiding simulated vehicle-history updates from
routing. Direct simulation callers retain the original history-aware method.
The VDF table is updated between simulation/routing phases as before.

VDF input/output retains the existing double-encoded binary file format for
compatibility; values are narrowed only in memory. The command-line switch above
controls the observed collector; optional VDF tables always use float storage.

## Benchmark and validation

`CheckNetworkMemoryUsage` uses the shared float collector for optimized SpeedyALT
with 16 and 32 landmarks and restrictions, in separate worker JVMs. The original
SpeedyALT cases (with and without restrictions)
use the original MATSim collector. All cases populate every 900-second bin
including overflow from identical observations and route the same 100 random
trips, using four threads by default. The float table is prepared once and logs
`StorageStatistics`; its observation-count bytes must be zero during routing.
The comparison checks both router and storage changes; differences can result
from precision as well as route choice.

Tests cover float averaging, count release, parallel non-mutating reads, missing
links, reset/update across iterations, interpolation, congestion spillover,
separate and aggregated modes, vehicle-speed limits, routing-provider precedence,
a two-iteration controller run, VDF read-only delay handling and legacy files.

A packaged-JAR check on the 80-by-80 restricted grid (25,280 links, 121 bins,
100 trips, four routing workers, `-Xms128M -Xmx1G`) measured these post-GC
component sizes:

| Travel-time storage | Before | Float collector |
| --- | ---: | ---: |
| Retained travel-time heap estimate | 24.50 MiB | 13.25 MiB |
| Retained bin-value payload | 23.34 MiB, including packed counts | 11.67 MiB |

All 100 original/optimized SpeedyALT routes matched exactly on this grid, using
original and float travel-time storage respectively. This grid has exactly
representable free-flow times; precision changes can affect other networks.
The reduction is in routing-phase travel-time storage, not a halving of total JVM memory or
mobsim peak memory. Short benchmark timings are not a reliable speed comparison.
