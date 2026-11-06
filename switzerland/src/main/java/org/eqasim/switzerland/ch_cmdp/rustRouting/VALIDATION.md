<!-- Records reproducible native-adapter checks and the scope of the Geneva smoke runs. -->
# Integration validation — 30 October 2026

Environment: Windows x86-64, Java 25, Rust 1.99.1. Java tests/runs used `-Xmx10g`;
concurrent routing and both simulation runs used 12 workers. The release native library was bundled
in `switzerland/target/switzerland.jar` by the `standalone,rust-router` Maven profiles.

## Automated checks

- Rust: `cargo test --workspace -- --test-threads=12`: 26 tests passed.
- Rust: `cargo fmt --all --check` and `cargo clippy --workspace --all-targets --all-features -- -D warnings`: passed.
- Java: `TestRustRouting` (5 tests) and `TestPtRouteCache` (12 tests): all passed, none skipped.
- Config serialization/reloading checks include native settings and the corrected runtime XML comment.
- A hand-calculated two-vehicle fixture checks onboard dwell, waiting, same-stop transfer buffer,
  directional transfer penalty, passenger mode `pt`, network distances and boarding times.
- 120 concurrent queries on 12 Java threads return independently mutable MATSim itineraries.
- No-path fallback, double-close/closed handle rejection, invalid offsets and unsupported settings
  are checked. A real controller injector exercises the native backend through the disk/RAM cache.

To repeat the Java checks with a built native library, use these properties (adjust local paths
for another machine):

```powershell
$env:_JAVA_OPTIONS = "-Xmx10g"
mvn -pl switzerland -am test `
  "-Dtest=TestRustRouting,TestPtRouteCache" "-Dsurefire.failIfNoSpecifiedTests=false" `
  "-Drust.router.test.library=C:/Users/dabdelkader/Desktop/work/codes/MultiModalRouter/target/release/matsim_pt_router.dll" `
  "-Drust.router.test.geneva.config=C:/Users/dabdelkader/Desktop/work/codes/cmdp_scenario/geneva_config.xml" `
  "-Drust.router.test.geneva.benchmark=C:/Users/dabdelkader/Desktop/work/codes/MultiModalRouter/examples/geneva/trip_results_8"
```

The two Geneva properties enable the optional local-data test; without them that check is skipped.
The other native tests require the
native-library property. The original standalone benchmark report remains unchanged.

## Geneva observed-trip check

The same 1,000 observed PT queries from `MultiModalRouter/examples/geneva/trip_results_8` were
routed through the actual JNI adapter, with 12 concurrent Java workers. All returned transit routes,
with finite nonnegative network distances and travel times. Every generalized cost was no greater
than its saved Rust reference (tolerance 1e-8). This is a cost check, not an assertion of identical
link/stop sequences. The adapter allows up to 64 boardings while the older benchmark allowed 5.
Preparing 4,269 stops and 13,820 departures took roughly 0.5–0.8 seconds in these local runs.
That is an initialization observation, not a national-scale speedup measurement.

## Actual RunSimulation checks

Two complete runs used the full Geneva network and benchmark timetable, a disposable sample of
64 people selected from the observed-trip requests, and iterations 0 and 1:

| Engine | PT cache | Outcome |
| --- | --- | --- |
| DSim | enabled | Both iterations and normal shutdown completed, exit 0 |
| QSim | disabled | Both iterations and normal shutdown completed, exit 0 |

Mode-choice replanning handled 12 selected people; the DSim cache recorded 64 PT query misses
routed by the native backend. Both logs contain native initialization and no SwissRailRaptor
transfer-dataset construction. Attribute cleanup remained enabled. The shutdown check also found
and fixed an existing runtime-config comment containing XML's forbidden double hyphen.

Disposable inputs, run script and final logs are under `eqasim-java/target/rust-integration/`
(`simulation.log`, `qsim.log`). Their source config predates current repository changes: the test
copy removes legacy child calibration activation flags, disables its empty calibration objective,
resolves cluster file paths locally, omits four obsolete OVGK coefficients in a copied parameter
file, and overrides storage capacity factor to equal flow capacity factor (0.1). Test output and
population paths are overridden; periodic bulk writers are disabled. These are test-only changes.

The source Geneva config, population and timetable were not modified. As in the earlier comparison,
the benchmark timetable excludes the documented service with a negative interior offset and
normalizes unused terminal departure offsets. Production initialization rejects bad used offsets
with original line/route/stop context. No full-Switzerland speedup or Linux execution was measured;
Linux deployment requires building its own host library using the same Maven profile.

## Automatic installation checks — 5 November 2026

`RunSimulation --useRustRouter true` now starts native acquisition and timetable preprocessing
asynchronously. The option still defaults to false. Explicit/bundled/system libraries are preferred;
otherwise the module uses a verified version/platform cache, downloads a release, or clones and
builds its pinned source. Installation is separate from query processing and does not change the
schedule or routing objective.

- All 25 targeted Java tests passed, none skipped: 8 installation tests, 5 native adapter tests and
  12 PT-cache tests. This includes the existing 1,000-query Geneva check and 12-thread concurrency.
- Installation tests cover disabled activation, argument overrides, config round trips and relative
  paths, OS/CPU selection, archive SHA-256 validation, controlled extraction, concurrent cache reuse,
  pinned clone commands and isolated Cargo output. A development checkout cannot replace the cached
  official release.
- A real GitHub release download was verified, extracted and loaded through JNI with Git and Cargo
  removed from the JVM's PATH. It then reused its cached library. Credentials came from the process
  environment and were not saved in the config or test logs.
- A separate first-time source installation cloned `v0.2.0`, built with `cargo build --release
  --locked`, loaded the library and reused the cache. No existing source checkout was modified.
- The standalone `switzerland.jar` was rebuilt with its Windows library bundled. Normal Maven builds
  remain possible without Rust; the `rust-router` build profile is optional for prebundling.
- A separate test jar with all bundled native resources removed completed QSim iterations 0 and 1
  using the downloaded installation cache, with Git/Cargo removed from PATH and GitHub tokens absent.
  It used the existing disposable 64-person Geneva fixture and benchmark timetable described above,
  12 threads and `-Xmx10g`. The PT cache recorded 64 misses handled by Rust and prefetching for 12
  selected people. Startup, replanning and normal shutdown completed with exit 0. Logs are in
  `target/rust-install-simulation/simulation.log`. The production scenario files were not modified.

Acquisition logs are under `target/rust-install-smoke/`, and Maven results are in
`target/rust-install-final-tests.log` and `target/rust-install-package.log`. These Windows checks do
not claim a new national-scale performance result or Linux/macOS execution.

## Default standalone bundling — 5 November 2026

The normal command now acquires and embeds the host's Rust library; the additional `rust-router`
profile is no longer necessary:

```text
mvn -Pstandalone --projects switzerland --also-make package -DskipTests
```

This exact command completed successfully. The jar contains
`native/windows-x86_64/matsim_pt_router.dll`, version/platform/SHA-256 metadata and the upstream MIT
license. The obsolete unqualified native resource was removed. Its checksum agrees with the
packaged metadata.

All 26 targeted tests passed without skips: 9 installation/bundling checks, 5 native routing checks
and 12 PT-cache checks. Native routing tests used the newly bundled binary, including the saved
1,000-query Geneva comparison.

A separate JVM loaded the finished jar's native library with `autoInstall=false`, an empty
`java.library.path`, no Git/Cargo on PATH and no GitHub token variables. The JNI protocol handshake
passed. This verifies that runtime does not depend on a source checkout or installation fallback.
Java runs used `-Xmx10g`. Logs are in `target/rust-standalone-default-build.log`,
`target/rust-standalone-bundle-tests.log` and `target/rust-bundled-smoke/runtime.log`.

Only the build host's OS/CPU library is embedded by default. These checks do not establish Windows
DLL portability to Linux/macOS; those deployments need their own matching build. Compiling native
source still requires a Rust toolchain at build time; `rust.router.library` accepts a precompiled
library instead.

## Latest published source and portable packaging — 6 November 2026

The default standalone build now resolves the Rust repository's latest `main` commit and builds
both Windows x86-64 and Linux x86-64 when invoked on Windows. This supersedes the earlier host-only
default described above. The ordinary Maven command was verified without local source or native
overrides against published Rust revision `8e9c38d3768302a318f2541d24e0d366fff49adb`.
Both platform resources record that immutable revision and their own SHA-256. Linux is
cross-compiled for glibc 2.17; these checks do not claim execution on Linux.

The current regression suite passes 43 Rust, 14 Python and 28 targeted Java tests. The Java checks
include actual JNI calls, concurrent routing, exact legacy reference comparison, configurable
coordinate heuristics, interruption fallback and the latest-source cache/build rules. See
`docs/benchmarks/rust-coordinate-heuristics.md` for the separate eight-worker Swiss comparison.
