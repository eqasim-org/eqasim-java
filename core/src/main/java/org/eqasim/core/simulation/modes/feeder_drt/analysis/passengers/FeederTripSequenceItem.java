package org.eqasim.core.simulation.modes.feeder_drt.analysis.passengers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

public class FeederTripSequenceItem {
    public Link originLink;
    public Link destinationLink;
    public Id<Person> personId;
    public String operator;
    public Id<Vehicle> accessVehicleId;
    public Id<Vehicle> egressVehicleId;
    public double accessDepartureTime = Double.NaN;
    public double accessArrivalTime = Double.NaN;
    public double egressDepartureTime = Double.NaN;
    public double egressArrivalTime = Double.NaN;
    public Id<TransitLine> accessTransitLineId;
    public Id<TransitRoute> accessTransitRouteId;
    public Id<TransitLine> egressTransitLineId;
    public Id<TransitRoute> egressTransitRouteId;
    public double ptDepartureTime = Double.NaN;
    public double ptArrivalTime = Double.NaN;
    public Id<TransitStopFacility> accessTransitStopId;
    public Id<TransitStopFacility> egressTransitStopId;
}
