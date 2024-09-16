MATSim cutter debug notes
===================================
This is the notes for recording of the debug process with matsim cutter for cutting tranpost east simulations.

Remainning bugs when testing with the cutting process for 10% TE siulations:
1. `NullPointerException` error during the cutting process:
This the bug that the cutter routed the post-cutting population and more details are described below

2. Missing `walk` parameters when validated the TE 10% post-cutting simulation.


### Progress of debugging the `NullPointerException` when processing the cutter. 

It failed with the following error when ran the cutter for TE simulations:

```xml
2024-07-18T12:44:53,471  INFO SpeedyALTData:173 calculate min travelcost...
2024-07-18T12:44:53,526  INFO SpeedyALTData:173 calculate min travelcost...
2024-07-18T12:44:53,530  INFO SpeedyALTData:173 calculate min travelcost...
java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null
        at ch.sbb.matsim.routing.pt.raptor.DefaultRaptorStopFinder.addInitialStopsForParamSet(DefaultRaptorStopFinder.java:197)
        at ch.sbb.matsim.routing.pt.raptor.DefaultRaptorStopFinder.findIntermodalStops(DefaultRaptorStopFinder.java:153)
        at ch.sbb.matsim.routing.pt.raptor.DefaultRaptorStopFinder.findAccessStops(DefaultRaptorStopFinder.java:103)
        at ch.sbb.matsim.routing.pt.raptor.DefaultRaptorStopFinder.findStops(DefaultRaptorStopFinder.java:91)
        at ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor.findAccessStops(SwissRailRaptor.java:250)
        at ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor.calcRoute(SwissRailRaptor.java:91)
        at ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorRoutingModule.calcRoute(SwissRailRaptorRoutingModule.java:70)
        at org.matsim.core.router.TripRouter.calcRoute(TripRouter.java:180)
        at org.eqasim.core.scenario.routing.PlanRouter.run(PlanRouter.java:58)
        at org.eqasim.core.scenario.routing.PopulationRouter$Worker.run(PopulationRouter.java:90)
        at java.base/java.lang.Thread.run(Thread.java:840)
Exception in thread "main" java.lang.RuntimeException: Found errors in routing threads
        at org.eqasim.core.scenario.routing.PopulationRouter.run(PopulationRouter.java:61)
        at org.eqasim.core.scenario.cutter.RunScenarioCutter.main(RunScenarioCutter.java:167)
```

with setting up the intermodal access
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

The intermodal access settings(also known as raptor) in the `swissRailRaptor` in the matsim config file lead to issues when trying to find the corresponding stops for each mode. It might not have stops which leading to issues when the SwissRailRaptor tries to find suitable stops.


## RUNNING THE CUTTER LOCALLY ON TE

The following notes were from Michael when ran the cutter locally via dubbger. 

- Yuhao's launch command (on an EC2 instance):

``` 
--config-path /mnt/efs/analysis/ys/matsim_cutter/input_files_locations_facilities_10pc_20240807/matsim_config_cutter_intermodal_bike.xml \
 --output-path /mnt/efs/analysis/ys/matsim_cutter/output_with_intermodal_bike_20240813_10pct \
 --extent-path /mnt/efs/analysis/ys/matsim_cutter/cut_shape_file/te_bury_st_edmund_27700.shp \
 --config:plans.inputPlansFile /mnt/efs/analysis/ys/matsim_cutter/input_files_locations_facilities_10pc_20240807/output_plans.xml.gz\
 --prefix TE_cutter_ \
 --prefix TE_cutter_ \
 --threads 4
 ```

- Grabbed all of the input files off the TE EFS, dropped them into `/Users/mickyfitz/matsim-cutter/cutter-inputs/te/`

- Edited cutter output in the `~/matsim-cutter/cutter-inputs/te/matsim_config_cutter_intermodal_bike.xml` config file:
  - previously pointed to the EFS
  - changed to `/Users/mickyfitz/matsim-cutter/cutter-outputs/te` 

- My local runtime params (running in IntelliJ):

```
--config-path /Users/mickyfitz/matsim-cutter/cutter-inputs/te/matsim_config_cutter_intermodal_bike.xml
--output-path /Users/mickyfitz/matsim-cutter/cutter-outputs/te
--extent-path /Users/mickyfitz/matsim-cutter/cutter-inputs/te/shape-file/te_bury_st_edmund_27700.shp
--config:plans.inputPlansFile /Users/mickyfitz/matsim-cutter/cutter-inputs/te/output_plans.xml.gz
--prefix TE_cutter_
--threads 4
```

- Some model metadata:
  - network nodes: 447,610
  - network links: 985,170
  - number of agents: 

- Failed with the same NPE:

```
java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null
  at ch.sbb.matsim.routing.pt.raptor.DefaultRaptorStopFinder.addInitialStopsForParamSet(DefaultRaptorStopFinder.java:197)
  at ch.sbb.matsim.routing.pt.raptor.DefaultRaptorStopFinder.findIntermodalStops(DefaultRaptorStopFinder.java:153)
  at ch.sbb.matsim.routing.pt.raptor.DefaultRaptorStopFinder.findAccessStops(DefaultRaptorStopFinder.java:103)
  at ch.sbb.matsim.routing.pt.raptor.DefaultRaptorStopFinder.findStops(DefaultRaptorStopFinder.java:91)
  at ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor.findAccessStops(SwissRailRaptor.java:250)
  at ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor.calcRoute(SwissRailRaptor.java:91)
  at ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorRoutingModule.calcRoute(SwissRailRaptorRoutingModule.java:70)
  at org.matsim.core.router.TripRouter.calcRoute(TripRouter.java:180)
  at org.eqasim.core.scenario.routing.PlanRouter.run(PlanRouter.java:58)
  at org.eqasim.core.scenario.routing.PopulationRouter$Worker.run(PopulationRouter.java:91)
  at java.base/java.lang.Thread.run(Thread.java:833)
Exception in thread "main" java.lang.RuntimeException: Found errors in routing threads
  at org.eqasim.core.scenario.routing.PopulationRouter.run(PopulationRouter.java:61)
  at org.eqasim.core.scenario.cutter.RunScenarioCutter.main(RunScenarioCutter.java:167)
```


- I added some logging around the error to pinpoint the agent and plan in `PopulationRouter.Worker.run()` (ln 93)
```java
        for (Person person : localTasks) {
          for (Plan plan : person.getPlans()) {
            try {
              router.run(plan, replaceExistingRoutes, modes);
            } catch (Exception e) {
              System.err.println(String.format("Error routing plan for agent %s", person.getId()));
              System.err.println(String.format("The problematic plan is %s", plan.getId()));
              System.err.println(String.format("The problematic plan is %s", plan));
              throw e;
            }
          }
        }
```

- Some warnings about network links at cutter startup:
```
length=0.0 of link id 5174822858083189091_5174822858083189091 may cause problems
```

- The problematic agents:
```
Error routing plan for agent 100416
The problematic plan is null
The problematic plan is [score=undefined][nof_acts_legs=5][type=null][personId=100416]
```
```xml
        <person id="100416">
                <attributes>
                        <attribute name="age" class="java.lang.Integer">23</attribute>
                        <attribute name="age_group" class="java.lang.String">21 to 25</attribute>
                        <attribute name="area_type" class="java.lang.String">urban</attribute>
                        <attribute name="carAvail" class="java.lang.Boolean">true</attribute>
                        <attribute name="car_avail" class="java.lang.String">yes</attribute>
                        <attribute name="car_competition" class="java.lang.Double">1.0</attribute>
                        <attribute name="disabled" class="java.lang.String">no</attribute>
                        <attribute name="ev" class="java.lang.Boolean">false</attribute>
                        <attribute name="gender" class="java.lang.String">male</attribute>
                        <attribute name="hasBike" class="java.lang.Boolean">true</attribute>
                        <attribute name="hasCar" class="java.lang.Boolean">true</attribute>
                        <attribute name="hasDisability" class="java.lang.Boolean">false</attribute>
                        <attribute name="hasLicence" class="java.lang.Boolean">true</attribute>
                        <attribute name="hcounty" class="java.lang.String">Suffolk</attribute>
                        <attribute name="hhincome" class="java.lang.String">high</attribute>
                        <attribute name="hholdnumchildren" class="java.lang.Integer">0</attribute>
                        <attribute name="hhsize" class="java.lang.Integer">2</attribute>
                        <attribute name="hid" class="java.lang.String">100416</attribute>
                        <attribute name="hid_old" class="java.lang.String">2006006952_6</attribute>
                        <attribute name="householdid" class="java.lang.Integer">2006006952</attribute>
                        <attribute name="hzone" class="java.lang.String">E02006277</attribute>
                        <attribute name="indincome" class="java.lang.String">low</attribute>
                        <attribute name="individualid" class="java.lang.Integer">2006016698</attribute>
                        <attribute name="intermodalBike" class="java.lang.String">yes</attribute>
                        <attribute name="intermodalCar" class="java.lang.String">yes</attribute>
                        <attribute name="intermodalPassenger" class="java.lang.String">no</attribute>
                        <attribute name="intermodalTaxi" class="java.lang.String">yes</attribute>
                        <attribute name="marital_status" class="java.lang.String">single</attribute>
                        <attribute name="region" class="java.lang.String">East of England</attribute>
                        <attribute name="seasontickettype" class="java.lang.String">season_ticket</attribute>
                        <attribute name="sex" class="java.lang.String">m</attribute>
                        <attribute name="subpopulation" class="java.lang.String">high</attribute>
                        <attribute name="surveyyear" class="java.lang.Integer">2006</attribute>
                        <attribute name="vehicles" class="org.matsim.vehicles.PersonVehicles">{"car":"100416","taxi":"100416_taxi","car_passenger":"100416_car_passenger"}</attribute>
                        <attribute name="workstatus" class="java.lang.String">full-time</attribute>
                </attributes>
                <plan score="153.7967889176747" selected="no">
                        <activity type="home" link="5176972368624430675_5176972369251901811" facility="f_auto_3957" x="586860.0" y="264147.0" start_time="00:00:00" end_time="07:15:00" >
                        </activity>
                        <leg mode="walk" dep_time="07:15:00" trav_time="00:43:34">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">bus</attribute>
                                </attributes>
                                <route type="generic" start_link="5176972368624430675_5176972369251901811" end_link="5176971628156524759_5176971628129992151" trav_time="00:43:34" distance="2178.834128610987"></route>
                        </leg>
                        <activity type="work" link="5176971628156524759_5176971628129992151" facility="f_auto_1012" x="585190.0" y="264289.0" start_time="08:15:00" end_time="18:20:00" >
                        </activity>
                        <leg mode="walk" dep_time="18:20:00" trav_time="00:06:34">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">bus</attribute>
                                </attributes>
                                <route type="generic" start_link="5176971628156524759_5176971628129992151" end_link="5176971626509406753_5176971626509406753" trav_time="00:06:34" distance="329.14342336546457"></route>
                        </leg>
                        <activity type="pt interaction" link="5176971626509406753_5176971626509406753" x="585113.7817854984" y="264552.37120805687" max_dur="00:00:00" >
                        </activity>
                        <leg mode="walk" dep_time="18:26:34" trav_time="00:00:01">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">bus</attribute>
                                </attributes>
                                <route type="generic" start_link="5176971626509406753_5176971626509406753" end_link="5176971626509406753_5176971626509406753" trav_time="00:00:01" distance="0.0"></route>
                        </leg>
                        <activity type="pt interaction" link="5176971626509406753_5176971626509406753" x="585132.9173118277" y="264535.6685015094" max_dur="00:00:00" >
                        </activity>
                        <leg mode="bus" dep_time="18:26:35" trav_time="00:15:25">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">bus</attribute>
                                </attributes>
                                <route type="default_pt" start_link="5176971626509406753_5176971626509406753" end_link="5176971781817045223_5176971781817045223" trav_time="00:15:25" distance="764.3628558597625">{"transitRouteId":"12298_3","boardingTime":"18:40:00","transitLineId":"12298","accessFacilityId":"390GBURY1","egressFacilityId":"390050800"}</route>
                        </leg>
                        <activity type="pt interaction" link="5176971781817045223_5176971781817045223" x="585350.9101181736" y="265156.846492815" max_dur="00:00:00" >
                        </activity>
                        <leg mode="walk" dep_time="18:42:00" trav_time="00:01:44">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">bus</attribute>
                                </attributes>
                                <route type="generic" start_link="5176971781817045223_5176971781817045223" end_link="5176971782047279209_5176971782047279209" trav_time="00:01:44" distance="87.0"></route>
                        </leg>
                        <activity type="pt interaction" link="5176971782047279209_5176971782047279209" x="585301.8659321591" y="265201.775893673" max_dur="00:00:00" >
                        </activity>
                        <leg mode="walk" dep_time="18:43:44" trav_time="00:00:00">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">bus</attribute>
                                </attributes>
                                <route type="generic" start_link="5176971782047279209_5176971782047279209" end_link="351882" trav_time="00:00:00" distance="0.0"></route>
                        </leg>
                        <activity type="pt interaction" link="351882" x="584873.589101083" y="265219.2209167288" max_dur="00:00:00" >
                        </activity>
                        <leg mode="car" dep_time="18:43:44" trav_time="00:05:08">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">bus</attribute>
                                </attributes>
                                <route type="links" start_link="351882" end_link="5176972368624430675_5176972369251901811" trav_time="00:05:08" distance="4252.04439399038" vehicleRefId="null">351882 149803 173985 5176971781650859901_5176971620856238905 66628 114785 5176971621009784379_5176971621013124615 619265 517281 198796 5176971779363914867_5176971779453028437 278389 571492 5176971779669336995_5176971779678391225 129891 609458 172720 162570 5176972406610113833_5176972406600626477 133548 311113 11811 250171 5176972407293168211_5176972407501322301 188897 396780 75816 5176972368624430675_5176972369251901811</route>
                        </leg>
                        <activity type="home" link="5176972368624430675_5176972369251901811" facility="f_auto_3957" x="586860.0" y="264147.0" start_time="19:30:00" end_time="24:00:00" >
                        </activity>
                </plan>

                <plan score="209.4593927492598" selected="yes">
                        <activity type="home" link="5176972368624430675_5176972369251901811" facility="f_auto_3957" x="586860.0" y="264147.0" start_time="00:00:00" end_time="07:02:03" >
                        </activity>
                        <leg mode="walk" dep_time="07:02:03" trav_time="00:43:34">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">bus</attribute>
                                </attributes>
                                <route type="generic" start_link="5176972368624430675_5176972369251901811" end_link="5176971628156524759_5176971628129992151" trav_time="00:43:34" distance="2178.834128610987"></route>
                        </leg>
                        <activity type="work" link="5176971628156524759_5176971628129992151" facility="f_auto_1012" x="585190.0" y="264289.0" start_time="08:15:00" end_time="18:21:48" >
                        </activity>
                        <leg mode="walk" dep_time="18:21:48" trav_time="00:43:34">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">bus</attribute>
                                </attributes>
                                <route type="generic" start_link="5176971628156524759_5176971628129992151" end_link="5176972368624430675_5176972369251901811" trav_time="00:43:34" distance="2178.834128610987"></route>
                        </leg>
                        <activity type="home" link="5176972368624430675_5176972369251901811" facility="f_auto_3957" x="586860.0" y="264147.0" start_time="19:30:00" end_time="24:12:58" >
                        </activity>
                </plan>

        </person>
```

```
Error routing plan for agent 103660
The problematic plan is null
The problematic plan is [score=undefined][nof_acts_legs=7][type=null][personId=103660]
```
```xml
        <person id="103660">
                <attributes>
                        <attribute name="age" class="java.lang.Integer">18</attribute>
                        <attribute name="age_group" class="java.lang.String">16 to 20</attribute>
                        <attribute name="area_type" class="java.lang.String">urban</attribute>
                        <attribute name="carAvail" class="java.lang.Boolean">true</attribute>
                        <attribute name="car_avail" class="java.lang.String">yes</attribute>
                        <attribute name="car_competition" class="java.lang.Double">1.0</attribute>
                        <attribute name="disabled" class="java.lang.String">no</attribute>
                        <attribute name="ev" class="java.lang.Boolean">false</attribute>
                        <attribute name="gender" class="java.lang.String">female</attribute>
                        <attribute name="hasBike" class="java.lang.Boolean">true</attribute>
                        <attribute name="hasCar" class="java.lang.Boolean">true</attribute>
                        <attribute name="hasDisability" class="java.lang.Boolean">false</attribute>
                        <attribute name="hasLicence" class="java.lang.Boolean">true</attribute>
                        <attribute name="hcounty" class="java.lang.String">Suffolk</attribute>
                        <attribute name="hhincome" class="java.lang.String">low</attribute>
                        <attribute name="hholdnumchildren" class="java.lang.Integer">0</attribute>
                        <attribute name="hhsize" class="java.lang.Integer">2</attribute>
                        <attribute name="hid" class="java.lang.String">103660</attribute>
                        <attribute name="hid_old" class="java.lang.String">2006008433_5</attribute>
                        <attribute name="householdid" class="java.lang.Integer">2006008433</attribute>
                        <attribute name="hzone" class="java.lang.String">E02006278</attribute>
                        <attribute name="indincome" class="java.lang.String">low</attribute>
                        <attribute name="individualid" class="java.lang.Integer">2006020140</attribute>
                        <attribute name="intermodalBike" class="java.lang.String">yes</attribute>
                        <attribute name="intermodalCar" class="java.lang.String">yes</attribute>
                        <attribute name="intermodalPassenger" class="java.lang.String">no</attribute>
                        <attribute name="intermodalTaxi" class="java.lang.String">yes</attribute>
                        <attribute name="marital_status" class="java.lang.String">single</attribute>
                        <attribute name="region" class="java.lang.String">East of England</attribute>
                        <attribute name="seasontickettype" class="java.lang.String">season_ticket</attribute>
                        <attribute name="sex" class="java.lang.String">f</attribute>
                        <attribute name="subpopulation" class="java.lang.String">low</attribute>
                        <attribute name="surveyyear" class="java.lang.Integer">2006</attribute>
                        <attribute name="vehicles" class="org.matsim.vehicles.PersonVehicles">{"car":"103660","taxi":"103660_taxi","car_passenger":"103660_car_passenger"}</attribute>
                        <attribute name="workstatus" class="java.lang.String">full-time</attribute>
                </attributes>
                <plan score="102.83088242589389" selected="yes">
                        <activity type="home" link="5176971422848555195_5176971422678725439" facility="f_auto_5287" x="584480.0" y="265597.0" start_time="00:00:00" end_time="13:20:00" >
                        </activity>
                        <leg mode="walk" dep_time="13:20:00" trav_time="00:07:40">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="generic" start_link="5176971422848555195_5176971422678725439" end_link="5176971428130569763_5176971428130569763" trav_time="00:07:40" distance="383.93481572394035"></route>
                        </leg>
                        <activity type="pt interaction" link="5176971428130569763_5176971428130569763" x="584318.3487180443" y="265333.49845732463" max_dur="00:00:00" >
                        </activity>
                        <leg mode="walk" dep_time="13:27:40" trav_time="00:00:01">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="generic" start_link="5176971428130569763_5176971428130569763" end_link="5176971428130569763_5176971428130569763" trav_time="00:00:01" distance="0.0"></route>
                        </leg>
                        <activity type="pt interaction" link="5176971428130569763_5176971428130569763" x="584322.8886494723" y="265346.92306213506" max_dur="00:00:00" >
                        </activity>
                        <leg mode="bus" dep_time="13:27:41" trav_time="00:06:00">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="default_pt" start_link="5176971428130569763_5176971428130569763" end_link="5176971782264587763_5176971782264587763" trav_time="00:06:00" distance="2062.1549659646353">{"transitRouteId":"6761_1","boardingTime":"13:28:00","transitLineId":"6761","accessFacilityId":"390050846","egressFacilityId":"390050804"}</route>
                        </leg>
                        <activity type="pt interaction" link="5176971782264587763_5176971782264587763" x="585373.8724144999" y="265218.841602804" max_dur="00:00:00" >
                        </activity>
                        <leg mode="walk" dep_time="13:33:41" trav_time="00:01:51">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="generic" start_link="5176971782264587763_5176971782264587763" end_link="5176971782047279209_5176971782047279209" trav_time="00:01:51" distance="97.0"></route>
                        </leg>
                        <activity type="pt interaction" link="5176971782047279209_5176971782047279209" x="585301.8661946068" y="265201.778045431" max_dur="00:00:00" >
                        </activity>
                        <leg mode="rail" dep_time="13:35:37" trav_time="01:05:23">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="default_pt" start_link="5176971782047279209_5176971782047279209" end_link="5177011483058159475_5177011483058159475" trav_time="01:05:23" distance="41551.1341413336">{"transitRouteId":"55095_2","boardingTime":"13:55:00","transitLineId":"55095","accessFacilityId":"910GBSTEDMS","egressFacilityId":"910GCAMBDGE"}</route>
                        </leg>
                        <activity type="pt interaction" link="5177011483058159475_5177011483058159475" x="546196.9010489563" y="257246.6591357863" max_dur="00:00:00" >
                        </activity>
                        <leg mode="walk" dep_time="14:41:00" trav_time="00:00:00">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="generic" start_link="5177011483058159475_5177011483058159475" end_link="72085" trav_time="00:00:00" distance="0.0"></route>
                        </leg>
                        <activity type="pt interaction" link="72085" x="546110.2881200865" y="256703.126647814" max_dur="00:00:00" >
                        </activity>
                        <leg mode="car" dep_time="14:41:00" trav_time="00:01:44">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="links" start_link="72085" end_link="417585" trav_time="00:01:44" distance="1616.2390590780249" vehicleRefId="null">72085 402743 176248 475628 5177011590460321117_5177011590309233653 313606 45072 300363 5177011585251686375_5177011585831667673 567970 223505 417585</route>
                        </leg>
                        <activity type="work" link="417585" facility="f_auto_494" x="545271.0" y="258180.0" start_time="13:45:00" end_time="21:55:00" >
                        </activity>
                        <leg mode="bike" dep_time="21:55:00" trav_time="00:06:50">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="generic" start_link="417585" end_link="5177011483058159475_5177011483058159475" trav_time="00:06:50" distance="1709.1010171858297"></route>
                        </leg>
                        <activity type="pt interaction" link="5177011483058159475_5177011483058159475" x="546196.8985057314" y="257246.6632130217" max_dur="00:00:00" >
                        </activity>
                        <leg mode="walk" dep_time="22:01:50" trav_time="00:00:01">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="generic" start_link="5177011483058159475_5177011483058159475" end_link="5177011483058159475_5177011483058159475" trav_time="00:00:01" distance="0.0"></route>
                        </leg>
                        <activity type="pt interaction" link="5177011483058159475_5177011483058159475" x="546196.9010489563" y="257246.6591357863" max_dur="00:00:00" >
                        </activity>
                        <leg mode="rail" dep_time="22:01:51" trav_time="01:27:09">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="default_pt" start_link="5177011483058159475_5177011483058159475" end_link="5176971782047279209_5176971782047279209" trav_time="01:27:09" distance="43100.41522111105">{"transitRouteId":"55095_5","boardingTime":"22:47:00","transitLineId":"55095","accessFacilityId":"910GCAMBDGE","egressFacilityId":"910GBSTEDMS"}</route>
                        </leg>
                        <activity type="pt interaction" link="5176971782047279209_5176971782047279209" x="585301.8661946068" y="265201.778045431" max_dur="00:00:00" >
                        </activity>
                        <leg mode="walk" dep_time="23:29:00" trav_time="00:00:00">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="generic" start_link="5176971782047279209_5176971782047279209" end_link="351882" trav_time="00:00:00" distance="0.0"></route>
                        </leg>
                        <activity type="pt interaction" link="351882" x="584873.589101083" y="265219.2209167288" max_dur="00:00:00" >
                        </activity>
                        <leg mode="car" dep_time="23:29:00" trav_time="00:03:58">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="links" start_link="351882" end_link="5176971422848555195_5176971422678725439" trav_time="00:03:58" distance="2111.3364227782304" vehicleRefId="null">351882 149803 5176971781593483131_5176971782264587763 5176971782264587763_5176971782266928655 5176971782266928655_5176971782288252011 5176971782288252011_5176971782370369451 5176971782370369451_5176971785393766433 438549 5176971786148556487_5176971786294073777 5176971786294073777_5176971810418408001 5176971810418408001_5176971810471052063 59819 5176971810218828391_5176971810928830397 221894 5176971809472173411_5176971809440890277 550156 5176971809398977613_5176971809248564755 5176971809248564755_5176971809182413881 5176971809182413881_5176971799083264243 551553 5176971799369299617_5176971799369892369 5176971799369892369_5176971799550374741 460797 268673 12495 5176971800887596115_5176971801588016817 5176971801588016817_5176971801526896535 524022 5176971417707667685_5176971417717510415 523192 425397 5176971417824443831_5176971417800606061 5176971417800606061_5176971423086769507 303139 5176971422678725439_5176971422848555195 5176971422848555195_5176971422678725439</route>
                        </leg>
                        <activity type="home" link="5176971422848555195_5176971422678725439" facility="f_auto_5287" x="584480.0" y="265597.0" start_time="22:15:00" end_time="24:00:00" >
                        </activity>
                </plan>

                <plan score="-142.94496100135188" selected="no">
                        <activity type="home" link="5176971422848555195_5176971422678725439" facility="f_auto_5287" x="584480.0" y="265597.0" start_time="00:00:00" end_time="13:33:42" >
                        </activity>
                        <leg mode="taxi" dep_time="13:33:42" trav_time="00:03:03">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="links" start_link="5176971422848555195_5176971422678725439" end_link="351882" trav_time="00:03:03" distance="2110.4628954533277" vehicleRefId="null">5176971422848555195_5176971422678725439 546049 5176971423086769507_5176971417800606061 5176971417800606061_5176971417824443831 479151 624260 5176971417717510415_5176971417707667685 339356 5176971801526896535_5176971801588016817 5176971801588016817_5176971800887596115 201183 124775 96881 5176971799550374741_5176971799369892369 5176971799369892369_5176971799369299617 398607 5176971799083264243_5176971809182413881 5176971809182413881_5176971809248564755 354415 5176971809284191073_5176971809302446207 364152 276859 456343 5176971810974267095_5176971810971127253 5176971810971127253_5176971810928830397 5176971810928830397_5176971810218828391 244593 5176971810471052063_5176971810418408001 5176971810418408001_5176971786294073777 5176971786294073777_5176971786148556487 377296 5176971785393766433_5176971782370369451 5176971782370369451_5176971782288252011 5176971782288252011_5176971782266928655 5176971782266928655_5176971782264587763 5176971782264587763_5176971781593483131 351882</route>
                        </leg>
                        <activity type="pt interaction" link="351882" x="584873.589101083" y="265219.2209167288" max_dur="00:00:00" >
                        </activity>
                        <leg mode="walk" dep_time="13:36:45" trav_time="00:00:00">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="generic" start_link="351882" end_link="5176971782047279209_5176971782047279209" trav_time="00:00:00" distance="0.0"></route>
                        </leg>
                        <activity type="pt interaction" link="5176971782047279209_5176971782047279209" x="585301.8659321591" y="265201.775893673" max_dur="00:00:00" >
                        </activity>
                        <leg mode="walk" dep_time="13:36:45" trav_time="00:00:01">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="generic" start_link="5176971782047279209_5176971782047279209" end_link="5176971782047279209_5176971782047279209" trav_time="00:00:01" distance="0.0"></route>
                        </leg>
                        <activity type="pt interaction" link="5176971782047279209_5176971782047279209" x="585301.8661946068" y="265201.778045431" max_dur="00:00:00" >
                        </activity>
                        <leg mode="rail" dep_time="13:36:46" trav_time="01:04:14">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="default_pt" start_link="5176971782047279209_5176971782047279209" end_link="5177011483058159475_5177011483058159475" trav_time="01:04:14" distance="41551.1341413336">{"transitRouteId":"55095_2","boardingTime":"13:55:00","transitLineId":"55095","accessFacilityId":"910GBSTEDMS","egressFacilityId":"910GCAMBDGE"}</route>
                        </leg>
                        <activity type="pt interaction" link="5177011483058159475_5177011483058159475" x="546196.9010489563" y="257246.6591357863" max_dur="00:00:00" >
                        </activity>
                        <leg mode="walk" dep_time="14:41:00" trav_time="00:03:55">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="generic" start_link="5177011483058159475_5177011483058159475" end_link="5177011476836422541_5177011476836422541" trav_time="00:03:55" distance="200.0"></route>
                        </leg>
                        <activity type="pt interaction" link="5177011476836422541_5177011476836422541" x="546097.8844187699" y="257129.53941952455" max_dur="00:00:00" >
                        </activity>
                        <leg mode="bus" dep_time="14:45:00" trav_time="00:14:00">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="default_pt" start_link="5177011476836422541_5177011476836422541" end_link="5177011578203780053_5177011578203780053" trav_time="00:14:00" distance="1089.297654735279">{"transitRouteId":"46103_0","boardingTime":"14:52:00","transitLineId":"46103","accessFacilityId":"0500CCITY528","egressFacilityId":"0500CCITY321"}</route>
                        </leg>
                        <activity type="pt interaction" link="5177011578203780053_5177011578203780053" x="545399.8929396417" y="257943.37673075608" max_dur="00:00:00" >
                        </activity>
                        <leg mode="walk" dep_time="14:59:00" trav_time="00:07:00">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="generic" start_link="5177011578203780053_5177011578203780053" end_link="417585" trav_time="00:07:00" distance="350.2865895646185"></route>
                        </leg>
                        <activity type="work" link="417585" facility="f_auto_494" x="545271.0" y="258180.0" start_time="13:45:00" end_time="21:43:02" >
                        </activity>
                        <leg mode="walk" dep_time="21:43:02" trav_time="17:17:30">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="generic" start_link="417585" end_link="5176971422848555195_5176971422678725439" trav_time="17:17:30" distance="51875.661858910295"></route>
                        </leg>
                        <activity type="home" link="5176971422848555195_5176971422678725439" facility="f_auto_5287" x="584480.0" y="265597.0" start_time="22:15:00" end_time="24:07:03" >
                        </activity>
                </plan>

        </person>
```

```
Error routing plan for agent 106656
The problematic plan is null
The problematic plan is [score=undefined][nof_acts_legs=7][type=null][personId=106656]
```
```xml
        <person id="106656">
                <attributes>
                        <attribute name="age" class="java.lang.Integer">54</attribute>
                        <attribute name="age_group" class="java.lang.String">50 to 59</attribute>
                        <attribute name="area_type" class="java.lang.String">rural</attribute>
                        <attribute name="carAvail" class="java.lang.Boolean">true</attribute>
                        <attribute name="car_avail" class="java.lang.String">yes</attribute>
                        <attribute name="car_competition" class="java.lang.Double">1.0</attribute>
                        <attribute name="disabled" class="java.lang.String">no</attribute>
                        <attribute name="ev" class="java.lang.Boolean">false</attribute>
                        <attribute name="gender" class="java.lang.String">male</attribute>
                        <attribute name="hasBike" class="java.lang.Boolean">true</attribute>
                        <attribute name="hasCar" class="java.lang.Boolean">true</attribute>
                        <attribute name="hasDisability" class="java.lang.Boolean">false</attribute>
                        <attribute name="hasLicence" class="java.lang.Boolean">true</attribute>
                        <attribute name="hcounty" class="java.lang.String">Suffolk</attribute>
                        <attribute name="hhincome" class="java.lang.String">medium</attribute>
                        <attribute name="hholdnumchildren" class="java.lang.Integer">2</attribute>
                        <attribute name="hhsize" class="java.lang.Integer">4</attribute>
                        <attribute name="hid" class="java.lang.String">106656</attribute>
                        <attribute name="hid_old" class="java.lang.String">2006009260_7</attribute>
                        <attribute name="householdid" class="java.lang.Integer">2006009260</attribute>
                        <attribute name="hzone" class="java.lang.String">E02006229</attribute>
                        <attribute name="indincome" class="java.lang.String">low</attribute>
                        <attribute name="individualid" class="java.lang.Integer">2006022136</attribute>
                        <attribute name="intermodalBike" class="java.lang.String">yes</attribute>
                        <attribute name="intermodalCar" class="java.lang.String">yes</attribute>
                        <attribute name="intermodalPassenger" class="java.lang.String">no</attribute>
                        <attribute name="intermodalTaxi" class="java.lang.String">yes</attribute>
                        <attribute name="marital_status" class="java.lang.String">married</attribute>
                        <attribute name="region" class="java.lang.String">East of England</attribute>
                        <attribute name="seasontickettype" class="java.lang.String">other</attribute>
                        <attribute name="sex" class="java.lang.String">m</attribute>
                        <attribute name="subpopulation" class="java.lang.String">medium</attribute>
                        <attribute name="surveyyear" class="java.lang.Integer">2006</attribute>
                        <attribute name="vehicles" class="org.matsim.vehicles.PersonVehicles">{"car":"106656","taxi":"106656_taxi","car_passenger":"106656_car_passenger"}</attribute>
                        <attribute name="workstatus" class="java.lang.String">full-time</attribute>
                </attributes>
                <plan score="150.71178913965176" selected="no">
                        <activity type="home" link="5176980547372196967_5176980550095014727" facility="f_auto_19125" x="586499.0" y="246549.0" start_time="00:00:00" end_time="10:28:00" >
                        </activity>
                        <leg mode="car" dep_time="10:28:00" trav_time="00:18:28">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">car</attribute>
                                </attributes>
                                <route type="links" start_link="5176980547372196967_5176980550095014727" end_link="5176971741277727863_5176971741659784981" trav_time="00:18:28" distance="20761.067909516" vehicleRefId="null">5176980547372196967_5176980550095014727 5176980550095014727_5176980550092474555 5176980550092474555_5176980550092086249 568213 490141 281314 5176980723378447539_5176980723247839255 246892 5176980722975253219_5176980722386530563 282593 416032 5176980706784673533_5176980707182857833 398690 439553 585810 398074 43650 326296 213591 252095 471083 491216 91838 193939 550316 5176979305021912187_5176979305131857375 603774 588381 5176979195776822355_5176979195777722855 21551 156879 583787 287490 551879 5176978779116777199_5176978779128804231 512618 253784 5176978720783650437_5176978720341453745 5176978720341453745_5176978717830843381 5176978717830843381_5176978717977742037 476975 430134 353123 188345 316485 344043 612388 251084 5176978654358268531_5176978654358602329 232582 5176972821864324957_5176972821869618193 156211 49292 501283 368948 346606 437980 125857 319010 507265 306015 5176972894324164917_5176972523524374329 627215 5176972523196455301_5176972522421494983 508507 186995 174087 345930 5176972503160839915_5176972503221302419 5176972503221302419_5176972456966770231 251182 5176972441047809397_5176972441128663439 124624 5176972433734010305_5176972432212189681 603300 5176972431923588405_5176971701759856261 5176971701759856261_5176971701822774873 76896 579246 5176971702985041941_5176971702987790479 598664 41169 625535 5176971703170502679_5176971703165203813 123778 5176971706100072663_5176971706499144429 5176971706499144429_5176971706453229565 5176971706453229565_5176971706432951471 252695 5176971707120419305_5176971707262174163 5176971707262174163_5176971707384702831 5176971707384702831_5176971707361614315 5176971707361614315_5176971683187197809 406969 337817 198169 5176971682852182903_5176971682907408325 343097 498391 567350 363572 5176971679222041181_5176971679218512717 55828 550469 91960 501338 463030 415415 448245 620346 5176971724591834687_5176971724592380649 5176971724592380649_5176971724703679907 148730 5176971724700722331_5176971724711964771 5176971724711964771_5176971724687346919 493303 5176971726082629801_5176971726080961375 5176971726080961375_5176971726171863147 628705 238062 529408 592424 490248 304820 251655 5176971729334606037_5176971729347682217 531454 5176971729237121859_5176971729643317991 5176971729643317991_5176971718215106641 5176971718215106641_5176971741277727863 5176971741277727863_5176971741659784981</route>
                        </leg>
                        <activity type="shop" link="5176971741277727863_5176971741659784981" facility="f_auto_3052" x="585973.0" y="264540.0" start_time="10:44:00" end_time="11:02:00" >
                        </activity>
                        <leg mode="car" dep_time="11:02:00" trav_time="00:03:39">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">car</attribute>
                                </attributes>
                                <route type="links" start_link="5176971741277727863_5176971741659784981" end_link="5176973176231468937_5176973176244935583" trav_time="00:03:39" distance="2709.800648078889" vehicleRefId="null">5176971741277727863_5176971741659784981 5176971741659784981_5176971741277727863 5176971741277727863_5176971718215106641 5176971718215106641_5176971729643317991 5176971729643317991_5176971729237121859 32650 5176971729347682217_5176971729334606037 44122 61851 300736 554296 78527 531571 323179 5176971726171863147_5176971726080961375 630737 5176971724711964771_5176971724700722331 182009 5176971724703679907_5176971724592380649 5176971724592380649_5176971724591834687 143341 26785 325883 134617 2763 146668 544556 5176971680597679929_5176971680619306729 113344 563134 542642 5176971681735506617_5176971681734028625 94549 246597 5176971683187197809_5176971707361614315 5176971707361614315_5176971707384702831 5176971707384702831_5176971707262174163 5176971707262174163_5176971707120419305 480609 5176971706432951471_5176971706453229565 5176971706453229565_5176971706499144429 5176971706499144429_5176971706100072663 489664 5176971703165203813_5176971703170502679 5176971703170502679_5176971703124389725 557262 93875 420215 321186 129291 614988 5176971702617341701_5176971702529042143 432689 396662 556889 558070 507876 459729 290838 358719 37654 5176973176231468937_5176973176244935583</route>
                        </leg>
                        <activity type="other" link="5176973176231468937_5176973176244935583" facility="f_auto_3640" x="585797.0" y="262729.0" start_time="11:07:00" end_time="14:48:00" >
                        </activity>
                        <leg mode="car" dep_time="14:48:00" trav_time="00:16:48">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">car</attribute>
                                </attributes>
                                <route type="links" start_link="5176973176231468937_5176973176244935583" end_link="5176980547372196967_5176980550095014727" trav_time="00:16:48" distance="19609.726361424488" vehicleRefId="null">5176973176231468937_5176973176244935583 5176973176244935583_5176973176231468937 538801 390015 96704 218118 567296 456584 358685 294944 554621 5176971696638932979_5176971696628990567 513726 353661 348788 420215 5176971702987790479_5176971702985041941 542867 13000 296388 5176971701822774873_5176971701759856261 5176971701759856261_5176972431923588405 595651 5176972432212189681_5176972433734010305 155300 5176972441128663439_5176972441047809397 599690 517697
2456966770231_5176972503221302419 5176972503221302419_5176972503160839915 493098 491942 326190 91228 5176972522421494983_5176972523196455301 521019 5176972523524374329_5176972894324164917 216742 181714 521015 632073 236136 61481 298400 598901 356881 424720 5176972821869618193_5176972821864324957 239773 5176978654358602329_5176978654358268531 409983 452943 405859 27061 288388 480353 74890 341219 5176978717977742037_5176978717830843381 5176978717830843381_5176978720341453745 5176978720341453745_5176978720783650437 66943 559804 5176978779128804231_5176978779116777199 266208 303702 321840 280110 379427 5176979195777722855_5176979195776822355 338198 318894 5176979305131857375_5176979305021912187 485227 432620 366196 122305 360896 392634 303552 387063 551604 395279 632195 394217 463090 5176980707182857833_5176980706784673533 522064 22993 5176980722386530563_5176980722975253219 591258 5176980723247839255_5176980723378447539 44403 623650 575193 5176980550092086249_5176980550092474555 5176980550092474555_5176980550095014727 5176980550095014727_5176980547372196967 5176980547372196967_5176980550095014727</route>
                        </leg>
                        <activity type="home" link="5176980547372196967_5176980550095014727" facility="f_auto_19125" x="586499.0" y="246549.0" start_time="15:15:00" end_time="24:00:00" >
                        </activity>
                </plan>

                <plan score="-174.69179409349144" selected="yes">
                        <activity type="home" link="5176980547372196967_5176980550095014727" facility="f_auto_19125" x="586499.0" y="246549.0" start_time="00:00:00" end_time="10:28:00" >
                        </activity>
                        <leg mode="walk" dep_time="10:28:00" trav_time="00:00:29">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="generic" start_link="5176980547372196967_5176980550095014727" end_link="5176980550095014727_5176980550095014727" trav_time="00:00:29" distance="24.508945955231585"></route>
                        </leg>
                        <activity type="pt interaction" link="5176980550095014727_5176980550095014727" x="586510.0529568413" y="246557.85261155193" max_dur="00:00:00" >
                        </activity>
                        <leg mode="walk" dep_time="10:28:29" trav_time="00:00:01">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="generic" start_link="5176980550095014727_5176980550095014727" end_link="5176980550095014727_5176980550095014727" trav_time="00:00:01" distance="0.0"></route>
                        </leg>
                        <activity type="pt interaction" link="5176980550095014727_5176980550095014727" x="586506.0123527828" y="246566.50039572024" max_dur="00:00:00" >
                        </activity>
                        <leg mode="bus" dep_time="10:28:30" trav_time="02:10:30">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="default_pt" start_link="5176980550095014727_5176980550095014727" end_link="5176981723945701739_5176981723945701739" trav_time="02:10:30" distance="8211.69615332553">{"transitRouteId":"16053_1","boardingTime":"12:27:20","transitLineId":"16053","accessFacilityId":"390010157","egressFacilityId":"390G10229"}</route>
                        </leg>
                        <activity type="pt interaction" link="5176981723945701739_5176981723945701739" x="587484.1355733429" y="241231.54179517739" max_dur="00:00:00" >
                        </activity>
                        <leg mode="walk" dep_time="12:39:00" trav_time="00:00:00">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="generic" start_link="5176981723945701739_5176981723945701739" end_link="5176981723945701739_5176981723945701739" trav_time="00:00:00" distance="0.0"></route>
                        </leg>
                        <activity type="pt interaction" link="5176981723945701739_5176981723945701739" x="587484.1355733429" y="241231.54179517739" max_dur="00:00:00" >
                        </activity>
                        <leg mode="bus" dep_time="12:39:01" trav_time="01:20:59">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="default_pt" start_link="5176981723945701739_5176981723945701739" end_link="5176971627666261419_5176971627666261419" trav_time="01:20:59" distance="38008.476906595104">{"transitRouteId":"31289_0","boardingTime":"13:00:00","transitLineId":"31289","accessFacilityId":"390G10229","egressFacilityId":"390050876"}</route>
                        </leg>
                        <activity type="pt interaction" link="5176971627666261419_5176971627666261419" x="585175.9298012826" y="264316.01510483475" max_dur="00:00:00" >
                        </activity>
                        <leg mode="walk" dep_time="14:00:00" trav_time="00:12:42">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="generic" start_link="5176971627666261419_5176971627666261419" end_link="5176971725613457087_5176971725613457087" trav_time="00:12:42" distance="635.0"></route>
                        </leg>
                        <activity type="pt interaction" link="5176971725613457087_5176971725613457087" x="585651.6538349463" y="264352.95547463984" max_dur="00:00:00" >
                        </activity>
                        <leg mode="walk" dep_time="14:12:42" trav_time="00:09:18">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="generic" start_link="5176971725613457087_5176971725613457087" end_link="5176971741277727863_5176971741659784981" trav_time="00:09:18" distance="465.2266123071701"></route>
                        </leg>
                        <activity type="shop" link="5176971741277727863_5176971741659784981" facility="f_auto_3052" x="585973.0" y="264540.0" start_time="10:44:00" end_time="11:02:00" >
                        </activity>
                        <leg mode="walk" dep_time="11:02:00" trav_time="00:47:18">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="generic" start_link="5176971741277727863_5176971741659784981" end_link="5176973176231468937_5176973176244935583" trav_time="00:47:18" distance="2365.391707519074"></route>
                        </leg>
                        <activity type="other" link="5176973176231468937_5176973176244935583" facility="f_auto_3640" x="585797.0" y="262729.0" start_time="11:07:00" end_time="14:48:00" >
                        </activity>
                        <leg mode="car" dep_time="14:48:00" trav_time="00:06:12">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="links" start_link="5176973176231468937_5176973176244935583" end_link="351882" trav_time="00:06:12" distance="3980.709925552379" vehicleRefId="null">5176973176231468937_5176973176244935583 5176973176244935583_5176973176231468937 538801 390015 96704 218118 5176971693109358891_5176971693062169271 549169 498886 238528 82506 314604 434532 357034 5176971673831087679_5176971673816753561 5176971673816753561_5176971673914680415 5176971673914680415_5176971673941725167 5176971673941725167_5176971674106668353 5176971674106668353_5176971674099643953 91944 282508 237622 5176971675681325729_5176971675678624033 68821 5176971675704033793_5176971675706327425 262062 5176971636658826097_5176971636658048039 23299 617290 5176971639046934039_5176971639061442719 570810 325784 301791 253004 444992 5176971614713702391_5176971614736160173 5176971614736160173_5176971614754731811 5176971614754731811_5176971614088669701 175293 616957 634050 154479 5176971619607718013_5176971619616954679 571773 512385 93226 5176971620373952531_5176971620488895985 5176971620488895985_5176971620530051299 5176971620530051299_5176971621169311015 85127 355092 485209 77717 5176971620856238905_5176971781650859901 246813 351882</route>
                        </leg>
                        <activity type="pt interaction" link="351882" x="584873.589101083" y="265219.2209167288" max_dur="00:00:00" >
                        </activity>
                        <leg mode="walk" dep_time="14:54:12" trav_time="00:00:00">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="generic" start_link="351882" end_link="5176971782047279209_5176971782047279209" trav_time="00:00:00" distance="0.0"></route>
                        </leg>
                        <activity type="pt interaction" link="5176971782047279209_5176971782047279209" x="585301.8659321591" y="265201.775893673" max_dur="00:00:00" >
                        </activity>
                        <leg mode="walk" dep_time="14:54:12" trav_time="00:04:35">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="generic" start_link="5176971782047279209_5176971782047279209" end_link="5176971785393766433_5176971785393766433" trav_time="00:04:35" distance="230.0"></route>
                        </leg>
                        <activity type="pt interaction" link="5176971785393766433_5176971785393766433" x="585296.908028936" y="265377.86864908243" max_dur="00:00:00" >
                        </activity>
                        <leg mode="bus" dep_time="14:58:47" trav_time="00:29:13">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="default_pt" start_link="5176971785393766433_5176971785393766433" end_link="5176971612828285077_5176971612828285077" trav_time="00:29:13" distance="7158.785775111365">{"transitRouteId":"6757_0","boardingTime":"15:04:40","transitLineId":"6757","accessFacilityId":"390050806","egressFacilityId":"390050871"}</route>
                        </leg>
                        <activity type="pt interaction" link="5176971612828285077_5176971612828285077" x="584776.9502975785" y="264502.8709783093" max_dur="00:00:00" >
                        </activity>
                        <leg mode="walk" dep_time="15:28:00" trav_time="00:11:30">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="generic" start_link="5176971612828285077_5176971612828285077" end_link="5176971610368624159_5176971610368624159" trav_time="00:11:30" distance="579.0"></route>
                        </leg>
                        <activity type="pt interaction" link="5176971610368624159_5176971610368624159" x="584794.9089341155" y="264947.84872200113" max_dur="00:00:00" >
                        </activity>
                        <leg mode="bus" dep_time="15:39:35" trav_time="00:45:23">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="default_pt" start_link="5176971610368624159_5176971610368624159" end_link="5176980722984262763_5176980722984262763" trav_time="00:45:23" distance="22416.49197484107">{"transitRouteId":"31013_0","boardingTime":"15:45:00","transitLineId":"31013","accessFacilityId":"390051024","egressFacilityId":"390010648"}</route>
                        </leg>
                        <activity type="pt interaction" link="5176980722984262763_5176980722984262763" x="586790.023249241" y="246960.76919718873" max_dur="00:00:00" >
                        </activity>
                        <leg mode="walk" dep_time="16:24:58" trav_time="00:12:39">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="generic" start_link="5176980722984262763_5176980722984262763" end_link="5176980550095014727_5176980550095014727" trav_time="00:12:39" distance="632.0"></route>
                        </leg>
                        <activity type="pt interaction" link="5176980550095014727_5176980550095014727" x="586510.0529568413" y="246557.85261155193" max_dur="00:00:00" >
                        </activity>
                        <leg mode="walk" dep_time="16:37:37" trav_time="00:00:29">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">rail</attribute>
                                </attributes>
                                <route type="generic" start_link="5176980550095014727_5176980550095014727" end_link="5176980547372196967_5176980550095014727" trav_time="00:00:29" distance="24.508945955231585"></route>
                        </leg>
                        <activity type="home" link="5176980547372196967_5176980550095014727" facility="f_auto_19125" x="586499.0" y="246549.0" start_time="15:15:00" end_time="24:00:00" >
                        </activity>
                </plan>

        </person>
```

```
Error routing plan for agent 108953
The problematic plan is null
The problematic plan is [score=undefined][nof_acts_legs=5][type=null][personId=108953]
```
```xml
      <person id="108953">
                <attributes>
                        <attribute name="age" class="java.lang.Integer">34</attribute>
                        <attribute name="age_group" class="java.lang.String">30 to 39</attribute>
                        <attribute name="area_type" class="java.lang.String">urban</attribute>
                        <attribute name="carAvail" class="java.lang.Boolean">true</attribute>
                        <attribute name="car_avail" class="java.lang.String">yes</attribute>
                        <attribute name="car_competition" class="java.lang.Double">2.0</attribute>
                        <attribute name="disabled" class="java.lang.String">no</attribute>
                        <attribute name="ev" class="java.lang.Boolean">false</attribute>
                        <attribute name="gender" class="java.lang.String">male</attribute>
                        <attribute name="hasBike" class="java.lang.Boolean">true</attribute>
                        <attribute name="hasCar" class="java.lang.Boolean">true</attribute>
                        <attribute name="hasDisability" class="java.lang.Boolean">false</attribute>
                        <attribute name="hasLicence" class="java.lang.Boolean">true</attribute>
                        <attribute name="hcounty" class="java.lang.String">Suffolk</attribute>
                        <attribute name="hhincome" class="java.lang.String">high</attribute>
                        <attribute name="hholdnumchildren" class="java.lang.Integer">0</attribute>
                        <attribute name="hhsize" class="java.lang.Integer">2</attribute>
                        <attribute name="hid" class="java.lang.String">108953</attribute>
                        <attribute name="hid_old" class="java.lang.String">2007001330_2</attribute>
                        <attribute name="householdid" class="java.lang.Integer">2007001330</attribute>
                        <attribute name="hzone" class="java.lang.String">E02006277</attribute>
                        <attribute name="indincome" class="java.lang.String">medium</attribute>
                        <attribute name="individualid" class="java.lang.Integer">2007003176</attribute>
                        <attribute name="intermodalBike" class="java.lang.String">yes</attribute>
                        <attribute name="intermodalCar" class="java.lang.String">yes</attribute>
                        <attribute name="intermodalPassenger" class="java.lang.String">no</attribute>
                        <attribute name="intermodalTaxi" class="java.lang.String">yes</attribute>
                        <attribute name="marital_status" class="java.lang.String">married</attribute>
                        <attribute name="region" class="java.lang.String">East of England</attribute>
                        <attribute name="seasontickettype" class="java.lang.String">season_ticket</attribute>
                        <attribute name="sex" class="java.lang.String">m</attribute>
                        <attribute name="subpopulation" class="java.lang.String">high</attribute>
                        <attribute name="surveyyear" class="java.lang.Integer">2007</attribute>
                        <attribute name="vehicles" class="org.matsim.vehicles.PersonVehicles">{"car":"108953","taxi":"108953_taxi","car_passenger":"108953_car_passenger"}</attribute>
                        <attribute name="workstatus" class="java.lang.String">full-time</attribute>
                </attributes>
                <plan score="219.30483944025386" selected="no">
                        <activity type="home" link="5176972412007611595_5176972412159172221" facility="f_auto_3596" x="587058.0" y="263703.0" start_time="00:00:00" end_time="07:50:00" >
                        </activity>
                        <leg mode="car" dep_time="07:50:00" trav_time="00:00:43">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">car</attribute>
                                </attributes>
                                <route type="links" start_link="5176972412007611595_5176972412159172221" end_link="5176972322351857303_5176972322891286597" trav_time="00:0
0:43" distance="670.9840580289804" vehicleRefId="null">5176972412007611595_5176972412159172221 5176972412159172221_5176972412007611595 485623 5176972319128107751_517697231
8470344371 5176972318470344371_5176972318424877323 5176972318424877323_5176972318401530301 5176972318401530301_5176972320061073363 5176972320061073363_5176972320062781433
5176972320062781433_5176972320671039309 33835 256889 5176972322351857303_5176972322891286597</route>
                        </leg>
                        <activity type="work" link="5176972322351857303_5176972322891286597" facility="f_auto_1337" x="587580.0" y="263932.0" start_time="09:15:00" end_tim
e="17:30:00" >
                        </activity>
                        <leg mode="car" dep_time="17:30:00" trav_time="00:00:47">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">car</attribute>
                                </attributes>
                                <route type="links" start_link="5176972322351857303_5176972322891286597" end_link="5176972412007611595_5176972412159172221" trav_time="00:0
0:47" distance="670.9840570394069" vehicleRefId="null">5176972322351857303_5176972322891286597 5176972322891286597_5176972322351857303 412948 313200 5176972320671039309_51
76972320062781433 5176972320062781433_5176972320061073363 5176972320061073363_5176972318470344371 5176972318470344371_5176972319128107751 624634 5176972412007611595_517697
2412159172221</route>
                        </leg>
                        <activity type="home" link="5176972412007611595_5176972412159172221" facility="f_auto_3596" x="587058.0" y="263703.0" start_time="19:00:00" end_tim
e="24:00:00" >
                        </activity>
                </plan>

                <plan score="223.92776380502244" selected="yes">
                        <activity type="home" link="5176972412007611595_5176972412159172221" facility="f_auto_3596" x="587058.0" y="263703.0" start_time="00:00:00" end_tim
e="07:50:00" >
                        </activity>
                        <leg mode="walk" dep_time="07:50:00" trav_time="00:14:49">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">tram</attribute>
                                </attributes>
                                <route type="generic" start_link="5176972412007611595_5176972412159172221" end_link="5176972322351857303_5176972322891286597" trav_time="00:14:49" distance="741.0285082235365"></route>
                        </leg>
                        <activity type="work" link="5176972322351857303_5176972322891286597" facility="f_auto_1337" x="587580.0" y="263932.0" start_time="09:15:00" end_time="17:30:00" >
                        </activity>
                        <leg mode="walk" dep_time="17:30:00" trav_time="00:14:49">
                                <attributes>
                                        <attribute name="routingMode" class="java.lang.String">tram</attribute>
                                </attributes>
                                <route type="generic" start_link="5176972322351857303_5176972322891286597" end_link="5176972412007611595_5176972412159172221" trav_time="00:14:49" distance="741.0285082235365"></route>
                        </leg>
                        <activity type="home" link="5176972412007611595_5176972412159172221" facility="f_auto_3596" x="587058.0" y="263703.0" start_time="19:00:00" end_time="24:00:00" >
                        </activity>
                </plan>

        </person>
```


- Is this relevant?
```
2024-08-19T21:14:37,009  WARN PreProcessEuclidean:60 There are links with stored length smaller than their Euclidean distance in this network. Thus, A* cannot guarantee to calculate the least-cost paths between two nodes.
```

- Amended the cutter to catch all plan routing errors, continue through all plans, then summarise the failed plans and fail.
There are multiple worker threads, and each one has some problematic plans:
```
!!! 23 plans had errors (there were 105 good plans)

[score=undefined][nof_acts_legs=19][type=null][personId=101246]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=7][type=null][personId=102136]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=7][type=null][personId=103320]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=9][type=null][personId=103157]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=11][type=null][personId=100417]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=3][type=null][personId=102035]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=5][type=null][personId=100416]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=7][type=null][personId=102646]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=5][type=null][personId=103317]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=3][type=null][personId=102552]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=7][type=null][personId=100477]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=7][type=null][personId=102646]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=3][type=null][personId=10077]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=7][type=null][personId=101681]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=5][type=null][personId=101037]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=15][type=null][personId=101244]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=7][type=null][personId=100818]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=11][type=null][personId=10081]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=11][type=null][personId=100417]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=3][type=null][personId=102880]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=7][type=null][personId=1025]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=3][type=null][personId=101681]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=5][type=null][personId=100416]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null
``` 

```
!!! 15 plans had errors (there were 113 good plans)

[score=undefined][nof_acts_legs=5][type=null][personId=105162]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=7][type=null][personId=103660]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=7][type=null][personId=105566]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=3][type=null][personId=105971]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=3][type=null][personId=104300]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=7][type=null][personId=10393]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=9][type=null][personId=104992]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=3][type=null][personId=106586]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=7][type=null][personId=103660]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=5][type=null][personId=104409]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=15][type=null][personId=104511]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=5][type=null][personId=105120]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=9][type=null][personId=10624]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=9][type=null][personId=10624]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=7][type=null][personId=105077]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null
```

```
!!! 19 plans had errors (there were 112 good plans)

[score=undefined][nof_acts_legs=19][type=null][personId=108072]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=3][type=null][personId=108130]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=5][type=null][personId=107669]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=3][type=null][personId=108017]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=7][type=null][personId=107285]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=7][type=null][personId=106656]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=3][type=null][personId=106659]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=7][type=null][personId=107135]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=5][type=null][personId=108172]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=3][type=null][personId=107796]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=5][type=null][personId=108184]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=3][type=null][personId=108319]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=7][type=null][personId=107346]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=3][type=null][personId=107421]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=3][type=null][personId=107301]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=5][type=null][personId=108169]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=3][type=null][personId=107285]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=5][type=null][personId=107352]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=3][type=null][personId=106658]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null
```

```
!!! 25 plans had errors (there were 114 good plans)

[score=undefined][nof_acts_legs=3][type=null][personId=110891]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=11][type=null][personId=111003]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=5][type=null][personId=109482]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=3][type=null][personId=109473]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=5][type=null][personId=108953]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=9][type=null][personId=110123]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=3][type=null][personId=111002]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=3][type=null][personId=109010]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=5][type=null][personId=109472]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=7][type=null][personId=109094]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=3][type=null][personId=109335]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=11][type=null][personId=109145]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=3][type=null][personId=109335]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=3][type=null][personId=110587]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=3][type=null][personId=109682]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=7][type=null][personId=109009]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=9][type=null][personId=110122]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=11][type=null][personId=111003]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=3][type=null][personId=110587]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=5][type=null][personId=109482]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=9][type=null][personId=110122]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=7][type=null][personId=109336]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=3][type=null][personId=109473]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=5][type=null][personId=109472]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null,
[score=undefined][nof_acts_legs=3][type=null][personId=110603]=java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null
```

Michael also caught all the exceptions from routing and the validator, but print the validator errors.

```
2024-08-27T15:47:49,855 ERROR ScenarioValidator:140 Person 99747 has car leg without a route
2024-08-27T15:47:49,855 ERROR ScenarioValidator:140 Person 99773 has rail leg without a route
2024-08-27T15:47:49,855 ERROR ScenarioValidator:140 Person 99800 has walk leg without a route
2024-08-27T15:47:49,855 ERROR ScenarioValidator:140 Person 99852 has walk leg without a route
2024-08-27T15:47:49,855 ERROR ScenarioValidator:140 Person 99852 has walk leg without a route
2024-08-27T15:47:49,855 ERROR ScenarioValidator:140 Person 99852 has outside leg without a route
2024-08-27T15:47:49,855 ERROR ScenarioValidator:140 Person 99852 has walk leg without a route
2024-08-27T15:47:49,855 ERROR ScenarioValidator:140 Person 99852 has walk leg without a route
2024-08-27T15:47:49,855 ERROR ScenarioValidator:140 Person 99852 has walk leg without a route
2024-08-27T15:47:49,855 ERROR ScenarioValidator:140 Person 99852 has walk leg without a route
2024-08-27T15:47:49,855 ERROR ScenarioValidator:140 Person 99852 has outside leg without a route
2024-08-27T15:47:49,855 ERROR ScenarioValidator:140 Person 99852 has walk leg without a route
2024-08-27T15:47:49,855 ERROR ScenarioValidator:140 Person 99852 has walk leg without a route
```
The error occurred because some leg of an agent’s trip do not have a valid route according to [validation code](https://github.com/eqasim-org/eqasim-java/blob/develop/core/src/main/java/org/eqasim/core/scenario/validation/ScenarioValidator.java#L139C7-L140).  So simulation could not determine how those agents were supposed to travel between activities for those legs.

Potential Causes of the Errors:
- Incorrect plans or misconfigured modes: Some agents’ legs appear “incorrect” after the population is trimmed.

- Missing or misconfigured networks: The network somehow disconnected or for a particular mode (car, rail, walk, etc.) might be missing.

- Incorrect transit schedule: There may be issues with the transit schedule input.


## Test the simulations using cutter outputs
Michael helped to worte cutter entire scenario outputs and the outputs can be found on TE EFS at `/mnt/efs/analysis/ys/matsim_cutter/te-10pc-outputs-ignoring-errors`.

When I tested the scenarios by rerunning the simulations with cut outputs based on the following command 

The command to run the simulation: 
```
java -cp target/columbus-2.1.0-jar-with-dependencies.jar com.arup.cm.RunArupMatsim /mnt/efs/analysis/ys/matsim_cutter/te-10pc-outputs-ignoring-errors/matsim_config_TE_cutting_validation_with_intermodal.xml
``` 

It failed with similar errors: 
```
024-08-28T14:29:28,700 ERROR ParallelPersonAlgorithmUtils$ExceptionHandler:164 Thread PersonPrepareForSim.0 died with exception while handling events.
java.lang.NullPointerException: Cannot invoke "org.matsim.pt.transitSchedule.api.TransitStopFacility.getCoord()" because "nearestStop" is null
        at ch.sbb.matsim.routing.pt.raptor.DefaultRaptorStopFinder.addInitialStopsForParamSet(DefaultRaptorStopFinder.java:175) ~[columbus-2.1.0-jar-with-dependencies.jar:2.1.0]
        at ch.sbb.matsim.routing.pt.raptor.DefaultRaptorStopFinder.findIntermodalStops(DefaultRaptorStopFinder.java:131) ~[columbus-2.1.0-jar-with-dependencies.jar:2.1.0]
        at ch.sbb.matsim.routing.pt.raptor.DefaultRaptorStopFinder.findAccessStops(DefaultRaptorStopFinder.java:83) ~[columbus-2.1.0-jar-with-dependencies.jar:2.1.0]
        at ch.sbb.matsim.routing.pt.raptor.DefaultRaptorStopFinder.findStops(DefaultRaptorStopFinder.java:71) ~[columbus-2.1.0-jar-with-dependencies.jar:2.1.0]
        at ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor.findAccessStops(SwissRailRaptor.java:252) ~[columbus-2.1.0-jar-with-dependencies.jar:2.1.0]
        at ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor.calcRoute(SwissRailRaptor.java:74) ~[columbus-2.1.0-jar-with-dependencies.jar:2.1.0]
        at ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorRoutingModule.calcRoute(SwissRailRaptorRoutingModule.java:58) ~[columbus-2.1.0-jar-with-dependencies.jar:2.1.0]
        at org.matsim.core.router.TripRouter.calcRoute(TripRouter.java:182) ~[columbus-2.1.0-jar-with-dependencies.jar:2.1.0]
        at org.matsim.core.router.PlanRouter.run(PlanRouter.java:101) ~[columbus-2.1.0-jar-with-dependencies.jar:2.1.0]
        at org.matsim.core.population.algorithms.PersonPrepareForSim.run(PersonPrepareForSim.java:219) ~[columbus-2.1.0-jar-with-dependencies.jar:2.1.0]
        at org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils$PersonAlgoThread.run(ParallelPersonAlgorithmUtils.java:145) ~[columbus-2.1.0-jar-with-dependencies.jar:2.1.0]
        at java.lang.Thread.run(Thread.java:840) ~[?:?]
```

When I did the spot check for the `agent 99852` in the cut population, I found in the plan activities,  the Leg looks a bit weird.

Here is the part of content for the leg, do you know should the legs normally have `routingMode` and `trav_time` in the leg attributes or it’s optional?

```
<plan selected="yes">
        <activity type="outside" link="217523" facility="outside_107" x="581674.3013465618" y="266268.765120827" end_time="12:54:02" >
        </activity>
        <leg mode="walk">
        </leg>
        <activity type="education" link="5176971604409504091_5176971605843912959" facility="f_auto_833" x="584455.0" y="264501.0" start_time="07:40:00" end_time="12:30:00" >
        </activity>
```

I did a simple test with trimmed agent plans, I only left the top 5 agents in the cut `TE_cutter_population.xml` which has more “complete” plans with `routingMode `attribute in their legs. The test simulation can successfully run to end.


I did another two tests to run the sims with adding  `agent 99852`  plans into the 5 agents test population input.

When the `agent 99852` has "weird" plans without `routingMode` between legs (The original plans we got from the cutting process), the test simulation failed with NEP error.

```xml
<plan selected="yes">
        <activity type="outside" link="217523" facility="outside_107" x="581674.3013465618" y="266268.765120827" end_time="12:54:02" >
        </activity>
        <leg mode="walk">
        </leg>
        <activity type="education" link="5176971604409504091_5176971605843912959" facility="f_auto_833" x="584455.0" y="264501.0" start_time="07:40:00" end_time="12:30:00" >
        </activity>
        <leg mode="walk">
        </leg>
        <activity type="outside" link="217523" facility="outside_107" x="581674.3013465618" y="266268.765120827" end_time="13:15:00" >
        </activity>
        <leg mode="outside">
        </leg>
        <activity type="outside" link="217523" facility="outside_107" x="581674.3013465618" y="266268.765120827" end_time="18:49:02" >
        </activity>
        <leg mode="walk">
        </leg>
        <activity type="education" link="5176971604409504091_5176971605843912959" facility="f_auto_833" x="584455.0" y="264501.0" start_time="13:30:00" end_time="14:20:00" >
        </activity>
        <leg mode="walk">
        </leg>
        <activity type="outside" link="217523" facility="outside_107" x="581674.3013465618" y="266268.765120827" >
        </activity>
</plan>
```

When I manually added the `routingMode` into the agents' plans, 

```xml
<attributes>
	<attribute name="routingMode" class="java.lang.String">walk</attribute>
</attributes>
```
between the legs into the plans, the test simulation can run to the end.


The original plans of the `agent 99852` from the population,
```xml
<plan score="-574.0437376231156" selected="yes">
        <activity type="home" link="66637" facility="f_auto_5689" x="571705.0" y="273772.0" start_time="00:00:00" end_time="07:26:12" >
        </activity>
        <leg mode="walk" dep_time="07:26:12" trav_time="06:49:52">
                <attributes>
                        <attribute name="routingMode" class="java.lang.String">walk</attribute>
                </attributes>
                <route type="generic" start_link="66637" end_link="5176971604409504091_5176971605843912959" trav_time="06:49:52" distance="20493.62242967309"></route>
        </leg>
        <activity type="education" link="5176971604409504091_5176971605843912959" facility="f_auto_833" x="584455.0" y="264501.0" start_time="07:40:00" end_time="12:14:05" >
        </activity>
        <leg mode="walk" dep_time="12:14:05" trav_time="06:49:52">
                <attributes>
                        <attribute name="routingMode" class="java.lang.String">walk</attribute>
                </attributes>
                <route type="generic" start_link="5176971604409504091_5176971605843912959" end_link="66637" trav_time="06:49:52" distance="20493.62242967309"></route>
        </leg>
        <activity type="home" link="66637" facility="f_auto_5689" x="571705.0" y="273772.0" start_time="12:45:00" end_time="12:59:27" >
        </activity>
        <leg mode="walk" dep_time="12:59:27" trav_time="06:49:52">
                <attributes>
                        <attribute name="routingMode" class="java.lang.String">walk</attribute>
                </attributes>
                <route type="generic" start_link="66637" end_link="5176971604409504091_5176971605843912959" trav_time="06:49:52" distance="20493.62242967309"></route>
        </leg>
        <activity type="education" link="5176971604409504091_5176971605843912959" facility="f_auto_833" x="584455.0" y="264501.0" start_time="13:30:00" end_time="14:31:16" >
        </activity>
        <leg mode="walk" dep_time="14:31:16" trav_time="06:49:52">
                <attributes>
                        <attribute name="routingMode" class="java.lang.String">walk</attribute>
                </attributes>
                <route type="generic" start_link="5176971604409504091_5176971605843912959" end_link="66637" trav_time="06:49:52" distance="20493.62242967309"></route>
                                        </leg>
        <activity type="home" link="66637" facility="f_auto_5689" x="571705.0" y="273772.0" start_time="14:40:00" end_time="23:45:58" >
        </activity>
</plan>

</person>
```
It seems cutter somehow didn’t deal with `agent 99852` and other agents correctly.


### Validate the connectivity of the network
I have used the `networkX` package to validate connectivity based on the script at `/mnt/efs/analysis/ys/matsim_cutter/network_validation.py`. The network doesn’t have any dead end nodes or isolated node and some nodes are disconnected.



### Checking the output trainsit schedule
For the cutter output `transit_schedule.xml` at 
`/mnt/efs/analysis/ys/matsim_cutter/te-10pc-outputs-ignoring-errors/TE_cutter_transit_schedule.xml`, it seems all of the stop facilities have been updated the `bikeAccessible` and `carAccessible` attribute which maybe not the reason induce the error but incorrectly.

```xml
<stopFacility id="390GBURY1" x="585132.9173118277" y="264535.6685015094" linkRefId="5176971626509406753_5176971626509406753" name="Bury St Edmunds" isBlocking="false">
                <attributes>
                        <attribute name="bikeAccessible" class="java.lang.String">True</attribute>
                        <attribute name="carAccessible" class="java.lang.String">True</attribute>
                </attributes>

        </stopFacility>
```
Before the cutting, the stop id `390GBURY1` which could not be accessible for bike and car since it doesn’t have these attributes:
```xml
<stopFacility id="390GBURY1" x="585132.9173118277" y="264535.6685015094" linkRefId="5176971626509406753_5176971626509406753" name="Bury St Edmunds" isBlocking="false"/>
```

### Progress of debugging the missing `walk` mode parameters when rerun the simulations with post cutting inputs.

When I validate the simulation by rerunning using the inputs after cutting, the cutter works without intermodal setting in the matsim config for 0.01% simulation inputs.

It failed with the following error when tried the same process for 10% .

```
2024-08-06T15:30:33,240 ERROR ParallelEventsManager$ExceptionHandler:414 Thread SingleHandlerEventsManager: class org.matsim.core.scoring.ScoringFunctionsForPopulation died with exception while handling events.
java.lang.RuntimeException: just encountered mode for which no scoring parameters are defined: walk
	at org.matsim.core.scoring.functions.CharyparNagelLegScoring.calcLegScore(CharyparNagelLegScoring.java:113) ~[columbus-2.1.0-jar-with-dependencies.jar:2.1.0]
	at org.matsim.core.scoring.functions.CharyparNagelLegScoring.handleLeg(CharyparNagelLegScoring.java:191) ~[columbus-2.1.0-jar-with-dependencies.jar:2.1.0]
	at org.matsim.core.scoring.SumScoringFunction.handleLeg(SumScoringFunction.java:114) ~[columbus-2.1.0-jar-with-dependencies.jar:2.1.0]
	at org.matsim.core.scoring.ScoringFunctionsForPopulation.handleLeg(ScoringFunctionsForPopulation.java:226) ~[columbus-2.1.0-jar-with-dependencies.jar:2.1.0]
	at org.matsim.core.scoring.EventsToLegs.handleEvent(EventsToLegs.java:353) ~[columbus-2.1.0-jar-with-dependencies.jar:2.1.0]
	at org.matsim.core.scoring.ScoringFunctionsForPopulation.handleEvent(ScoringFunctionsForPopulation.java:176) ~[columbus-2.1.0-jar-with-dependencies.jar:2.1.0]
	at org.matsim.core.events.SingleHandlerEventsManager.callHandlerFast(SingleHandlerEventsManager.java:298) ~[columbus-2.1.0-jar-with-dependencies.jar:2.1.0]
	at org.matsim.core.events.SingleHandlerEventsManager.computeEvent(SingleHandlerEventsManager.java:229) ~[columbus-2.1.0-jar-with-dependencies.jar:2.1.0]
	at org.matsim.core.events.SingleHandlerEventsManager.processEvent(SingleHandlerEventsManager.java:182) ~[columbus-2.1.0-jar-with-dependencies.jar:2.1.0]
	at org.matsim.core.events.ParallelEventsManager$ProcessEventsRunnable.run(ParallelEventsManager.java:381) ~[columbus-2.1.0-jar-with-dependencies.jar:2.1.0]
```

The errors pointed a transport mode walk in the population plans that hasn't been defined in the scoring parameters of the planCalcScore module in the MATSim config([code](https://github.com/matsim-org/matsim-libs/blob/master/matsim/src/main/java/org/matsim/core/scoring/functions/CharyparNagelLegScoring.java#L131)).

I have checked the matsim config scoring module, the walk mode parameters have been added to each supopulation expect the freight subpopulation, e.g lgv, hgv which only requires the car mode for delivery.

When I added the `walk` mode parameter for the freight to rerun, it threw another error:

```
2024-09-09T13:56:28,336  INFO Realm:325 Hermes running at 09:00:00
2024-09-09T13:56:28,378  INFO Realm:325 Hermes running at 10:00:00
2024-09-09T13:56:28,365 ERROR ParallelEventsManager$ExceptionHandler:414 Thread SingleHandlerEventsManager: class org.matsim.core.scoring.ScoringFunctionsForPopulation died with exception while handling events.
java.lang.NullPointerException: Cannot read field "marginalUtilityOfTraveling_s" because the return value of "java.util.Map.get(Object)" is null
	at org.matsim.core.scoring.functions.CharyparNagelLegScoring.handleEvent(CharyparNagelLegScoring.java:168) ~[columbus-2.1.0-jar-with-dependencies.jar:2.1.0]
	at org.matsim.core.scoring.SumScoringFunction.handleEvent(SumScoringFunction.java:149) ~[columbus-2.1.0-jar-with-dependencies.jar:2.1.0]
	at org.matsim.core.scoring.ScoringFunctionsForPopulation.handleEvent(ScoringFunctionsForPopulation.java:135) ~[columbus-2.1.0-jar-with-dependencies.jar:2.1.0]
	at org.matsim.core.events.SingleHandlerEventsManager.callHandlerFast(SingleHandlerEventsManager.java:298) ~[columbus-2.1.0-jar-with-dependencies.jar:2.1.0]
	at org.matsim.core.events.SingleHandlerEventsManager.computeEvent(SingleHandlerEventsManager.java:229) ~[columbus-2.1.0-jar-with-dependencies.jar:2.1.0]
	at org.matsim.core.events.SingleHandlerEventsManager.processEvent(SingleHandlerEventsManager.java:182) ~[columbus-2.1.0-jar-with-dependencies.jar:2.1.0]
	at org.matsim.core.events.ParallelEventsManager$ProcessEventsRunnable.run(ParallelEventsManager.java:381) ~[columbus-2.1.0-jar-with-dependencies.jar:2.1.0]

```

I have checked the freight subpopulation and it only used `car` leg.

The current solution is to remove freight population, the simulations using post-cutting inputs can successfully rerun to the end.

More investigation can be potential carried out on why the error induced in the freight subpopulation. 

