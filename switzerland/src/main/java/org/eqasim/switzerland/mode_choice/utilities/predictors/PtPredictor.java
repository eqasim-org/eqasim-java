package org.eqasim.switzerland.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.components.transit.routing.EnrichedTransitRoute;
import org.eqasim.switzerland.mode_choice.costs.PtCostModel;
import org.eqasim.switzerland.mode_choice.utilities.variables.PersonVariables;
import org.eqasim.switzerland.mode_choice.utilities.variables.PtVariables;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.geometry.CoordUtils;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class PtPredictor {
	private PtCostModel costModel;

	public PtPredictor(PtCostModel costModel) {
		this.costModel = costModel;
	}

	public PtVariables predict(PersonVariables personVariables, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
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

		double inVehicleDistance_km = 0.0;

		for (PlanElement element : elements) {
			if (element instanceof Leg) {
				Leg leg = (Leg) element;

				switch (leg.getMode()) {
				case TransportMode.access_walk:
				case TransportMode.egress_walk:
					accessEgressTime_min += leg.getTravelTime() / 60.0;
					break;
				case TransportMode.transit_walk:
					waitingTime_min += leg.getTravelTime() / 60.0;
					break;
				case TransportMode.pt:
					EnrichedTransitRoute route = (EnrichedTransitRoute) leg.getRoute();

					inVehicleTime_min += route.getInVehicleTime() / 60.0;

					if (!isFirstWaitingTime) {
						waitingTime_min += route.getWaitingTime() / 60.0;
					} else {
						isFirstWaitingTime = false;
					}

					inVehicleDistance_km += route.getDistance() / 1000.0;
					numberOfVehicularTrips++;
					break;
				default:
					throw new IllegalStateException("Unknown mode in PT trip: " + leg.getMode());
				}
			}
		}

		int numberOfLineSwitches = Math.max(0, numberOfVehicularTrips - 1);

		// Calculate cost
		double originHomeDistance_km = CoordUtils.calcEuclideanDistance(personVariables.homeLocation,
				trip.getOriginActivity().getCoord()) * 1e-3;
		double destinationHomeDistance_km = CoordUtils.calcEuclideanDistance(personVariables.homeLocation,
				trip.getDestinationActivity().getCoord()) * 1e-3;
		double homeDistance = Math.max(originHomeDistance_km, destinationHomeDistance_km);

		double crowflyDistance_km = CoordUtils.calcEuclideanDistance(trip.getOriginActivity().getCoord(),
				trip.getDestinationActivity().getCoord()) * 1e-3;

		double cost_CHF = costModel.calculate_CHF(personVariables, inVehicleDistance_km, homeDistance,
				crowflyDistance_km);

		return new PtVariables(inVehicleTime_min, waitingTime_min, accessEgressTime_min, numberOfLineSwitches, cost_CHF,
				crowflyDistance_km);
	}
}
