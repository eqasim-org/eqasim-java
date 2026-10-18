# Shared routing in calibration and servers

`RunSimulation` selects optimized SpeedyALT and float observed travel times by
default. `--memory-efficient-routing false` restores the original configured
router; `--float-travel-times false` restores the original collector.
Calibration receives the named car travel-time binding, retaining simulated
conditions rather than substituting synthetic free-speed observations.

All road-routing paths in `org.eqasim.core.components.network_calibration`
inherit these bindings:

| Component | Routing and travel times |
| --- | --- |
| Freespeed calibration | Private worker routers; shared named car travel times; fresh optimized preprocessing each pass |
| Background relocation (freight and cross-border) | Worker-local `PlanRouter`/`TripRouter`, using the simulation's configured road routers and shared travel times |
| Cross-border cloning | Worker-local `PlanRouter`/`TripRouter`, using the same simulation bindings for car/truck |
| Cost calibration | Changes road disutility; retains the simulation's router and travel-time bindings |
| Agent ASCs, capacity correction, counts/flow processing | No independent path calculator or travel-time table |

The optimized modules must be installed by the application, as in the Switzerland
runner; `NetworkCalibrationModule` does not install duplicate collectors or
override mode-specific bike/VDF travel times. PT continues to use its transit
router, and teleported modes retain their configured routing.

Calibration uses at most `global.numberOfThreads` workers, capped by trip count.
Workers take trips dynamically and own separate mutable routers and statistics.
They share one travel-time view and graph/landmark preprocessing per pass. The
optimized factory is scoped to a calibration pass: freespeed factors can change
the preprocessing lower bounds afterward, and obsolete calibration landmark sets
must not accumulate in the simulation's factory. Every worker finishes before
statistics are returned or factors change. The selected factory and worker count
are logged. Penalties are restored even if calibration routing fails.

The freespeed handler retains the source simulation network and refreshes the
speeds in its car-only network before each routing pass. The MATSim mode filter
copies links, so without this refresh subsequent factor updates would leave
free-flow fallback times and preprocessing minimum costs using old speeds.
The shared observed travel-time table is neither copied nor changed by routing.

## Standalone servers

`MultiModeRouterServer` and `TripsRouterServer` now select optimized SpeedyALT
directly for network routing; no additional switch is needed. PT in the multimode
server continues to use SwissRailRaptor. Teleported modes retain their configured
beeline/freespeed-factor behavior.

Event-derived `RecordedTravelTime` tables use primitive `float[]` rows shared by
all workers. The recorder uses float averages and temporary integer counts; its
returned snapshot owns separate arrays and has no routing-time writes. Existing
binary files still encode doubles on disk and remain readable/writable. The
multimode server also stores cached bike link times as floats. Formula-based
walk/free-flow queries do not need a stored per-thread table. Routing clocks,
accumulated costs and ALT bounds retain double precision.

Both servers keep a bounded worker pool and reuse private routers across
requests. Graphs, landmarks, spatial indices and travel-time tables are shared.
Requests are serialized against the routing pool, so concurrent clients do not
multiply routing threads and preprocessing. HTTP health handling remains separate
from queued routing requests. Work batches are taken dynamically; `batch-size`
is an upper limit, reduced for short requests to keep workers busy. Output rows
remain in input order. Changes to `TripsRouterServer`'s distance utility rebuild
its routing session before the next request, preserving preprocessing validity.

Recorded-time routing uses minima across recorded bins for ALT lower bounds,
since observed values can be faster than the network's free-flow time.

## PT cost work follows the request

`computeCost: false` skips traveler-field parsing, fare calculation, and saved PT
itinerary serialization. The `cost` and `pt_cost_data` CSV columns remain present
but empty. There is no separate saved-data option. For example, `POST /route`:

```json
{
  "inputCsv": "trips.csv",
  "outputCsv": "routes.csv",
  "computeCost": false
}
```

`computeCost: true` calculates prices and includes saved PT itinerary/traveler
data. Start with `--compute-cost true` to load pricing inputs. `/cost` requests
automatically skip saved-data serialization because their output contains only
prices. Without saved PT data, later pricing requires the original OD/traveler
input and another routing search. Routing times, distances and route selection
are unaffected by requesting costs.

The CSV `subscriptions` column uses mode choice's case-insensitive codes: `GA`
(general subscription), `HT` (half fare), `VA` (regional), and `VA+HT` (regional
plus half fare). Blank, missing or unrecognized codes have no subscription, as
in mode choice. Provide `age` (default 30) and `home_x`/`home_y` for regional
coverage. When `subscriptions` is absent, legacy boolean subscription columns
remain accepted; when present, it takes precedence. Canonical home coordinates
take precedence over legacy `homeX`/`homeY` aliases.

## Missing routes

An unreachable row in `MultiModeRouterServer` no longer aborts the CSV request.
For car, car passenger, walk, bike and PT, missing routes return `NaN` for travel
time/distance and access/egress distance, `link_count=-1`, and an empty link list.
Requested monetary cost is also `NaN`; disabled cost computation stays blank.
Repricing saved unreachable rows preserves their NaN costs. Invalid inputs and
configuration errors still fail the request.

PT direct-walk fallback itineraries with no transit leg are treated as missing
PT journeys, except for zero-distance requests. A configured beeline mode always
has its formula-based route and does not depend on network connectivity.
The JSON trips server also returns NaN metrics for unreachable car paths; Jackson
represents these non-finite values as the JSON string `"NaN"`.

If an HTTP response contains `No pt path for trip ...`, check the deployed JAR
and restart the server after replacing it. That exception text belongs to the
older implementation; the current implementation returns a NaN row and HTTP 200.
Replacing a JAR does not update classes already loaded by a running JVM. Startup
logs include the routing code-source location and the missing-route behavior.

Regression coverage includes concurrent private-router use, small-batch worker
utilization, repeated/concurrent requests, HTTP health during routing, session
reuse/reconfiguration, legacy recorded files, and missing routes for every mode
including freespeed-factor pseudo-transit routing.
