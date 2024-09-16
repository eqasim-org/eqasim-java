## Background and use case

Since the movements of millions of agents need to be simulated in MATSim, simulations
are computationally expensive. Two strategies are commonly applied to reduce computational cost. First, the population can be scaled down  such that, for instance, only 10% or less of all households are simulated. 

Second, if possible, only the region of interest should be simulated. While it is in principle possible to just create a scenario for a city or region from scratch, cutting them out from a larger scenario makes sense as interactions with the surroundings can be considered in the cutting process.

Here is an example of 10% populations statisitcs before cutting:
```
Population Stats:
+--------------+--------+
|         stat | total  |
+--------------+--------+
|          hhs | 398691 |
|      persons | 398691 |
+--------------+--------+
Modes:
+---------------+--------+
|         modes | total  |
+---------------+--------+
|           car | 793577 |
|          bike | 53111  |
|          rail | 90146  |
|          tram |  2156  |
|        subway | 26145  |
|           bus | 96143  |
|          taxi | 73516  |
|         ferry |  185   |
| car_passenger | 155192 |
|          walk | 565795 |
+---------------+--------+
```

After cutting: 
```
Population Stats:
+--------------+-------+
|         stat | total |
+--------------+-------+
|          hhs | 15367 |
|      persons | 15367 |
+--------------+-------+
Modes:
+---------------+-------+
|         modes | total |
+---------------+-------+
|           car | 24208 |
|           bus |  501  |
|          taxi |  120  |
|          walk |  6009 |
|          bike |  764  |
| car_passenger |  3341 |
|       outside |  5397 |
+---------------+-------+
```

The cutter is based on the [eqasim-java](https://github.com/eqasim-org/eqasim-java/tree/develop) open-source framework.
There is a short description of cutting process [here](https://github.com/eqasim-org/eqasim-java/blob/develop/docs/cutting.md) where the cutter can been implemented on eqasim scenarios.

More details of the cutting tool development can be found in chapter 4.2 [here](https://www.research-collection.ethz.ch/handle/20.500.11850/419837).

This following document provides detailed instructions on setting up and running the cutter with simulations running by **city-modelling**.

## Cutter Setup Guidance
Since our simulation inputs (network, population) and configuration setup differ from the simulations supported by eqasim, a few changes are necessary to get the cutter working with the Transport East (TE) simulations.

The inputs files we used cutter are based on the **Transport East AWS account #815306348607** simulations. Two scales of the simulations were tested via the whole cutting process(0.01% and 10%).  You can test the cutter process on the same AWS account by following the guidance described below.

Please follow the instructions from the section to ensure the necessary configurations and files are correctly prepared. 

## Prerequisites

Before starting the cutting procedure, the following files need to be prepared:

- MATSim configurations and simulation inputs
- Network car geometry for snapping facilities to activities
- A shapefile for cutting, set in the correct coordinate reference system (CRS). For cutting Transport East(TE) simulation, the CRS is `EPSG:27700`.

### Step 1: Snap Facilities to Nearest Car Links
The cutter expects all of the links used in the agent plans that allow to use the `car` mode since code is hardcoding [here](https://github.com/eqasim-org/eqasim-java/blob/develop/core/src/main/java/org/eqasim/core/scenario/validation/ScenarioValidator.java#L113).
If cars are not allowed on the link, an error is logged, indicating that the person has an activity attached to a non-car link. 

The solution is to use the [PAM CLI](https://github.com/arup-group/pam/pull/276) to snap facilities to the nearest car links, which is mapping activity locations to the agents' activities.

**Command Example:**
```
pam snap-facilities \
/mnt/efs/population/SE_2019_v1_1pct_2019_20231123/combined_intermodal/final/plans_001.xml \
/mnt/efs/population/SE_2019_v1_1pct_2019_20231123/combined_intermodal/final/plans_001_snapped_facilities.xml \
/mnt/efs/networks/network_with_access_and_0.75_freespeed_v2/standard_outputs/graph/geometry_only_subgraphs/subgraph_geometry_car_27700.geojson 
```

The first argument in the above command is the population input file, and the second argument is the output file where the snapped population data will be saved. The third argument is the network geometry of car links. By doing so, the population data will be updated with adding links attributes to the activity element and the updated plans files will be saved to a new xml file.

An example of the 0.01% population file after snapping the facilities can be found at: 
`/mnt/efs/population/SE_2019_v1_1pct_2019_20231123/combined_intermodal/final/plans_001_facilities_20240705.xml`

For the 10% population, the example command to snap the facilities:
```
pam snap-facilities \
/mnt/efs/population/SE_2019_v1_1pct_2019_20231123/combined_intermodal/final/plans.xml \
/mnt/efs/population/SE_2019_v1_10pct_2019_20240702/combined/final/plans_20240731_kdtree.xml \
/mnt/efs/networks/network_with_access_and_0.75_freespeed_v2/standard_outputs/graph/geometry_only_subgraphs/subgraph_geometry_car_27700.geojson 
```
The file updated 10% population xml file after snapping the facilities can be found at:
`/mnt/efs/population/SE_2019_v1_10pct_2019_20240702/combined/final/plans_20240731_kdtree.xml`

Example of the agent's activity before snapping:
```xml
<activity type="home" start_time="00:00:00" end_time="06:57:00" x="623126.0" y="262969.0"/>
```

Example of the agent's activity after snapping:

```xml
<activity type="home" start_time="00:00:00" end_time="06:57:00" link="402201" x="623126.0" y="262969.0"/>
``` 

### Step 2: Ensure a Non-Empty Facilities File

The cutter requires a non-empty facilities file to function properly. The TE simulations did not previously extract the facility data. Therefore, it is necessary to generate this file from the simulation plans by rerunning the simulations.

TO create a output_facilities.xml, it requires to add the following module to the MATSim config:
**MATSim Config Example:**
```xml
<module name="facilities">
  <param name="facilitiesSource" value="onePerActivityLocationsInPlansFile"/>
</module>
```

For 0.01% simulations, config file path for generating the facilities file:
```
/mnt/efs/simulations_refresh/1pc/skims_fix_new_population_20231123/0/matsim_config_generate_facilities_locations.xml
```

Example command to run the simulations via bitsim:
```
bitsim batch run matsim \
  -c "com.arup.cm.RunArupMatsim /mnt/efs/simulations_refresh/1pc/skims_fix_new_population_20231123/0/matsim_config_generate_facilities_locations.xml" \
  -d "758645626094.dkr.ecr.eu-west-1.amazonaws.com/columbus:arup-matsim" \
  -m 160000 \
  -q spot_100_queue
```
After rerunning the simulation, the `output_facilities.xml` will be updated accordingly. This file will be used as an input for the cutter.

For 10% simulations, the matsim config file path for generating the facilities file can be found [here](https://github.com/arup-group/eqasim-java/docs/cutting-scenarios-CML/matsim_configs_example/matsim_config_generate_facilities.xml) or on TE AWS account at:
```
/mnt/efs/simulations_refresh/10pc/2019_freight_update_baseline_20240726/0/matsim_config_generate_facilities.xml
```

Example command to run the simulations via bitsim:
```
bitsim batch run matsim 
  -c "com.arup.cm.RunArupMatsim /efs/simulations_refresh/10pc/2019_freight_update_baseline_20240726/0/matsim_config_generate_facilities.xml" 
  -d "758645626094.dkr.ecr.eu-west-1.amazonaws.com/columbus:arup-matsim" 
  -m 240000 
  -q spot_100_queue
```


### Step 3: Provide a Shapefile
Ensure the shapefile is saved with the `.shp` extension and uses the same CRS as input data. You can use [geojson.io](https://geojson.io/) to draw the shape for cutting. Make sure it is a compact shape without holes. The shapefile should only contain a single polygon feature.
The cordinate system can be found at the very beginning of the matsim config:
```xml
<param name="coordinateSystem" value="EPSG:27700"/>
```

Shapefile example was saved at:
```
/mnt/efs/analysis/ys/matsim_cutter/cut_shape_file/te_bury_st_edmund_27700.shp
```


### Step 4: Update MATSim Configuration

Several input files need to be prepared for cutting, and most of them come from the previous simulations:
1. Matsim config: Use the previous input `matsim_config.xml` (the one when previous ran simulations to generate the facilities) as a new config to make some changes for cutter. Then a few more changes need to be made to matsim config when rerun the simulations with the outputs from the cutter
2. Population: Use the `output_plans.xml.gz` and `output_vehicles.xml.gz` from simulation outputs. 
3. Network and transit files: Use the `output_network.xml.gz`, `output_transitSchedule.xml.gz`, `output_transitVehicles.xml.gz` from simulation outputs
4. Facilty: Use the `output_facilities.xml.gz` from simulation outputs
5. Shape files: The subset area shape file `te_bury_st_edmund_27700.shp` as descibed in the previous step.

#### Updates to the `matsim_config.xml`
1. Update the input file paths to the correct files, including the paths of network, population, and transit files.

2. Add teleported `outside` mode into the `planscalcroute` module
```xml
<parameterset type="teleportedModeParameters">
  <param name="beelineDistanceFactor" value="1.3"/>
  <param name="mode" value="outside"/>
  <param name="teleportedModeFreespeedFactor" value="null"/>
  <param name="teleportedModeSpeed" value="4.166666666666667"/>
</parameterset>
```

3. Update the `facilities` module setting in the config.
Update the config to use pre-defined facilities data from the previous simulations facilities outputs.
```xml
<module name="facilities">
  <param name="facilitiesSource" value="fromFile"/>
  <param name="inputFacilitiesFile" value="output_facilities.xml.gz"/>
</module>
```

4. Make changes to the `swissRailRaptor` module.
During the cutting process, we found the routing algorithm is being given requests between facilities with link IDs that are in the cut network. Raptor has a reference to a network here and that network is a subsetted network. But when raptor is being asked to calculate the route, it produces legs referencing links that no longer exist in the network which induced an NullPointerException error.

The temporary solution is to comment out the `intermodalAccessEgress` parametersets in the `swissRailRaptor` for `bike`, `car`, `taxi` and `car_passenger` since it's not compatible with the cutter, as shown below:

```xml
<parameterset type="intermodalAccessEgress">
      <param name="mode" value="bike"/>
      <param name="initialSearchRadius" value="2000"/>
      <param name="maxRadius" value="5000"/>
      <param name="personFilterAttribute" value="intermodalBike"/>
      <param name="personFilterValue" value="yes"/>
      <param name="stopFilterAttribute" value="bikeAccessible"/>
      <param name="stopFilterValue" value="true"/>
    </parameterset>
    <parameterset type="intermodalAccessEgress">
      <param name="mode" value="car"/>
      <param name="initialSearchRadius" value="5000"/>
      <param name="searchExtensionRadius" value="1000.0"/>
      <param name="maxRadius" value="10000"/>
      <param name="linkIdAttribute" value="accessLinkId_car"/>
      <param name="personFilterAttribute" value="intermodalCar"/>
      <param name="personFilterValue" value="yes"/>
      <param name="stopFilterAttribute" value="carAccessible"/>
      <param name="stopFilterValue" value="true"/>
    </parameterset>
    <parameterset type="intermodalAccessEgress">
      <param name="mode" value="taxi"/>
      <param name="initialSearchRadius" value="5000"/>
      <param name="searchExtensionRadius" value="1000.0"/>
      <param name="maxRadius" value="10000"/>
      <param name="linkIdAttribute" value="accessLinkId_car"/>
      <param name="personFilterAttribute" value="intermodalTaxi"/>
      <param name="personFilterValue" value="yes"/>
      <param name="stopFilterAttribute" value="carAccessible"/>
      <param name="stopFilterValue" value="true"/>
    </parameterset>
    <parameterset type="intermodalAccessEgress">
      <param name="mode" value="car_passenger"/>
      <param name="initialSearchRadius" value="5000"/>
      <param name="searchExtensionRadius" value="1000.0"/>
      <param name="maxRadius" value="10000"/>
      <param name="linkIdAttribute" value="accessLinkId_car"/>
      <param name="personFilterAttribute" value="intermodalPassenger"/>
      <param name="personFilterValue" value="yes"/>
      <param name="stopFilterAttribute" value="carAccessible"/>
      <param name="stopFilterValue" value="true"/>
    </parameterset>
```

5. Comment out `FastAStarLandmarks` since the cutter didn't support this parameter.

```xml
<param name="routingAlgorithmType" value="FastAStarLandmarks"/> 
```

6. Run the cutter
Here are examples of the updated MATSim config can be found at:

For cutting 0.01% sims: `/mnt/efs/analysis/ys/matsim_cutter/input_files_locations_facilities/matsim_config_cutter.xml`
For cutting the 10% sims: `/mnt/efs/analysis/ys/matsim_cutter/input_files_locations_facilities_10pc_20240909/matsim_config_cutter_no_intermodal_bike.xml`

The example running command is saved in a bash script: `/mnt/efs/analysis/ys/matsim_cutter/run_cutter.sh`

For cutting 0.01% sims:
```sh
java -Xmx12G -cp "core-1.5.0.jar:libs/matsim-2024.0/matsim-2024.0.jar:libs/matsim-2024.0/libs/*" \
 org.eqasim.core.scenario.cutter.RunScenarioCutter \
 --config-path /mnt/efs/analysis/ys/matsim_cutter/input_files_locations_facilities/matsim_config_cutter.xml \
 --output-path output_20240729_001pct \
 --extent-path /mnt/efs/analysis/ys/matsim_cutter/cut_shape_file/te_bury_st_edmund_27700.shp \
 --config:plans.inputPlansFile /mnt/efs/analysis/ys/matsim_cutter/input_files_locations_facilities/output_plans.xml\
 --prefix TE_cutter_ \
 --threads 4 
```

For cutting the 10% sims:

The example of the matsim config for cutter can be found [here](https://github.com/arup-group/eqasim-java/docs/cutting-scenarios-CML/matsim_configs_example/matsim_config_cutter_no_intermodal_bike.xml)

Command to run the cutter:
```sh
java -Xmx12G -cp "core-1.5.0.jar:libs/matsim-2024.0/matsim-2024.0.jar:libs/matsim-2024.0/libs/*" \
 org.eqasim.core.scenario.cutter.RunScenarioCutter \
 --config-path /mnt/efs/analysis/ys/matsim_cutter/input_files_locations_facilities_10pc_20240909/matsim_config_cutter_no_intermodal_bike.xml \
 --output-path /mnt/efs/analysis/ys/matsim_cutter/output_no_intermodal_20240912_10pct \
 --extent-path /mnt/efs/analysis/ys/matsim_cutter/cut_shape_file/te_bury_st_edmund_27700.shp \
 --config:plans.inputPlansFile /mnt/efs/analysis/ys/matsim_cutter/input_files_locations_facilities_10pc_20240909/output_plans.xml.gz \
 --prefix TE_cutter_ \
 --threads 4
```

### Changes to the Cutter Source Code

The JAR file `core-1.5.0.jar` was compiled using the `eqasim-java/core/pom.xml`. One change was made to the source code to make the cutter work. The changes can be found in the forked eqasim repo on the branch named `cutter_test` ([link](https://github.com/arup-group/eqasim-java/tree/cutter-test)).
Michael helped with creating the `matsim-2024.0.jar` from the latest version of the [matsim-libs](https://github.com/matsim-org/matsim-libs). It was built from the latest source for all of matsim, contribs, and dependencies, and then dumped into a libs directory grabbed all of the config, shape, with editing them as required to fix paths, put them all in a input-files directory. The jar files and dependencies can be found at TE AWS account: `/mnt/efs/analysis/ys/matsim_cutter`. 


1. We need to update the `provideModeAwareTripProcessor` method to register the modes with their appropriate processors from the input plans. The original code can be found [here](https://github.com/eqasim-org/eqasim-java/blob/develop/core/src/main/java/org/eqasim/core/scenario/cutter/population/PopulationCutterModule.java#L137-L139). 
Otherwise, it will throw `NullPointerException` error since the used transit modes are not available. 

Updated code:
```java
		if (transitConfig.isUseTransit()) {
			// TODO: This may not only be "pt"!
			List<String> transitModes = Arrays.asList("pt", "bus", "rail", "subway", "ferry", "taxi", "tram");
			for (String mode : transitModes) {
				tripProcessor.setProcessor(mode, transitTripProcessor);
			}
		}
```

After making changes, compile the code using the Maven POM file located at `eqasim-java/core/pom.xml`. 

If the cutter runs successfully to the end, you will obtain the outputs including facilities, network, population, transit schedule vehicles, and the MATSim config.


### Re-run the simulations with cutting inputs
The final step is to rerun and validate the simulation outputs. 

It is recommended to make some changes to the original matsim config by adding the `outside` activity and mode into the `planCalcScore` module, and add `outside` into the `transitModes` parameter in the `transit` module.

The updated config for 0.01% simulation can be found at `/mnt/efs/analysis/ys/matsim_cutter/output_20240729_001pct/matsim_config_TE_cutting_validation.xml`.

For 0.01% simulations, the command to rerun the simulation is as follows:
```
bitsim batch run matsim 
  -c "com.arup.cm.RunArupMatsim /efs/simulations_refresh/10pc/2019_freight_update_baseline_20240726/0/matsim_config_generate_facilities.xml" 
  -d "758645626094.dkr.ecr.eu-west-1.amazonaws.com/columbus:arup-matsim" 
  -m 240000 
  -q spot_100_queue
```

For rerun the 10%, when repeating the same process, it threw another errors saying the scoring parameters for the `walk` mode may not have been properly initialized or accessed. From the investigations, the error was likely caused by a mode or scoring mismatch between the freight subpopulation (which should not have been using the `walk` mode but it seems somehow) and the config of the simulation. By removing the freight agents, using the simplified population xml file, the simulation and avoided the scenario where missing walk mode error. This allowed the simulation to complete successfully. More investigations need to be done to understand why the freight subpopulation are incompatible with simulations.

There is python script using to remove the freight population in the population xml at `/eqasim-java/docs/cutting-scenarios-CML/python_scripts/remove_freight_subpop.py`.

The updated config for 10% simulation can be found at [here](https://github.com/arup-group/eqasim-java/docs/cutting-scenarios-CML/matsim_configs_example/matsim_config_TE_cutting_validation_no_intermodal_no_freight.xml) or TE AWS account at:
`/mnt/efs/analysis/ys/matsim_cutter/output_no_intermodal_20240909_10pct/matsim_config_TE_cutting_validation_no_intermodal.xml`.

For 10% simulations, the command to rerun the simulation is as follows:
```
bitsim batch run matsim 
  -c "com.arup.cm.RunArupMatsim /efs/analysis/ys/matsim_cutter/output_no_intermodal_20240909_10pct/matsim_config_TE_cutting_validation_no_intermodal_no_freight.xml" 
  -d "758645626094.dkr.ecr.eu-west-1.amazonaws.com/columbus:arup-matsim" 
  -m 240000 
  -q spot_100_queue
```
