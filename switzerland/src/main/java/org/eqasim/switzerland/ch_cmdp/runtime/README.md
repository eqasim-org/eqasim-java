# Temporary simulation adaptations

`RunSimulation` uses `SimulationRuntime` to own DSim preparation, optional bike teleportation,
attribute cleanup, and final export restoration. The configured scenario is the baseline, after
command-line/configurator overrides and before engine adaptations. Input files are never edited.

`RunGenerateConfig` includes a `dsim` module with the requested worker count and selects
`controller.mobsim=qsim` by default. Set `controller.mobsim=dsim` to run DSim directly;
`RunSimulation` automatically supplies its adaptations and JVM module access. Alternatively,
`--use-dsim true` temporarily enables it on a QSim config. The exported engine is the baseline
engine: XML-selected DSim stays DSim, whereas an argument-only switch returns to QSim.
`--use-dsim false` does not turn off DSim selected in XML.

DSim defaults to teleporting bicycles and using timetable-based PT without road buses.
Only `--dsim-simulate-bikes true` creates an isolated bicycle network layer; this option works
with either XML-selected or argument-selected DSim. Teleportation never changes network topology
or link mode permissions. Attribute cleanup, when requested, remains a separate preprocessing step.

The constructor calls `preprocessConfig()` before scenario loading. After loading and ordinary
scenario adjustments, `preprocess()` prepares DSim data and archives removed attributes.
`postprocess()` restores those owned changes and prepares the export config before final writers.
Each operation has a separate documented method. Only changed capacities/scalar config values
still equal to the installed preprocessing value are reverted. Other mode parameters and newer
attributes remain intact; missing stable objects are not recreated.
Home-coordinate normalization is also journaled in the compressed archive: original values and
missing keys are restored only while the values installed by cleanup remain unchanged.

On normal shutdown (including early termination), the final-writer decorator:

1. Restores archived person, household and link attributes.
2. Removes the isolated bike layer **if this run created it**, returns bike mode to its original
   road links, restores removed bike turn restrictions and vehicle PCUs, and maps activity,
   route and vehicle initial-link references to original links. Existing input layers stay intact.
3. Invalidates bike leg routes when preparation changed between network routing and teleportation,
   so the next simulation prepares routes compatible with the baseline.
4. Runs MATSim's normal final writers using the runtime config (in particular, experienced-plan
   output stays disabled for DSim, which cannot produce its required partial-score file).
5. Writes the previously prepared export config to `output_config.xml` and `output_config_reduced.xml` with the original engine and
   mode settings, while retaining current calibration parameters and the DSim module.
6. Deletes the attribute archive after successful final export.

MATSim locks its live config. Restoration therefore uses an unlocked export copy; the in-memory
runtime config and startup `run_config.xml` describe the engine actually run.
The two final output configs are the restored versions. Intermediate iteration files,
events, experienced plans and link statistics describe the actual engine/network used during the run;
they are not rewritten. On unexpected shutdown, no conversion is attempted and the archive is retained.
With `dumpDataAtEnd=false`, scenario restoration still happens, but no final files are created.

Bike teleportation can be requested with `--teleport-bike true` or:

```xml
<module name="switzerlandSimulation">
    <param name="teleportBike" value="true" />
</module>
```

The CLI overrides XML. Default: false. Speed: **4 m/s**; beeline factor: **1.4**, as in
`ch_cmdp.scenario.RunAdaptConfig`. Bikes are removed from network routing, QSim main modes and
measured modes, but physical bike links remain in the input network. This conflicts with
`--dsim-simulate-bikes true`. DSim already teleports bikes by default.
