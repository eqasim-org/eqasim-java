package org.eqasim.projects.astra16.pricing.business_model;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

import ch.ethz.matsim.av.analysis.FleetDistanceListener;
import ch.ethz.matsim.av.analysis.FleetDistanceListener.OperatorData;
import ch.ethz.matsim.av.data.AVOperator;
import ch.ethz.matsim.av.framework.AVModule;

public class BusinessModelUpdater implements AfterMobsimListener, PersonDepartureEventHandler {
	private final FleetDistanceListener distanceListener;
	private final BusinessModel provider;
	private final Id<AVOperator> operatorId;
	private final BusinessModelListener listener;

	private int numberOfTrips = 0;

	public BusinessModelUpdater(Id<AVOperator> operatorId, FleetDistanceListener distanceListener,
			BusinessModel provider, BusinessModelListener listener) {
		this.operatorId = operatorId;
		this.distanceListener = distanceListener;
		this.provider = provider;
		this.listener = listener;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(AVModule.AV_MODE)) {
			numberOfTrips++;
		}
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		OperatorData data = distanceListener.getData(operatorId);

		double vehicleDistance_km = 1e-3 * (data.emptyDistance_m + data.occupiedDistance_m);
		double passengerDistance_km = 1e-3 * data.passengerDistance_m;

		BusinessModelData businessModelData = provider.update(vehicleDistance_km, passengerDistance_km, numberOfTrips);
		listener.handleBusinessModel(businessModelData);
		numberOfTrips = 0;
	}
}
