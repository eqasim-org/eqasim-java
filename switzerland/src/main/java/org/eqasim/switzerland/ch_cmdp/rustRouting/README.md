<!-- Describes native PT integration, worker ownership, build, and supported behavior. -->
# Native Rust public transport routing

The router is maintained publicly at [matsim-eth/RustRouter](https://github.com/matsim-eth/RustRouter).
The local checkout may still be named `MultiModalRouter`. Its `docs/interop.md` covers the shared
Python/standalone Java API and clone/build workflow. MATSim keeps this specialized adapter so its
queries use numeric JNI calls directly, without JSON conversion or nested query pools. The native
library protocol is checked at startup; deploy a matching jar/library pair.

Run the simulation with `--useRustRouter true` (default false). This replaces scheduled PT search;
road routing and mode-choice utility/fare/subscription code remain in Java. `--cache-pt-routes true`
can still wrap the backend. Cache hits bypass JNI completely.

```powershell
mvn -pl switzerland -am package -Pstandalone -DskipTests
java -Xmx10g -jar switzerland/target/switzerland.jar --config-path C:/scenario/config.xml --useRustRouter true
```

The normal `standalone` profile now embeds the host's native library and Linux x86-64 automatically. The separate
`rust-router` profile remains a compatibility alias; it is no longer necessary. Your usual command is:

```text
mvn -Pstandalone --projects switzerland --also-make package -DskipTests
```

The build resolves the latest Rust repository `main` commit on every invocation and builds that
same immutable revision for every bundled platform. It caches compiled outputs by commit and checks
their SHA-256 before reuse. It never silently selects an older release or a sibling working checkout.
Git, Cargo/Rust and a native linker are required on the build machine. Windows-to-Linux builds also
require Python; Maven provisions isolated Zig/cargo-zigbuild tools and targets glibc 2.17. The build
fails if a requested platform cannot be produced. Non-standalone development builds do not acquire Rust.
Use `-Drust.router.project=/path/to/checkout` only to explicitly build a local working tree instead.

To build without a Rust toolchain, supply already compiled libraries for all requested platforms:

```text
mvn -Pstandalone --projects switzerland --also-make package -DskipTests -Drust.router.nativeDirectory=/path/to/natives
```

Use `libmatsim_pt_router.so` for Linux or `libmatsim_pt_router.dylib` for macOS. A jar built on Windows
contains Windows plus Linux x86-64 by default, so it can be copied to a Linux cluster. Set
`-Drust.router.platforms=host` for a deliberately host-only jar, or provide a comma-separated list
such as `host,linux-x86_64,linux-aarch64` when matching compiled packages are available. Foreign
platforms other than Linux x86-64 require explicit compiled libraries or matching release packages;
they are never silently built using the host Cargo target.
For offline builds, `-Drust.router.nativeDirectory=/path/to/natives` supplies files laid out as
`<platform>/<library>`. Resources are namespaced as
`native/<platform>/<library>` to avoid mixing architectures. SHA-256/version metadata and the Rust
library's license are included. No Git, Rust, GitHub token or download is required **at runtime** for
a matching bundled platform. The native library is extracted to a temporary directory automatically.

With the argument absent/false there is no runtime installation,
download, clone or native initialization, and SwissRailRaptor remains selected. The config's
`enabled` setting can also select the backend; an explicit argument overrides it.

## Automatic setup

All setup runs in the background alongside controller construction. The first routing request
waits for it if needed. The resolution order is:

1. Explicit `libraryPath`, resolved against the MATSim config.
2. The platform library already bundled in the simulation jar.
3. An installed library on `java.library.path`.
4. A verified, cached installation of the selected release/platform.
5. Download the matching compiled GitHub release, verify its SHA-256 and extract only its JNI library.
6. If permitted and no compiled package is available, clone the pinned release tag and build it.

Installations are cached in `~/.eqasim/rust-router/<version>/<platform>/`. A file lock coordinates
simultaneous simulations; content-addressed libraries are immutable while JVMs use them. Downloads
and partial builds are never treated as ready installations. Reusing the cache needs no network.
No backend silently falls back to SwissRailRaptor after a Rust initialization error.

The Rust repository is currently private. Downloading needs `GITHUB_TOKEN` or `GH_TOKEN` in the
process environment with access to that repository. Tokens are not stored in MATSim config or output
files, passed to Cargo, or forwarded to download hosts. Source cloning can instead use Git's existing
credentials; it runs noninteractively. Windows x64 has a full compiled release and Linux x86-64 has
a native-only JNI release package. Other platforms need a supplied/bundled library or a source
build on their host platform until matching release assets exist.

Only source builds require Git, Rust/Cargo and a native linker. The module does not install system
compilers. First-time source compilation can take several minutes; logs are kept in the installation
directory's `installation.log`. A supplied/bundled library avoids this entirely. Set `autoInstall=false`
for strictly offline deployment. Set `allowSourceBuild=false` to require a compiled release.

`sourceDirectory` optionally selects an existing checkout when automatic installation is needed.
It builds that checkout as-is without changing its Git state. Cargo output stays in the installation
cache, not the checkout; this explicit source setting is rebuilt incrementally on each startup.

When compiling the native library, Rust/Cargo and a native linker are needed at build time.
The default build source is a cached checkout of the latest published `main` commit. Override
`-Drust.router.project=/path/to/MultiModalRouter` for an explicit working tree and, if Cargo is absent
from PATH and its standard home directory, `-Dcargo.executable=/path/to/cargo`. The default build
version is `latest`; `-Drust.router.version=vX.Y.Z` instead selects release acquisition. Runtime
automatic installation without a bundled library has its own configured release version.
No server or Rust installation is needed at runtime.
For `java -cp`, include `--enable-native-access=ALL-UNNAMED`; the executable jar grants native access
through its manifest. Native timetable/workspace memory is outside `-Xmx`; measure process RSS too.

Alternatively set `swissRustRouting.libraryPath` to a downloaded or separately built library,
resolved relative to the config file. Explicit/bundled libraries are used without downloading or
compiling anything. An incompatible library fails at startup.

```xml
<module name="swissRustRouting">
    <param name="enabled" value="false" />
    <param name="libraryPath" value="" />
    <param name="autoInstall" value="true" />
    <param name="version" value="v0.2.0" />
    <param name="installationDirectory" value="" />
    <param name="allowSourceBuild" value="true" />
    <param name="sourceDirectory" value="" />
    <param name="maxBoardings" value="64" />
    <param name="maxDurationSeconds" value="172800.0" />
</module>
```

The argument overrides `enabled`. These explicit search limits bound very long itineraries;
increase duration for multi-day schedules. Native times are unsigned milliseconds since service-day
midnight (under 49.7 days). Coordinates must be projected metres, as in Switzerland scenarios.
Costs come from the loaded Eqasim/Raptor configuration after command overrides: walking, waiting,
submode utilities and directional or duration-dependent transfer penalties. Rust applies penalties
once per actual transfer. It does not reproduce SwissRailRaptor 2026's observed duplicate
mode-penalty charging; route choices can differ even with the same coefficients.

## Startup and concurrency

To reuse the module in another `ch_cmdp` entry point, prepare after adapting/loading the scenario:

```java
var settings = ConfigUtils.addOrGetModule(config, RustRoutingConfigGroup.class);
settings.enabled = true;
try (var rust = RustRoutingModule.prepare(scenario, settings)) {
    var controller = new Controler(scenario);
    // Install the application's normal modules first, then select the PT backend.
    controller.addOverridingModule(new RustRoutingModule(rust));
    controller.run();
}
```

`prepare` returns null without starting anything when disabled. `RunSimulation` already handles
this case and installs the optional PT cache after selecting the backend. The adapter passes the
actual in-memory schedule and imported Eqasim/Raptor parameters, including command overrides, into
Rust. Re-reading the original config there would miss scenario and parameter adaptations.

After scenario adjustments and cleanup, one background worker streams a compact snapshot of the
actual loaded schedule and preprocesses the native timetable/search indices. This overlaps
controller construction. Starting after network edits prevents concurrent network reads/writes
when DSim creates its isolated bicycle layer. Snapshotting never changes scenario objects. Routing waits only when it
needs the completed timetable. The temporary snapshot is deleted on completion or failure.
MATSim's existing DMC/routing threads call JNI directly, share immutable timetable/cost indices,
and reuse a native workspace per thread. There is no inner worker pool, HTTP, JSON per query,
per-person timetable, or lock held during search. Small numeric arrays cross JNI; schedule IDs,
modes and network-distance metadata are indexed once during startup.

Every result becomes fresh MATSim passenger routes, walking legs and PT interaction activities.
Line/route/stop IDs, boarding time, waiting-inclusive travel time, network distance and totalRouteCost
remain available to simulation and fare estimation. Unreachable queries follow MATSim's normal walk
fallback. Stop search uses the same nearest-stop extension and retries up to five wider radii when
no PT path is found. A cheaper direct walk also follows the usual walk fallback.

Shutdown releases handles, including after failed startup/controller exceptions. In-flight queries
hold shared ownership so close cannot free their timetable underneath them. Rust panics and invalid
requests become Java exceptions; ordinary no-path results do not.

## Supported scope

Static, single-departure scheduled PT with teleported walking and global Eqasim costs is supported.
Capacity-sensitive routing, range queries, intermodal/network walking, passenger mode mappings and
chained departures fail explicitly. Schedule-change events also fail rather than returning stale
routes. The core respects boarding/alighting permissions, explicit stop-pair transfer times, stop
access/egress times, dwell and fractional times. Physical transfers use Raptor's rounded walking
time/minimum transfer time; the output converter applies its transfer-walk margin. Rust can explore
additional valid stop-level alternatives that Raptor's optimized route-stop pruning omits.

Invalid used offsets fail with line/route/stop context. Only an unused terminal departure offset
is normalized to terminal arrival. No scenario data are modified and services are never silently
dropped. Geneva's original schedule has a known invalid interior offset; integration checks use the
previously documented benchmark schedule excluding that bad service. Repair production input
separately. See MultiModalRouter/examples/geneva reports.

Native integration tests use `-Drust.router.test.library=/absolute/path/to/library`. Without it,
ordinary Maven tests do not require a native installation. Concurrency checks use 12 calling threads.

## Large Swiss timetables

The native router now activates boardable trips through shared stop/continuation indices, clears
only touched workspaces, and uses shared geographic-cell potentials with local bound refinement.
Regions guide admissible pruning; they do not clip routes to city boundaries. Difficult no-path
queries can be rejected by a temporal feasibility check before expanding more cost frontiers.

At iteration end the adapter logs native query count, empty results, expanded searches, mean
native-call duration and summed parallel worker time. Worker time is not elapsed iteration time.
The Java snapshot shares static access/egress durations across workers. Native protocol 1 is retained.

The standard standalone build resolves the latest published Git `main` commit and builds matching
Windows/host and Linux x86-64 libraries automatically:

```text
mvn -Pstandalone --projects switzerland --also-make package -DskipTests
```

It checks GitHub on each build and caches compiled binaries by immutable commit and platform.
`native/<platform>/build.properties` records that commit and the library SHA-256. If the latest
revision cannot be resolved or built, packaging fails instead of silently using an older release.
The first build requires Git, Cargo/Rust and the host linker. Windows-to-Linux builds additionally
use Python to provision isolated, pinned Zig/cargo-zigbuild tools and a Rust Linux target.
Later builds reuse verified binaries when the remote commit has not changed. Private-repository
access uses the existing noninteractive Git credential helper. The resulting jar needs no such
tools or network access on the cluster.

Optional overrides remain available: `-Drust.router.version=vX.Y.Z` pins a release,
`-Drust.router.project=/path/to/checkout` builds that working tree without updating it,
`-Drust.router.nativeDirectory=/path/to/platform-directories` bundles supplied libraries, and
`-Drust.router.platforms=host` disables the extra Linux platform. Explicit missing binaries fail.
Copy the resulting jar to the cluster and keep the usual `--useRustRouter true` command.

Service-aware nested partitioning is controlled by `networkPartition` (default true).
The optional `transferBlocks` setting (default false) adds shared destination-cell and mode-change
suffix bounds. Set it with `--config:swissRustRouting.transferBlocks true` and measure its benefit
on the target workload. Both mechanisms preserve generalized-cost routing; these are cost-aware
adaptations of the papers' ideas, not arrival-time-only hard route flags.

Query-local lower-bound preparation now defaults to at most 1024 settled stops. It limits only
heuristic preparation, never the route search. The actual weighted route objective is unchanged.
Set `--config:swissRustRouting.localBoundSettlements 0` to restore unlimited bound preparation for
diagnostics; non-default values require a native library implementing this new search option.
The optimized default also limits future-service activation using the current feasible journey,
all ready labels and the minimum time rate. Ties and zero-rate settings remain supported.

Local Swiss checks at eight workers found approximately 13-14% lower native time and about 6%
lower time through the full MATSim transit-leg adapter. All 1000 itineraries matched. These are
query benchmarks, not whole-simulation or 48-worker measurements. See the Rust Switzerland example.

## Cluster comparison with SwissRailRaptor

The standalone jar includes `org.eqasim.switzerland.ch_cmdp.rustRouting.RunComparePublicTransportRouters`.
For example, inside an allocation with 48 CPUs:

```sh
java -Xmx10g --enable-native-access=ALL-UNNAMED -cp switzerland.jar \
  org.eqasim.switzerland.ch_cmdp.rustRouting.RunComparePublicTransportRouters \
  --config-path switzerland_config.xml --threads 48
```

Only the config and thread count are required. The default samples 1000 actual selected-plan trips
uniformly using seed 6012 in one streaming pass over the configured population. Freight, trips under
200 m, departures outside 05:00-22:00, and trips missing coordinates/departure times are excluded.
Coordinates fall back to activity-link coordinates when necessary. The full population is not kept
in memory. Config input paths resolve relative to the config file, and `--config:` overrides work.

Both routers use the effective Eqasim and SwissRailRaptor parameters, including mode-specific
transfer penalties and resolved walking speeds. Shared preprocessing is done once per engine;
each worker owns its SwissRailRaptor instance and Rust workspace. Every worker is initialized
before measurement. Both engines then warm 1000 requests and run three timed passes in alternating
order on exactly the same requests. Query timing includes access/egress, radius expansion and
MATSim transit-leg construction. Input loading, sampling, preprocessing, warmup and route analysis
are excluded. Both indexes remain loaded, so this is not an isolated memory comparison.

Outputs default to `pt_router_benchmark/`: `requests.json` (coordinates/departure seconds, without
person IDs), `routes.csv` (per-trip metrics, latency and errors) and `summary.json` (preprocessing,
wall times, throughput, route comparison and effective speed). A summary table is printed to stdout.
Unavailable PT values are NaN; a walk-preferred result and an unreachable scheduled journey both
count as no PT. Unexpected routing exceptions are counted separately. Route differences are reported,
not treated as an exact-match failure. Similar travel times use max(60 seconds, 5% of the longer time).
Means for route metrics include successful PT journeys only; paired differences use PT in both.
Latency metrics come from the last measured pass; summed query worker time is not CPU utilization.

To repeat the exact same sample and skip population reading, add:
`--trips-path pt_router_benchmark/requests.json`. This accepts a JSON array of `origin: [x,y]`,
`destination: [x,y]`, and `departure` in seconds or HH:MM:SS format, including the earlier Swiss
example's `local/queries.json`. The request-file checksum is recorded in the summary.

Other options: `--requests` (1000), `--seed` (6012), `--repeats` (3), `--warmup` (request count),
`--output-path` (directory), `--time-tolerance-seconds` (60), and `--time-tolerance-percent` (5).
A value above 1 for the printed Rust speed ratio means higher Rust throughput. No controller,
simulation, calibration, event writing or transit pricing is run.

## Difficult coordinate queries

The `swissRustRouting` config group exposes these coordinate-search heuristics:

| Parameter | Default | Meaning |
| --- | ---: | --- |
| `coordinateWorkBudget` | 2000000 | Scanned connections plus considered transfers per candidate search; 0 disables the budget. |
| `qualityWaitSeconds` | 1800 | Expand a found route when a pre-departure gap exceeds this; 0 disables this trigger. |
| `qualityJourneySeconds` | 10800 | Expand a found route when its total duration exceeds this; 0 disables this trigger. |
| `qualityRadiusMeters` | 2000 | Maximum walking radius for quality-triggered expansion. |

At budget exhaustion a feasible route may be returned before proving minimum generalized cost.
Such legs carry `rustRoutingBudgetLimited=true`; iteration logs count these results. This can change
route choices. The budget is a work bound, not a hard time limit: unresolved queries fall back to
exact routing rather than falsely returning no PT. Truncated results are also resolved exactly
before discarding them in favor of direct walking. Quality expansion retains the original utility
coefficients and uses the effective Raptor walking speed. Gaps can include onboard dwell.
Cheap 250000-work-unit probes can reject a poor stop set before using the full budget. A promising
truncated probe receives the full configured budget before it can be accepted.

For exact legacy coordinate behavior, set all four parameters to zero. Both explicit-endpoint native
routing and the generic stop-to-stop API remain exact. Older native releases continue to work but
do not support these policies or report budget-limited results; startup logs show capability support.
For cluster use, bundle a native library built from this source rather than a previous release.
