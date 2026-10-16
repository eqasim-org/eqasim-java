# Server map interface — full specification

## 1. Goal and scope

Add a simple, map-first browser interface to the existing `server` module. It must expose the four existing operations without changing their request/response contracts:

| Mode | HTTP operation | Availability |
| --- | --- | --- |
| Road route | `POST /router/road` | Always, when the server starts successfully |
| Road isochrone | `POST /isochrone/road` | Always, when the server starts successfully |
| Transit route | `POST /router/transit` | Only with transit enabled |
| Transit isochrone | `POST /isochrone/transit` | Only with transit enabled |

The audience is someone exploring one loaded simulation scenario interactively, one query at a time. The map fills the viewport behind a compact control/results panel. This is a visualization of the existing API, not a new routing engine or an isochrone polygon generator. The API remains independently usable by other clients.

## 2. Existing server behavior that the interface must respect

- `RunServer` registers `GET /interface` only when launched with `--provide-interface true`; its current `InterfaceEndpoint.get` is a placeholder. `GET /services` returns an object with a `services` array of available relative operation names, such as `router/road`.
- Every operation accepts JSON in an envelope of the form `{ "request": { ... } }` and returns **one response object** for this form. The alternative `batch` envelope returns an array and is not used by the UI.
- Request coordinate values (`origin_x`, `origin_y`, `destination_x`, `destination_y`) and response coordinates/WKT are in the scenario's projected CRS, not longitude/latitude. The map operates in geographic coordinates; conversion in both directions belongs in the browser.
- Time inputs use numeric `departure_time_s` measured since midnight at the start of the **first schedule day**. They are not Unix timestamps or dates. Response arrival times are on the same continuous time axis and may exceed 86,400 seconds.
- Road routing can return `road_geometry`, `access_geometry`, and `egress_geometry` as projected WKT `LINESTRING`s when `provide_geometry` is true. Road route time components are `access_time_min`, `in_vehicle_time_min`, `egress_time_min`, `total_travel_time_min`; distances are `access_distance_km`, `in_vehicle_distance_km`, `egress_distance_km`. The field `arrivalTime_s` in the road response is calculated before egress: for a user-facing arrival after the complete trip, use departure time plus `total_travel_time_min * 60`.
- Transit routing returns summary fields including `total_travel_time_min`, `arrivalTime_s`, `in_vehicle_distance_km`, access/egress/transfer walk distances and times, waiting times, `transfers`, `is_only_walk`, and per-mode breakdowns. With both `provide_itinerary` and `provide_geometry` true, `itinerary.legs` contains leg types (`access`, `egress`, `transfer`, `vehicle`), mode, times, distance, and optional projected WKT `geometry`; `itinerary.stops` contains names, coordinates, and times. Vehicle geometry follows available network links; walk geometry is a straight line, not a pedestrian-network route. A walking-only response is valid and must be displayed as such.
- Road isochrones return a `points` array of projected `{x,y,total_travel_time_min,...}` values, primarily nodes (and optional intermediate points). Transit isochrones return a `stops` array of projected `{x,y,total_travel_time_min,...}` values. These are **point samples**, not areas or polygons. Both responses can be empty. `provide_geometry` is unnecessary for drawing them because they already include coordinates.
- Road isochrones currently use free-speed travel times in their service, even when road routing uses time-dependent VDF travel times. Do not promise identical road-route and road-isochrone travel times. Transit isochrones use their own stop reachability service, not the transit route service.

## 3. Delivery and runtime integration

1. Serve one HTML document from `InterfaceEndpoint.get` at `GET /interface`, with `text/html` content type. Keep the application markup, styles, and application logic together in a single resource under `server/src/main/resources`; load that classpath resource rather than using a source-tree-relative path, so it works from Maven builds and a standalone shaded JAR.
2. Use Vue.js for state, controls, request lifecycle, and result rendering; Leaflet for the map; proj4js (or a browser projection library with equivalent capabilities) for CRS conversion. Load frontend libraries from version-pinned public CDNs. Use public OpenStreetMap standard tiles with visible attribution. Internet access is therefore required for tiles and CDN libraries. Do not require a frontend build process or a separate web server.
3. Add a same-origin JSON metadata GET alongside `/interface`, for example `GET /interface/metadata`, registered only when the interface is enabled. Preserve the behavior and shape of `/services` and all four POST operations. Request both `/services` and metadata on page load; use relative URLs so non-root deployments or reverse proxies can retain the server's path prefix.
4. Supply metadata from the scenario actually loaded by `ServiceBuilder`: the CRS of the original scenario network (obtain using MATSim `ProjectionUtils.getCRS(network)`), a **browser-usable projection definition** derived server-side from that CRS, the extent of the **cleaned car-only network** used by road services, and the number of schedule days. Pass metadata into `InterfaceEndpoint` via startup construction rather than reloading the MATSim scenario for each GET. Ensure metadata coordinates remain in the same CRS and axis order as service coordinates. If CRS or conversion metadata cannot be produced, give a clear startup or page-load error rather than silently assuming WGS84.
5. `GET /interface/metadata` response contract:

   ```json
   {
     "crs": "EPSG:2154",
     "projection_definition": "<definition accepted by the chosen browser projection library>",
     "network_extent": {
       "min_x": 0.0, "min_y": 0.0, "max_x": 1.0, "max_y": 1.0
     },
     "schedule_day_count": 1
   }
   ```

   The numeric extent example illustrates shape only; emit actual bounds from the cleaned car-network **nodes**. `crs` is the human-readable identifier; `projection_definition` is sufficient to instantiate forward/inverse conversions for the active network, including EPSG codes not built into the browser library (this repository uses, among others, EPSG:2154, EPSG:2056, EPSG:2227 and EPSG:29183). A WKT definition is acceptable only if the chosen client library actually supports that WKT dialect and CRS. No browser-side call to an external EPSG registry is required. An empty or degenerate network is a metadata error with an informative message, not invalid map bounds.

## 4. Schedule and departure-time rules

- With transit enabled, derive `schedule_day_count` from the loaded schedule's latest finite absolute stop event time (departure time plus each stop departure/arrival offset; include relevant events for every scheduled route departure). Use `max(1, floor(latest_event_seconds / 86400) + 1)`. Thus an event at exactly `24:00:00` belongs to Day 2. Do not cap the count at seven: a longer schedule exposes its actual days. Without a loaded transit schedule, the count is 1. Do not infer calendar dates or weekday names from the data.
- Show a Day 1…Day N selector and a 24-hour HH:mm clock input, initially Day 1 at 08:00. `departure_time_s = (selected_day - 1) * 86400 + hour * 3600 + minute * 60`. Do not modulo by 86,400 when submitting a request; reuse this convention for road and transit modes. A day/time without service may yield an empty or walking-only result; it is not automatically a transport API failure.
- Format every absolute time returned by the server as `Day K HH:mm` (optionally seconds when useful), derived from the continuous seconds count. Times beyond 24:00 must not wrap to Day 1. Durations remain in minutes and distances in kilometers. Do not display a wall-clock date or browser timezone.
- Validate day selection against metadata and HH:mm as a real 00:00–23:59 time. Default minute granularity is sufficient; submit numeric seconds as required by the API.

## 5. Common page layout and interaction

- Fit the map to metadata's car-network extent on initial load, converting its bounds to WGS84 in the browser (transform all four corners; use a safe bounds computation if the chosen projection makes corner-only bounds insufficient). Do not use a hardcoded city or center. Keep normal pan/zoom controls and OpenStreetMap attribution visible. Provide a “Fit network” action to restore this view.
- The overlay panel contains: a clearly labeled four-mode selector (Road route, Road isochrone, Transit route, Transit isochrone), location controls, shared departure day/time, a Run button, a loading state, and a results section. Show only operations listed in `/services`; with transit disabled do not present transit modes as executable choices. Choose the first available mode, normally Road route.
- Locations are entered as editable WGS84 **longitude and latitude** decimal fields, with explicit labels and reasonable precision. A user selects an explicit map-click target (Start or End for routes; Origin for isochrones), then clicks the map; the corresponding marker appears and can be dragged. Map clicks and dragging update the coordinate fields; valid typed coordinates update the markers. There is no address geocoding. For routing, distinguish start and end markers clearly; for isochrones use one origin marker.
- Preserve day/time and entered points where meaningful when switching modes; an origin may remain the start. Remove stale operation overlays and result data when a different mode is selected or its inputs change. Keep the underlying map pan/zoom state on mode changes. A new successful query replaces the previous overlay; a pending query must not overwrite a newer result if inputs or mode changed during its execution (abort or ignore stale responses).
- Disable Run until required coordinates, day, time, and isochrone maximum are valid. Validate longitude [-180,180], latitude [-90,90], finite numbers and positive maximum travel time; show concise inline errors. Do not send invalid, missing, or unprojectable coordinates. The map can still be panned outside the network, but show a useful empty/no-route response rather than claiming that off-network selections are covered.
- Fit visible results after a successful query when useful, without preventing the user from manually panning/zooming. Preserve input markers and show a legend when an isochrone overlay is visible. Use a responsive layout that leaves the map and controls usable on desktop and narrow/mobile screens. Interactive controls need labels, keyboard operation, visible focus, and status/error text (not color alone).
- Show a local loading indicator while querying. For network/HTTP/parse/server errors display a nontechnical, actionable message and allow retry. For empty isochrone arrays show “No reachable points/stops” and retain the origin. Avoid rendering API-provided strings (e.g. stop names) as HTML.

## 6. Mode-specific requests and presentation

### 6.1 Road route

Send:

```json
{"request":{"origin_x":0,"origin_y":0,"destination_x":0,"destination_y":0,"departure_time_s":28800,"provide_geometry":true}}
```

The numeric coordinates above are placeholders for frontend-projected coordinates. Leave other routing parameters at server defaults. Parse the three returned WKT lines (`road_geometry`, `access_geometry`, `egress_geometry`), convert every vertex to WGS84, and draw a prominent road line with visually distinct dashed/thinner access and egress segments. Accommodate empty/zero-length lines (including a trivial same-node route) without treating them as a page failure. Keep the exact selected point markers visible.

Results: total trip duration; derived end-to-end arrival (`departure_time_s + total_travel_time_min * 60`); in-vehicle distance and time; access and egress walking distances/times; and optionally total distance as the **sum of the three distance fields**, labeled as such. Use response field names exactly, including camel-case `arrivalTime_s` if exposing the server's pre-egress arrival in a detail view. Do not label it as final arrival.

### 6.2 Transit route

Send:

```json
{"request":{"origin_x":0,"origin_y":0,"destination_x":0,"destination_y":0,"departure_time_s":28800,"provide_itinerary":true,"provide_geometry":true}}
```

Draw each `itinerary.legs[*].geometry` WKT line transformed to WGS84: transit legs visually distinct from straight-line walk/access/transfer legs. Mark itinerary stop coordinates where available; avoid duplicating overlapping stop markers. Provide a readable itinerary in leg order with walk/vehicle mode, line name or ID when present, depart/arrive times, travel time and distance; add summary duration, `arrivalTime_s`, in-vehicle distance, transfers, aggregate walk and waiting times, and per-mode totals when available. Clearly label `is_only_walk` routes as walking-only and draw their walk leg. Treat optional geometries/stops as optional while still displaying summary data.

### 6.3 Road isochrone

Provide an editable **Maximum travel time (minutes)** field, default **30**. Send:

```json
{"request":{"origin_x":0,"origin_y":0,"departure_time_s":28800,"maximum_travel_time_min":30}}
```

Use returned `points` coordinates directly; transform each to WGS84 and draw **all** returned points as small colored road-network node/segment markers. No interpolation, hull, polygon, or fabricated street geometry. Color by `total_travel_time_min` on a continuous low-to-high travel-time gradient with a numeric legend from 0 to the selected cutoff. The `is_origin` points can be distinguished from the selected origin marker. Support inspecting a point's travel time (and arrival time) through a tooltip/popup. There is no default `provide_nodes` or `provide_geometry` requirement.

### 6.4 Transit isochrone

Reuse the same editable cutoff field, default **30**. Send:

```json
{"request":{"origin_x":0,"origin_y":0,"departure_time_s":28800,"maximum_travel_time_min":30}}
```

`maximum_travel_time_min` is mandatory for this UI: the current service requires at least it or `maximum_transfers`, and its defaults provide neither. Render **all** returned `stops` as colored point markers with the same continuous 0-to-cutoff scale/legend and a tooltip/popup showing travel time, arrival and transfers. Do not draw a polygon, route, or transit line from this response. Leave access-distance and transfer-distance options at API defaults; a nearest-stop fallback may yield an origin stop beyond the requested access radius. `is_origin` is a stop origin, not the user's exact clicked coordinate.

For both isochrone modes, do not request WKT point geometry simply to draw points. Use `total_travel_time_min`, not in-vehicle time, as the color metric. Retain points whose reported time exceeds the requested cutoff only if returned by the API (e.g. an initial origin); display their true time and clamp their display color to the top of the legend rather than misrepresenting their value.

## 7. Coordinate conversion and geometry requirements

- The server performs no per-request CRS conversion for this UI. Browser input is `[longitude, latitude]`; transform to `[x, y]` in the metadata CRS when constructing JSON. Transform returned `[x, y]` and each WKT coordinate pair back to `[longitude, latitude]` before passing to Leaflet's `[latitude, longitude]` API. Do not swap axes or assume projected x/y are meters for frontend calculations (the service already handles its own units).
- Initialize projection definitions once after loading metadata; verify round-trip conversion of the network extent/representative point produces finite geographic coordinates. Handle invalid CRS definition/conversion as an interface initialization error with an explanation. The projection metadata must work for the actual model's CRS, including non-default EPSG projections.
- Parse `LINESTRING` (and, if useful for itinerary stops, `POINT`) WKT returned by the service. Geometry may be absent or empty. Use each geometry only as a display overlay; the API remains the source of time/distance values. Do not transform WKT on the Java side.

## 8. Implementation boundaries and verification

- Keep the Java changes focused on interface resource serving, metadata generation, and route registration/wiring in the server module. Avoid changing the four existing POST request classes or computational services merely to implement display behavior. A metadata DTO can be separate from `ServicesEndpoint.Response`.
- The UI should remain usable from a shaded standalone JAR with `--provide-interface true`; with the flag absent, its current opt-in behavior remains. Metadata should be exposed under the same opt-in condition as the page.
- Verify both transit-enabled and `--use-transit false` startups: `/services`, mode availability, metadata day count and network bounds. Test schedule-day calculation across midnight and beyond seven days, plus a one-day scenario. Check CRS round-trip and initial map bounds on at least two different projected CRSs, one not native to the browser projection library.
- Exercise each mode with real endpoints: request envelope, nonwrapped response, projected input/output, geometry overlays, continuous isochrone legend, default 08:00 and Day N seconds calculation. Check empty arrays, walking-only transit result, zero-length/missing geometry, stale responses, invalid coordinates, and failed metadata/POST calls. Confirm Java packaging includes the HTML resource and no file-system path dependency.

## 9. Acceptance criteria

1. With `--provide-interface true`, navigating to `/interface` opens a viewport-filling map fitted to the active cleaned road network and exposes only operations reported by `/services`.
2. A user can place/edit points, choose Day N and HH:mm, execute road/transit routing, see the route and a meaningful summary/itinerary, and distinguish walking from in-vehicle geometry.
3. A user can run road/transit isochrones with a 30-minute editable cutoff and see colored nodes/stops (not polygons), a travel-time legend, and inspectable values.
4. The same UI works across the scenario's projected CRSs by consuming server-provided CRS metadata and converting coordinates entirely in the browser.
5. Departure seconds and displayed arrivals honor multi-day schedules; road-only operation exposes Day 1. Existing programmatic APIs retain their payloads and behavior.
