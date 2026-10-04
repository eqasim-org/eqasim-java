package org.eqasim.switzerland.ch_cmdp.transit_dwell;

import java.util.List;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.*;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * Uses MATSim's standard complex stop handler with passenger service times divided by the
 * population sample size. Actual passenger lists, capacity checks, callbacks, door operation
 * and one-second processing remain native. Schedule holding remains in the transit driver.
 */
public final class SampledTransitStopHandler implements TransitStopHandler {
    private final TransitStopHandler delegateHandler;

    public SampledTransitStopHandler(TransitStopHandlerFactory delegateFactory, Vehicle vehicle, double sampleSize) {
        validateSampleSize(sampleSize);
        // The native constructor reads only these three timing properties. This private
        // timing-only vehicle is never registered or passed to passenger callbacks.
        VehicleType timings = VehicleUtils.getFactory().createVehicleType(vehicle.getType().getId());
        VehicleUtils.setAccessTime(timings, VehicleUtils.getAccessTime(vehicle.getType()) / sampleSize);
        VehicleUtils.setEgressTime(timings, VehicleUtils.getEgressTime(vehicle.getType()) / sampleSize);
        VehicleUtils.setDoorOperationMode(timings, VehicleUtils.getDoorOperationMode(vehicle.getType()));
        delegateHandler = delegateFactory.createTransitStopHandler(
                VehicleUtils.createVehicle(vehicle.getId(), timings));
    }

    static void validateSampleSize(double sampleSize) {
        if (!Double.isFinite(sampleSize) || sampleSize <= 0.0 || sampleSize > 1.0) {
            throw new IllegalArgumentException("Transit dwell scaling requires eqasim.sampleSize in (0, 1], got " + sampleSize);
        }
    }

    @Override
    public double handleTransitStop(TransitStopFacility stop, double now,
            List<PTPassengerAgent> leavingPassengers, List<PTPassengerAgent> enteringPassengers,
            PassengerAccessEgress accessEgress, MobsimVehicle vehicle) {
        return delegateHandler.handleTransitStop(stop, now, leavingPassengers, enteringPassengers, accessEgress, vehicle);
    }
}
