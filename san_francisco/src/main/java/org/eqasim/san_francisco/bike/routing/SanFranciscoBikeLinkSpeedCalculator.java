package org.eqasim.san_francisco.bike.routing;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.vehicles.Vehicle;

public class SanFranciscoBikeLinkSpeedCalculator implements LinkSpeedCalculator {

    private double maxSpeed = 4.16666666;

    public double getMaximumVelocityForLink(Link link, Vehicle vehicle) {

        double maxBicycleSpeed = vehicle == null ? maxSpeed : vehicle.getType().getMaximumVelocity();

        return Math.min(maxBicycleSpeed, link.getFreespeed());
    }

    @Override
    public double getMaximumVelocity(QVehicle qVehicle, Link link, double time) {
        if (isBike(qVehicle))
            return getMaximumVelocityForLink(link, qVehicle.getVehicle());
        else
            return getDefaultMaximumVelocity(qVehicle, link, time);
    }

    private double getDefaultMaximumVelocity(QVehicle qVehicle, Link link, double time) {
        return Math.min(qVehicle.getMaximumVelocity(), link.getFreespeed(time));
    }

    private boolean isBike(QVehicle qVehicle) {
        return qVehicle.getVehicle().getType().getId().toString().equals(TransportMode.bike);
    }
}
