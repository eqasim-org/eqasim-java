package org.eqasim.components.transit.connection;

import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

public class DefaultTransitConnection implements TransitConnection {
	final private Departure departure;
	final private TransitRouteStop accessStop;
	final private TransitRouteStop egressStop;
	final private double inVehicleTime;
	final private double waitingTime;

	public DefaultTransitConnection(Departure departure, TransitRouteStop accessStop, TransitRouteStop egressStop,
			double inVehicleTime, double waitingTime) {
		this.departure = departure;
		this.accessStop = accessStop;
		this.egressStop = egressStop;
		this.inVehicleTime = inVehicleTime;
		this.waitingTime = waitingTime;
	}

	@Override
	public Departure getDeparture() {
		return departure;
	}

	@Override
	public TransitRouteStop getAccessStop() {
		return accessStop;
	}

	@Override
	public TransitRouteStop getEgressStop() {
		return egressStop;
	}

	@Override
	public double getInVehicleTime() {
		return inVehicleTime;
	}

	@Override
	public double getWaitingTime() {
		return waitingTime;
	}
}
