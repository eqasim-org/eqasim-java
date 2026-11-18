# Cutting scenarios while retaining routes

`org.eqasim.core.scenario.cutter.RunScenarioCutter` accepts `--skip-routing true`.
The default is `false`, which reroutes the retained trips after cutting.

With `--skip-routing true`, the cutter preserves unchanged legs, routes, vehicle
IDs and interaction activities. It trims affected routes instead of replacing
trips with unrouted legs. This is useful when cutting a scenario again after
rebuilding and routing its network.

* Access and egress walks are shortened at the extent boundary. Boarding stops,
  vehicle-access locations and the main journey remain unchanged when they are
  retained. The new origin time is advanced by the removed access-walk time,
  preserving arrival at the boarding stop or vehicle. Removing an egress-walk
  tail brings arrival forward.
* Network legs retain the applicable sub-sequence of links, including crossing
  links. The cutter still represents excluded sections using `outside` legs
  and activities. Repeated cuts recognize endpoints already on crossing links.
* PT passenger routes retain the last outside boarding stop before the first
  inside stop and the first outside alighting stop after the last inside stop,
  bounded by the passenger's original boarding and alighting stops. Exits and
  re-entries between these stops retain the full sequence. Line and route IDs,
  scheduled departure times and stop offsets remain consistent with the cut
  schedule. Access-only cuts preserve the original passenger route.
* Teleported legs are clipped geometrically, with distance and duration scaled
  to the retained fraction. A genuinely walk-only retained section stays walk.
* Boundary coordinates are preserved when assigning outside facilities, rather
  than moved to the centre of the nearest road link.

Input plans must already be routed and contain departure/travel times, as
required to locate and time crossings. The cutter processes the population even
with `--skip-routing true`; this flag skips only final routing. The output is
validated in both modes.
