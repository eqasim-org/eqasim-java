package org.eqasim.ile_de_france.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFPtVariables;
import org.eqasim.ile_de_france.routing.IDFRaptorUtils;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class IDFPtPredictor extends CachedVariablePredictor<IDFPtVariables> {
	private final CostModel costModel;
	private final TransitSchedule schedule;

	@Inject
	public IDFPtPredictor(@Named("pt") CostModel costModel, TransitSchedule schedule) {
		this.costModel = costModel;
		this.schedule = schedule;
	}

	@Override
	public IDFPtVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		/*
		 * Note: For mode choice, we do not consider any waiting time *before* the first
		 * vehicular stage. This implies that agents are able to minimize their waiting
		 * time by departing at the proper time.
		 */

		int numberOfVehicularTrips = 0;
		int numberOfBusTrips = 0;

		boolean isFirstWaitingTime = true;

		// Track relevant variables
		double inVehicleTime_min = 0.0;
		double waitingTime_min = 0.0;
		double initialWaitingTime_min = 0.0;
		double accessEgressTime_min = 0.0;

		for (PlanElement element : elements) {
			if (element instanceof Leg) {
				Leg leg = (Leg) element;

				if (leg.getMode().contains("walk")) {
					accessEgressTime_min += leg.getTravelTime().seconds() / 60.0;
				} else if (leg.getMode().startsWith(IDFRaptorUtils.PT_MODE_PREFIX)) {
					TransitPassengerRoute route = (TransitPassengerRoute) leg.getRoute();

					double departureTime = leg.getDepartureTime().seconds();
					double waitingTime = route.getBoardingTime().seconds() - departureTime;
					double inVehicleTime = leg.getTravelTime().seconds() - waitingTime;

					inVehicleTime_min += inVehicleTime / 60.0;

					if (isFirstWaitingTime) {
						initialWaitingTime_min = waitingTime;
						isFirstWaitingTime = false;
					} else {
						waitingTime_min += waitingTime / 60.0;
					}

					if (getTransportMode(route).equals("bus")) {
						numberOfBusTrips++;
					}

					numberOfVehicularTrips++;
				} else {
					throw new IllegalStateException("Unknown mode in PT trip: " + leg.getMode());
				}
			}
		}

		int numberOfLineSwitches = Math.max(0, numberOfVehicularTrips - 1);

		// Calculate cost
		double cost_CHF = costModel.calculateCost_MU(person, trip, elements);

		double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

		// Get headway
		double headway_min = IDFPredictorUtils.getHeadway_min(trip.getOriginActivity());

		// Only bus
		boolean isOnlyBus = numberOfBusTrips > 0 && numberOfBusTrips == numberOfVehicularTrips;

		return new IDFPtVariables(inVehicleTime_min, waitingTime_min, accessEgressTime_min, numberOfLineSwitches,
				cost_CHF, euclideanDistance_km, headway_min, isOnlyBus, initialWaitingTime_min);
	}

	private String getTransportMode(TransitPassengerRoute passengerRoute) {
		return schedule.getTransitLines().get(passengerRoute.getLineId()).getRoutes().get(passengerRoute.getRouteId())
				.getTransportMode();
	}
}
