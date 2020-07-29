package org.eqasim.core.simulation.mode_choice.utilities.predictors;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import java.util.List;

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
		if (elements.size() == 1) { // basic case

			Leg leg = (Leg) elements.get(0);

			double travelTime_min = leg.getTravelTime() / 60.0 + parameters.car.constantParkingSearchPenalty_min;
			double cost_MU = costModel.calculateCost_MU(person, trip, elements);

			double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);
			double accessEgressTime_min = parameters.car.constantAccessEgressWalkTime_min;

			return new CarVariables(travelTime_min, cost_MU, euclideanDistance_km, accessEgressTime_min);

		} else if (elements.size() == 5) { // case where we have access and egress legs

			Leg legAccess = (Leg) elements.get(0);
			Leg legCar = (Leg) elements.get(2);
			Leg legEgress = (Leg) elements.get(4);
			double travelTime_min = legCar.getTravelTime() / 60.0 + parameters.car.constantParkingSearchPenalty_min;
			double cost_MU = costModel.calculateCost_MU(person, trip, elements);

			double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);
			double accessEgressTime_min = legAccess.getTravelTime() / 60.0 + legEgress.getTravelTime() / 60.0;

			return new CarVariables(travelTime_min, cost_MU, euclideanDistance_km, accessEgressTime_min);

		} else { // other cases

			throw new IllegalStateException("We do not support multi-stage car trips yet.");

		}
	}
}
