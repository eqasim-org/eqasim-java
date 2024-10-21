package org.eqasim.ile_de_france.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFPtVariables;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.inject.Inject;

public class IDFPtPredictor extends CachedVariablePredictor<IDFPtVariables> {
	static public final String PARIS_ATTRIBUTE = "isParis";

	private final TransitSchedule schedule;

	@Inject
	public IDFPtPredictor(TransitSchedule schedule) {
		this.schedule = schedule;
	}

	protected CostModel getCostModel() {
		return null;
	}

	@Override
	public IDFPtVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		int numberOfVehicularLegs = 0;

		// Track relevant variables (from standard estimator)
		double inVehicleTime_min = 0.0;
		double waitingTime_min = 0.0;
		double accessEgressTime_min = 0.0;

		// Track IDF variables
		int busCount = 0;
		int subwayCount = 0;
		int otherCount = 0;

		TransitPassengerRoute firstRoute = null;
		TransitPassengerRoute lastRoute = null;

		for (PlanElement element : elements) {
			if (element instanceof Leg leg) {
				switch (leg.getMode()) {
				case TransportMode.walk:
				case TransportMode.non_network_walk:
					accessEgressTime_min += leg.getTravelTime().seconds() / 60.0;
					break;
				case TransportMode.transit_walk:
					// different than standard estimator
					accessEgressTime_min += leg.getTravelTime().seconds() / 60.0;
					break;
				case TransportMode.pt:
					TransitPassengerRoute route = (TransitPassengerRoute) leg.getRoute();

					double departureTime = leg.getDepartureTime().seconds();
					double waitingTime = route.getBoardingTime().seconds() - departureTime;
					double inVehicleTime = leg.getTravelTime().seconds() - waitingTime;

					inVehicleTime_min += inVehicleTime / 60.0;
					waitingTime_min += waitingTime / 60.0;

					numberOfVehicularLegs++;
					break;
				default:
					throw new IllegalStateException("Unknown mode in PT trip: " + leg.getMode());
				}
				
				if (leg.getRoute() instanceof TransitPassengerRoute route) {
					TransitLine transitLine = schedule.getTransitLines().get(route.getLineId());
					TransitRoute transitRoute = transitLine.getRoutes().get(route.getRouteId());
					String transportMode = transitRoute.getTransportMode();

					if (transportMode.equals("bus")) {
						busCount++;
					} else if (transportMode.equals("subway")) {
						subwayCount++;
					} else {
						otherCount++;
					}

					if (firstRoute == null) {
						firstRoute = route;
					}

					lastRoute = route;
				}
			}
		}

		int numberOfLineSwitches = Math.max(0, numberOfVehicularLegs - 1);

		double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

		boolean isOnlyBus = busCount > 0 && subwayCount == 0 && otherCount == 0;
		boolean hasOnlySubwayAndBus = (busCount > 0 || subwayCount > 0) && otherCount == 0;

		boolean isWithinParis = false;

		if (firstRoute != null) {
			TransitStopFacility startFacility = schedule.getFacilities().get(firstRoute.getAccessStopId());
			TransitStopFacility endFacility = schedule.getFacilities().get(lastRoute.getEgressStopId());

			Boolean startParis = (Boolean) startFacility.getAttributes().getAttribute(PARIS_ATTRIBUTE);
			Boolean endParis = (Boolean) endFacility.getAttributes().getAttribute(PARIS_ATTRIBUTE);

			isWithinParis = startParis != null && endParis != null && startParis && endParis;
		}

		return new IDFPtVariables(inVehicleTime_min, waitingTime_min, accessEgressTime_min, numberOfLineSwitches,
				euclideanDistance_km, isOnlyBus, hasOnlySubwayAndBus, isWithinParis);
	}
}