# Retrying unsuccessful walking-access PT searches

`SwissRailRaptor.java` deliberately shadows the MATSim 2026.0 SBB class, retaining its
public API and upstream license. Factories, builders, mode choice, routing servers and
the PT cache's delegate therefore use the correction without an additional module or flag.
On upgrades, compare this file with the upstream version before replacing the dependency.

The first query uses the original parameters and stop finder. If the core search finds
no itinerary containing an actual transit leg, `RaptorSearchExpansion` tries again with
a larger radius at both origin and destination. It allows **five total routing attempts**
(initial attempt plus at most four retries), stopping immediately once a transit itinerary
is found. Walking-only results do not prevent these retries.

The radius grows to the larger of twice the previously examined extent or that extent
plus the configured extension radius (minimum increment one metre). The examined extent
includes the distances to the returned candidates, so a nearest-stop extension beyond the
initial radius is taken into account. Normal radii of 100, 200, 400, 800, 1600 metres are
used when all candidates are within each requested radius and the extension is 100 metres.

After searching, the original direct-walk preference and routing-module walking fallback
still apply. Finding PT does not force its selection when walking is preferable. Both
single-departure and range queries, including the route-list API, use the retry policy.
One-to-all tree queries are unchanged because they have no destination to test for success.

Expanded parameters are request-local views. Shared/person parameters and config groups
are never modified; costs, travel speeds, schedules and core routing settings remain the
same. Intermodal access/egress retains its existing per-mode search radii and maximum-radius
rules; this extension applies to the standard walking stop search, not those separate rules.

`TestRaptorSearchExpansion` builds a real timetable with two nearby but unusable stops at
each end and a useful station farther away. It checks recovery, range/list queries, early
success, five-attempt exhaustion, walking fallback and parameter isolation across requests.
