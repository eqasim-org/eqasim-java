# Subpopulation demand calibration

The entry point is `Calibrator`. At an update iteration it rebuilds one shared
`TrafficScoringTracker`, runs background-traffic location calibration, then
runs any scheduled cross-border volume calibration against the same live state.

## Background-traffic location flow

`BackgroundTrafficCalibrator.update` handles both fixed-mode freight and
cross-border plans. It is intentionally organized into three phases:

1. `selectCandidates` contains the complete background-agent selection policy.
2. `createProposals` relocates and routes isolated plan copies in parallel.
3. `applyProposals` previews and commits proposals sequentially so every
   decision sees the effects of all earlier accepted moves.

`BackgroundPlanRelocator` contains all location behavior, including immutable
anchors, the MATSim facility index, radius calculation, plan copying, routing,
and vehicle assignment. It only returns an isolated candidate plan and never
changes the population.

- One-trip freight relocates either its origin or destination.
- Cross-border traffic relocates the movable internal activity closest to the
  center of the trip chain. `outside`, `border`, and one-second connector
  activities remain fixed. The selected activity is shared by the arriving and
  departing trips, and only those two trips are rerouted.

For a common four-trip chain, the selected activity is `A2`:

```text
A0 outside -> A1 border -> A2 inland -> A3 border -> A4 outside
                              ^
                 destination of T1 and origin of T2
```

`CrossBorderVolumeCalibrator` remains a separate second stage. Relocation
changes route distribution at fixed demand; clone/remove/restore changes the
number of vehicles crossing a detected border station.

## Scoring flow

`RouteImpact.Extractor` converts a MATSim person, plan, or trip into counted-link
passages. `TrafficScoringTracker` stores observed counts, current simulated
flows, per-passage contributions, and cached route impacts. It does not define
the score.

`TrafficScore` is the single place for score behavior:

- `compute` classifies raw station inputs and calculates the complete score;
- `station` and `stationGroup` calculate link and physical-station status;
- `isBetterThan` defines whether a proposed freight relocation is accepted.

To change the score, edit `TrafficScore`. To change which background agents are
tried, edit `BackgroundTrafficCalibrator.selectCandidates`. To change what is
moved or where it can move, edit `BackgroundPlanRelocator`. To change protected
cross-border activity definitions, edit `CrossBorderActivityRules`.

## Configuration compatibility

The existing `freightRelocationRadiusFactor`, `freightMinimumRadius`,
`freightMaximumRadius`, and `freightRelocationTryFraction` XML keys now apply to
both freight and cross-border location proposals. Their legacy names remain so
existing scenario configuration files continue to load unchanged. The
`destinationSelectionProbability` parameter applies only to one-trip freight.

## Units

For sample fraction `s`, observation-window duration `H` hours, and `L` lanes,
one simulated passage contributes `1 / (s * H * L)` veh/h/lane. The tracker
starts from the simulated event flow and applies this contribution immediately
after every accepted population change.
