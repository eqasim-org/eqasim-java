package org.eqasim.switzerland.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.switzerland.mode_choice.costs.CarCostModel;
import org.eqasim.switzerland.mode_choice.parameters.ModeChoiceParameters;
import org.eqasim.switzerland.mode_choice.utilities.variables.CarVariables;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.geometry.CoordUtils;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class CarPredictor {
	private final CarCostModel costModel;
	private final ModeChoiceParameters parameters;

	public CarPredictor(ModeChoiceParameters parameters, CarCostModel costModel) {
		this.costModel = costModel;
		this.parameters = parameters;
	}

	public CarVariables predict(DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		Leg leg = (Leg) elements.get(0);

		double travelTime_min = leg.getTravelTime() / 60.0 + parameters.car.constantParkingSearchPenalty_min;
		double distance_km = leg.getRoute().getDistance() * 1e-3;

		double cost_CHF = costModel.calculate_CHF(distance_km);

		double crowflyDistance_km = CoordUtils.calcEuclideanDistance(trip.getOriginActivity().getCoord(),
				trip.getDestinationActivity().getCoord()) * 1e-3;
		double accessEgressTime_min = parameters.car.constantAccessEgressWalkTime_min;

		return new CarVariables(travelTime_min, cost_CHF, crowflyDistance_km, accessEgressTime_min);
	}
}
