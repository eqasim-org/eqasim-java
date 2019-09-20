# CHANGELOG

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
