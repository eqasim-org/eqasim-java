# Memory-efficient SpeedyALT

The optional implementation in `org.eqasim.core.components.routing.speedy` is
adapted from the MATSim **2026.0-2025w33** sources used by this branch. It does not
replace classes in the `org.matsim` namespace or require a MATSim upgrade.

Build the standalone artifact with
`mvn -Pstandalone -pl switzerland -am package`. The runnable artifact is
`switzerland/target/switzerland-2.0.0.jar`; it can be copied as `switzerland.jar`.

## Switzerland simulation

Add `--memory-efficient-routing true` to the arguments of
`org.eqasim.switzerland.ch_cmdp.RunSimulation`. The controller configuration must
select `routingAlgorithmType=SpeedyALT`. The switch defaults to `false`; omitting
it or setting it to `false` uses the original MATSim implementation.

```text
java -Xmx16G -cp switzerland.jar org.eqasim.switzerland.ch_cmdp.RunSimulation --config-path lausanne_config.xml --memory-efficient-routing true
```

The override replaces the network path-calculator factory. It receives the same
travel times and disutilities as before, including toll and calibration costs.
It does not replace SwissRailRaptor, QSim, or the intersection-delay model.
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
- Each worker initially stores one node-to-record index and allocates cost,
  arrival-time, incoming-link and queue-position records as nodes are visited.
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
