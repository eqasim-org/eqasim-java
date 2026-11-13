# Truck routing

`org.eqasim.switzerland.ch_cmdp.trucks.TruckRoutingModule` is installed in
`RunSimulation` after the road cost overrides. It delegates to the live car
travel-time instance without copying its storage.

Car routing in this runner combines:

- Observed link travel time, which already reflects congestion and simulated
  intersection delays. Calibrated freespeeds also influence these times.
- `RoutingPenalty`: the sum of configured policy routing penalties and the
  network-calibration penalty from `PenaltiesAdapter`. The latter can use current
  calibrated factors or saved link attributes without calibrating again.
- The configured routing distance preference.
- Optional reproducible routing randomness applied to the above subtotal.
- Monetary link tolls converted to routing cost using vehicle-specific toll sensitivity.

Truck disutility is calculated independently in `TruckTravelDisutility`, with no
car disutility/factory calls and no `RoutingPenalty` dependency:

```
cost = (carTravelTime + distanceCost) * randomMultiplier + roadClassPenalty
distanceCost = distanceWeight * length / 100
randomMultiplier = max(1 + sigma * reproducibleNormalDraw, 0.001)
```

With sigma zero the multiplier is one. `TruckTravelDisutilityFactory` currently
initializes distanceWeight and sigma from the Eqasim routing settings and the seed
from the global config. These are inputs, not a dependency on car cost expressions.
Edit the truck factory to change those defaults, or the formula and small helper
methods in `TruckTravelDisutility` to change truck behavior. The monetary toll
term is currently disabled for trucks; its helper remains available in that class.
Measured times still include congestion and simulated junction delays.

The truck-specific surcharge is added afterwards, in seconds of link free-flow
travel time (`length / freespeed`): 0% for motorway/trunk/primary, 10% for secondary,
15% for tertiary, and 20% for other or missing highway types. `_link` ramps follow
their parent class. Classification reads `osm:way:highway`. Freespeed is read at
calculation time so calibration cannot leave stale surcharge values. The surcharge
changes routing cost only, not the reported travel time or vehicle simulation.

`getLinkMinimumTravelDisutility` supplies the router's free-flow lower bound,
including the road-class surcharge and the random multiplier's minimum when
randomness is enabled. If the expression is changed, keep this bound conservative.
