package org.eqasim.core.simulation.vdf.engine;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.VehicleUsingAgent;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

class TraversalTime {
    private final double simulationStep;
    private final Network network;
    private final TravelTime travelTime;

    TraversalTime(Network network, TravelTime travelTime, double simulationStep) {
        this.network = network;
        this.travelTime = travelTime;
        this.simulationStep = simulationStep;
    }

    double getTraversalTime(double now, Id<Link> linkId, MobsimDriverAgent agent) {
        // The current VDF travel time does not need a person object.
        // We just pass one to respect the interface.

        Person person = null;
        if (agent instanceof HasPerson hasPerson) {
            person = hasPerson.getPerson();
        }

        Vehicle vehicle = null;
        if (agent instanceof VehicleUsingAgent vehicleUsing) {
            if (vehicleUsing.getVehicle() != null) {
                vehicle = vehicleUsing.getVehicle().getVehicle();
            }
        }

        Link link = network.getLinks().get(linkId);

        double traversalTime = travelTime.getLinkTravelTime(link, now, person, vehicle);

        // step size logic from QueueWithBuffer
        return simulationStep * Math.floor(traversalTime / simulationStep);
    }
}
