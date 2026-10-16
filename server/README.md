# Scenario map interface

Build the standalone server from the repository root:

```sh
mvn -pl server -am package -Pstandalone -DskipTests
```

Enable the interface when starting your scenario server:

```sh
java -cp server/target/server-2.2.0.jar org.eqasim.server.RunServer \
  --config-path /path/to/scenario/config.xml --port 8080 --provide-interface true
```

Supply the scenario's usual `--eqasim-configurator` option if needed. Open
`http://localhost:8080/interface`. Use `--use-transit false` for road-only operation.
The page and `GET /interface/metadata` are available only when the interface flag
is enabled. Vue, Leaflet and proj4js are loaded from pinned CDN versions; map tiles
come from OpenStreetMap, so the browser needs internet access.

Metadata comes from the loaded network CRS, cleaned car-network nodes and loaded
transit schedule. Missing/unknown CRS attributes or empty/degenerate car networks
produce an explanatory startup error. The browser validates projection conversion
before enabling queries. No EPSG registry lookup or frontend build is needed.

Enter WGS84 longitude/latitude or choose a map-click target and place/drag markers.
Day numbers refer to the schedule's continuous time axis, starting with Day 1;
arrivals beyond midnight display the next day. Isochrones show reachable point
samples, not coverage polygons. Road isochrones use free-speed times, which may
differ from time-dependent road routes. Both isochrone modes send
`consider_access: true` so travel times include access from the selected origin
to the road network or transit stops.

## Verification

```sh
mvn -pl server -am test \
  -Dtest=TestInterfaceMetadata,TestInterfaceServer \
  -Dsurefire.failIfNoSpecifiedTests=false
```

These tests cover schedule events across midnight and beyond seven days, invalid
metadata, cleaned-network bounds, projection definitions for EPSG:2154, 2056,
2227 and 29183, real HTTP requests for all four services, transit-disabled
operation, HTML resource serving and opt-in route registration. Projection
reference pairs are emitted to `server/target/interface-projections.json` for
comparison with browser proj4js conversions.
