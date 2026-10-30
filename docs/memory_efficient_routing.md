# Memory-efficient SpeedyALT

The optional implementation in `org.eqasim.core.components.routing.speedy` is
adapted from MATSim **2026.0-2025w33** and updated for the branch's MATSim
**2026.0** runtime. The SpeedyALT factory is bound through Eqasim. Separate
routing and QSim compatibility overrides are described below.

Build the standalone artifact with
`mvn -Pstandalone -pl switzerland -am package`. The runnable artifact is
`switzerland/target/switzerland-2.0.0.jar`; it can be copied as `switzerland.jar`.

## Switzerland simulation

`org.eqasim.switzerland.ch_cmdp.RunSimulation` enables this implementation by
default. The controller configuration must select
`routingAlgorithmType=SpeedyALT`. Set `--memory-efficient-routing false` to use
the original MATSim implementation, or pass `--memory-efficient-routing true`
explicitly to select the optimized implementation.

```text
java -Xmx16G -cp switzerland.jar org.eqasim.switzerland.ch_cmdp.RunSimulation --config-path lausanne_config.xml --memory-efficient-routing true
```

The override replaces the network path-calculator factory. It receives the same
travel times and disutilities as before, including toll and calibration costs.
This switch selects the network path calculator. The QSim selector and
deterministic route acceptance described below apply independently of this switch.
The factory shares immutable graph/landmark data; every routing worker still
receives its own mutable search workspace. Different minimum-cost functions get
separate landmark tables, even if they use the same network object.

## Compare memory and time

Run `CheckNetworkMemoryUsage` once. It runs four cases in separate child JVMs:
original SpeedyALT with original MATSim travel-time storage and turn restrictions,
optimized SpeedyALT with **16 landmarks** and shared float travel times,
optimized SpeedyALT with **32 landmarks** and shared float travel times (both with
turn restrictions), and original SpeedyALT with original storage without turn
restrictions. The same 100 random trips, departure times, thread count and JVM
heap/GC options are used
throughout. Threads default to four when `--threads` is omitted. All inputs are
read-only. The original MATSim collector stores packed means/counts in 64-bit
values rather than a `double[]`; both optimized cases retain 32-bit float means.

```text
java -Xmx16G -cp switzerland.jar org.eqasim.switzerland.ch_cmdp.utils.routing.CheckNetworkMemoryUsage --network-path network.xml.gz --threads 8
```

The final table shows all four cases, including requested landmark counts and
compact shared landmark-table payloads for both optimized cases. A second table
shows percentage changes relative to original SpeedyALT with restrictions, plus
a direct **32 vs 16** comparison. Travel-time comparisons check original vs each
optimized case and optimized 16 vs optimized 32 on the same OD/departure
data. The default allowed difference is the larger of **1 second or 1% of the
reference travel time**. Override these with `--travel-time-tolerance-seconds` and
`--travel-time-tolerance-percent`. Different paths and costs do not cause a failed
similarity check. The report shows per-trip differences, mean/p95/maximum absolute
and relative differences, and flags reachability changes or invalid reachable
travel times. Trips unreachable in both cases are counted separately; when no
trips are reachable in both, similarity is reported as not evaluated. Float
precision can change values and route choices, so original-vs-optimized results
combine router/storage changes. The 16-vs-32 comparison isolates the landmark
count. Changing landmark counts can also change restricted-network search results;
both optimized counts are tested against pinned MATSim at the corresponding count.
The legacy `--memory-efficient-routing` flag
is accepted with a warning; this benchmark always runs all four cases.

The ordinary simulation/server factory still defaults to 16 landmarks. The
benchmark explicitly constructs `MemoryEfficientSpeedyALTFactory(16)` and `(32)`;
no extra benchmark arguments are required. On very small graphs the count is
capped by the available legacy node slots; actual counts appear in the graph
statistics. Each case shares its landmark tables across workers.

The optional `--config-path` reads the benchmark's travel-time bin settings and
distance/noise parameters; defaults match Lausanne (900 s, 30 h, distance utility
0.42, zero routing noise). The benchmark uses synthetic free-speed observations,
not a full simulation. Its component heap figures are post-GC estimates, and
heap averages/peaks are sampled. Process RSS/working set is reported separately.
Use repeated runs for timing comparisons: 100 cold queries are not a stable
throughput benchmark. Total simulation savings depend on mode graphs, worker
counts, restriction density and other components.

Observed link/bin times in the Switzerland runner and the optimized benchmark
case use shared float storage; see [Shared float travel times](float_travel_times.md)
for precision, observation-count release, mode separation and VDF behavior.

## Storage and construction changes

Calibration and standalone-server wiring, worker ownership and missing-route
behavior are described in [Shared routing in calibration and servers](routing_servers.md).

- Dense graph-local node and link indices eliminate unused global ID slots.
  The original relative index order and adjacency insertion order are preserved.
- MATSim's `TurnRestrictionsContext` constructs the same expanded topology. Only
  compact starting-link and original-node mappings survive graph construction;
  coloured builder objects, lists and maps can be collected.
- Expanded links store four topology integers instead of six; unused cached
  length/free-speed values are removed (8 bytes saved per expanded link).
- Four preprocessing workers reuse one distance array and heap each. Completed
  tasks write their landmark columns and return no distance arrays. The landmark
  selector also reuses scratch storage. Workers terminate before publication.
- `NodeMinHeap` uses its existing position array for decrease-key lookup instead
  of scanning the heap, preserving its comparisons and tie behaviour.
- Search state stores cost and arrival time. Unused cumulative distance is
  removed, and predecessors are obtained from the stored incoming link.
- Each router creates its node-to-record index on its first query and allocates
  cost, arrival-time, incoming-link and queue-position records as nodes are visited.
  Unused routers retain only their small initial buffers.
  Only touched indices are cleared between sparse queries. Heap entries retain
  graph node IDs, preserving priority-queue tie order.
- Broad queries trigger a permanent switch to dense storage between queries
  once record capacity reaches 87.5% of the graph. This prevents retaining a full
  sparse layout indefinitely; growth/conversion still needs temporary memory.
  Sparse primitive storage is `4*N + 24*C + 4*P + 12*H + 16*L` bytes per worker;
  dense storage is `28*N + 12*H + 16*L`. Here `N` is expanded nodes, `C` is record
  capacity, `P` is queue-position capacity, `H` is frontier capacity, and `L` is
  landmarks. Headers are excluded. The original MATSim workspace uses `52*N`.
- Forward and backward landmark vectors are deduplicated separately using exact
  double bit patterns. Compression is retained only with at least 10% primitive
  storage savings, including the row index. Block storage compacts in place and
  releases unused blocks without allocating another full landmark table.
- Destination landmark values are cached once per query. Each visited node's
  landmark rows are resolved once before evaluating the same bounds in the same
  order. Distances, costs, arrival times and queue priorities remain doubles.
- Factory initialization is serialized, preventing duplicate construction. The
  immutable graph is shared by network identity; minimum-cost compatibility is
  checked before sharing landmarks. Searches do not acquire the factory lock.

## Equivalence and limits

The reference is this branch's pinned MATSim version, not a newer upstream
router. The default implementation keeps double precision, the original 16-landmark
selection/count rules, restriction expansion, adjacency order, queue comparisons,
time-dependent cost evaluation and path reconstruction semantics.

Tests compare exact link/node sequences and bitwise travel times, costs and
landmark tables. Cases cover overlapping multi-link restrictions, link endpoints,
ties, disconnected/tiny networks, unused global IDs, concurrent factory requests,
and incompatible minimum costs. A controller integration runs two iterations.
Storage tests cover sparse growth/reset, dense conversion, generation wrap,
queue remapping/reopening, and exact landmark bits across block compaction.
The benchmark logs visited/expanded nodes, maximum frontier, retained record
capacity, dense/sparse mode and primitive workspace bytes for each optimized worker.
An optional real-network regression can be enabled with the Maven system property
`-Dspeedy.validationNetwork=/path/to/network.xml.gz`.

Topology and minimum link costs must remain stable after graph construction, as
for the original factory; time-dependent observed travel times remain live. A
new factory is required after changing topology or preprocessing lower bounds.
Explicit factory construction in other tools is not affected by the controller
override. Any MATSim dependency upgrade requires rerunning the equivalence suite
and reviewing this adaptation against the new implementation.

## Routing lookup allocations

Two additional lookup changes apply independently of the SpeedyALT flag:

- The local `ch.sbb.matsim.routing.pt.raptor.RaptorStaticConfig` compatibility
  override reuses a boxed zero for missing mode-to-mode transfer penalties.
  `Map.getOrDefault` evaluates its default argument even for an existing key;
  the pinned implementation can therefore allocate a `Double` on every lookup.
  The maps, transfer costs and missing-mode behavior are otherwise unchanged.
  This class is copied from MATSim **2026.0-2025w33** and must be reviewed when
  upgrading that dependency.
- `Normal.get(first, second, seed)` hashes the two strings consecutively without
  allocating their concatenation. `EqasimTravelDisutilityFactory` additionally
  stores each registered link ID's prefix hash once and shares that immutable
  array across its routing workers. It uses eight bytes per registered link ID,
  only when routing noise is enabled, and retains no person data. IDs created
  after the initial snapshot use the uncached calculation. Like the routers,
  this cache belongs to one MATSim ID registry; create a new factory when
  resetting the registry between simulations.

These changes preserve the original UTF-16 hashing, seed mixing, floating-point
operations and random draws. They reduce repeated work and temporary allocation;
that does not imply the same reduction in the live heap or required `-Xmx`.
`TestNormal`, `TestRoutingNoise` and `TestTransferPenaltyLookup` check exact draw
and cost bits, Unicode and null handling, late-created links, changed noise
parameters, map collisions/overwrites and concurrent readers.


### National lookup benchmark (17–18 October 2026)

A paired diagnostic used the same prepared Switzerland 5% population, a 45 GiB
maximum heap with compact object headers, six replanning workers, twelve QSim
workers and twelve event workers. Only the four classes implementing the lookup
changes differed between the frozen benchmark JARs.

Iteration-one replanning took 1542.5 seconds before and 1542.2 seconds after.
JFR estimated cumulative allocation fell from 1778.1 to 1198.1 GiB (32.6%); GC
reclaimed-byte totals corroborated this decrease. GC pauses fell from 21.86 to
18.36 seconds. Median two-minute post-GC occupancy floors were 23.03 and 23.32
GiB, respectively: this demonstrates less allocation churn, not a lower minimum
heap requirement. Neither measured replanning phase had a full GC.

All selected per-person modes and scores matched across 598,002 people. After
normalizing XML attribute ordering, 21 selected plans still differed in road
routes and/or timing. The entire iteration-zero leg table and all 253,618,127 events were identical
after ignoring event order within equal timestamps. Same-time event order
differed before replanning. Exact old/new random-draw bits
also matched for 8,294 actual link/person pairs from the differing routes.
The follow-up on 18 October traced all 21 first divergences to the existing
`PlanRouter.doReRoute` acceptance rule: one run kept the original trip while the
other accepted a faster trip. That rule calls `Math.random()`, outside the MATSim
seed, and is identical in the two frozen JARs. Resetting the MATSim seed does not
reproduce its decisions. Replaying both event streams also produced identical
raw travel-time counts and float means on all 6,543 links used by the differing
routes. This explains the observed variation; it does not prove equivalence for
every possible routing query. The comparison below replaces its uncontrolled draw while retaining the acceptance probabilities.

These are single paired runs on an interactive host. Background CPU load differed,
and QSim timings do not establish a speed improvement. Candidate startup included
host sleep; monotonic-clock checks confirmed that no reported timed phase did.
JFR allocation estimates exclude the earliest timestamp sample per thread within
each phase, whose weight can include allocations before recording started.


A separate QSim iteration-zero configuration test used the optimized JAR and
reduced event workers from twelve to four, retaining twelve QSim workers. Runtime
was 770.58 versus 793.73 seconds (2.9% lower in this pair), estimated allocation
287.9 versus 319.9 GiB, and average JVM CPU consumption approximately 6.11 versus
7.28 cores on the ten-core host. Queue-node allocation fell from about 45.6 to
15.8 GiB. Post-GC occupancy floors remained approximately 28.4 GiB. The complete
leg table, 1,606,123-row daily traffic table and all hourly active/lost-agent
counts matched. This supports using fewer event workers to reduce overhead;
background load differed, so it does not establish a precise speedup or an optimal
thread count. The user's simulation configuration was not changed by this test.


### Isolated QSim selector experiment (18 October 2026)

The follow-up prototype replaces repeated seepage priority-queue copies with a
scan for a unique earliest eligible vehicle. Exact priority ties use one reusable
scratch queue per worker, preserving the original insertion/poll order and
clearing references after each lookup. Changed priorities of queued transit
vehicles and the existing seepage restriction are preserved. This was initially an isolated benchmark patch. It is now installed in the
production sources, with the pinned upstream version documented in the override.

With the same inputs, twelve QSim workers, four event workers and a 45 GiB heap,
iteration-zero QSim took 622.85 versus 770.58 seconds (19.2% less in this pair).
Estimated cumulative allocation was 118.3 versus 287.9 GiB (58.9% less), and GC
pause time was 11.49 versus 13.75 seconds. Post-GC occupancy floors were 28.84
versus 28.44 GiB, so this does not establish a lower required heap. No full GC
occurred in either timed phase. The current run had no additional swap writes.

Validation covered 18,000 selector edge-case checks and 120,000 concurrent checks.
The complete 253,618,127-event stream matched after ignoring equal-timestamp
ordering; the entire leg table was byte-identical, and all 1,606,123 daily traffic
rows and 31 hourly agent-count observations matched. One run per variant supports
the direction of improvement, but not a precise repeatable speedup or later-
iteration calibration equivalence. The report and reviewable prototype patch are
under `target/diagnostics/followup_20261018/`; raw recordings and outputs are under
`/private/tmp/eqasim_followup_20261018/`.


### Combined laptop changes (18 October 2026)

The current source adds four focused changes:

- `PlanRouter` uses a stateless uniform draw keyed by the configured seed, person,
  iteration and trip ordinal. The existing acceptance probabilities are retained;
  `ReRoute` passes the current iteration. This fixes the uncontrolled `Math.random()`
  variation observed above, and changes the realization of those random decisions.
  It does not make other simulation components independent of worker count.
- `QueueWithBuffer` uses the validated seepage selector above. Unique earliest
  vehicles require only a scan; ties reuse worker-local scratch storage and release
  vehicle references after each lookup. The upstream queue restriction remains.
- `LinkCategorizer` reuses ten immutable penalty keys per configured region.
  It still computes each link's category from its current speed, lanes and attributes.
  `PenaltyGroupKey` preserves the previous hash exactly without varargs/boxing;
  `PenaltyManager` shares the boxed default zero and still reads current penalties.
- `SearchWorkspace` creates its network-sized node index on the first query,
  instead of for every constructed router. Active routers retain their index for
  reuse; unused modes and waiting strategies no longer reserve this large array.

`ReRoute` and `QueueWithBuffer` are compatibility copies of MATSim
2026.0-2025w33 and must be checked against upstream when updating that dependency.
Focused tests cover unchanged QSim vehicle identities/tie order (including mutated
queued priorities), independent scratch buffers, live category/penalty updates,
lazy workspace growth/reset, and route acceptance across iterations and worker
counts. The Switzerland package builds; 38 focused tests pass and one optional
real-network test is skipped without its external input.

The paired national benchmark uses the same deterministic acceptance patch in
both variants, isolating the three performance changes. Both runs use a 45 GiB
heap, compact object headers, six replanning workers, twelve QSim workers and four
event workers. Raw artifacts are in `/private/tmp/eqasim_laptop_20261018/`.

The paired runs completed through QSim iteration one:

| Measured phase | Reference | Updated | Runtime reduction |
| --- | ---: | ---: | ---: |
| QSim iteration zero | 794.4 s | 613.1 s | 22.8% |
| Replanning iteration one | 1604.3 s | 1547.6 s | 3.5% |
| QSim iteration one | 689.2 s | 535.6 s | 22.3% |

Replanning allocated an estimated 1199.3 versus 995.5 GiB cumulatively (17.0%
less). Its GC pause totals were essentially unchanged, 30.95 versus 31.07 s.
The observed router-array peak fell from 1.938 to 1.454 GiB (25.0%). In the direct
waiting-memory comparison, all 72 ReRoute routers waiting for mode choice used
446.37 MiB before and 0.98 MiB after. Six unused mode-choice routers saved another
43.42 MiB: approximately 489 MiB less array storage during mode choice without
reducing the six active workers.

Median two-minute post-GC occupancy floors during replanning were 23.31 versus
22.36 GiB. QSim floors did not fall: 28.35 versus 28.44 GiB in iteration zero,
and 31.93 versus 32.25 GiB in iteration one. Young-GC floors include some garbage,
and weakly observed router payloads can include unreachable objects until GC.
These observations establish a smaller routing component, not a lower minimum
whole-simulation heap. Keep the tested 45 GiB limit until a lower-heap test is run.
Neither timed run had a full GC, a sleep gap, probe errors, or additional swap
writes. This is one paired comparison on an interactive laptop, not a confidence
interval for the exact speedup. Later calibration iterations were not benchmarked.

All 598,002 full-person records, comprising 710,913 stored plans, matched after
normalizing XML attribute-map ordering; no route or timing differences remained.
The full 253,618,127-event iteration-zero stream matched as multisets within each
timestamp. The entire iteration-zero leg table was byte-identical, all 1,606,123
rows of daily traffic counts matched, and all 31 hourly agent-count observations
matched in both iterations. Iteration-one leg-duration outputs also matched;
other iteration-one aggregate files were not available before the diagnostic
stop. The modified classes in the rebuilt standalone JAR match the frozen
benchmark candidate. The local report is at
`target/diagnostics/laptop_20261018/report.html`.

The strategy-lifetime audit found one mode-choice strategy and three `ReRoute`
strategies for different subpopulations. MATSim initializes every distinct
strategy before queueing plans, then finishes them sequentially. This creates
waiting router objects; it does not create four simultaneous pools of active
workers. Completed multithreaded strategy modules clear their worker references.
Graph and landmark storage is already shared. A future shared `ReRoute` instance
could combine the three subpopulation queues while retaining their selection
weights, but must be validated for plan-selection RNG consumption, mode-specific
state and output equivalence. Its savings overlap with lazy allocation and must
not be added to the unused-array saving without measurement.

A smaller follow-up than combining strategy instances is to defer private
SwissRailRaptorCore arrays until the first PT query, preserving construction order
and random-stream assignment. The array layout and current timetable imply about
13.7 MiB per core: approximately 0.24 GiB while all 18 ReRoute cores wait during
mode choice. Cross-border plans include PT, so only the six freight cores can be
assumed unused for PT from these chains (about 0.08 GiB). These are estimates;
this follow-up is not implemented or benchmarked here.

## Conventional scoring and lazy link statistics

The CH CMDP runner enables `--analysis-only-scoring true` by default. For a
single-plan DMC configuration using only `DiscreteModeChoice`, `ReRoute` and
`KeepLastSelected`, it removes conventional per-person scorers, partial-score
histories and temporary trip-scoring plans. The event-to-leg/activity delegates
and experienced-day records remain active: travel-distance statistics,
passenger-hours/kilometres, trip/leg/activity exports and DMC utility estimation
still use their existing paths.

Conventional plan scores become **zero placeholders**, including score fields in
plan XML and conventional score statistics. They are not DMC utilities. Pass
`--analysis-only-scoring false` when actual MATSim scores are needed, including
custom extensions that consume them. The module automatically retains standard
scoring for unknown/score-dependent strategies (even initially zero-weight
ones), multiple-plan memory, score-based removal, score smoothing, score
explanations or score-bearing experienced-plan exports. It also requires DMC's
single-plan consistency checks. The option is scoped to the CH CMDP runner;
other runners keep their existing factory.

`CalcLinkStats` now allocates hourly rows on first accumulation, read or explicit
write. Reset releases those rows. This preserves the existing averaging window,
min/max/weighted averages, file format and reset-time link set. In the diagnostic
configuration (write interval 70, eight averaging iterations), rows are needed
for accumulation over iterations 63–70 and 133–140, rather than from startup.
They remain available until the existing listener resets at the next iteration.
The event volumes and travel-time collectors are separate and remain enabled.

Both changes use narrowly modified compatibility copies of
`ScoringFunctionsForPopulation` and `CalcLinkStats` from MATSim
`2026.0-2025w33`; review these copies when upgrading that dependency.

Validation on 2026-10-18:

- Eight focused regression tests pass.
- Paired small controllers complete iterations 0–2 with car/walk travellers,
  active DMC, rerouting and plan retention. All 22 compared outputs match:
  event content, trip/leg/activity tables, travel distances,
  passenger-hours/kilometres, DMC utilities and averaged link statistics.
- The optimized run has no scorer, partial-score, trip-scoring or driver-map
  allocation; its selected-plan scores are zero. An incompatible configuration
  uses conventional scorers and produces nonzero scores. Historical experienced
  records also finalize safely after a person is removed from the population.
- Link-statistics tests compare against the actual calculator loaded from the
  pinned upstream JAR: multiple-iteration output and getters, zero-flow links,
  reset, writes without observations, scaled reads, both current/legacy input
  formats, empty/header-only files and network growth after reset.

The earlier national heap snapshot attributed approximately 0.92 GiB to
conventional scoring and 2.06 GiB to an unused link-statistics table. This suggests
**about 3 GiB less live heap outside averaging windows**, with a small residual
ID-table cost. During active averaging the link table is required again, leaving
roughly the scoring saving. These are estimates from the previous snapshot,
not a new national performance measurement. Experienced-plan recording and
event-to-leg reconstruction are deliberately retained, so their roughly
1.18 GiB is not included in the saving. No new speedup or lower safe heap limit
has yet been established for these two changes.
