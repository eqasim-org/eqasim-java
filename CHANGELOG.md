# CHANGELOG

This change log is written in descending order. Changes that happen after version
`X` was released, are written *above* that version number, because they will be
included in the (note yet determined) next version number.

**Development version**

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
