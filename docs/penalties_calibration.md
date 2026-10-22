# Penalty calibration

This adds a `penalty` objective under `eqasim:networkCalibration`.

## Link penalty attribute behavior

The network can carry a per-link `penalty` attribute. It is a dimensionless
factor: the routing penalty in seconds is `length / freespeed * penalty`,
with the configured ramp/trunk multiplier applied once.

- If `penalty` is omitted from the `calibrate` selector and `penaltiesFile`
  is not set, this link attribute is used directly, preserving differences
  between links in the same road category. Missing/invalid values mean zero.
- If `calibrate="penalty"`, link penalties are converted into category-level initial values.
- If `penaltiesFile` is set, CSV values override attribute-derived initialization.

In fixed attribute mode, special-region files are not loaded, even if their
paths remain in the config. No counts file is needed. Keep the module and
penalty objective active, clear the penalty CSV, and omit `penalty` from
`calibrate` to reuse a calibrated network without regional GeoJSON files.
An explicit grouped penalty CSV still needs region definitions for its
regional keys. An explicit `resetPenaltiesToZeros=true` continues to override
stored penalties.

At the end of the last iteration, before MATSim writes `output_network`,
calibrated penalties are written back to each link as the `penalty` attribute
(using the existing rounded average of the last four values). Fixed runs
preserve their stored attributes; explicit CSV overrides are saved to links
so they can be reused without the CSV on the next run.

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

