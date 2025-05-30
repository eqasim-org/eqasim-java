# Changelog

## [2.0.0](https://github.com/eqasim-org/eqasim-java/compare/v1.5.0...v2.0.0) (2025-05-08)


### ⚠ BREAKING CHANGES

* scenarios without vehicles will not work anymore

### Features

* [FeederDrt] a more flexible structure for access and egress stop selection ([#258](https://github.com/eqasim-org/eqasim-java/issues/258)) ([83a53fe](https://github.com/eqasim-org/eqasim-java/commit/83a53fe7e947312e6045574427343e0dea6c7031))
* a new scenario cutter without mode choice limitations ([#251](https://github.com/eqasim-org/eqasim-java/issues/251)) ([47417e7](https://github.com/eqasim-org/eqasim-java/commit/47417e77feaee1c4bedd844bfda5ded7de00c25f))
* ability to restrict VDF updates to a shapefile area ([#247](https://github.com/eqasim-org/eqasim-java/issues/247)) ([f3609f0](https://github.com/eqasim-org/eqasim-java/commit/f3609f06d09ce7ea4d18c44f1881c251605155b1))
* add activity analysis ([#260](https://github.com/eqasim-org/eqasim-java/issues/260)) ([f527a86](https://github.com/eqasim-org/eqasim-java/commit/f527a866410398b7bce5446664f5c8c45d6315ab))
* add configurable policies ([#254](https://github.com/eqasim-org/eqasim-java/issues/254)) ([b338bf8](https://github.com/eqasim-org/eqasim-java/commit/b338bf80a303914a93efbb6028ed3f6e25f91f1c))
* add trip and leg inex for stop-to-stop traversal analysis ([#306](https://github.com/eqasim-org/eqasim-java/issues/306)) ([bd7c6c5](https://github.com/eqasim-org/eqasim-java/commit/bd7c6c59d559b7b5b4e3732baa8d2f0f3c57387e))
* adding a sampler for simulated PT vehicle size in switzerland ([#354](https://github.com/eqasim-org/eqasim-java/issues/354)) ([d769d31](https://github.com/eqasim-org/eqasim-java/commit/d769d31d6008e38b770aa8ea48e44a8501abd60a))
* adding the script to run switzerland with vdf ([#319](https://github.com/eqasim-org/eqasim-java/issues/319)) ([8d60d8c](https://github.com/eqasim-org/eqasim-java/commit/8d60d8cab82e1d63a0b7fd518a9241dbb11c614a))
* Adding vehicles to swiss config ([#318](https://github.com/eqasim-org/eqasim-java/issues/318)) ([835e2bb](https://github.com/eqasim-org/eqasim-java/commit/835e2bbe3d21c31eb8dbfc6128545a285abc1eab))
* allowing to create vehicles only in service area ([#243](https://github.com/eqasim-org/eqasim-java/issues/243)) ([3a8b9e7](https://github.com/eqasim-org/eqasim-java/commit/3a8b9e7616623d2f1c58a79a56b6f0c939ec024a))
* analyse stop-to-stop traversals ([#299](https://github.com/eqasim-org/eqasim-java/issues/299)) ([96059cc](https://github.com/eqasim-org/eqasim-java/commit/96059ccf830b0216e34d397bb20f041eac98dc8b))
* compatible with un-mapped transit schedules ([#275](https://github.com/eqasim-org/eqasim-java/issues/275)) ([8dc3328](https://github.com/eqasim-org/eqasim-java/commit/8dc3328f42b3a58dafea0c6dac86d3136d87572e))
* consider leg mode for traversals ([#263](https://github.com/eqasim-org/eqasim-java/issues/263)) ([a9324c7](https://github.com/eqasim-org/eqasim-java/commit/a9324c75bc241bcf22f85d059a3fef7d2fad340c))
* define scenario extent in gpkg for cutting ([#255](https://github.com/eqasim-org/eqasim-java/issues/255)) ([ea96dd7](https://github.com/eqasim-org/eqasim-java/commit/ea96dd701e99e4d28e32018cd9207fcea233bc7a))
* departureId in PublicTransitEvent and in eqasim_pt.csv ([#279](https://github.com/eqasim-org/eqasim-java/issues/279)) ([60520f1](https://github.com/eqasim-org/eqasim-java/commit/60520f109cb0c60a2433b4e3554e3e46a72ecc8b))
* DrtServiceAreaConstraint ([#287](https://github.com/eqasim-org/eqasim-java/issues/287)) ([38a4c7a](https://github.com/eqasim-org/eqasim-java/commit/38a4c7ac93d06cd911890677977ab955d74bcdb1))
* EditConfig can now add and remove parameter sets ([#320](https://github.com/eqasim-org/eqasim-java/issues/320)) ([90a737d](https://github.com/eqasim-org/eqasim-java/commit/90a737dce5adac26843d797fbcb04a45b23aaf1a))
* EditConfig script ([#286](https://github.com/eqasim-org/eqasim-java/issues/286)) ([c7a9346](https://github.com/eqasim-org/eqasim-java/commit/c7a93467f5793994c3b0643ba21b65e1fa9a8e01))
* export network routes to geopackage ([#235](https://github.com/eqasim-org/eqasim-java/issues/235)) ([80fa272](https://github.com/eqasim-org/eqasim-java/commit/80fa272adfac9827e08d3c3d5f3b6283e820abf2))
* export transit stops to gpkg ([#303](https://github.com/eqasim-org/eqasim-java/issues/303)) ([d6c8a77](https://github.com/eqasim-org/eqasim-java/commit/d6c8a7777d1fa5cd89357948d5e7477d4665b6c2))
* exract traversals by mode ([#241](https://github.com/eqasim-org/eqasim-java/issues/241)) ([9644e9b](https://github.com/eqasim-org/eqasim-java/commit/9644e9be63fa2698c3e7886f07cf1b8acae6ef1b))
* Feeder DRT automatically detects DRT service areas and considers them during routing ([#248](https://github.com/eqasim-org/eqasim-java/issues/248)) ([49986d3](https://github.com/eqasim-org/eqasim-java/commit/49986d3ee4d0c5fd8f12e8ac54b58f683afa5932))
* generalize pseudo random errors ([#290](https://github.com/eqasim-org/eqasim-java/issues/290)) ([d4af27e](https://github.com/eqasim-org/eqasim-java/commit/d4af27eb6262c8543f51cf4e3bbc24575b5142aa))
* improve integration of policies ([#291](https://github.com/eqasim-org/eqasim-java/issues/291)) ([d584c71](https://github.com/eqasim-org/eqasim-java/commit/d584c71f6ed915ace2df6cce673321620ed1db82))
* main mode entry in PassengerRideItem ([#244](https://github.com/eqasim-org/eqasim-java/issues/244)) ([9fa3d45](https://github.com/eqasim-org/eqasim-java/commit/9fa3d456ef1b0d1f7f938321ca8497a13f3a9ca0))
* make vdf travel times useable in routing server ([#309](https://github.com/eqasim-org/eqasim-java/issues/309)) ([4393e86](https://github.com/eqasim-org/eqasim-java/commit/4393e86938c80b4900cf89826463c2bc93dc550a))
* minimum iterations ([#304](https://github.com/eqasim-org/eqasim-java/issues/304)) ([9dde302](https://github.com/eqasim-org/eqasim-java/commit/9dde302c5e573c15bba3164347ac4c42340cde35))
* network-modes argument supported in CreateDrtVehicles ([#250](https://github.com/eqasim-org/eqasim-java/issues/250)) ([4477954](https://github.com/eqasim-org/eqasim-java/commit/4477954b9e60c2eba1623d19c65b78e0c1013ff0))
* require vehicles for IDF by default ([#227](https://github.com/eqasim-org/eqasim-java/issues/227)) ([ece4932](https://github.com/eqasim-org/eqasim-java/commit/ece4932ffa8e5c8421b371d2db47f6bd34e1672e))
* routing and isochrone server ([#217](https://github.com/eqasim-org/eqasim-java/issues/217)) ([24e9d71](https://github.com/eqasim-org/eqasim-java/commit/24e9d713796798ea2fca1980b6de7897090951f2))
* script to export link traversals ([#238](https://github.com/eqasim-org/eqasim-java/issues/238)) ([9e76449](https://github.com/eqasim-org/eqasim-java/commit/9e76449a99ac5fe58a45414f3707f632a6086df5))
* separate transit routing costs ([#215](https://github.com/eqasim-org/eqasim-java/issues/215)) ([cc81ec8](https://github.com/eqasim-org/eqasim-java/commit/cc81ec80b5d21db76f7174e2dc8daf90efdb3e42))
* separate travel time recording from general analysis ([#271](https://github.com/eqasim-org/eqasim-java/issues/271)) ([0597c86](https://github.com/eqasim-org/eqasim-java/commit/0597c862f412d586bf635b65b982499a99cae536))
* **server:** optional transit and freespeed calibration functionality ([#264](https://github.com/eqasim-org/eqasim-java/issues/264)) ([f37713d](https://github.com/eqasim-org/eqasim-java/commit/f37713d53d0f20020b41953cd0b04633ebfd8c2d))
* **server:** process requests in batch from a file ([#308](https://github.com/eqasim-org/eqasim-java/issues/308)) ([8aac748](https://github.com/eqasim-org/eqasim-java/commit/8aac7487dedfc0ae9dc3ed370372a742e66c4415))
* **server:** provide leg counts independent of itinerary ([#221](https://github.com/eqasim-org/eqasim-java/issues/221)) ([514572a](https://github.com/eqasim-org/eqasim-java/commit/514572a8ddc8df0b2e0c388e34fd690019519616))
* **server:** specify routing parameters for the whole batch ([#220](https://github.com/eqasim-org/eqasim-java/issues/220)) ([2136a1a](https://github.com/eqasim-org/eqasim-java/commit/2136a1abf3375fa09333be863d0bf64dd235dedc))
* skip scenario check ([#216](https://github.com/eqasim-org/eqasim-java/issues/216)) ([f1af717](https://github.com/eqasim-org/eqasim-java/commit/f1af717654521f5a24dfc229cd990dbd59eb08ea))
* sparse horizon handler for vdf ([#276](https://github.com/eqasim-org/eqasim-java/issues/276)) ([7ed1950](https://github.com/eqasim-org/eqasim-java/commit/7ed1950498ca0f6b73a4540e40de6037af4a682b))
* Standalone Mode Choice as a core functionality and some documentation and refractoring ([#224](https://github.com/eqasim-org/eqasim-java/issues/224)) ([18eb5c3](https://github.com/eqasim-org/eqasim-java/commit/18eb5c3f651904295a6cc4e4bd1acffd9fb59831))
* **StandaloneModeChoice:** passing down extra parameters to simulations + supporting of transit with abstract access ([#265](https://github.com/eqasim-org/eqasim-java/issues/265)) ([9074122](https://github.com/eqasim-org/eqasim-java/commit/9074122cc70e2883f73abec5af10c49facea392d))
* Transit with abstract access ([#214](https://github.com/eqasim-org/eqasim-java/issues/214)) ([640b0c6](https://github.com/eqasim-org/eqasim-java/commit/640b0c6235144c918bd72b043339cab356012615))
* update swiss vdf  ([#343](https://github.com/eqasim-org/eqasim-java/issues/343)) ([ec1a2bf](https://github.com/eqasim-org/eqasim-java/commit/ec1a2bfbaa8347cb1d4e9a2b7a786b19582be604))
* write out legs in standalone mode choice ([#270](https://github.com/eqasim-org/eqasim-java/issues/270)) ([feb5793](https://github.com/eqasim-org/eqasim-java/commit/feb57932b28bd905803498d51bf5d959a2cd57d6))


### Bug Fixes

* adapt to new way of configuring link speed calculator ([#273](https://github.com/eqasim-org/eqasim-java/issues/273)) ([74befe8](https://github.com/eqasim-org/eqasim-java/commit/74befe8991ba068645826ffcebd588a66a6727f5))
* adding new mode to DMC cached modes ([#252](https://github.com/eqasim-org/eqasim-java/issues/252)) ([d858ce6](https://github.com/eqasim-org/eqasim-java/commit/d858ce64988e1c9590fc3b5241a1eab200830a75))
* attributes in legs rather than in interaction activities ([#245](https://github.com/eqasim-org/eqasim-java/issues/245)) ([0c9a6c6](https://github.com/eqasim-org/eqasim-java/commit/0c9a6c6581b5a6cde16f6c14e3ef5859b853b8c3))
* broken leg index counter ([#229](https://github.com/eqasim-org/eqasim-java/issues/229)) ([73ac087](https://github.com/eqasim-org/eqasim-java/commit/73ac087222823f94fa1abd4ea2d7e46d2239f286))
* correct writing of accessTransitStopId, ref eqasim-org/eqasim-java[#316](https://github.com/eqasim-org/eqasim-java/issues/316) ([#317](https://github.com/eqasim-org/eqasim-java/issues/317)) ([492c2d6](https://github.com/eqasim-org/eqasim-java/commit/492c2d67cad106c814fe8bf56bde2af1479c27cf))
* enable activity listener ([#296](https://github.com/eqasim-org/eqasim-java/issues/296)) ([b3e7319](https://github.com/eqasim-org/eqasim-java/commit/b3e73194154da6a000f4fb54ec2183e6fc940470))
* external inlinks for LTZ policy ([#295](https://github.com/eqasim-org/eqasim-java/issues/295)) ([7734704](https://github.com/eqasim-org/eqasim-java/commit/773470412839d4dd14b9c0156629ebb659f5e077))
* failsafe stop traversals ([#305](https://github.com/eqasim-org/eqasim-java/issues/305)) ([09ee4cc](https://github.com/eqasim-org/eqasim-java/commit/09ee4cced4186c1305a36f345cd5d102a4a2a9a0))
* handling already existing optional config groups ([#267](https://github.com/eqasim-org/eqasim-java/issues/267)) ([6abc3a3](https://github.com/eqasim-org/eqasim-java/commit/6abc3a37bfb29941d951a5a0565234e34e0a2c34))
* move emission tests to core ([#211](https://github.com/eqasim-org/eqasim-java/issues/211)) ([35a595b](https://github.com/eqasim-org/eqasim-java/commit/35a595b761e6b2363a598d3ebfd7a4157caeb020))
* only initialize SpeedyALT data once ([#222](https://github.com/eqasim-org/eqasim-java/issues/222)) ([68333f4](https://github.com/eqasim-org/eqasim-java/commit/68333f4ac34ff9a3e0198b72d87e1963218d80b2))
* preventing the summing of two error terms in FeederDrtUtilityEstimator ([#246](https://github.com/eqasim-org/eqasim-java/issues/246)) ([a68ff1a](https://github.com/eqasim-org/eqasim-java/commit/a68ff1a976eaf596de4bf0c3c3712a211701b9be))
* properly considering the --plans-path argument ([#261](https://github.com/eqasim-org/eqasim-java/issues/261)) ([dc15b31](https://github.com/eqasim-org/eqasim-java/commit/dc15b3164402dcf2c9a2ef1c01769673430f92e3))
* properly filtering links inside area of interest ([#297](https://github.com/eqasim-org/eqasim-java/issues/297)) ([14df7ba](https://github.com/eqasim-org/eqasim-java/commit/14df7ba204252289ce0cca4dabf0ee018e2d3c50))
* properly write leg mode for traversals ([#298](https://github.com/eqasim-org/eqasim-java/issues/298)) ([2041c67](https://github.com/eqasim-org/eqasim-java/commit/2041c67d36fc1b37af5ffb17aa663c2943e51ba9))
* regex expression ([#249](https://github.com/eqasim-org/eqasim-java/issues/249)) ([d36be2e](https://github.com/eqasim-org/eqasim-java/commit/d36be2e9af0f64066120a7e1a416cabda0a9f014))
* RunAdaptConfig ([#302](https://github.com/eqasim-org/eqasim-java/issues/302)) ([f2563dd](https://github.com/eqasim-org/eqasim-java/commit/f2563dd6bcf499ce38e592a01758a840e5800c85))
* **server:** add eqasim raptor config group ([#223](https://github.com/eqasim-org/eqasim-java/issues/223)) ([a1bbe91](https://github.com/eqasim-org/eqasim-java/commit/a1bbe91e8a5a5e0ce77b04da00babc5ad69ec622))
* **server:** fix problem when setting use-transit false ([#310](https://github.com/eqasim-org/eqasim-java/issues/310)) ([ed0322b](https://github.com/eqasim-org/eqasim-java/commit/ed0322bb8b4f9f43a9cd90268e5507221b301597))
* **server:** shutdown thread pool at end ([#311](https://github.com/eqasim-org/eqasim-java/issues/311)) ([f829cdb](https://github.com/eqasim-org/eqasim-java/commit/f829cdb8b690c1107998ff02629a416e445adf2b))
* using leg travel times when possible ([#234](https://github.com/eqasim-org/eqasim-java/issues/234)) ([d179e3c](https://github.com/eqasim-org/eqasim-java/commit/d179e3c29bb48c1b059408b1f3aa027fb64e4c7e))
* various fixes for vdf ([#274](https://github.com/eqasim-org/eqasim-java/issues/274)) ([d73625f](https://github.com/eqasim-org/eqasim-java/commit/d73625f3107e878db8d065c6d6947b5f06f6db91))

## Historical changelog

- Improve Emissions tools in order to handle unknown Osm highway tag values when mapping HBEFA road types
- add configurable policies for IDF
- Introduce `travelTimeRecordingInterval` config option that decouples travel time writing from general analysis
- Add eqasim_activities.csv for analysis
- The cutters now take a GeoPackage file as an alterative to a ShapeFile
- Emissions tools have been moved to core package (from ile_de_france)
- Switched to MATSim 2025 (PR)
- In switzerland one can now switch off vehicles waiting to enter traffic
- In swiss module: adjusted the adapt config to allow parametrizing capacity factors and freight in config
- In swiss module: added vehicles to chonfig
- In swiss module: adding the possibility to run a simulation with VDF 
- Adding Zurich model
- adding config comments for some parameters

**1.5.0**

- Add terminaton criterion
- Several cleanups in the recent commits
- Updated to MATSim 15
- Improve emissions tools and add tests
- Add support for multi-stage taxi trips in Sao Paulo
- fix: make compatible with downstream population pipelines
- Ensure outside activity id doesn't already exist
- Network-based (car) routing now generates access and egress walk legs
- Convert initial-routing only-walk legs to actual walk (instead of transit)
- Don't put activities on motorway/trunk/link in the network
- Updated to MATSim 14
- Isolated the mode choice model in a standalone runnable script
- Fixed LegIndex count between iterations in legs analysis
- Improved batch routing tools
- Allow boolean values in parameter definition
- Added stop area to transit leg analysis output
- Improve functionality of routing tools to set utilities in detail
- Fix bug in EqasimTransitQSimModule: first check if EqasimConfigGroup has TransitEngineModule before removing it
- Updated config option from `eqasim.tripAnalysisInterval` to `eqasim.analysisInterval`
- Automatically produce analysis output for legs and public transport information
- Move analysis classes to `org.eqasim.core.analysis.run`
- Add option for configuring custom activities in the Switzerland config file
- Add air pollution emissions computation and analysis
- Add fixed epsilon functionality
- Transform scenario configurators to proper classes (formerly static methods)
- Set routing parameter for waiting time in Île-de-France to -1.0
- Allow cutter to process events to (1) perform routing based on recorded travel time, and (2) find crossing points based on the actual link enter/leave times of a previous simulation
- Simplify cutting by introducing `--plans-path` option which is interpreted as relative to CWD
- Location assignment functionality for scenario preparation functionality has been removed. A more advanced is used in the upstream Python pipelines for scenario generation.
- Add volume-delay function module
- Add routing utilities based on scenarios
- Fix bug in plan to CSV conversion when arrival activity start time is not set
- Make use of new convergence markers in MATSim
- Change trips output file name to `eqasim_trips.csv`
- Port analysis code from AMoDeus to DRT example
- Add example how to treat rejections in the examples

**1.3.1**

- Removed unmaintained Auckland scenario (check *v1.3.0* to recover)
- Fix certificate issues while generating shaded jar
- Shift to packagecloud
- Shift to Github Actions for testing

**1.3.0**

- Updated `examples` with an example for running DRT in the Corsica test scenario
- Remove `automated_vehicles` module, `drt` should be used now
- Update MATSim to version `13.0`
- Bugfix: `RecordedTravelTime::readFromEvents` was not actually reading events.

**1.2.1**

- Consider link / facility coordinates in plan-based trip analysis if network / facilities are provided
- Bugfix: Rare issue in `EqasimTransitEngine` leading to inconsistent ordering of event times
- Homefinder is updated in SF, LA, and SP. It is now in eqasim
- Added business activity in LA and adapted config file accordingly
- Recalibrated parameters and udpated car costs in SP
- In SP run script we currently make car->walk to allow to reach equilibrium
- SP config is updated

**1.2.0**

- Properly perform mode choice for "free" activity chains or those which do not start or end with "home"
- Added Corsica unit test case for running simulation and cutting French scenarios
- Remove `EnrichedTransitRoute` and use default `TransitPassengerRoute`
- Enable support for network-based public transport simulation by setting `eqasim.useScheduleBasedTransport` to `false` (it is activated by default)

**1.1.0**

- Cutter: Cut transit routes with only *one* stop are not included anymore
- `ModularUtilityEstimator` -> `ModalUtilityEstimator`
- Removed `UniversalTourFinder` in favor of well-configured `ActivityTourFinder`
- Introduction of routed_distance (in contrary to Euclidean distance) and vehicle_distance (all distance covered link by link) into the trip analysis. Changed header, e.g. `preceding_purpose` and `following_purpose`, `euclidean_distance`
- Update to *MATSim 12.0*

**1.0.6**

- Set default mode choice rate to 5%
- Set default iterations to 60
- Consolidate validated Île-de-France simulation
- Fix: Updating geotools repository
- Fix: Correct cutting of public transit lines
- Fix: Consider special "parallel link" cases for network route cutting
- Remove bike from Sao Paulo
- Add Los Angeles and San Francisco
- Add possibility to choose modes to RunPopulationRouting
- Bugfix: EnrichedTransitRoutingModule wrote wrong link and facilities in "pt interaction" activities, which lead to problems when cutting the population
- Bugfix: Added non-default estimators for car to SF and LA
- Bugfix: Chnaged utility functions in SF to WTP space
- Added additional constraints in SF (walk and car_passenger)
- Fixed files read for Cost Parameters in LA and SF
- Added new version of sao paulo

**1.0.5**

- Fix transit_walk detection in EqasimMainModeIdentifier

**1.0.4**

- Update Auckland parameters
- Add deployment target for Maven

**1.0.3**

- Add custom MainModeIdentifier
- Bugfix: Shape extent did not cut teleportation trips properly
- Fix analysis interval for trip analysis
- Make AvCostWriter close file on shutdown
- Disable calibration output by default (since reference data is not always available)
- Add iml and idea to gitignore
- Add calibration utilities
- Improve code structure of the choice models in `core` and `switzerland`
- Improve trips analysis with PersonFilter and give access to other modules if present
- Add analysis of trips via config file
- Add better support for command line parameters
- Bugfix: Utility calculation for car/pt did not consider cost
- Add interpolation for AV cost calculation and output
- Add `fleet-size` parameter to Zurich/AV example
- Add `examples` package to demonstrate integration of multiple eqasim modules
- Add `automated_vehicles` package for integration with `av` package version `1.0.0`
- Add trip analysis
- Switch DiscreteModeChoice to version 1.0.7

**1.0.2**

- Add tools for ÖV Güteklasse to Switzerland scenario
- Add Sé shape file

**1.0.1**

- Modularize mode choice via injection and configuration

**1.0.0**

- Initial version
