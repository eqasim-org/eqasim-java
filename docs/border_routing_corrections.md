# Border-link preparation and destination-turn corrections

The changes are based on `b168c2b28f5825446b2aa0b592e33ea3a784eb49`, the
Java revision used by the audited prepared Switzerland scenario.

## Length preparation

`AdjustLinkLength` uses the horizontal node separation as its lower bound.
The supplied road length and existing one-second minimum travel-time rule remain.
A missing elevation encoded as zero must not turn a short cross-border link into
an artificial several-hundred-metre climb. This does not change elevation inputs,
link geometry, freespeed, or previously supplied longer road lengths.

Consequently, rerun preparation from the upstream network. Running the new check
on an already prepared network does not shorten its inflated lengths.

## Destination activity links

The routing module appends the destination link after SpeedyALT returns its
internal path. A link query must therefore reach an approach state that permits
that final transition. Checking only the physical destination node previously
allowed a prohibited final U-turn. The new check uses outgoing edges of the
restriction-expanded graph, including multi-link restriction history. If no
legal approach exists, the router returns no route rather than accepting a
forbidden turn. It does not change activity link assignments or invent restrictions.

Destination-aware queries also need safe landmark lower bounds. Several colored
states can represent the same physical destination. The forward target bound is
the minimum over those states. Existing landmark preprocessing consolidates some
original rows using minima from colored copies; a consolidated forward row cannot
safely be subtracted at the current original state, and a consolidated backward
row cannot safely be subtracted at the destination. Those bounds are disabled
where affected, while other bounds remain available. Infinite bounds are evaluated
independently so NaN does not mask a valid bound. Node queries retain their legacy
behavior. Dead-end pruning includes colored destination components.

One shared integer per routing node indexes equivalent states (only in graphs
with restrictions). There is no extra landmark table or per-query destination
list. Weaker bounds can expand more nodes on some queries; the small local replay
is not a simulation performance benchmark.

`RunPopulationRouting` now installs `MemoryEfficientRoutingModule` for SpeedyALT
by default, as the Switzerland simulation already does. Other routing algorithms
are unchanged. Explicit `--memory-efficient-routing false` selects the upstream
implementation and does not receive the destination fix. Imported routes are
reused as provided; regenerate them to benefit from this correction.

## Validation

Focused command, JDK 25, Maven offline:

```sh
mvn -o -pl core -am test -Djava.awt.headless=true \
  -Dtest=TestMemoryEfficientSpeedyALT,TestCompactRoutingStorage,TestMemoryEfficientRoutingModule,TestDestinationLinkRestrictions,TestAdjustLinkLength \
  -Dsurefire.failIfNoSpecifiedTests=false
```

25 tests passed; one optional external-network test was skipped. Coverage includes
6,400 link-pair comparisons against zero-heuristic Dijkstra on a restricted grid,
full-route restriction checks, time-dependent comparisons, unchanged node queries,
no-legal-approach cases, and a small two-iteration controller run. Exact parity
with upstream link queries is intentionally not required where its final turn or
landmark bound is incorrect; unrestricted queries still require exact parity.

A separate replay retained 16 start/end link pairs from four problematic saved
travellers. The regional network contained 293,063 car links and a 20 km buffer
around the saved routes. Static costs were free-flow time plus 0.01 times distance;
678 restriction sequences leaving the extract were removed. This is a controlled
routing diagnostic, not a reproduction of calibrated national demand.

| Replay | Prohibited sequences | Immediate reverse-link pairs | Summed distance |
| --- | ---: | ---: | ---: |
| Upstream router, original preparation | 4 | 9 | 805.58 km |
| Corrected router, original preparation | 0 | 6 | 792.38 km |
| Corrected router, horizontal preparation | 0 | 6 | 776.27 km |

All 16 routes remained reachable. The six remaining reversals are allowed by the
retained network restrictions. Mapped border-passage totals for these four people
were unchanged. Other repeated crossings can depend on candidate locations,
endpoint geography and legal road approaches; this fix does not establish that
all crossing assignments or group identities are correct.

## Using the build

Build the standalone Switzerland JAR:

```sh
mvn -o -Pstandalone --projects switzerland --also-make package -DskipTests
```

Set the Python pipeline's existing `eqasim_path` option to the absolute path of
`switzerland/target/switzerland.jar`, keep SpeedyALT enabled, and rerun
`matsim.simulation.prepare`. This rebuilds prepared network lengths and routes
from upstream inputs. Do not overwrite old caches or reuse old imported routes.
Use the same corrected JAR for simulation. A full pipeline/simulation rerun is
still needed to measure final crossing counts and performance.
