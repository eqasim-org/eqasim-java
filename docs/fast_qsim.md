# Parallel QSim scheduling

`EqasimTrafficQSimModule` selects `FastQNetsimEngine` by default when VDF is
absent. Switzerland's existing `RunSimulation` picks this up automatically.
This is a scheduling and storage adaptation originally based on MATSim
**2026.0-2025w33**, updated to run with MATSim **2026.0**.
Native network/node behavior, lane traffic rules, link speeds, intersection
delays, transit integration and both node/link phase barriers remain.

## Implementation

- Several fixed logical blocks share the configured QSim threads (four blocks
  per thread initially; one block for a single-thread simulation).
- Weighted spatial bisection estimates work from selected network routes,
  retains a small baseline weight for unused nodes, and splits along the widest
  coordinate extent. It requires no native partitioning library. Initialization
  uses temporary primitive arrays/maps and does not add scenario attributes.
  Two local graph-refinement passes reduce boundary links while limiting block
  load and keeping every block nonempty. This uses the existing fastutil dependency;
  no native partitioner or platform-specific library is required.
  A controller-scoped cache avoids rescanning national plans. On every 256th step,
  sparse per-node and outgoing-link timings collect actual processing work. The next
  iteration uses these normalized weights, plus a small cold-node baseline, to
  repartition. Individual samples are capped to limit OS-pause distortion. Assignment
  changes happen only before any QSim activation; running blocks never migrate their
  nodes. Node/edge counts, IDs, coordinates, outgoing topology, network modes and
  block-count changes invalidate the cache. Feedback retains an additional double
  per node between iterations; measurement uses a primitive index and double array.
- A node and **all outgoing links** always have the same owner block. The native
  base engine first assigns its private round-robin activators; the adaptation
  replaces them during initialization, asserting that no block is active yet.
- Spatially neighbouring block IDs share a preferred worker. Persistent workers
  process their own group first, then steal whole unstarted blocks from nearby
  groups. Large cities can span several blocks/workers; administrative city borders
  are not used. The coordinator participates, so the total executing threads equals
  the configured QSim thread count. Worker preference is Java-thread affinity, not
  OS CPU pinning, and does not guarantee NUMA placement.
- Node and link costs are sampled separately every 16 steps. Every 128 steps,
  each phase's blocks are stably ordered by their recent sampled processing time,
  longest first within each preferred worker's queue. Empty blocks are excluded;
  a single active block runs directly. Reducing active workers coalesces neighbouring
  groups rather than hashing blocks to unrelated workers.
- Scheduling reuses task arrays and atomic counters, without per-block Future
  or task-wrapper allocation. Workers briefly spin, then park until explicitly
  signalled. Completion and phase publication use volatile/atomic visibility.
  Worker exceptions propagate, and shutdown joins all workers.
  Each participant publishes its completed block count once after draining work,
  avoiding a shared atomic update per block. Claimed blocks pin the phase until
  that publication. Only the final completion wakes a waiting coordinator,
  avoiding repeated wake/park cycles for intermediate completions.
  Completion waits for all blocks, without waiting for unused workers to wake
  and acknowledge. Each worker queue has a generation-tagged atomic cursor;
  padded cursor slots reduce false sharing and distribute claim contention.
  Delayed workers cannot claim a different phase when arrays are reused.
- The number of participating threads adapts to sampled block costs and measured
  coordination overhead, separately for node and link phases. The target is at
  least 8 microseconds or four times estimated overhead per requested worker.
  Quiet phases run directly or wake fewer workers; substantial phases can use all
  configured threads. New blocks with unknown costs retain full parallelism until
  sampled. A two-worker probe every 1024 steps prevents stale overhead estimates
  from permanently disabling useful parallelism. Overhead estimates are bounded
  and smoothed; they are heuristics, not measurements of pure wake-up latency.
- Active links and surviving active nodes use reusable arrays with stable
  compaction. New node activations reserve distinct slots in a reusable array,
  sized to the number of nodes owned by the block. All producers finish before
  the node-phase consumer drains it; the existing phase barrier publishes slot
  writes. The consumer clears references and resets the reservation count.
  There is no per-activation queue-node allocation. This is deliberately a
  phase-specific buffer, not a general queue that permits concurrent consumption.
  Survivor order and reservation order are retained; concurrent producers may
  interleave differently, as with the original parallel queue. Swap-removal is
  not used. Duplicate activations/capacity violations fail instead of dropping work.
- Lane exit buffers use lazy circular arrays of vehicle references and primitive
  double flow consumptions. Unused lanes allocate no exit-buffer object or arrays.
  Repeated vehicle transfers reuse storage instead of allocating linked-list
  nodes, pairs and boxed consumption values. Capacity is still subtracted in FIFO
  order, using the same doubles, rather than a cached sum with different rounding.
  Previous-vehicle/time history uses direct fields. The exact native default flow
  calculator has a primitive fast path that reads the vehicle's flow-efficiency
  factor; custom calculators and subclasses still receive the native arguments.
  Fast capacity updates check the existing time guard before scanning buffered
  consumptions, avoiding redundant scans within the same simulation tick.

The engine logs initialization time, cumulative node/link phase elapsed times,
direct/parallel phase counts and sampled block work. Sampled worker occupancy
measures elapsed time inside block processing versus available worker time in
sampled parallel phases. It includes contention/preemption inside blocks and
must not be interpreted as CPU utilization. Logs also report requested worker
counts, the sampled fraction of stolen blocks and estimated coordination cost.
Requested workers are not a count of simultaneously busy physical cores.

## Asynchronous event delivery

The pinned `ProcessEventThread` adaptation transfers each existing producer
buffer as an array-backed batch instead of copying its events into individual
`LinkedBlockingQueue` nodes. Event handling remains sequential per handler group,
with the same accepted FIFO order, buffer threshold, end-marker protocol and
interrupt checks between events. This optimizes event delivery for scoring,
travel-time collection and other handlers, including iterations with no event
file output.

The producer owns a buffer until publication through the blocking queue. The
consumer clears each consumed event reference immediately and returns a completed
array through an atomic ownership transfer. A worker retains at most one spare
array. Producer and consumer never mutate the same published array concurrently.
The existing single-event queues remain available with
`-Deqasim.events.batchQueues=false`; the default is `true`.

This applies to `ParallelEventsManagerImpl`, selected with an explicit
`eventsManager.numberOfThreads`, `oneThreadPerHandler=false` and
`synchronizeOnSimSteps=false`. The step-synchronized event manager retains its
own implementation. These changes do not parallelize an individual handler or
change the configured number of event workers.

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
- `-Deqasim.qsim.localScheduling=false` uses a shared work queue instead of
  preferred-worker queues (the persistent scheduler and phase barriers remain).
- `-Deqasim.qsim.adaptiveScheduling=false` restores the fixed per-worker work
  threshold. Setting `minWorkPerThreadNanos=0` disables both cost-based adaptations.
- `-Deqasim.qsim.partitionFeedback=false` keeps route-derived partition weights
  between iterations and disables per-element timing feedback.
- `-Deqasim.qsim.refinePartitions=false` skips graph-boundary refinement.
- `-Deqasim.qsim.arrayActivations=false` restores the concurrent linked activation
  queue. These five new optimizations default to `true`.
- `-Deqasim.qsim.compactBuffers=false` restores linked-list exit-buffer storage
  for comparison. The default is `true`; this lane optimization also applies
  when selecting the native thread-pool scheduler. Previous-vehicle/time fields
  and the exact-default flow-calculator fast path and same-tick capacity guard
  remain optimized in this fallback.

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
Review the pinned asynchronous `ProcessEventThread` source and its end-marker
protocol when upgrading MATSim as well.

Validation includes scheduler publication/exactly-once execution over thousands
of phases, failure and interruption handling, deterministic partition coverage,
and native-versus-fast event comparisons through congested merging/splitting
networks using FIFO, PassingQ and SeepageQ with one and four threads.
A delayed-worker test holds a worker before its loop while the coordinator
finishes successive phases, then releases it to exercise stale requests.

Exit-buffer validation compares all event attributes/timestamps with linked-list
storage through congested merges/splits: FIFO, PassingQ and SeepageQ; queue,
withHoles and kinematicWaves; fast and slow capacity updates; one and four workers.
Vehicle types have different non-unit flow-efficiency factors. Additional runs
compare history-dependent custom flow-calculator inputs and resulting events,
including a subclass of the native default calculator. Buffer tests cover wrapped
growth, bitwise capacity subtraction, reference release and zero steady-state
allocation. Snapshot integration remains covered by the seepage QSim tests.

Event-queue tests compare batch and single-event storage under concurrent
producers, partial final buffers, queued events after the end marker, interruptions
both while idle and inside a batch, and handler exceptions. A four-group test
checks every event and each producer's order over repeated iterations, verifies
handler thread isolation, and checks native synchronous delivery after finishing.
The shared-float-times controller test runs two iterations with four asynchronous
event workers.

The 2026-10-17 Lausanne before/after recordings, timing and outcome comparisons
are in `target/qsim-optimization-20261017/REPORT.md`. Allocation numbers from JFR
are weighted sampling estimates, not retained heap measurements.

## Scaling validation (2026-10-18)

The scheduler is tested with 24/48 configured workers, skewed preferred ownership,
changing parallelism, delayed workers, empty phases and 48 simultaneous task
participants. Phase buffers are stress-tested with concurrent producers over
repeated barriers. Complete synthetic events are compared against native QSim,
including high worker counts and the linked activation fallback. Partition tests
check graph-boundary reduction, work balance and feedback isolation across
iterations.

The benchmark machine is an AMD Ryzen 7 PRO 6850U with eight physical cores
and 16 logical processors. Runs requesting 24/48 QSim
threads test oversubscription and correctness, not physical-core scaling. A Swiss
national workload on a machine with 24/48 available cores is needed to establish
its best thread count. Keep CPU capacity available for event handlers and other
concurrent work; configuring more workers cannot create more hardware resources.
Current benchmark cases and measurements are in
`target/qsim-scaling-20261018/REPORT.md`.
