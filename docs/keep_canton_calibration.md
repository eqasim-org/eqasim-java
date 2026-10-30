# Keep initial canton mode shares

The CH_CMDP runner supports a fourth fast-calibration level:

```xml
<module name="eqasim:alphaCalibration">
    <param name="activate" value="true" />
    <param name="level" value="keep_canton:geneva;vaud" />
    <param name="calibratedModes" value="car,pt,walk,bike,car_passenger" />
    <param name="beta" value="0.3" />
</module>
```

Names after `keep_canton:` are separated by semicolons and must match person
`cantonName` attributes, for example `geneva`, `vaud`, or `zurich`. No target
CSV or `filePath` is needed. A requested canton with no eligible input trips
causes a descriptive error rather than using invented targets.

`KeepCantonAlphaCalibrator` captures each selected canton's mode shares once,
from the input population's selected plans during initialization. Shares count
trips, rather than giving every person the same weight. Later iterations use
the same census and shared canton-calibration filters: exclude freight and
cross-border persons, trips touching an `outside` activity, trips whose origin
and destination have identical coordinates, and modes outside car, PT, walk,
bike and car passenger. Trips without endpoint coordinates are skipped.

Starting after the first iteration, the calibrator updates only the configured
modes' canton constants for the selected cantons. It uses the usual smoothed
log-share-ratio correction, with PT as reference and a small share floor for
zero shares. The existing PT constant is preserved. `beta` is the retained
fraction of the old constant: a larger value damps the update more. Global
constants and other cantons are left unchanged. There are no alternating
global updates or minimum replanned-trip batches: measurements cover the full
eligible population, including selected plans that did not replan.

This is feedback toward initial shares, rather than a hard constraint on each
individual mode choice. If a canton has no eligible trips in a later iteration,
its update is skipped. The targets remain the initial snapshot throughout the
run. A new simulation captures a new snapshot of its own input population.

Outputs:

- `keep_canton_initial_shares.csv`: immutable initial targets and trip counts.
- `ITERS/it.N/N.keep_canton_shares.csv`: targets, current shares, counts and ASCs.
- `ITERS/it.N/N.mode_parameters.yml`: complete current mode parameters.
- `output_mode_parameters.yml`: final parameters, saved before the output
  config is written. The output config refers to this file by relative name.
