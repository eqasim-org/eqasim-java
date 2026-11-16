# Walking-time extension

`RunSimulation` installs `org.eqasim.switzerland.ch_cmdp.walk.WalkModule`.
Walking remains teleported. The optional `TeleportationWalkTravelTime` preserves
MATSim's existing calculation, including speed, distance factor, person-speed
attributes, and rounding. PT access/egress, transfers, transfer penalties, stop
selection and timetable data retain their existing internal calculations.

## Fitted MOBIS model

Select it through `swissWalking.model=mobis` in the config. `model=teleportation`
selects the original behavior. RunSimulation has no dedicated walking argument.
MOBIS is now the default for a new walking config. Existing model selections are preserved by adaptation.
The choice and fitted coefficients are saved in output configs. It applies to
complete walk trips and network-mode walking access/egress, including cars where
those legs are enabled. It does not replace PT's internal walking calculations.
`new WalkModule(MobisWalkTravelTime.class)` is also supported for programmatic use.

`MobisWalkTravelTime` implements the fitted equation from
`calibration/walking_speed/model.py`, using rounded defaults from the full-MOBIS fit:

```
v0 = 1.430 m/s
k = 3.159
uphill_penalty = 0.870
flat_grade = 0.002
detour_factor = 1.311

D = hypot(destination.x - origin.x, destination.y - origin.y)
L = detour_factor * D
g = (nearestDestinationNode.z - nearestOriginNode.z) / L
v = v0 * exp(-k * max(abs(g) - flat_grade, 0)
             - uphill_penalty * max(g - flat_grade, 0))
t = L / v
```

Distance uses actual endpoint XY coordinates. Endpoint/activity Z is ignored.
Elevation comes only from the nearest network nodes, and grade is divided by
`L`, not the horizontal distance between those nodes or the generic context slope.
Missing node elevations use zero elevation change (fitted flat walking speed).
In this network, node Z values within 0.001 m of zero indicate missing preprocessing
elevation. If either endpoint node has such a value, elevation is ignored for that trip.
Coincident endpoints return zero seconds. The fitted detour factor is applied once
inside this model; the configured teleportation speed/distance factor does not
enter its time expression. Route distance metadata still uses the configured
distance factor. A constructor accepting all five coefficients supports other fits.

The `swissWalking` config parameters use the same names as the Python model:
`model`, `v0_m_s`, `k`, `uphill_penalty`, `flat_grade`, and `detour_factor`.
All five coefficients default to the fitted values above, and are read by WalkModule
when it constructs the selected MOBIS model.

At the end of `ch_cmdp.scenario.RunAdaptConfig`, optional arguments can set any of
these independently, without mixing them into the existing adaptation logic:

```
--walk-model mobis
--walk-v0_m_s 1.430
--walk-k 3.159
--walk-uphill_penalty 0.870
--walk-flat_grade 0.002
--walk-detour_factor 1.311
```

Use `--walk-model teleportation` there to switch back. Omitted arguments preserve
input settings (or defaults if the walking config is new). RunSimulation then
uses the saved config. Legacy teleportation speed and factor remain configured
in the normal routing group as before.

## Custom expression

Implement `WalkTravelTime` and install `new WalkModule(MyWalkTravelTime.class)`
instead of `new WalkModule()` in `RunSimulation`. Implementations may use Guice
constructor injection and must be thread-safe. The module creates one shared
implementation per simulation.

```java
public final class MyWalkTravelTime implements WalkTravelTime {
    @Override public double calculate(WalkTravelTimeContext context) {
        // Replace with your expression. The result must be finite nonnegative seconds.
        return context.defaultTravelTime();
    }
}
```

The context contains actual endpoint coordinates, their Euclidean distance,
nearest network nodes, `nodesDistance` and signed `nodesElevationChange`.
Distances/elevations are metres. `hasElevation` is false and elevation change is zero when
elevations are missing, nodes coincide, or the network is empty. Node references
are null only on an empty network. Treat all supplied objects as read-only.
The default implementation avoids all nearest-node queries.

The expression replaces walking duration for whole walk trips and walking access
and egress to network modes. Route distances retain their configured distance factor.
RAPTOR uses its default stop finder, and Rust retains its original native query
path. Their internal access/egress and transfer calculations do not call this model.
No changes are made to their stop searches, timetable data or itinerary caches.

With a custom expression, the legacy `walkConstantTimeToLink` access setting is
changed to `accessEgressModeToLink` so network access/egress actually calls the walk
router instead of returning fixed link times. Other access settings are preserved,
including `none` (no access/egress legs) and the explicit plus-time-constant setting.
With the default implementation, config and network access behavior are unchanged.
Network walking and freespeed-factor walking are not supported by this module.
If `qsim.usingTravelTimeCheckInTeleportation` is enabled, QSim may clamp times that
are faster than the configured teleportation speed. Its default is false; keep it
false when the expression must determine the exact simulated walking duration.
Existing complete plans are not automatically rerouted by installing this module:
the expression applies whenever a walk or access/egress route is calculated.
