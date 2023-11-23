# Using Discrete Mode Choice in MATSim through Eqasim-java



This java/maven project contains a core module with generic functionalities for enabling commonly used Discrete Mode Choice (DMC) models in MATSim.
The other modules are use-case specific where appropriate cost models can be implemented and mode choice parameters can be specified.

This early draft is an attempt to document the whole DMC process.
For now, we start with illustrations on the **ile_de_france** package, however they should apply easily to the other packages.


## Running a simulation
The **org.eqasim.ile_de_france** class is the entry point for running ile_de_france simulations.
The only required parameter is **config-path** pointing to a matsim configuration file that has been properly set with the **DiscreteModeChoice** and **Eqasim** modules.
However, parameters present in the config file can be overridden through the command line through the syntax **config.module[.paremeterSetName]\*param=value**. For example, to change the output directory and the number of iterations:

```commandline
JAVA COMMAND --config:controler.lastIteration=10 --config:controler.outputDirectory=output_folder
```

## DMC model configuration
A typical configuration for the DMC module is pasted below. This should be present in your configuration file. The comments in the xml are self explanatory.

```xml
<module name="DiscreteModeChoice" >
    <param name="accumulateEstimationDelays" value="true" />
    <!-- Trips tested with the modes listed here will be cached for each combination of trip and agent during one replanning pass. -->
    <param name="cachedModes" value="pt, car, truck, car_passenger, walk, bike" />
    <!-- Defines whether to run a runtime check that verifies that everything is set up correctl for a 'mode-choice-in-the-loop' setup. -->
    <param name="enforceSinglePlan" value="true" />
    <!-- Defines what happens if there is no feasible choice alternative for an agent: IGNORE_AGENT, INITIAL_CHOICE, EXCEPTION -->
    <param name="fallbackBehaviour" value="EXCEPTION" />
    <!-- Defines how home activities are identified. Built-in choices: FirstActivity, ActivityBased -->
    <param name="homeFinder" value="EqasimHomeFinder" />
    <!-- Defines which ModeAvailability component to use. Built-in choices: Default, Car -->
    <param name="modeAvailability" value="IDFModeAvailability" />
    <!-- Main model type: Trip, Tour -->
    <param name="modelType" value="Tour" />
    <!-- Defines whether the DiscreteModeChoice strategy should be followed by a rerouting of all trips. If the estimator returns alternatives with routes attached this is not necessary. -->
    <param name="performReroute" value="false" />
    <!-- Defines which Selector component to use. Built-in choices: Maximum, MultinomialLogit, Random -->
    <param name="selector" value="Maximum" />
    <!-- Defines a number of TourConstraint components that should be activated. Built-in choices: FromTripBased, VehicleContinuity, SubtourMode -->
    <param name="tourConstraints" value="FromTripBased, EqasimVehicleTourConstraint" />
    <!-- Defines which TourEstimator component to use. Built-in choices: MATSimDayScoring, Cumulative, Uniform -->
    <param name="tourEstimator" value="Cumulative" />
    <!-- Defines a number of TourFilter components that should be activated. Built-in choices: TourLength -->
    <param name="tourFilters" value="OutsideOnlyFilter" />
    <!-- Defines which TourFinder component to use. Built-in choices: PlanBased, ActivityBased, HomeBased -->
    <param name="tourFinder" value="ActivityBased" />
    <!-- Defines a number of TripConstraint components that should be activated. Built-in choices: VehicleContinuity, ShapeFile, LinkAttribute, TransitWalk -->
    <param name="tripConstraints" value="PassengerConstraint, OutsideConstraint, TransitWalk, OutsideRelatedTripConstraint" />
    <!-- Defines which TripEstimator component to use. Built-in choices: MATSimTripScoring, Uniform -->
    <param name="tripEstimator" value="EqasimUtilityEstimator" />
    <!-- Defines a number of TripFilter components that should be activated. Built-in choices:  -->
    <param name="tripFilters" value="" />
    <parameterset type="homeFinder:ActivityBased" >
        <!-- Comma-separated activity types which should be considered as home. -->
        <param name="activityTypes" value="home" />
    </parameterset>
    <parameterset type="modeAvailability:Car" >
        <!-- Defines which modes are avialable to the agents. -->
        <param name="availableModes" value="pt, car, walk, bike" />
    </parameterset>
    <parameterset type="modeAvailability:Default" >
        <!-- Defines which modes are avialable to the agents. -->
        <param name="availableModes" value="pt, car, walk, bike" />
    </parameterset>
    <parameterset type="selector:MultinomialLogit" >
        <!-- Defines whether candidates with a utility lower than the minimum utility should be filtered out. -->
        <param name="considerMinimumUtility" value="false" />
        <!-- Candidates with a utility above that threshold will be cut off to this value. -->
        <param name="maximumUtility" value="700.0" />
        <!-- Candidates with a utility lower than that threshold will not be considered by default. -->
        <param name="minimumUtility" value="-700.0" />
    </parameterset>
    <parameterset type="tourConstraint:SubtourMode" >
        <!-- Modes for which the sub-tour behaviour should be replicated. If all available modes are put here, this equals to SubTourModeChoice with singleLegProbability == 0.0; if only the constrained modes are put here, it equals singleLegProbability > 0.0 -->
        <param name="constrainedModes" value="" />
    </parameterset>
    <parameterset type="tourConstraint:VehicleContinuity" >
        <!-- Defines which modes must fulfill continuity constraints (can only be used where they have been brough to before) -->
        <param name="restrictedModes" value="car, bike" />
    </parameterset>
    <parameterset type="tourFilter:TourLength" >
        <!-- Defines the maximum allowed length of a tour. -->
        <param name="maximumLength" value="10" />
    </parameterset>
    <parameterset type="tourFinder:ActivityBased" >
        <!-- Comma-separated activity types which should be considered as start and end of a tour. If a plan does not start or end with such an activity additional tours are added. -->
        <param name="activityTypes" value="outside, home" />
    </parameterset>
    <parameterset type="tripConstraint:LinkAttribute" >
        <!-- Link attribute that will be considered for feasibility of the trip. -->
        <param name="attributeName" value="null" />
        <!-- Value that the link attributes should equal. -->
        <param name="attributeValue" value="null" />
        <!-- Modes for which the constraint will be considered. -->
        <param name="constrainedModes" value="" />
        <!-- Defines the criterion on when a trip with the constrained mode will be allowed: ORIGIN, DESTINATION, BOTH, ANY, NONE -->
        <param name="requirement" value="BOTH" />
    </parameterset>
    <parameterset type="tripConstraint:ShapeFile" >
        <!-- Modes for which the shapes will be considered. -->
        <param name="constrainedModes" value="" />
        <!-- Path to a shape file, which should have the same projection as the network. -->
        <param name="path" value="null" />
        <!-- Defines the criterion on when a trip with the constrained mode will be allowed: ORIGIN, DESTINATION, BOTH, ANY, NONE -->
        <param name="requirement" value="BOTH" />
    </parameterset>
    <parameterset type="tripConstraint:VehicleContinuity" >
        <!-- Defines if the advanced constriant is used (vehicles must be brought back home). -->
        <param name="isAdvanced" value="true" />
        <!-- Defines which modes must fulfill continuity constraints (can only be used where they have been brough to before) -->
        <param name="restrictedModes" value="car, bike" />
    </parameterset>
    <parameterset type="tripEstimator:MATSimTripScoring" >
        <!-- Modes which are considered as public transit, i.e. they involve waiting for a vehicle. -->
        <param name="ptLegModes" value="pt" />
    </parameterset>
</module>
```

The xml above specifies the internal configuration of the DMC model, however it doesn't specify how the DMC model itself is used by MATSim.
This is done in the **strategy** module configuration.

```xml
<module name="strategy" >
    <!-- the external executable will be called with a config file as argument.  This is the pathname to a possible skeleton config, to which additional information will be added.  Can be null. -->
    <param name="ExternalExeConfigTemplate" value="null" />
    <!-- time out value (in seconds) after which matsim will consider the external strategy as failed -->
    <param name="ExternalExeTimeOut" value="3600" />
    <!-- root directory for temporary files generated by the external executable. Provided as a service; I don't think this is used by MATSim. -->
    <param name="ExternalExeTmpFileRootDir" value="null" />
    <!-- fraction of iterations where innovative strategies are switched off.  Something like 0.8 should be good.  E.g. if you run from iteration 400 to iteration 500, innovation is switched off at iteration 480 -->
    <param name="fractionOfIterationsToDisableInnovation" value="Infinity" />
    <!-- maximum number of plans per agent.  ``0'' means ``infinity''.  Currently (2010), ``5'' is a good number -->
    <param name="maxAgentPlanMemorySize" value="1" />
    <!-- strategyName of PlanSelector for plans removal.  Possible defaults: WorstPlanSelector SelectRandom SelectExpBetaForRemoval ChangeExpBetaForRemoval PathSizeLogitSelectorForRemoval . The current default, WorstPlanSelector is not a good choice from a discrete choice theoretical perspective. Alternatives, however, have not been systematically tested. kai, feb'12 -->
    <param name="planSelectorForRemoval" value="NonSelectedPlanSelector" />
    <parameterset type="strategysettings" >
        <!-- iteration after which strategy will be disabled.  most useful for ``innovative'' strategies (new routes, new times, ...). Normally, better use fractionOfIterationsToDisableInnovation -->
        <param name="disableAfterIteration" value="-1" />
        <!-- path to external executable (if applicable) -->
        <param name="executionPath" value="null" />
        <!-- strategyName of strategy.  Possible default names: SelectRandom BestScore KeepLastSelected ChangeExpBeta SelectExpBeta SelectPathSizeLogit      (selectors), ReRouteTimeAllocationMutatorTimeAllocationMutator_ReRouteChangeSingleTripModeChangeTripModeSubtourModeChoice (innovative strategies). -->
        <param name="strategyName" value="DiscreteModeChoice" />
        <!-- subpopulation to which the strategy applies. "null" refers to the default population, that is, the set of persons for which no explicit subpopulation is defined (ie no subpopulation attribute) -->
        <param name="subpopulation" value="null" />
        <!-- weight of a strategy: for each agent, a strategy will be selected with a probability proportional to its weight -->
        <param name="weight" value="0.05" />
    </parameterset>
    <parameterset type="strategysettings" >
        <param name="disableAfterIteration" value="-1" />
        <param name="executionPath" value="null" />
        <param name="strategyName" value="KeepLastSelected" />
        <param name="subpopulation" value="null" />
        <param name="weight" value="0.95" />
    </parameterset>
</module>
```

The most interesting parameter above is the `<param name="weight" value="0.05">` inside the DiscreteModeChoice related parameter set.
This means that at each iteration, only 5% of the population will undergo DiscreteModeChoice at each iteration.
The choices of the 95% will remain unchanged. Note that if you want to have the DMC model applied for 50% of the population, you should not only set its weight to 0.5 but also the weight of the **KeepLastSelected** strategy. 



## Extending the model 

### Architecture of the DMC module and Eqasim
It could be good if we could have some diagrams here showing the different components. 

### Extension points
