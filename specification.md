The server package contains an HTTP server that has four endpoints:
- Route a trip from A to B at a specific time on the road network
- Obtain isochrones on the road network around a given point
- Route a trip from A to B at a specific time  on the transit network
- Obtain isochrones on the transit network around a given point

These APIs work already well programmatically. The goal is now to create
a SIMPLE web interface that exposes this functionality.

- It should be centered around a screen-wide map.
- One should choose between the four functionalities listed above.

Routing mode
- The user should be able to put a start and end location and specify a departure time
- The route information (travel time, distance, etc.) should then be displayed
- Also, the route on the map should be dispalyed

Isochrone mode
- The user should be able to put a location and to specify a departure time
- The isochrone should be displayed on the map
  - For car, the road network / nodes should be colored 
  - For transit, the stops should be colored

Technical details
- There is already a prepared InterfaceEndpoint::get method that should return the HTML interface
- Ideally, only create one HTML file (which can live in the resources and will be exposed through InterfaceEndpoint)
- Use Vuejs as the frontend library, except there is a good reason why not too

Coordinate Reference Systems
- CRS calculations should happen frontend-side
- The server API expects projected coordinates, for that the CRS needs to be communicated to the frontend
- This can happen through a new GET in the InterfaceEndpoint
- The CRS can be obtained through ProjectionUtils.getCRS(network) in MATSim

Map centering
- The initial map should be centered on the active network area.
- For that, provide the extents of the network to the frontend.
- This can happen through a new GET in the InterfaceEndpoint (potentially the same as CRS)

Departure times
- Departure time are given in "seconds after midnight", by default we only cover one day with the schedule
- HOWEVER, we may have week-long schedules, in this case the departure time is encoded as "seconds after 00:00:00 of the first day"
- Consider this when designing the interface
