package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.estimators;

import com.google.inject.Inject;
import org.eqasim.core.components.calibration.writer.VariablesWriter;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.CarUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissModeParameters;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SwissCarUtilityEstimator extends CarUtilityEstimator {
	private final SwissModeParameters parameters;
	private final SwissPersonPredictor predictor;
	private final CarPredictor carPredictor;

	@Inject
	public SwissCarUtilityEstimator(SwissModeParameters parameters, CarPredictor carPredictor,
                                    SwissPersonPredictor personPredictor) {
		super(parameters, carPredictor);

		this.predictor = personPredictor;
		this.parameters = parameters;
		this.carPredictor = carPredictor;
	}

	protected double estimateRegionalUtility(SwissPersonVariables variables) {
		if (variables.statedPreferenceRegion == 1) {
			return parameters.swissCar.betaStatedPreferenceRegion1_u;
		} else if (variables.statedPreferenceRegion == 3) {
			return parameters.swissCar.betaStatedPreferenceRegion3_u;
		} else {
			return 0.0;
		}
	}

	protected double estimateCantonUtility(Person person) {
		Object cantonObj = person.getAttributes().getAttribute("cantonName");
		if (cantonObj instanceof String canton) {
            return parameters.swissCanton.car.getOrDefault(canton, 0.0);
		}
		return 0.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		SwissPersonVariables personVariables = predictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += super.estimateUtility(person, trip, elements);
		utility += estimateRegionalUtility(personVariables);
		utility += estimateCantonUtility(person);

		if(VariablesWriter.isInitiated()) {
			CarVariables carVariable = carPredictor.predictVariables(person, trip, elements);
			writeVariablesToCsv(person, trip, carVariable, personVariables, utility);
		}

		return utility;
	}

	private void writeVariablesToCsv(Person person, DiscreteModeChoiceTrip trip, CarVariables carVariable,
                                     SwissPersonVariables personVariables, double utility) {
		double departureTime = trip.getDepartureTime();
		int tripIndex = trip.getIndex();
		String personId = person.getId().toString();

		Map<String, String> carAttributes = new HashMap<>();
		carAttributes.put("travelTime_min", String.valueOf(carVariable.travelTime_min));
		carAttributes.put("accessEgressTime_min", String.valueOf(carVariable.accessEgressTime_min));
		carAttributes.put("euclideanDistance_km", String.valueOf(carVariable.euclideanDistance_km));
		carAttributes.put("cost_MU", String.valueOf(carVariable.cost_MU));
		carAttributes.put("statedPreferenceRegion", String.valueOf(personVariables.statedPreferenceRegion));

		VariablesWriter.writeVariables("car", personId, tripIndex, departureTime, utility, carAttributes);
	}
}