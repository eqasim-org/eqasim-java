# Running a simulation with standalone on-demand services (using the DRT module).
The functionalities for supporting DRT services in Eqasim are mainly implemented in the `org.eqasim.core.simulation.modes.drt` package. 
MATSim config files that include a `multiModeDrt` config are natively supported by the `RunSimulation` scripts of the Eqasim pipeline. If you write your own run script, make sure you use the `org.eqasim.core.EqasimConfigurator` (either directly or by defining a subclass of it) and call the `addOptionalConfigGroups` and `configureController` methods. 
If your MATSim config files are not already configured to simulate DRT, two tools present in the `org.eqasim.core.simulation.modes.drt.utils` package that help you do so are documented below. 

**Note**: Keep in mind that just adding a `multiModeDrt` config will enable simulating DRT trips that already exist in your population file. Further configurations are needed to fully support DRT modes in the mode choice model. Refer to the parts about [AdaptConfigForDrt](#adaptconfigfordrt) and [Mode choice integration of DRT](#mode-choice-integration-of-drt) for more details.

## Quick usage
### CreateDrtVehicles
The `org.eqasim.core.simulation.modes.drt.utils.AdaptConfigForDrt` script allows to create a drt vehicles xml file with a certain number of vehicles distributed randomly through the network.
This tool accepts the following command line arguments:
- `network-path`: (required) the path to the network file on which the vehicles should be generated.
- `output-vehicles-path` (required) the path to the resulting vehicles file. 
- `vehicles-number`: (required) the number of vehicles to generate.
- `vehicles-capacity`: (optional) the capacity (number of seats) of the generated vehicles. Default value is 4.
- `service-begin-time`: (optional) the start time (in seconds) of the vehicles availability for the operator. Default value is 0.
- `service-end-time`: (optional) the end time (in seconds) of the vehicles availability for the operator. Default value is 24 * 3600.
- `vehicle-id-prefix`: (optional) the prefix to put at the beginning of the vehicles' IDs. The whole ID will be constructed by appending to vehicles' number (in the generation sequence) to this prefix. default prefix value is `vehicle_drt_`
- `random-seed`: (optional) the seed to use for the random number generator that is used to select links from the network as starting locations for the vehicles. Default seed is 1234.

### AdaptConfigForDrt
If your config file is already set to use DRT, you can skip to the next step. Otherwise, you need to first create a drt vehicles file. This can be done using the `org.eqasim.core.simulation.modes.drt.utils.CreateDrtVehicles`. Then you can run the `org.eqasim.core.simulation.modes.drt.utils.AdaptConfigForDrt` script introduced in #185 to generate a config file for DRT.  
This scripts takes the following command line arguments:
- `input-config-path`: (required) the path to base config file.
- `output-config-path`: (required) the path to the resulting config file.
- `vehicles-paths`: (required) a comma separated list of the paths of the drt vehicles files to use for the drt modes.
- `mode-names`: (optional) a comma separated list of the names of the DRT modes to add to the configs. Default value is `drt`.
- `operational-schemes`: (optional) a comma separated list of the operational schemes (`door2door` or `stopbased` or `serivceAreaBased`). Default value is `door2door`.
- `cost-models`: a comma separated list of the names of the cost models to configure Eqasim to use in the estimation of the cost of drt trips. Default value is the `ZeroCostModel`.
- `estimators`: a comma separated list of the names of the estimators to configure in eqasim to be used for the evaluation of trip alternatives using the new added drt modes. Default value is `DrtUtilityEstimator`.
- `qsim-endtime`: the value by which the end time of the qsim module config will be replaced. Default value is 30:00:00.
- `mode-availability`: if provided, will replace the `modeAvailability` in the `DiscreteModeChoice` config group.
- `configurator-class`: the canonical name of a class extending the `org.eqasim.core.simulation.EqasimConfigurator` class. If provided, a configurator instance of this class will be used to load the input config file and handle potential extra config groups. Otherwise, the base `EqasimConfigurator` is used.  Note that the class to use needs to be present in your classpath.

The comma separated list arguments are applied on the drt modes according to their respective order of appearence. However if one list contains only one element, that element will be applied on all the modes.
Other configuration parameters to override at the end of the process can be supplied in the command line using the usual `--config:module.param=value` syntax.

## Implementation details
### Analysis
A `DrtAnalysisModule` is provided in the `org.eqasim.core.simulation.modes.drt.analysis` and used by default in simulations feature drt modes. 
It follows the same logic as other analyses in the Eqasim pipeline by generating files at the frequency specified in the `analysisInterval` param of the `eqasim` xml config and at the end of the simulation.
The following CSV files are generated:
- `eqasim_drt_passenger_rides.csv`

| person_id | operator_id | vehicle_id | origin_link_id | origin_x | origin_y | destination_link_id | destination_x | destination_y | departure_time | arrival_time | waiting_time | distance |
|-----------|-------------|------------|----------------|----------|----------|---------------------|---------------|---------------|----------------|--------------|--------------|----------|

- `eqasim_drt_vehicle_movements.csv`

| operator_id | vehicle_id | origin_link_id | origin_x | origin_y | destination_link_id | destination_x | destination_y | departure_time | arrival_time | distance | number_of_passengers |
|-------------|------------|----------------|----------|----------|---------------------|---------------|---------------|----------------|--------------|----------|----------------------|

- `eqasim_drt_vehicle_activities.csv`

| operator_id | vehicle_id | link_id | x | y | start_time | end_time | type |
|-------------|------------|---------|---|---|------------|----------|------|

### Mode choice integration of DRT
- To make your DRT modes considered in the mode choice modules, you need to make sure they are allowed by your `ModeAvailability`.
If you want to consider all DRT mode alternatives for all trips, you can simply wrap your existing `ModeAvailability` implementation by a `DrtModeAvailabilityWrapper`.
- Since the DRT router can build a route that consists only a walk leg, it is necessary to make sure that trips using a DRT mode contain at least a DRT leg. This is ensured by the `org.eqasim.core.simulation.modes.drt.mode_choice.constraints.DrtWalkConstraint`. It can be used in the mode choice module by adding `DrtWalkConstraint` to the `tripConstraints` param in the `DiscreteModeChoice` xml config.
- A default `DrtUtilityEstimator` is proposed in `org.eqasim.core.simulation.modes.drt.mode_choice.utilities.estimators`. It relies on an implementation of `org.eqasim.core.simulation.modes.drt.mode_choice.predictors.DrtPredictor` interface to predict `org.eqasim.core.simulation.modes.drt.mode_choice.variables.DrtVariables` that consist of a waiting time, travel time, access-egress time, euclidean distance and monetary cost.
- A `DefaultDrtPredictor` is proposed in the same package, all the variables are estimated directly from the DRT route except the monetary cost. A cost model is used for the latter one. No default cost model is implemented yet, you'll need to implement your own. If you want to use the default estimator with the default predictor but without a cost model, you can use the `ZeroCostModel` as configured by the AdaptConfigForDrt script.


# Running a simulation with on-demand services acting as transit feeders

Similar to the features presented above, the functionalities for supporting intermodal drt services are implemented in the `org.eqasim.core.simulation.modes.feeder_drt`.
This functionality is mainly enabled by a `MultiModeFeederDrtModule` that allows to define and simulate multiple intermodal drt modes. 
In the current implementation, the intermodal routing is done heuristically by choosing the closest PT stations from the origin and destination of the trip as access and egress stops.
Whereas this feature is currently targeted for the combination between DRT and PT, some effort is already done here to be able to move towards a combination of PT and any other mode, and maybe later a combination of any main mode and any secondary mode for access and egress.


## Quick usage

### AdaptConfigForFeederDrt
The `org.eqasim.core.simulation.modes.feeder_drt.utils.AdaptConfigForFeederDrt` script allows to go from a config file featuring a `multiModeDrtModule` to a config file where the drt modes are usable in an intermodal setting. 
This script typically take a config generated by the [AdaptConfigForDrt](#adaptconfigfordrt) script. Below the exhaustive list of supported parameters:
- `input-config-path`: (required) the path to base config file containing the definitions of one or more DRT modes.
- `output-config-path`: (required) the path to the resulting config file.
- `mode-names`: a comma separated list of the names of the feeder drt modes to configure. Default value is `feeder_drt`
- `base-pt-modes`: a comma separated list of the names of the main modes on which the feeder modes will rely to build the central segments. Default value is `pt`.
- `base-drt-modes`: a comma separated list of the names of the access/egress drt modes on which the feeder modes will rely to build access and egress segments. Default value is `drt`.
- `access-egress-transit-stop-modes`: a comma separated list of lists. Each inner list contains the transit modes (rail, tram...) that should be matched by transit stops where intermodality between pt and drt can happen. these modes are separated by a pipe `|` in the inner lists.
- `estimators`: a comma separated list of the names of the estimators to configure in eqasim to be used for the evaluation of trip alternatives using the new added modes.
- `mode-availability`:  same function as the `mode-availability` argument detailed above.
- `configurator-class`: same function as the `configurator-class` argument detailed above.

Same as for `AdaptConfigForDrt`, the comma separated list arguments are applied on the drt modes according to their respective order of appearence. However if one list contains only one element, that element will be applied on all the modes.
The script also add `activityParams` for the stage activities related to the new modes.

### XML config format for the multiModeFeederDrtModule
The `MultiModeFeederDrt` config can also be edited directly in the xml. Below an example:
```xml
<module name="multiModeFeederDrtModule" >
  <!-- Whether or not to perform the analysis for feeder drt services. If set to true, will follow the analysis interval specified in the configuration of the eqasim module -->
  <param name="performAnalysis" value="true" />
  <parameterset type="feederDrt" >
    <!-- The name of the drt mode to use for access and egress segments -->
    <param name="accessEgressModeName" value="drt_for_feeder_b" />
    <!-- Mode which will be handled by PassengerEngine and VrpOptimizer (passengers'/customers' perspective) -->
    <param name="mode" value="feeder_b" />
    <!-- The name by which the pt mode is known to the agents. Most usually pt -->
    <param name="ptModeName" value="pt" />
    <parameterset type="accessEgressStopSelector" >
        <parameterset type="closestAccessEgressStopSelector" >
        <!-- Comma separated list of PT transit modes (rail, bus...) where intermodality can happen, leave empty to allow intermodality everywhere -->
        <param name="accessEgressTransitStopModes" value="rail,tram,subway" />
        <!-- A regex that if matches with the from facility id (resp to facility id) will result in an access (resp egress) drt leg not being constructed. Leave empty to consider access and egress segments at all times -->
        <param name="skipAccessAndEgressAtFacilities" value="outside" />
        </parameterset>
    </parameterset>
  </parameterset>
  <parameterset type="feederDrt" >
    <param name="accessEgressModeName" value="drt_for_feeder_a" />
    <param name="mode" value="feeder_a" />
    <param name="ptModeName" value="pt" />
    <parameterset type="accessEgressStopSelector" >
        <parameterset type="closestAccessEgressStopSelector" >
        <!-- Comma separated list of PT transit modes (rail, bus...) where intermodality can happen, leave empty to allow intermodality everywhere -->
        <param name="accessEgressTransitStopModes" value="rail,tram,subway" />
        <!-- A regex that if matches with the from facility id (resp to facility id) will result in an access (resp egress) drt leg not being constructed. Leave empty to consider access and egress segments at all times -->
        <param name="skipAccessAndEgressAtFacilities" value="outside" />
        </parameterset>
    </parameterset>
  </parameterset>
</module>
```
The example above defines two intermodal feeder drt services relying on two separate drt modes for access and egress. Both allowing intermodality at transit stops located on `rail`, `tram` and `subway` pt routes.
Whereas adding this to your xml config is enough to make the routing and simulation of feeder modes possible. Further configuration of eqasim is needed to fully support these modes in the mode choice. This is what the `AdaptConfigForFeederDrt` script is for.

## Implementation details
### Simulation and routing side
This feature is primarily allowed by the `MultiModeFeederDrtModule` which is inspired by the `MultiModeDrtModule` of the drt contrib. 
Each Feeder DRT mode is set up by a `FeederDrtModeModule` that extends the `AbstractDvrpModeModule` class to make use of modal bindings. 
This module adds a `FeederDrtRoutingModule` responsible for computing routes which requires: a router for access and egress modes (DRT); a router for PT; a `PopulationFactory` and an `AccessEgressStopsSelector` instance. The latter is an interface encapsulating the selection process of access and egress stops (where switching between DRT and PT happens).
The `FeederDrtRoutingModule` computes three route segments using the respective router: a first DRT segment is built from the origin to the access stop using the DRT router. Then a PT segment from the access to the egress stop is built using the PT router. Then another DRT segment from the egress stop to the origin. However, one of the access and egress stops is `null` or if the DRT router fails to calculate one of the access and egress segments, the PT route is prolonged to either start from the origin or end at the destination of the trip. 
The final trip then contains either 1, 2 or 3 segments, so possibly no DRT segment at all.
The plan elements generated by the router is a concatenation of the plan elements generated by the DRT and PT routers for each segment separated by ` interaction` activities. These activities are equipped with a `previousSegmentType` attribute (instance of the `FeederDrtRoutingModule.FeederDrtTripSegmentType` enum) that specifies whether the previous segment is a PT segment (`MAIN`) or a DRT segment (`DRT`).

The first implementation of the `AccessEgressStopsSelector` is the `ClosestAccessEgressStopSelector` which selects the closest stops from the origin and destination for access and egress. 
During initialization, we identify all the possible access and egress stations from the transit schedule using its related `accessEgressTransitStopModes` parameter to filter out certain stops.  
Then, when calculating routes, closest stops to the origin and destination are respectively identified as access and egress facilities.
Moreover, the configuration for `ClosestAccessEgressStopSelector` allows to specify a regex that if matches with the from facility id (resp to facility id) will return `null` as access and egress stops which will lead the router to not construct drt segments for access (resp egress). We typically use this to prevent the router from adding a DRT segment immediately next to an `ouside` activity.


###  Analysis
A `FeederDrtAnalysisModule` is implemented in the `analysis` package allowing to generate `eqasim_feeder_drt_trips.csv` file containing the details of trips performed with an intermodal drt mode. This module is used only if the `performAnalysis` of the `MultiModeFeederDrt` config is set to `true`. In which case the frequency of analysis follows the one defined for other eqasim analyses in the `eqasim.analysisInterval`.

The csv resulting file has the header below:

| person_id | origin_link_id | origin_x | origin_y | destination_link_id | destination_x | destination_y | access_vehicle_id | egress_vehicle_id | access_departure_time | egress_departure_time | access_arrival_time | egress_arrival_time | access_transit_stop_id | egress_transit_stop_id | access_transit_line_id | egress_transit_line_id | access_transit_route_id | egress_transit_route_id |
|-----------|----------------|----------|----------|---------------------|---------------|---------------|-------------------|-------------------|-----------------------|-----------------------|---------------------|---------------------|------------------------|------------------------|------------------------|------------------------|-------------------------|-------------------------|


### Mode choice integration of Feeder DRT

- This base functionality comes with a `FeederConstraint` that ensures that the route of a feeder drt alternative contains at least one PT and one DRT leg as this is not ensured by the router. This constraint also checks that a DRT segment is not immediately next to an `outside` activity.
- Morevoer, a basic utility estimator for feeder drt modes is proposed here (`DefaultFeederDrtUtilityEstimator`). It simply treats the segments as a sequence of trips and delegates the evaluation of each segment to the utility estimator of its related mode (PT or DRT) and sums the values. These base estimators then need to be configured properly in the `eqasim` config.
- You will still need to modify your mode availability for the new modes to be considered in the mode choice. If you just want to enable them for all agents, you can wrap your existing ModeAvailability object under the `org.eqasim.core.simulation.modes.feeder_drt.mode_choice.FeederDrtModeAvailabilityWrapper`, this will add all feeder modes declared under the `MultiModeFeederDrt` config as well as the drt modes under the `multiModeDrt` config that are not covered by a feeder mode.