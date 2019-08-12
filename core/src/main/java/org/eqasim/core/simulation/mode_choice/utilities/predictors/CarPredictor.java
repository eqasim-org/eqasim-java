package org.eqasim.core.simulation.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.costs.CostModel;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class CarPredictor {
	private final CostModel costModel;
	private final ModeParameters parameters;

	@Inject
	public CarPredictor(ModeParameters parameters, @Named("car") CostModel costModel) {
		this.costModel = costModel;
		this.parameters = parameters;
	}

	public CarVariables predictVariables(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		if (elements.size() > 1) {
			throw new IllegalStateException("We do not support intermodal car trips yet.");
		}

		Leg leg = (Leg) elements.get(0);

		double travelTime_min = leg.getTravelTime() / 60.0 + parameters.car.constantParkingSearchPenalty_min;

		double cost_CHF = costModel.calculateCost_MU(person, trip, elements);

		double euclideanDistance_km = PredictorUtils.getEuclideanDistance_km(trip);
		double accessEgressTime_min = parameters.car.constantAccessEgressWalkTime_min;

		return new CarVariables(travelTime_min, cost_CHF, euclideanDistance_km, accessEgressTime_min);
	}
}
