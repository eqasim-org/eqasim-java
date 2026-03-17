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

