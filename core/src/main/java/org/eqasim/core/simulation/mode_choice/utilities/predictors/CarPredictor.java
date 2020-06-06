package org.eqasim.core.simulation.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class CarPredictor extends CachedVariablePredictor<CarVariables> {
	private final CostModel costModel;
	private final ModeParameters parameters;

	@Inject
	public CarPredictor(ModeParameters parameters, @Named("car") CostModel costModel) {
		this.costModel = costModel;
		this.parameters = parameters;
	}

	@Override
	public CarVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		if (elements.size() > 1) {
			throw new IllegalStateException("We do not support multi-stage car trips yet.");
		}

		Leg leg = (Leg) elements.get(0);

		double travelTime_min = leg.getTravelTime().seconds() / 60.0 + parameters.car.constantParkingSearchPenalty_min;
		double cost_MU = costModel.calculateCost_MU(person, trip, elements);

		double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);
		double accessEgressTime_min = parameters.car.constantAccessEgressWalkTime_min;

		return new CarVariables(travelTime_min, cost_MU, euclideanDistance_km, accessEgressTime_min);
	}
}
