<!-- Records a controlled local comparison of exact legacy and heuristic coordinate routing. -->
# Rust coordinate routing: difficult Swiss journeys

The comparison replays the 1000 requests exported by cluster benchmark job `ch10_20016370`,
using the locally copied full Swiss timetable and original Eqasim/SwissRailRaptor coefficients.
Eight Python callers share one native engine; each caller owns its reusable Rust workspace.
Three timed passes follow eight warmup requests. Loading and preprocessing are excluded.
These are local Windows results, not new measurements on the 48-worker cluster.

| Policy | Median batch seconds | Feasible PT results | Budget-limited results |
| --- | ---: | ---: | ---: |
| Exact legacy coordinate search | 7.929 | 867 | 0 |
| Adaptive probe, 2-million-unit budget and quality expansion | 2.272 | 878 | 2 |

The measured speedup is 3.49x. Of the 867 paired PT results, 812 have the same generalized cost,
55 have lower cost, and none have higher cost, within 1e-8. Eleven previously walk-preferred or
unavailable results gain a PT alternative. This sample does not establish a universal cost bound.
There are 45 changed arrival times among paired results; wider access/egress sets intentionally
change available alternatives. Utility rates and mode-specific transfer fees are unchanged.

The worst previous query, request 943, drops from 5.449 to 0.278 seconds in the final measured pass.
Its journey falls from 29437 seconds (8h10m37s) to 8247 seconds (2h17m27s), and its cost falls
from 9.331692 to 3.034242. A useful station was farther away than the initial nearby bus-stop set.
Requests 596 and 599 remain budget-limited; their returned costs equal the exact legacy costs in
this sample. A budget-limited result is a feasible witness, not a proved optimum.

## Policy

Candidate searches first use a cheap 250000-unit probe while quality expansion remains possible.
Long waits (over 1800 seconds) or journeys (over 10800 seconds) trigger radius expansion up to
2000 m, within five attempts. A promising truncated probe is retried with the full 2000000-unit
budget. Work units count scanned connections plus considered transfers; this is not a wall-time cap.
If interruption leaves no feasible route, an exact legacy search resolves the uncertainty. An
incomplete route is also resolved exactly before rejecting it in favor of direct walking.

Aggressive 250000/500000 budgets were tested and rejected as defaults because some results became
substantially more costly. The larger budget plus cheap probes retains most of the speed benefit
with better sample quality. It still permits approximate results on other requests.

The `swissRustRouting` config exposes `coordinateWorkBudget`, `qualityWaitSeconds`,
`qualityJourneySeconds`, and `qualityRadiusMeters`. Set all four to zero to recover exact legacy
coordinate behavior. Explicit-endpoint weighted routing and stop-to-stop APIs remain exact.
MATSim legs mark approximations with `rustRoutingBudgetLimited=true`; iteration logs and the Java
comparison tool report their counts. Python JSON reports `search_truncated: true`.

## Verification

43 Rust tests, strict Clippy, 14 Python integration tests and 28 targeted Java tests pass, including
the historical 1000-query Geneva exact-reference comparison with heuristics disabled. Regression
tests cover interrupted searches, exact fallback, walking preference, shared-workspace reuse,
JNI flags, config roundtrip and Python overrides. Windows and Linux x86-64 JNI libraries are
packaged in the standalone jar; Linux is cross-compiled for glibc 2.17 and has not been executed
locally on Linux.

The final packaged jar was also checked through `RunComparePublicTransportRouters`, with the same
1000 requests, eight workers, `-Xmx10g`, 1000 warmup requests per engine and three timed passes:

| Complete MATSim adapter | Median seconds | PT results | Errors |
| --- | ---: | ---: | ---: |
| SwissRailRaptor | 4.223 | 860 | 0 |
| Rust with new coordinate policy | 2.355 | 878 | 0 |

Of 858 paired PT results, 790 meet the comparison's max(60 seconds, 5%) time tolerance. There are
637 identical line/route/stop sequences and 782 identical transfer counts. Rust's mean paired journey
time is 106.6 seconds shorter, with mean absolute difference 121.4 seconds. Two results are budget
limited. The changed walking candidates intentionally prevent an exact route-equivalence claim.
This adapter comparison includes MATSim leg construction; it is distinct from the native before/after
comparison above. Neither measurement includes full simulation iterations or proves 48-worker scaling.

The locally built jar uses explicitly staged native libraries from the working tree:

```sh
mvn -Pstandalone --projects switzerland --also-make package -DskipTests \
  -Drust.router.nativeDirectory=target/optimized-native
```

The default latest-source packaging path resolves and builds the published Rust repository. Rust
commit `8e9c38d` publishes this implementation; ordinary standalone Maven builds select it or a newer
`main` commit without local staging. Explicit native staging remains useful for unpublished changes.
