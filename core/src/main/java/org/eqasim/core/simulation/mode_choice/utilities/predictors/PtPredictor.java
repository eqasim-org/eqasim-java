package org.eqasim.core.simulation.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.variables.PtVariables;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.pt.routes.TransitPassengerRoute;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class PtPredictor extends CachedVariablePredictor<PtVariables> {
	private CostModel costModel;

	@Inject
	public PtPredictor(@Named("pt") CostModel costModel) {
		this.costModel = costModel;
	}

	@Override
	public PtVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		/*
		 * Note: For mode choice, we do not consider any waiting time *before* the first
		 * vehicular stage. This implies that agents are able to minimize their waiting
		 * time by departing at the proper time.
		 */

		int numberOfVehicularTrips = 0;
		boolean isFirstWaitingTime = true;

		// Track relevant variables
		double inVehicleTime_min = 0.0;
		double waitingTime_min = 0.0;
		double accessEgressTime_min = 0.0;

		for (PlanElement element : elements) {
			if (element instanceof Leg) {
				Leg leg = (Leg) element;

				switch (leg.getMode()) {
				case TransportMode.walk:
				case TransportMode.non_network_walk:
					accessEgressTime_min += leg.getTravelTime().seconds() / 60.0;
					break;
				case TransportMode.transit_walk:
					waitingTime_min += leg.getTravelTime().seconds() / 60.0;
					break;
				case TransportMode.pt:
					TransitPassengerRoute route = (TransitPassengerRoute) leg.getRoute();

					double departureTime = leg.getDepartureTime().seconds();
					double waitingTime = route.getBoardingTime().seconds() - departureTime;
					double inVehicleTime = leg.getTravelTime().seconds() - waitingTime;

					inVehicleTime_min += inVehicleTime / 60.0;

					if (!isFirstWaitingTime) {
						waitingTime_min += waitingTime / 60.0;
					} else {
						isFirstWaitingTime = false;
					}

					numberOfVehicularTrips++;
					break;
				default:
					throw new IllegalStateException("Unknown mode in PT trip: " + leg.getMode());
				}
			}
		}

		int numberOfLineSwitches = Math.max(0, numberOfVehicularTrips - 1);

		// Calculate cost
		double cost_CHF = costModel.calculateCost_MU(person, trip, elements);

		double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

		return new PtVariables(inVehicleTime_min, waitingTime_min, accessEgressTime_min, numberOfLineSwitches, cost_CHF,
				euclideanDistance_km);
	}
}
