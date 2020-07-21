package org.eqasim.san_francisco.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class SanFranciscoCarPredictor extends CachedVariablePredictor<CarVariables> {
	private final CostModel costModel;
	private final ModeParameters parameters;

	@Inject
	public SanFranciscoCarPredictor(ModeParameters parameters, @Named("car") CostModel costModel) {
		this.costModel = costModel;
		this.parameters = parameters;
	}

	@Override
	public CarVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {

		if (elements.size() != 5) {
			System.out.println("Number of elements: " + elements.size());
			System.out.println(elements.toString());
		}

		Leg legAccess = (Leg) elements.get(0);
		Leg legCar = (Leg) elements.get(2);
		Leg legEgress = (Leg) elements.get(4);
		double travelTime_min = legCar.getTravelTime() / 60.0 + parameters.car.constantParkingSearchPenalty_min;
		double cost_MU = costModel.calculateCost_MU(person, trip, elements);

		double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);
		double accessEgressTime_min = legAccess.getTravelTime() / 60.0 + legEgress.getTravelTime() / 60.0;

		return new CarVariables(travelTime_min, cost_MU, euclideanDistance_km, accessEgressTime_min);
	}
}
