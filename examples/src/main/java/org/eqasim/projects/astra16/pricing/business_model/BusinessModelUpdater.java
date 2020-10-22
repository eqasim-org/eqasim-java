package org.eqasim.projects.astra16.pricing.business_model;

import org.matsim.amodeus.analysis.FleetInformationListener;
import org.matsim.amodeus.analysis.FleetInformationListener.FleetInformation;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

public class BusinessModelUpdater implements AfterMobsimListener {
	private final FleetInformationListener distanceListener;
	private final BusinessModel provider;
	private final BusinessModelListener listener;

	public BusinessModelUpdater(FleetInformationListener distanceListener, BusinessModel provider,
			BusinessModelListener listener) {
		this.distanceListener = distanceListener;
		this.provider = provider;
		this.listener = listener;
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		FleetInformation data = distanceListener.getInformation();

		double vehicleDistance_km = 1e-3 * data.vehicleDistance_m;
		double passengerDistance_km = 1e-3 * data.passengerDistance_m;
		int numberOfTrips = data.numberOfRequests;

		BusinessModelData businessModelData = provider.update(vehicleDistance_km, passengerDistance_km, numberOfTrips);
		listener.handleBusinessModel(businessModelData);
	}
}
