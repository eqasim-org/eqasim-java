# Freespeed calibration

This adds a `freespeed` objective under `eqasim:networkCalibration`.

See [parent configuration and migration](network_calibration.md) for component
activation. Child parameter sets no longer have an `activate` switch.

## What it calibrates

- Links are grouped by:
  - link category (`LinkCategorizer`)
  - `municipalityType` link attribute
  - configured special region
- Each group has a multiplicative freespeed factor.
- At each update interval, observed trips are routed on the current network and per-group factors are updated from travel time mismatch.

Groups are captured once from the input network, before initial factors are
applied. Road-category thresholds therefore use the original freespeeds and
lanes. Applying a factor cannot move a link into another freespeed group, even
when its simulated speed crosses the 45 km/h category threshold. Loading,
application, routing statistics and final export all use the same immutable
group key, indexed by link ID and shared by links in the same group. Filtered
routing copies resolve to that same key. Penalty categorization remains live.

Before each update, the calibration routing copy refreshes its speeds from the
simulation network. These are the speeds used by the last completed mobsim;
routing uses that mobsim's shared float travel-time table. The refreshed speeds
provide fallback times, ALT bounds and the correction denominator without
changing group membership. New factors are applied afterwards as
`original freespeed * new factor`, without compounding the current link speed.

Observed trips retain their input order. Routing workers share a dynamic queue
and claim one trip at a time, each using its own private router. There is no
distance sorting or fixed batch assignment.

Timing logs use monotonic elapsed wall time in seconds:

- `Calibration copied-speed refresh` and `Calibration routing preparation`:
  updating the road copy and preparing the shared graph, landmarks and routers.
- `Calibration worker`: each worker's processed/routed/accepted counts, total
  target distance and elapsed time.
- `Calibration routing and statistics merge`: the parallel trip processing and
  final statistics merge, excluding preparation.
- `Freespeed observed-trip routing total`: the whole routing call, including
  refresh, preparation, trip processing and merge; also logged on failure.
- `Freespeed factor update`, `Freespeed factor application` and
  `Freespeed diagnostics output`: the remaining calibration stages.
- `Freespeed iteration-start listener`: the entire listener call, with iteration,
  whether an update was scheduled, completion status and total seconds. This is
  logged even on failure and on iterations without a scheduled update.

Worker times overlap and should not be added to the listener wall time.

Update direction:

- if simulated travel time is too low (underestimation), factor decreases (< 1.0)
- if simulated travel time is too high (overestimation), factor increases (> 1.0)

## Required config

Under `eqasim:networkCalibration`:

- `activate=true`
- `objective=freespeed`
- `calibrate=freespeed`

In the `freespeedCalibration` parameter set:

- `observedTripsFile=path/to/observed_trips.csv`

## Initialization precedence

During calibration, or when explicitly loading a grouped CSV, initial factors
are resolved in this order:

1. `speedFactor` link attribute (group-averaged by category + municipality type)
2. `factorsFile` CSV in `freespeedCalibration` (overrides matching groups)

When the freespeed objective is active but `freespeed` is omitted from the
`calibrate` selector (for example, `calibrate=""`), the loaded factors are
applied and kept fixed. With `calibrate="freespeed"`, these values are used
as initial factors before iterative updates.

Fixed runs without a factors CSV use each car link's `speedFactor` directly,
without averaging it into a group. Special-region files are not loaded, even
if their paths remain in the config; they can be omitted or unavailable.
Links without a valid positive factor retain their original speed. The
stored link attributes are preserved when saving the output network. An
explicit group CSV still requires the region definitions for its regional
keys, as does calibration of the freespeed component.

At the end of the last iteration, before MATSim writes `output_network`, the
module restores the original freespeeds and stores the factors in the
`speedFactor` link attribute. This applies both during calibration and when
using fixed factors. On restart, the saved freespeeds remain the base values:
the module loads the factors and applies them once for the simulation, then
restores the base values again before writing the next output network. A
configured factors CSV overrides the saved factors for matching groups.

Useful optional parameters:

- `updateInterval` (default `5`)
- `saveNetworkInterval` (default `5`)
- `categoriesToCalibrate` (default `1,2,3,4,5,11,12,13,14,15`)
- `beta` smoothing (default `0.5`)
- `minFreespeedFactor` / `maxFreespeedFactor` (defaults `0.5` / `1.5`)
- `minTripsPerGroup` (default `5`)

## Observed trips CSV format

Expected columns (comma-separated):

- `departure_x`
- `departure_y`
- `arrival_x`
- `arrival_y`
- `departure_hour`
- `travel_time` (seconds)
- `traveled_distance` (meters)

Accepted aliases are also supported (for example `origin_x`, `destination_x`, `travel_distance`).

## Outputs

At every update iteration:

- `freespeed_factors_by_group.csv`
- `freespeed_group_stats.csv`

If `saveNetworkInterval` matches the iteration:

- `network_calibrated_freespeed.xml.gz`

