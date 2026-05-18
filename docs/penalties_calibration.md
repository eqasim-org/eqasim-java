# Penalty calibration

This adds a `penalty` objective under `eqasim:networkCalibration`.

## Link penalty attribute behavior

The network can carry a per-link `penalty` attribute, interpreted as the effective routing penalty for this link.

- If `calibrate=false` and `penaltiesFile` is not set, this link attribute is used directly.
- If `calibrate=true`, link penalties are converted into category-level initial values.
- If `penaltiesFile` is set, CSV values override attribute-derived initialization.

At shutdown, calibrated penalties are written back to each link as the `penalty` attribute.

## Required config for calibration

Under `eqasim:networkCalibration`:

- `activate=true`
- `objective=penalty`
- `countsFile=...` or `averageCountsPerCategoryFile=...`

Useful optional parameters:

- `updateInterval`
- `beta`
- `minPenalty` / `maxPenalty`
- `categoriesToCalibrate`
- `penaltiesWarmupIterations`
- `rampFactor` / `trunkFactor`

