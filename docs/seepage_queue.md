# Copy-avoiding QSim seepage selection

Eqasim automatically uses the optimized queue when QSim's existing
`linkDynamics=SeepageQ` setting is selected. No new argument is required.
FIFO and PassingQ retain their native queues. Node logic,
flow/storage capacities, bike restriction and speed/delay calculators are unchanged.
Parallel scheduling is described separately in [fast_qsim.md](fast_qsim.md).

The native seepage selector builds a temporary passing queue containing all
vehicles on every lookup. The replacement keeps the same primary heap and a
lazy identity index containing only queued seepage vehicles:

- Car-only and empty queues immediately return the native head.
- Mixed queues inspect only the seepage vehicles. A unique earliest eligible
  seepage vehicle is selected directly; no eligible vehicle returns the head.
- Equal earliest times retain native tie breaking: all vehicles are inserted
  into a scratch heap in their original heap-iteration order. This exceptional
  path still copies references, but reuses heap storage per worker. References
  are cleared afterward, so it does not retain vehicles between queries. The
  scratch heap reproduces native sift/tie rules and reads each exit time once.

Exit times are read on every lookup, including after transit-style time changes.
Insertion, polling, arbitrary removal, iterator removal, clearing and transit
reinsertion maintain the seepage index. Removal does not inspect the driver,
which arrival callbacks may already have detached. As in native QSim, a vehicle
has one position in a lane queue and its mode remains fixed while queued.

The small per-lane index is allocated only when a seepage vehicle first enters;
car-only queues allocate no index. Scratch heaps retain capacity per worker,
rather than per link. Each lane remains owned by its existing QSim worker during
the movement phase, so no new locks are introduced.

## MATSim integration and upgrades

MATSim's lane class is final and its selection method is private. In addition,
native visualization code casts lane snapshot data to that class's inner type.
The repository therefore includes a version-pinned `QueueWithBuffer` adaptation
from **MATSim 2026.0-2025w33**, retaining its class identity and GPL license.
The only behavioral edits are seepage queue construction and dispatch to the
new selector. Existing explicit `VehicleQ` overrides continue to work.

When upgrading MATSim, update this source from the new dependency and reapply
those two edits. Do not carry the old lane mechanics into a new MATSim version.

## Validation

Differential tests compare vehicle identity and heap order against native
`PassingVehicleQ` selection across mixed modes, ties, changed exit times,
transit reinsertion, removal and parallel workers. Full QSim comparisons check
the complete event content for congested mixed traffic with both restricted and
unrestricted seepage, using one and four workers. Cross-worker event delivery
order is ignored; event times, vehicle/person IDs and other attributes must match.
Snapshot generation is exercised with a nonzero visualization node offset.

A local selection-only benchmark on JDK 21 used fixed 512-vehicle queues and
50,000 warmup/measured lookups per case. Native selection allocated approximately
5,912 bytes per lookup for car-only and mixed queues; the optimized paths,
including equal-time ties, allocated zero bytes per lookup after warmup in that
run. The tied case took approximately 5.9 microseconds natively versus 4.8
microseconds with reusable storage. These synthetic results isolate selection;
they do not predict total QSim speedup or include index insertion/removal costs.

The optimization removes avoidable work, but its effect on total iteration time
depends on queue lengths and mode mix. No national-scenario speedup is assumed.
