# Network calibration configuration

Component activation and calibration are controlled only by the parent
`eqasim:networkCalibration` module:

```xml
<module name="eqasim:networkCalibration">
    <param name="activate" value="true" />
    <param name="objective" value="penalty,freespeed" />
    <param name="calibrate" value="freespeed" />
    <parameterset type="costCalibration">
        <param name="updateInterval" value="3" />
    </parameterset>
    <parameterset type="freespeedCalibration">
        <param name="observedTripsFile" value="observed_trips.csv" />
    </parameterset>
</module>
```

- `activate=false` disables all network-calibration components.
- `objective` selects components to use: `penalty`, `freespeed`, `agent`,
  `subpopulations`. An empty selector enables none.
- `calibrate` selects which enabled components update during simulation.
  Enabled components omitted here use their existing input/default values.
  An empty selector keeps all enabled components fixed.

The example uses fixed link penalties while calibrating freespeed factors.
Selecting a component in `calibrate` alone does not enable it.

The four child parameter sets (`costCalibration`, `freespeedCalibration`,
`agentAscsCalibration`, `subpopulationsCalibration`) contain component settings
only. Remove their former `activate` parameters from existing configs and list
the desired components in the parent's `objective` instead. There is no child
activation override or synchronization. The separate `calibrateCrossBorder`
setting still controls which demand the subpopulation calibrator adjusts.
