# CMDP optimizer exports

`org.eqasim.core.components.calibration` exports evaluated mode-choice inputs and
candidate tours when `eqasim:calibration.activate=true`. With `runCalibration=true`,
the iteration-start optimizer uses preceding iteration exports, invokes Python,
and loads the returned mode parameters before replanning. This is parameter
calibration against mode-share targets, separate from count-based agent ASC
calibration in `network_calibration.demand_calibration`.

The CMDP mode-choice module binds `CmdpVariablesWriter`, `CmdpOptimizer`, and
`CmdpOptimizerHandler`. The writer exports all inputs needed by its five detailed
utility estimators, including raw destination densities, missing-density status,
high-income indicators, city and canton identifiers, ordinal home/destination PT
quality, the configured regional PT fare radius, parking costs, and the existing
person-specific car ASC. Times, utilities, and numerical inputs retain full
precision and decimal formatting is independent of the machine locale.

The same trip/mode can have different PT itineraries at different departure times
within candidate tours. Consequently, `person_id` and `trip_index` are not a
sufficient key for these exports. Each variable row has a `variables_id`, which
is attached to the returned trip candidate by the existing trip-estimator cache.
`detailed_utilities.csv` writes the corresponding reference for each candidate
trip. The Python CMDP backend joins these exact references, rather than choosing
an arbitrary evaluation of that trip. References are worker-local and retained
only for the current person. They are inactive outside calibration export.

`CmdpOptimizerHandler` writes `mode_parameters_input.yml` containing the complete
effective parameters, including defaults and preceding updates. The optimizer
preserves all unmodified YAML entries, including canton maps. An existing local
optimizer checkout is preserved at startup. The configured branch/commit is used
when cloning a missing optimizer. A nonzero Python exit or missing output file
is reported as an optimization failure and the existing fallback is used.

## Running the updated backend

Set the following in `eqasim:calibration`:

```xml
<param name="activate" value="true" />
<param name="runCalibration" value="true" />
<param name="startIteration" value="2" />
<param name="optimizerPath" value="EqasimParametersCalibration" />
<param name="pythonPath" value="PATH/TO/PYTHON" />
<param name="eqasimCachePath" value="PATH/TO/REAL/SYNTHESIS/CACHE" />
```

Install the optimizer's requirements in the selected Python environment. The
updated backend requires newly generated exports with `variables_id` references.
Do not combine old exports with the updated backend.

The relative `optimizerPath` is the checkout directory under the working
directory. An absolute path to an existing checkout is also supported. Canton
constants are used for all five modes. Individual constants can be optimized
using bounds such as `swissCanton.car.vaud:0.2`; output retains Java's canton-map
YAML format.

Fresh checkouts default to `https://github.com/matsim-eth/EqasimParametersCalibration.git`,
branch `polars`, pinned commit `07d401ab8f966e4fa1b4a4aa4034d45e424c093d`.
An existing config that explicitly sets an older `repoCommit` or another
`optimizerPath` overrides these defaults; update those entries when switching
to this backend.

## Validation

`TestCmdpCalibrationExport` exercises all five Java estimators with production
defaults and a second parameter set that activates normally zero effects. Cases
include all PT quality classes, missing densities, unknown/missing cantons,
threshold boundaries, nonunit scaling, fractional departure times, CSV-escaped
IDs, and a non-English numeric locale. The exported fixtures are checked by the
optimizer repository's `tests/test_cmdp_utilities.py`.

`TestCalibrationVariableReferences` verifies that equal-utility candidates at
different departure times keep distinct references, cache hits retain references,
and switching person releases prior bindings. `TestCmdpOptimizer` verifies that
local checkout contents are preserved and subprocess failures are reported.

The Lausanne validation uses an isolated config copy, QSim, 8 threads, `-Xmx10g`,
iterations 0 and 1, and suppressed unrelated scenario/event outputs. Network and
ASC calibration are disabled to isolate the optimizer export. The scenario's
legacy parameter file is converted in the test directory by removing names that
no longer exist, leaving the original files untouched. All exported mode inputs
and candidate tour utilities are compared with Python. The optimizer integration
smoke test uses synthetic targets and the actual Lausanne candidates, so it
checks invocation, parameter serialization and loading rather than empirical
calibration performance.
