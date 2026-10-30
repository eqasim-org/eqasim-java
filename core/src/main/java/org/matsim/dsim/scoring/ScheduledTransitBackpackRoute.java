package org.matsim.dsim.scoring;

import org.eqasim.core.components.transit.events.PublicTransitEvent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Route;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.vehicles.Vehicle;

/** Records actual timetable use without fabricating vehicle boarding/alighting events. */
public final class ScheduledTransitBackpackRoute implements BackpackRoute, Message {
    private Id<Link> from;
    private Id<Link> to;
    private double departure;
    private double arrival;
    private PublicTransitEvent transit;

    @Override public void handleEvent(Event event) {
        if (event instanceof PersonDepartureEvent e) { from = e.getLinkId(); departure = e.getTime(); }
        if (event instanceof PersonArrivalEvent e) { to = e.getLinkId(); arrival = e.getTime(); }
        if (event instanceof PublicTransitEvent e) transit = e;
    }

    @Override public Route finishRoute() {
        if (transit == null) throw new IllegalStateException("Missing timetable arrival for experienced PT route");
        var route = new DefaultTransitPassengerRoute(from, to, transit.getAccessStopId(), transit.getEgressStopId(),
                transit.getTransitLineId(), transit.getTransitRouteId());
        route.setBoardingTime(transit.getVehicleDepartureTime());
        route.setTravelTime(arrival - departure);
        route.setDistance(transit.getTravelDistance());
        return route;
    }
    @Override public Message toMessage() { return this; }
    @Override public Id<Vehicle> getVehicleId() { return null; }
}
