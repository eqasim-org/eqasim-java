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

## Initial routing and repairing a population

`RunSimulation` installs `MissingTripRoutingModule` in MATSim's initial preparation phase,
after vehicle/facility/routing-mode preparation and before the normal whole-plan checks.
It fills missing trip routes and replaces incompatible ones (for example, a generic teleported
bike route when a network route is now required, or a bike route referencing the original road
layer after creation of the isolated DSim layer). Complete trips, their route objects and stored
plans remain intact except for DSim's explicit bike reset: DSim always clears the routes and
travel times of bike legs in all stored plans during preprocessing. With `--dsim-simulate-bikes true`,
initial preparation routes those bike trips on the duplicated bike-only network; otherwise it
prepares fresh teleportation routes without a network search. QSim retains selective bike routing.
Normal iteration replanning is unchanged.

Work is parallel with a separate TripRouter per worker. Router construction is serialized to
protect network initialization; routing queries run in parallel. If no repair is needed, no
worker routers are created. Departure times account for preceding activities and preserved trips.
If bikes are teleported, missing teleportation data use the inexpensive beeline module, with no
network routing. This pass checks route presence/type/timing and network endpoint permissions;
it is not a full validation of every intermediate link in every stored route.

To repair plans without running a simulation:

```text
java -Xmx10g -cp switzerland.jar org.eqasim.switzerland.ch_cmdp.routing.RunMissingPopulationRouting --config-path config.xml --output-path routed_plans.xml.gz --threads 4
```

Optional `--modes bike,car` restricts the trip modes to repair; default: all modes. All stored
plans are inspected. The tool follows `routing.networkModes` in the config and uses the Swiss
routing modules with the optimized router and float travel-time storage by default. It creates
no DSim network layer and writes only the repaired population; the source plans remain untouched.

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

## Restarting final DSim outputs with QSim

Use the matching final `output_network`, `output_plans`, `output_vehicles`, `output_households`,
`output_transitSchedule`, `output_transitVehicles` and facilities files from a normal shutdown.
Update the input paths in the restart config to those files; `output_config.xml` does not
automatically redirect its original input paths to the output files. Do not pair raw `ITERS`
plans or `output_experienced_plans` with the restored final network: those files describe the
runtime DSim layer and are not postprocessed by this restoration hook.

For a QSim restart with network bicycles and road buses:

- Select `controller.mobsim=qsim`; omit `--use-dsim true` and `--dsim-simulate-bikes true`.
  `--use-dsim false` alone does not override DSim selected in XML.
- Include bike in `routing.networkModes` and `qsim.mainModes`, preserving the other desired
  modes. Remove the bike teleportation parameter set and disable `--teleport-bike` /
  `switzerlandSimulation.teleportBike`. The normal Swiss bike setup uses `SeepageQ`,
  `seepModes=bike` and `seepModeStorageFree=true`.
- Enable `transit.useTransit` and `transit.usingTransitInMobsim`, and set
  `eqasim.useScheduleBasedTransport=false` for actual bus boarding instead of Eqasim's
  timetable teleportation engine.

Restoration returns the original settings; it does not enable bikes or buses that were disabled
in the original configuration. The startup missing-route pass handles absent bike routes and
old teleported bike routes after network routing is enabled.

Two integration tests run DSim for iterations 0–1, reload its final files, and run QSim iteration 0
with network bikes and SBB road transit. They cover teleported and isolated-layer DSim bikes,
XML-selected and argument-selected DSim, previously disabled bike/bus simulation, restored
metadata, real bike/bus link events, passenger boarding and arrivals without stuck agents.
These are small-scenario regression tests, not a full Switzerland benchmark.
