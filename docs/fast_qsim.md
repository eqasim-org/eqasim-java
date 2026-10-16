# Parallel QSim scheduling

`EqasimTrafficQSimModule` selects `FastQNetsimEngine` by default when VDF is
absent. Switzerland's existing `RunSimulation` picks this up automatically.
This is a scheduling adaptation of **MATSim 2026.0-2025w33**, not a new traffic
model. Native QNetwork/QNode/lane implementations, capacity rules, link speeds,
intersection delays, transit integration and both node/link phase barriers remain.

## Implementation

- Several fixed logical blocks share the configured QSim threads (four blocks
  per thread initially; one block for a single-thread simulation).
- Weighted spatial bisection estimates work from selected network routes,
  retains a small baseline weight for unused nodes, and splits along the widest
  coordinate extent. It requires no native partitioning library. Initialization
  uses temporary primitive arrays/maps and does not add scenario attributes.
  A controller-scoped cache retains only the assignment array (four bytes per
  node) between iterations, avoiding another national route scan. Node/edge counts,
  node IDs/coordinates, outgoing degrees, network modes and block-count changes
  invalidate it. Later route
  changes are handled by runtime-based scheduling rather than repartitioning.
- A node and **all outgoing links** always have the same owner block. The native
  base engine first assigns its private round-robin activators; the adaptation
  replaces them during initialization, asserting that no block is active yet.
- Persistent workers and the coordinator claim whole unstarted blocks. The
  coordinator participates, so the total executing threads equals the configured
  QSim thread count, rather than adding another simulation worker.
- Node and link costs are sampled separately every 16 steps. Every 128 steps,
  each phase's blocks are stably ordered by their recent sampled processing time,
  longest first. Empty blocks are excluded; a single active block runs directly.
- Scheduling reuses task arrays and atomic counters, without per-block Future
  or task-wrapper allocation. Workers briefly spin, then park until explicitly
  signalled. Completion and phase publication use volatile/atomic visibility.
  Worker exceptions propagate, and shutdown joins all workers.
  Only the last block completing a phase wakes a waiting coordinator, avoiding
  repeated wake/park cycles for intermediate completions.
  Completion waits for all blocks, without waiting for unused workers to wake
  and acknowledge. A generation-tagged atomic cursor prevents delayed workers
  from claiming a different phase's work when the arrays are reused.
- The number of participating threads adapts to sampled block costs, targeting
  at least 8 microseconds of work per thread. Quiet phases run directly or wake
  fewer workers; substantial phases can use all configured threads. New blocks
  with unknown costs initially retain full parallelism until sampled.
- Active links and surviving active nodes use reusable arrays with stable
  compaction. New node activations still use a multi-producer concurrent queue;
  they are appended to the active array at the next node phase. Survivor order
  and incoming activation order are retained; swap-removal is not used.

The engine logs initialization time, cumulative node/link phase elapsed times,
direct/parallel phase counts and sampled block work. Sampled worker occupancy
measures elapsed time inside block processing versus available worker time in
sampled parallel phases. It includes contention/preemption inside blocks and
must not be interpreted as CPU utilization.

## Comparison and tuning

Java system properties precede `-cp`:

```text
java -Xmx32g -Deqasim.qsim.engine=native -cp switzerland.jar org.eqasim.switzerland.ch_cmdp.RunSimulation --config-path scenario_config.xml
java -Xmx32g -Deqasim.qsim.engine=fast -cp switzerland.jar org.eqasim.switzerland.ch_cmdp.RunSimulation --config-path scenario_config.xml
```

The default is `fast`. Optional developer tuning properties:

- `-Deqasim.qsim.blocksPerThread=4` (1..64).
- `-Deqasim.qsim.partitioning=weighted` or `roundRobin`.
- `-Deqasim.qsim.minWorkPerThreadNanos=8000` (0 disables cost-based adaptation).
- `-Deqasim.qsim.workerSpins=512` (0 parks immediately; larger values trade CPU
  time for lower wake-up latency).

QSim threads still come from the existing configuration pipeline. The Switzerland
configurator sets a default of at most 12 and command-line configuration is applied
afterward; use `--config:qsim.numberOfThreads N` to benchmark other counts.

Repartitioning and interleaving can change simultaneous event delivery order;
arbitrary external handlers must already tolerate native parallel QSim ordering.
Tests compare full event attributes/times after sorting across workers. Full-scale
outcomes and scaling must also be checked on representative national workloads.

## Upgrade maintenance

The engine extends MATSim's package-private base engine. A version-pinned
`AbstractQNetsimEngineRunner` retains the native no-argument path and adds a compact
queue constructor for this engine. The `native` switch retains the native runner
queues and native thread-pool engine for comparisons. Review both base classes,
activation ownership and phase ordering when upgrading MATSim. Existing QNetwork
factories, including the lanes factory, are retained rather than replaced.

Validation includes scheduler publication/exactly-once execution over thousands
of phases, failure and interruption handling, deterministic partition coverage,
and native-versus-fast event comparisons through congested merging/splitting
networks using FIFO, PassingQ and SeepageQ with one and four threads.
A delayed-worker test holds a worker before its loop while the coordinator
finishes successive phases, then releases it to exercise stale requests.
