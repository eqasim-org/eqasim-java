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

Keys use exact departure times, origin/destination coordinates and link/facility
identity where available, person ID/attributes and request/facility attributes.
Attribute maps are canonicalized before SHA-256 hashing. Unsupported attribute
types bypass caching and use the original router. This conservative key can miss
when an attribute changes even if that attribute does not affect this itinerary.

The binary itinerary retains legs, stage activities, route descriptions, boarding
information, distances, optional times and supported output attributes. Cache hits
create new MATSim objects; cached routes cannot be modified by a person's plan.
Null results are cached; exceptions are not. Fare estimation and mode-choice
utilities are recalculated normally, including calibrated constants.

The cache supports static single-departure-time timetable routing. It rejects
capacity constraints, range-query selection, intermodal access, network walk
routing, freespeed-based walk routing and time-variant networks when enabled.
`TransitScheduleChangedEvent` starts a new cache generation. Ordinary iteration
event-handler resets keep existing records available.

## Memory, disk and diagnostics

The hot-cache budget defaults to 64 MiB across 16 shards, accounting for payloads
and estimated entry overhead. A zero budget forces disk-only operation. The
indexed binary records use SQLite, already supplied by the GeoPackage dependency
and now declared explicitly. The database index stays on disk, with about 1 MiB of
SQLite page cache per shard in addition to the hot-cache budget. Java index memory
does not grow with the total number of stored routes. Disk usage grows with unique
requests. Use a local SSD for frequent lookups.

Prepared statements and transactions batched up to 128 inserts per shard reduce
write overhead. Iteration ends flush outstanding transactions; shutdown closes
all files. If disk access fails, routing continues with the bounded memory cache.
An unreadable itinerary invalidates the cache generation and falls back to routing.

Iteration and shutdown logs report cumulative requests, memory hits, disk hits,
misses, coalesced requests, bypasses and bytes written. These are diagnostic
counters rather than an exact partition: a cache miss whose output cannot be
serialized also contributes a bypass. Cache saturation depends on repeat requests
at the same departure time; changing tour departure times can require new routes.

Run `TestPtRouteCache` and `TestPtRouteCacheIntegration` for disk-only retrieval,
mutable-object isolation, parallel requests, exact time keys, metadata changes,
unreachable/error behavior, timetable invalidation, configuration parsing and
real RAPTOR transfer/boarding reconstruction.
