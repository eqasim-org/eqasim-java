# Freespeed calibration

This adds a `freespeed` objective under `eqasim:networkCalibration`.

## What it calibrates

- Links are grouped by:
  - link category (`LinkCategorizer`)
  - `municipalityType` link attribute
- Each group has a multiplicative freespeed factor.
- At each update interval, observed trips are routed on the current network and per-group factors are updated from travel time mismatch.

Update direction:

- if simulated travel time is too low (underestimation), factor decreases (< 1.0)
- if simulated travel time is too high (overestimation), factor increases (> 1.0)

## Required config

Under `eqasim:networkCalibration`:

- `activate=true`
- `objective=freespeed`
- `observedSpeedTripsFile=path/to/observed_trips.csv`

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

