package org.eqasim.core.scenario.cutter.population.trips.crossing.transit;

import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

public class TransitRouteCrossingPoint {
	final public TransitLine line;
	final public TransitRoute route;

	final public TransitRouteStop outsideStop;
	final public TransitRouteStop insideStop;

	final public double outsideDepartureTime;
	final public double insideDepartureTime;
	
	final public boolean isOutgoing;

	public TransitRouteCrossingPoint(TransitLine line, TransitRoute route, TransitRouteStop outsideStop,
			TransitRouteStop insideStop, double outsideDepartureTime, double insideDepartureTime, boolean isOutgoing) {
		this.line = line;
		this.route = route;
		this.outsideStop = outsideStop;
		this.insideStop = insideStop;
		this.outsideDepartureTime = outsideDepartureTime;
		this.insideDepartureTime = insideDepartureTime;
		this.isOutgoing = isOutgoing;
	}
}
