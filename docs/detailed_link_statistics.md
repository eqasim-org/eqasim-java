# Detailed link statistics and flow daily counts

`output_detailed_link_statistics.csv.gz` records vehicle passages by hour in the
final iteration. It is a sparse table: absent link/hour rows mean zero passages.
The `cars_french`, `cars_crossborder`, `cars_swiss`, and `cars_unknown` columns
partition `cars`; they must not be added to `cars` again. Freight drivers are
classified as trucks even when their event network mode is `car`.

These statistics and `output_traffic_flow_daily_counts.csv` use different event
and time definitions. Their unadjusted daily totals are not directly comparable.

| Definition | Detailed statistics | Flow daily counts |
| --- | --- | --- |
| Iteration | Final iteration only | Current iteration; final values written at shutdown |
| Events | `VehicleEntersTrafficEvent` and subsequent `LinkEnterEvent`s | `LinkEnterEvent` only |
| Default time interval | `0 <= time < 108000` (30 hours) | `eqasim:flow.startTime <= time <= eqasim:flow.endTime` (default 24 hours) |
| Cars/trucks | One per passage | One per eligible passage, independent of truck PCU |
| Bikes | Separate raw passage count | Excluded |
| Buses | One per passage | `eqasim.sampleSize` per eligible passage |
| Unknown/zero-PCU vehicles | May appear in raw categories | Excluded by `VehiclePcuLookup` |
| Non-bus transit | Excluded | Transit-only non-bus vehicles are excluded |

Daily counts are separate from the flow module's PCU-weighted, potentially
smoothed `traffic_flow.csv`. A truck can contribute one daily vehicle count but
several PCU to flow. A bus can have an adjusted PCU and still contribute exactly
`sampleSize` to daily counts.

## Comparing road links

For ordinary registered road vehicles, first use only the flow module's time
window and remove passages counted through `VehicleEntersTrafficEvent` on
departure links. For the resulting **link-entry-only** counts:

```text
daily_count = cars + trucks + sampleSize * buses
```

Bikes are excluded. Without buses, this reduces to `cars + trucks`.
At a population sample size of 0.1, ten bus passages add one daily count.
Do not divide the whole result by the sample size: the flow daily CSV is already
on the sampled-demand scale.

The flow end time is inclusive. Summing detailed hours 0–23 omits events at
exactly 86400 seconds, because the detailed table puts them in hour 24. Those
boundary events must be added separately; the rest of hour 24 must be excluded.
Similarly, a non-hour-aligned flow window needs event-level filtering rather
than a sum of complete hourly rows.

The existing detailed CSV does not identify departure passages separately, so
exact reconciliation generally requires the events file. Raw detailed totals
remain useful as physical passage counts, including departure links.

For unusual vehicle populations, apply the same `FlowUtils` bus classification
and `VehiclePcuLookup` eligibility as the flow module. Compare rail-only links
separately. `FlowUtils` consults the transit vehicle registry only for buses and
otherwise looks in the regular vehicle registry. Thus a train registered only
as transit contributes zero even if its vehicle type has a positive physical
PCU. A vehicle reused across bus and non-bus transit routes needs the flow
module's vehicle-level classification.

The flow counter accumulates daily counts in `float` and rounds its CSV values
to two decimals. Decimal bus weights can therefore introduce small differences
from aggregating integer bus counts in double precision. Replaying events in
the same order with float accumulation reproduces its exported values.

## Regression checks

`TestDetailedLinkFlowConsistency` sends the same events to both production
handlers. It checks roads with/without buses, a 10% population sample, truck
PCU versus daily vehicle weights, excluded bikes, departure links, the inclusive
24-hour boundary, the 30-hour detailed cutoff, and reset behavior. Existing
tests cover final-iteration registration, SBB link-event settings, bus route
classification, offline replay, and concurrent flow accumulation.

```sh
mvn -pl switzerland -am test \
  -Dtest=TestDetailedLinkFlowConsistency,TestDetailedLinkStatistics,TestRunDetailedLinkStatistics,TestVehiclePcuLookup,TestLinkFlowCounterConcurrency \
  -Dsurefire.failIfNoSpecifiedTests=false
```

The 2026-10-22 Lausanne audit and per-link comparisons are saved under
`target/link-statistics-validation-20261022/`.

The audit independently replayed the existing DSim final iteration 140 and a
fresh two-iteration QSim run using the Lausanne inputs. All 322,249 DSim and
334,746 QSim detailed rows matched their events exactly. Both daily flow files
also matched event-order reconstruction on all 22,458 network links.

After aligning event scope and time bounds, all 15,364 QSim road links without
bus entries matched exactly. Of the 6,713 road links with bus entries, 6,692
matched directly and 21 differed by 0.01 due to float accumulation; reproducing
the flow counter's event-order float accumulation matched every bus link exactly.
The run included 837,016 raw bus passages. All 22,077 DSim road links matched
exactly after alignment; that DSim output had no bus link passages.

The QSim audit used an isolated parameter file omitting four obsolete PT
coefficients rejected by the current JAR; the other 167 coefficients were
preserved. This checks counting correctness, not equivalence of calibrated
mode-choice outcomes. The original Lausanne inputs and outputs were preserved.
See the audit's `REPORT.md` for the run settings and per-link comparison files.
