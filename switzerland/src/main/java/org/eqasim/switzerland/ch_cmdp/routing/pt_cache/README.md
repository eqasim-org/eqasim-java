# PT itinerary cache

Enable in the usual Switzerland runner:

```text
java -Xmx10g -jar switzerland.jar --config-path geneva_config.xml --cache-pt-routes true
```

The default is disabled. `--cache-pt-routes false` explicitly disables it,
including when enabled in the XML. No scenario preprocessing is required.

Optional configuration:

```xml
<module name="swissPtRouteCache">
  <param name="enabled" value="true" />
  <param name="memoryMb" value="64" />
  <param name="directory" value="pt-cache" />
</module>
```

An empty/omitted directory uses `<controller output>/pt_route_cache`. An explicit
relative directory is resolved relative to the configuration file. Each simulation
creates its own `run-<UUID>` subdirectory. Cache records are reused across its
iterations, **not across separate simulation launches**. This prevents accidental
reuse after changing timetable, network, routing weights or software. Files remain
in the output directory after shutdown and follow the usual output cleanup policy.

## Routing and correctness

The wrapper calls the existing `SwissRailRaptorRoutingModuleProvider` on a miss.
It retains that provider's transfer calculators, walk fallback and route selection.
Each routing worker has its own delegate; workers share only the cache. Concurrent
requests with identical keys share one calculation without holding a database lock
while routing.

Keys contain only person ID and exact departure time, plus the timetable-change
generation. MATSim's run-local integer person index represents the unique person
ID. There is no SHA-256, attribute serialization, sorting, or coordinate lookup.
Changing calibrated mode-choice constants therefore does not invalidate PT routes.
Anonymous requests and non-finite departure times bypass caching.

This key follows the mode-choice workload's assumption that each person has only
one trip at a given departure time, with fixed locations and PT routing inputs.
Changes to locations or person-specific PT routing settings require invalidating
the cache. Departure times are still exact: candidate tour durations can shift
later departures even when activity locations stay fixed.

The binary itinerary retains legs, stage activities, route descriptions, boarding
information, distances, optional times and supported output attributes. Cache hits
create new MATSim objects; cached routes cannot be modified by a person's plan.
Null and empty results are cached, including through preloading; exceptions are
not. A failed search is remembered for that person and exact departure time.
Fare estimation and mode-choice
utilities are recalculated normally, including calibrated constants.

The cache supports static single-departure-time timetable routing. It rejects
capacity constraints, range-query selection, intermodal access, network walk
routing, freespeed-based walk routing and time-variant networks when enabled.
`TransitScheduleChangedEvent` starts a new cache generation. Ordinary iteration
event-handler resets keep existing records available.

## Selected-person preloading

When caching is enabled, the standard `DiscreteModeChoice` strategy is wrapped
without changing its plan selector, worker assignment, random draws or strategy
description. The wrapper records the people MATSim actually assigns to it. Before
its worker algorithms start, up to four cache workers preload their existing
records, using batches of up to 128 people per disk query. No additional routing
is performed during preloading. In direct, zero-worker mode, person-grouped demand
loading is used instead.

Records for unselected people leave the hot cache. Preloading stops when each
shard's memory budget fills; it does not load the whole selected population into
unbounded RAM. The remaining requests use normal demand loading. A cold demand
lookup also loads a person's cached departures together, avoiding one disk query
per trip. Oversized person groups or records fall back to exact disk lookups.

## Memory, disk and diagnostics

The hot-cache budget defaults to 64 MiB across 16 shards. It accounts for person
buckets, payloads, map overhead and estimated decoded-template space. Retained
records lazily decode a private template once; hits copy fresh legs, activities,
coordinates and routes rather than parsing the binary record again. Disk-only
records decode directly. The budget is an estimate of retained data, not a strict
limit on temporary allocations or process memory. A zero budget disables both
preloading and the hot cache.

SQLite records use the composite integer key `(generation, person, departure)`;
the primary index groups a person's departures together. The index stays on disk,
with about 1 MiB of SQLite page cache per shard in addition to the hot-cache budget.
Java index memory does not grow with the total number of stored routes. Disk usage
grows with unique requests. Use a local SSD for frequent lookups.

Prepared statements and transactions batched up to 128 inserts per shard reduce
write overhead. Iteration ends flush outstanding transactions; shutdown closes
all files. If disk access fails, routing continues with the bounded memory cache.
An unreadable itinerary invalidates the cache generation and falls back to routing.

Iteration and shutdown logs report cumulative requests, memory hits, disk hits,
misses, coalesced requests, bypasses, bytes written, disk read queries, prefetched
people/routes and estimated retained memory. These are diagnostic
counters rather than an exact partition: a cache miss whose output cannot be
serialized also contributes a bypass. Cache saturation depends on repeat requests
at the same departure time; changing tour departure times can require new routes.

Run `TestPtRouteCache` and `TestPtRouteCacheIntegration` for disk-only retrieval,
mutable-object isolation, parallel requests, exact person/time keys, ignored
metadata, selected-person preloading, grouped reads, memory limits, unreachable/
error behavior, timetable invalidation, configuration parsing and real RAPTOR
transfer/boarding reconstruction.
