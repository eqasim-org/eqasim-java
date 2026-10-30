package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Collection;
import java.util.Comparator;
import java.util.PriorityQueue;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.VehicleQ;

/** Exact seepage selection for MATSim 2026.0-2025w33 without per-lookup queue copies. */
final class SeepageVehicleSelector {
    private SeepageVehicleSelector() { }

    private static final ThreadLocal<PriorityQueue<QVehicle>> SCRATCH = ThreadLocal.withInitial(
            () -> new PriorityQueue<>(11, Comparator.comparingDouble(QVehicle::getEarliestLinkExitTime)));

    static QVehicle select(VehicleQ<QVehicle> vehicles, double now, Collection<String> seepModes) {
        QVehicle first = vehicles.peek();
        QVehicle earliest = null;
        boolean tied = false;
        // Transit-stop handling can change priorities while vehicles remain in
        // the heap. Inspect current values instead of trusting the current head.
        for (QVehicle vehicle : vehicles) {
            double time = vehicle.getEarliestLinkExitTime();
            if (time <= now && seepModes.contains(vehicle.getDriver().getMode())) {
                int order = earliest == null ? -1 : Double.compare(time, earliest.getEarliestLinkExitTime());
                if (order < 0) { earliest = vehicle; tied = false; }
                else if (order == 0) tied = true;
            }
        }
        if (earliest == null) return first;
        if (!tied) return earliest;
        PriorityQueue<QVehicle> scratch = SCRATCH.get();
        try {
            // Reproduce the original PassingVehicleQ insertion/poll sequence.
            // Explicit offers avoid bulk constructors with different tie order.
            for (QVehicle vehicle : vehicles) scratch.offer(vehicle);
            while (!scratch.isEmpty()) {
                QVehicle vehicle = scratch.poll();
                if (vehicle.getEarliestLinkExitTime() <= now && seepModes.contains(vehicle.getDriver().getMode())) return vehicle;
            }
            return first;
        } finally {
            // PriorityQueue.clear releases every reference without repeated
            // polling/reheapification; one capacity buffer is retained per worker.
            scratch.clear();
        }
    }
}
