# Multi-mode CSV routing server

Start the server using the Switzerland standalone JAR (built with
`mvn -pl switzerland -am package -Pstandalone -DskipTests`):

```sh
java -cp switzerland/target/switzerland-2.0.0.jar \
  org.eqasim.switzerland.ch_cmdp.utils.routing.MultiModeRouterServer \
  --network-path network.xml.gz --config-path config.xml \
  --port 8080 --threads 16 --batch-size 128 --chunk-size 65536
```

The network is required. The config is optional for a car-only server and is
required for `pt`, `walk`, and `bike`. The explicit network path takes precedence
over the network path in the config. Schedule and transit vehicle paths are
resolved relative to the config. Population, facilities, and simulation output
files are not loaded.

The HTTP listener starts first, then all data is loaded eagerly on a background
thread. No routing request is needed to trigger loading. You can start the Java
process from Python, do other work, and check readiness before routing.
`GET /health` is available during loading and always returns HTTP 200 with the
current state. `GET /ready` returns 200 when initialization is complete and 503
otherwise. `GET /ready?mode=car` (or `truck`, `pt`, `walk`, `bike`) checks a particular
mode. These endpoints remain responsive while routing requests are running.

An example status response during loading:

```json
{
  "status": "loading",
  "ready": false,
  "modes": {
    "car": {"status": "loading", "ready": false},
    "pt": {"status": "loading", "ready": false},
    "walk": {"status": "loading", "ready": false},
    "bike": {"status": "loading", "ready": false}
  }
}
```

Once every configured mode's workers and shared routing data are initialized,
the server reports `status: ready` and marks its available modes as ready.
Modes missing from the config are reported as `unavailable`, with `ready: false`.
A car-only server reports PT/walk/bike as unavailable. All supported modes become
ready together after initialization; a successful readiness check means a
routing request can be accepted immediately. Initialization failures leave the
status endpoint running with `status: failed` and an `error` message. Routing
requests during loading or after initialization failure return HTTP 503.

## Python background startup and readiness

The standard-library client is in `switzerland/python/multi_mode_router.py`.
Add that directory to your Python import path (or `PYTHONPATH`):

```python
import sys
sys.path.insert(0, "switzerland/python")
from multi_mode_router import start_server, RouterClient

router = start_server(
    "switzerland/target/switzerland-2.0.0.jar",
    "network.xml.gz",
    config_path="config.xml",
    events_path="events.xml.gz",  # omit to use freeflow car times
    threads=16,
    port=8080,
    log_path="router.log",
    jvm_args=["-Xmx8g"],
)
# Returns immediately; Java loads the data in the background.
# Your other Python functions can run here.

print(router.status())
print(router.ready_for_car())
print(router.ready_for_pt())
print(router.ready_for_walk())
print(router.ready_for_bike())

router.wait_until_ready("pt", timeout=600)
router.route("trips.csv", "pt-results.csv", "pt")
router.route("mixed-trips.csv", "mixed-results.csv")

# Keep the process running for later requests; close it when finished.
router.close()
```

`start_server` launches a background process without a console window on Windows
and writes Java output to the log file. It does not wait for loading. The process
remains running until stopped, even if the Python code that launched it finishes.
`RouterClient("http://127.0.0.1:8080")` attaches to an existing server.
`is_ready(mode)` and its convenience methods perform one status check, returning
false if the process has not bound its port yet, is loading, has failed, or cannot
route that mode. `wait_until_ready` raises promptly for a failed initialization,
an unavailable mode, or a launched process that exited; it raises `TimeoutError`
if the deadline expires. Routing calls wait for the completed CSV by default;
set `route_timeout` on the client to impose a time limit. CSV paths are relative
to the Java process's working directory unless you provide absolute paths.

The server initializes its network, optional recorded travel times, mode
networks, spatial indices, ALT landmarks, transit timetable data, and independent
worker routers once. A fixed pool routes batches in parallel. CSVs are streamed
in bounded chunks; results retain input order. Requests share the pool and run
one at a time, avoiding CPU oversubscription and bounding memory independently of
the number of requests. Existing output is replaced only after a successful
request. Closing the server shuts down its request and routing pools.

## Requests

`POST /route`, with a JSON object referencing paths on the **server's filesystem**:

```json
{"inputCsv":"trips.csv","outputCsv":"routed.csv","mode":"car"}
```

Omit `mode` to use the `mode` column on each input row:

```json
{"inputCsv":"mixed-trips.csv","outputCsv":"routed.csv"}
```

A successful response is `{"trips":123,"outputCsv":"routed.csv"}`. Invalid
requests return HTTP 400, interruption returns 503, and file/system failures
return 500. The default bind address is `127.0.0.1`; `--host` overrides it.

The same API is available directly in Java:

```java
var settings = new MultiModeRouter.Settings();
settings.networkPath = "network.xml.gz";
settings.configPath = "config.xml";
settings.threads = 16;
try (var router = new MultiModeRouter(settings)) {
    router.route("trips.csv", "car.csv", "car");
    router.route("mixed-trips.csv", "mixed.csv");
}
```

## CSV format and units

```csv
identifier,origin_x,origin_y,destination_x,destination_y,departure_time,mode
trip-1,2680000,1240000,2681000,1240500,28800,car
trip-2,2680000,1240000,2681000,1240500,28800,walk
```

Coordinates must already use the network's coordinate system. The four
coordinate columns are required and finite; `identifier` is optional.
`departure_time` is seconds and defaults to zero if its column is absent.
`--departure-time` overrides all rows. With an explicit request mode, the CSV
`mode` column is optional and ignored. Otherwise it is required. Supported values
are `car`, `truck`, `car_passenger`, `pt`, `walk`, and `bike`. Truck routing requires
links whose allowed modes include `truck`. Optional matching columns `person_id`,
`trip_id`, and `trip_index` are retained in routed output. Other
extra columns are ignored.

Output contains the input identifier, coordinates and departure time, plus
`mode`, `travel_time` (seconds), `travel_distance` (meters), `access_distance`,
`egress_distance` (meters), `link_count`, `links`, `cost` (CHF), and `pt_cost_data`.
The `cost` column is blank when cost computation is disabled.
`pt_cost_data` is JSON containing a PT itinerary and its traveler profile, saved
for later pricing. It is blank for other modes.
For network modes, endpoints snap to the closest nodes in that mode's network,
as in `TripsRouter`. Travel time/distance cover the network path; access/egress
distances report the straight-line gaps to those nodes separately.
`--return-links true` includes hyphen-separated network link IDs. Teleported and
PT trips leave network path metadata empty/zero. PT time includes walking,
waiting and in-vehicle time, and PT distance includes walking and transit legs.
PT endpoints use nearest-link facilities, matching `RunBatchPublicTransportRouter`.

Every successful PT row also returns the following mode-choice columns, using
the names and units of `RunBatchPublicTransportRouter`. They are computed from
the same routing search, whether or not costs or itinerary export are enabled:

| Columns | Meaning |
| --- | --- |
| `access_travel_time_min`, `access_distance_km` | Walk to the first boarding |
| `egress_travel_time_min`, `egress_distance_km` | Walk after the last alighting |
| `transfer_travel_time_min`, `transfer_distance_km` | Walking between transit legs |
| `total_walk_travel_time_min`, `total_walk_distance_km` | Sum of access, egress and transfer walking |
| `initial_waiting_time_min`, `transfer_waiting_time_min` | First boarding wait and subsequent boarding waits |
| `in_vehicle_time_{rail,subway,bus,tram,other,total}_min` | In-vehicle time excluding waiting, by transit mode and total |
| `in_vehicle_distance_{rail,subway,bus,tram,other,total}_km` | Transit distance by mode and total |
| `transfers` | Boardings minus one, with a minimum of zero |
| `is_only_walk` | 1 for a walk-only result, otherwise 0 |
| `headway_min` | Optional timetable headway estimate |

PT-specific columns are blank for other modes and unreachable trips. A valid
zero-distance walk-only PT result has zero in-vehicle time and transfers.
Nonzero direct-walk fallbacks remain unreachable PT results, as before.
The existing `access_distance` and `egress_distance` columns contain the same
PT walking distances in meters.

Headway defaults to `NaN`, matching the batch router when headway calculation
is disabled. Set `--pt-headway-interval 900` (or
`multiModeRouter.ptHeadwayInterval=900`; Java `settings.ptHeadwayInterval`) to
search 900 seconds before and after departure using `HeadwayCalculator`.
This performs an additional timetable search per successful PT trip; the other
components require no extra search. This option is independent of the events
and VDF `--interval` option. `--export-routes true` optionally adds the itinerary
in `route` while retaining all these columns.

## Exporting routes for population routing

Start the server with `--export-routes true`, or set
`multiModeRouter.exportRoutes=true` in the MATSim config. The default is false;
Java users can set `settings.exportRoutes = true`. Each successful routed row
then includes a `route` column containing versioned JSON. It is independent of
`--return-links` and `--compute-cost`.

The JSON preserves MATSim route types, complete network link ID arrays,
teleported routes, PT line/route/stop descriptions, leg departure/travel times,
and intermediate stage activities with their locations and times. Link IDs
containing hyphens are unambiguous. MARouter exports the assigned path and its
final congested travel time. Disabled export, unreachable trips, and empty
node-to-node network paths leave `route` blank; those rows do not replace a
population route. The format is separate from `pt_cost_data`, which is for fares.

Use this workflow with the same population, network, transit schedule and
coordinate system:

1. Export trips from the population, without routing:

   ```sh
   java -cp switzerland/target/switzerland-2.0.0.jar \
     org.eqasim.core.scenario.routing.RunPopulationRouting \
     --config-path config.xml --export-trips-path trips.csv --export-only true
   ```

   Use your normal `--eqasim-configurator` argument if needed. Export-only does
   not require `--output-path` or initialize routing workers. The config still
   supplies population, network, facilities and vehicles as in ordinary
   `RunPopulationRouting`.

2. Route this CSV on a server started with `--export-routes true`:

   ```python
   router.route("trips.csv", "routed-trips.csv")
   ```

3. Reuse the routes when writing the routed population:

   ```sh
   java -cp switzerland/target/switzerland-2.0.0.jar \
     org.eqasim.core.scenario.routing.RunPopulationRouting \
     --config-path config.xml --routes-path routed-trips.csv \
     --output-path routed-population.xml.gz
   ```

The trip export contains `person_id`, `trip_id`, `trip_index`,
coordinates, departure time, mode, and `origin_link_id`/`destination_link_id`.
Trip indices are **zero-based** and count all trips between
main activities, excluding stage activities, before any mode filtering. All
trips of each person's selected plan are exported, matching the single-plan
discrete mode choice workflow. Existing origin-activity `trip_id` attributes are retained;
otherwise the default trip ID is the string form of the trip index within that
plan. `RunPopulationRouting` also writes person/trip identifiers into the
origin activity's trip attributes in its output population.

You can also supply your own CSV directly. Provide `person_id` matching the
MATSim person ID and `trip_index` identifying the trip within that person's
selected plan. The server preserves these values; it does not derive them from
coordinates or the optional `identifier` row label. For example:

```csv
identifier,person_id,trip_index,origin_x,origin_y,destination_x,destination_y,departure_time,mode
my-row-A,person-123,0,2680000,1240000,2681000,1240500,28800,car
my-row-B,person-123,1,2681000,1240500,2680000,1240000,61200,car
```

Here `identifier` and `trip_id` are both optional. Route export retains
`person_id` and `trip_index`, which `RunPopulationRouting --routes-path ...`
uses for matching regardless of CSV row order. `identifier` alone is sufficient
for ordinary routing, but does not provide a population matching key.

Network requests with both endpoint link columns use link-based routing,
respecting turn restrictions, and export a complete path including its endpoint
links. The CSV time and distance cover that complete path. This keeps imported
network routes compatible with population activity/facility links. Requests
without these columns continue to use nearest nodes; their exported routes can
only be reused when their endpoint links match the population. Keep the endpoint
columns from the trip export for the normal round-trip workflow.

Import matches `person_id` plus `trip_index` (or `trip_id`) in the selected plan.
There is no `plan_index` column. If both trip keys are supplied,
they must agree. The row's mode must match the population trip's routing mode;
other-mode alternatives and unknown persons are skipped. Duplicate matching
routes are treated as conflicting and fall back to routing. Departure time, available OD coordinates,
network continuity, allowed modes, endpoint compatibility, and PT schedule
references are checked before reuse. Incompatible matching routes are skipped
and the affected trips are routed normally.

Matched trips keep the imported itinerary even though normal population routing
replaces existing routes. Missing rows or route columns, empty routes,
`NaN`/`null` route values, nonfinite travel times/distances, malformed or
unsupported JSON, mismatched trip keys, and invalid network/PT itineraries fall
back to ordinary routing. Invalid rows do not abort the job. A summary is logged,
with details for the first ten invalid rows. Validation does not modify activity
endpoints on rejected routes.
`--modes` limits both export and routing/reuse, without renumbering trips. When
all eligible trips are imported, no population routing injector or workers are
initialized. Import streams rows directly into plans, retaining only matched
activity references for skip checks.

## Optional monetary cost estimation

Enable it with `--compute-cost true --car-cost-model simple` (or `weiss`). The
same options can be stored in the MATSim config:

```xml
<module name="multiModeRouter">
  <param name="computeCost" value="true" />
  <param name="carCostModel" value="weiss" />
</module>
```

CLI values override the config. If `carCostModel` is absent from this module,
the router uses the car cost-model selection in the `eqasim` module created by
`RunAdaptConfig`; if that is absent too, the default is `simple`.
`computeCost` defaults to false. In Java, use `settings.computeCost = true` and
`settings.carCostModel = "weiss"`.

Each routed row/alternative gets a cost for its selected mode:

- **Car:** Reuses `SwissCarCostModel` or `SwissWeissCarCostModel` on the routed
  network distance. Simple pricing uses `carCost_CHF_km` from the Swiss cost
  parameters (default 0.26 CHF/km). The router loads the optional
  `eqasim.costParametersPath` YAML relative to the config, as the simulation does.
  These models return driving costs; parking and link toll amounts are not added
  to this monetary output. Tolls still affect route choice through disutility.
- **PT:** Uses the same fare calculation as `RunComputeTransitPrices`, including
  authority grouping, half-fare and subscription rules, rounding and fare caps.
  It prices the already calculated route. When PT is configured, cost estimation
  requires all three `ptZones` paths: `ptZonesFilePath`, `sbbDistancesPath`, and
  `pricingDescriptionPath`, resolved relative to the config. These inputs are
  loaded once before the server reports readiness, and shared by its workers.
  A PT alternative containing only walking has zero ticket cost.
- **Walk, bike, car_passenger:** Zero. `car_passenger` shares the car network,
  travel times and routing behavior and is available with a network-only server.
- **Truck:** `NaN`; a truck monetary cost model is not defined.

PT rows use the same `subscriptions` attribute as mode choice: `GA` (general
subscription), `HT` (half fare), `VA` (regional), or `VA+HT` (regional plus half
fare), case-insensitively. Blank, missing, and unrecognized codes grant no
subscription. `age` defaults to 30; children receive the mode-choice discounts,
including half fare through age 16. For `VA` and `VA+HT`, provide `home_x` and
`home_y` to check regional coverage (`ptRegionalRadius_km`, default 10 km).

When `subscriptions` is absent, legacy boolean fields remain supported: `hasGA`,
`hasHalbtaxSubscription`, `hasVerbundAbo`, `hasStreckenAbo`, `hasGleis7Abo`, and
`hasJuniorAbo` (true/false). A present `subscriptions` column takes precedence,
including when blank. `homeX`/`homeY` remain aliases; `home_x`/`home_y` take
precedence. Traveler fields are parsed only when the request asks for costs.

## Separate cost requests

Start with `--compute-cost true` to load the pricing inputs at startup. This
enables both combined routing/pricing and separate cost requests; `/health`
reports `costReady: true` when these inputs are ready.

To route without any pricing or saved fare-data work, send:

```json
{"inputCsv":"trips.csv","outputCsv":"routes.csv","computeCost":false}
```

Send this body to `POST /route`. Both `cost` and `pt_cost_data` remain empty.
To calculate prices later, send the original OD/traveler input to **`POST /cost`**:

```json
{"inputCsv":"trips.csv","outputCsv":"costs.csv"}
```

The output contains only `identifier`, `mode`, and `cost` (CHF), retaining row
order. Both APIs accept an optional `mode` override. A `/route` request can set
`computeCost: true` to include costs, or omit it to use the startup default.
Cost requests on a server started with cost computation disabled return HTTP 400
with instructions to enable it.

Cost-only input can be either raw trip requests or saved routing results:

- A car row with `travel_distance` (meters) is priced directly; it does not need
  coordinates. A car row with only OD coordinates is routed to obtain distance.
- A PT row with `pt_cost_data` is priced directly using the saved itinerary and
  traveler profile; it does not need coordinates or another routing search.
  Keep this column intact when passing saved results back to `/cost`. The loaded
  transit schedule must contain the saved lines, routes and stops.
- A PT row without `pt_cost_data` is routed from its OD coordinates to obtain
  the itinerary. Provide the original traveler columns for subscription pricing.
- Walk, bike and car_passenger rows need only their identifier and mode; their
  costs are zero and no routing search is performed.

`/route` creates saved PT itinerary/traveler data only when cost is requested
(`computeCost: true`, or the enabled server default). This data preserves
subscriptions for subsequent repricing. With `computeCost: false`, PT `/cost`
requests need the original OD/traveler columns and perform another routing
search. `/cost` itself produces no saved itinerary data. Costs are recalculated
from the loaded models; an input `cost` column is ignored. Cost jobs use the
existing parallel workers, bounded chunks, request queue, and safe output
replacement.

Java methods:

```java
router.route("trips.csv", "routes.csv", null, false);
router.cost("trips.csv", "costs.csv"); // Route without costs did not save PT fare data.
router.cost("car-distances.csv", "car-costs.csv", "car");
// MultiModeRouterServer also exposes both String-path cost(...) overloads.
```

From Python, the two requests can be made independently:

```python
import requests
base = "http://127.0.0.1:8080"
response = requests.post(base + "/route", json={
    "inputCsv": "trips.csv", "outputCsv": "routes.csv", "computeCost": False,
})
response.raise_for_status()
response = requests.post(base + "/cost", json={
    "inputCsv": "routes.csv", "outputCsv": "costs.csv",
})
response.raise_for_status()
```

## Incremental road assignment (`--marouter true`)

Enable incremental BPR assignment at startup:

```sh
java -cp switzerland/target/switzerland-2.0.0.jar \
  org.eqasim.switzerland.ch_cmdp.utils.routing.MultiModeRouterServer \
  --network-path network.xml.gz --config-path config.xml --port 8080 \
  --marouter true --threads 16
```

The existing `/route` API is unchanged. Put car and truck trips in **one mixed-mode
CSV request** to assign their traffic jointly. Each request starts with zero flow.
`/health` reports `marouter: true` once initialization completes; check
`/ready?mode=car` or `/ready?mode=truck` before requesting routes. A truck mode is
available only when the network explicitly permits `truck` on some links.

The router reuses `VDFConfigGroup`, `VDFScope`, and `BPRFunction` from the core VDF
implementation. Configure them in the existing MATSim config module:

```xml
<module name="eqasim:vdf">
  <param name="startTime" value="00:00:00" />
  <param name="endTime" value="24:00:00" />
  <param name="interval" value="01:00:00" />
  <param name="bpr:factor" value="0.15" />
  <param name="bpr:exponent" value="4.0" />
  <param name="capacityFactor" value="1.0" />
  <param name="minimumSpeed" value="1.3888888889" />
</module>
<module name="multiModeRouter">
  <param name="marouter" value="true" />
  <param name="marouterIncrement" value="0.05" />
  <param name="marouterTruckPcu" value="2.0" />
</module>
```

Without this module, `VDFConfigGroup` defaults apply (the example above shows
their relevant values). Explicit `--start-time`, `--end-time`, and `--interval`
override those VDF fields, in seconds. Other VDF fields are set in the config or
through Java `settings.vdfConfig` when no VDF module is supplied. `minimumSpeed`
and truck speed limits use meters/second. Simulation-specific VDF flow history,
input files, update areas and output settings are not used by this assignment.

For each departure-time bin, the router shuffles driver trips once with a fixed
seed, assigns disjoint 5% increments, and updates cumulative link flows after
each increment. The first increment uses freeflow. Every worker within an
increment sees the same frozen travel times. Each trip is routed once; the
previous paths stay assigned. The BPR calculation is:

```text
flow = sum(weight * vehiclePcu) over assigned trips using the link
capacity = sampleSize * capacityFactor * interval * link.capacity / network.capacityPeriod
travelTime = freeflowTime * (1 + bprFactor * (flow / capacity)^bprExponent)
```

Flow is **not** extrapolated by the fraction of increments processed. Cars count
as 1 PCU and trucks as 2 PCU by default. Trucks respect their mode's allowed links
and speed ceiling. Passengers route against the final bin times and contribute
no vehicle flow. PT, walk and bike retain their ordinary routing behavior. All
road trips report travel time along their assigned path using **final** bin
flows, including trips assigned in the first increment. Car monetary costs use
the assigned path distance as usual.

| CLI option | Config fallback | Default |
| --- | --- | --- |
| `--marouter` | `multiModeRouter.marouter` | false |
| `--marouter-increment` | `multiModeRouter.marouterIncrement` | 0.05 |
| `--marouter-seed` | `multiModeRouter.marouterSeed`, then `global.randomSeed` | 4711 without a config |
| `--marouter-truck-pcu` | `multiModeRouter.marouterTruckPcu` | 2 |
| `--truck-max-speed` | `multiModeRouter.truckMaxSpeed` | 80/3.6 m/s |
| `--marouter-sample-size` | `multiModeRouter.marouterSampleSize`, then `eqasim.sampleSize` | 1 |

With `eqasim.sampleSize=0.1`, MARouter uses 10% of the full interval capacity for
the sampled trips. For example, a capacity of 1000 PCU/hour becomes 100 PCU/hour
for a one-hour bin with `capacityFactor=1`. This is mathematically equivalent to
dividing sampled flow by 0.1 against full capacity; sampling is applied only once.
Sample size comes from `eqasim.sampleSize` unless explicitly overridden in the
router config or CLI.

An optional CSV column `weight` scales each car/truck's contribution to link
flow. Missing or blank weights default to 1. Weights must be finite and
non-negative; zero-weight trips are routed but contribute no flow. For example,
with the default truck PCU of 2, a truck of weight 0.5 contributes 1 PCU. Weights
affect congestion and subsequent path choices; they do not scale the reported
individual time, distance or cost. Other modes, including car passengers,
ignore this column and never contribute to MARouter flow.

Sample size and trip weight apply independently and can be used together.
Sample size scales link capacity; weight scales vehicle PCU consumption.
For example, keep `eqasim.sampleSize=0.1` when routing every person in a 10%
population sample. If only 50% are expected to drive, give each car alternative
`weight=0.5`: every trip gets a route, while each contributes 0.5 PCU against
the 10%-scaled capacity. A truck with the same weight contributes 1 PCU with
the default truck PCU of 2. Providing weights never changes or disables the
configured sample size. Increments remain fractions of trip rows, using their
weighted cumulative flows to update travel times.

The increment must be between
0.0001 and 1; a final smaller increment handles other fractions and small bins.
Link times are capped by the VDF minimum speed, with a one-second floor on used
nonzero-length links, and never fall below freeflow. Departures outside the VDF
time range are clamped to its first/last bin, following `VDFScope`.

This is static incremental assignment: all links of a trip use its departure
bin. It does not propagate queues or spill traffic into later bins, and it does
not iterate to equilibrium. With MARouter enabled, `--events-path` is ignored
(the file is not opened); the assignment starts from calibrated freeflow and
estimates its own congestion.

The network, mode graphs, spatial indices, landmarks and workers are loaded once
in the background. Routing runs in parallel within each increment; increments
and bins run sequentially to preserve shared-flow updates. Changing thread count
or CSV chunk size does not change seeded results. Input partitions and indexed
results are spooled to temporary `.marouter-*` files beside the output, then
deleted on success or failure. Allow temporary disk space for the request and
its assigned paths. Memory retains one bin's shuffle offsets (8 bytes per driver),
one set of link flow/time arrays, and bounded routing chunks.

Separate `/cost` requests price saved distances/itineraries directly. Raw car OD
rows perform a fresh incremental assignment using those raw car rows as demand.
To reprice the exact joint car/truck assignment, pass its saved routing output.

## Routing parameters

- **Car:** Freeflow link times unless `--events-path events.xml.gz` is given.
  Without events, the link's persisted `speedFactor` calibration attribute
  multiplies its network freespeed: `speed = max(0.1, freespeed * speedFactor)`
  in meters/second, matching `FreespeedAdapter`. Missing, malformed, nonfinite
  or nonpositive factors default to 1. Numeric and numeric-string attributes
  are accepted. This applies to both ordinary freeflow and the MARouter BPR
  baseline, including ALT routing lower bounds. Factors are read once at startup.
  The same calibrated times apply to car passengers and trucks, with trucks
  additionally respecting `--truck-max-speed`. Shared bike/PT network speeds
  are not modified. Event-derived times are used directly without applying the
  factor again. MARouter ignores events even when a path is supplied.
  Recorded times use `--start-time` (default 0), `--end-time` (86400), and
  `--interval` (900); unobserved links/time bins fall back to freeflow.
  `EqasimTollsTravelDisutility` adds numeric link `toll` attributes to the
  time/distance disutility. `--routingDistanceUtility` defaults to 0 and must be
  non-negative. Its contribution is `utility * length / 10`, matching Eqasim.
  `--tolls-value-of-time` controls the currency/hour conversion, and
  `--tolls-sigma` controls the normalized log-normal variation in toll sensitivity.
  The sensitivity draw is deterministic per CSV `identifier`, using the existing
  `MarginalCostOfTolls` implementation. Rows without an identifier use a stable ID
  derived from their coordinates and departure time. With sigma zero, every trip
  uses the population-average conversion (3600/tolls-value-of-time seconds per
  currency unit).
- **PT:** SwissRailRaptor uses the config's transit routing and scoring
  parameters. If an `eqasim:raptor` module is present, its mode-specific travel,
  waiting and transfer utilities are used. Transit must be enabled and a
  schedule provided in the config.
- **Walk/bike:** `routing.networkModes` selects network routing. Otherwise,
  teleported speed and beeline distance factor come from that mode's routing
  parameters. MATSim freespeed-factor teleportation is also supported.
  Network walk uses its configured walk speed and network link speed limits.
  Network bike uses `BikeGradientBasedLinkSpeedCalculator` and the road-type,
  gradient and distance penalties in `BikeDisutilities`. Costs are deterministic
  so parallel requests are reproducible; no per-edge random noise is added.
  Bike crossing delay is zero because CSV requests do
  not carry simulated vehicle state.

Startup values follow **explicit CLI option → config parameter → Settings default**:

| CLI option | Config parameter | Settings default |
| --- | --- | --- |
| `--threads` | `global.numberOfThreads` | Available processors |
| `--routingDistanceUtility` | `eqasim.routingDistanceUtility` | 0 |
| `--tolls-value-of-time` | `eqasim:networkCalibration.tollsValueOfTime` | 15 currency/hour |
| `--tolls-sigma` | `eqasim:networkCalibration.tollsSigma` | 0 |
| `--compute-cost` | `multiModeRouter.computeCost` | false |
| `--export-routes` | `multiModeRouter.exportRoutes` | false |
| `--car-cost-model` | `multiModeRouter.carCostModel`, then Eqasim car cost-model selection | simple |

Config values are resolved during the background initialization, from the same
config read used for mode routing. Missing modules or individual parameters leave
the Settings defaults intact. Explicit CLI zero values take precedence too.
Invalid selected values fail initialization and appear in `/health` as a failure.
The former `--value-of-time` option is renamed to `--tolls-value-of-time`.
Other server options retain their Settings defaults when omitted; they do not
have corresponding entries in these config modules. Direct Java users can set
the Settings fields explicitly, including `tollsValueOfTime` and `tollsSigma`.

Increase `--batch-size` to reduce
scheduling overhead, or decrease it for uneven route costs. `--chunk-size`
controls peak CSV/result memory. Initialization stores one mutable search state
per worker and mode, while graph, landmarks and timetable data are shared.
Bike link times and costs are also precomputed and shared.

On Windows, if the JDK reports `Unable to establish loopback connection` with
`UnixDomainSockets.connect` before binding the HTTP server, its internal selector
socket setup is failing. Passing
`-Djdk.net.unixdomain.tmpdir=C:/unavailable-unix-socket-directory` before `-cp`
(using a directory that does not exist) makes the JDK use its TCP fallback. This
was needed to run the HTTP integration test on the local JDK 21.0.12.

Run the routing tests with:

```sh
mvn -pl switzerland -am test -Dtest=TestMultiModeRouterSettings,TestMultiModeRouterCosts,TestMarouterAssignment,TestReusableRouting,TestMultiModeRouterBikes -Dsurefire.failIfNoSpecifiedTests=false
```
