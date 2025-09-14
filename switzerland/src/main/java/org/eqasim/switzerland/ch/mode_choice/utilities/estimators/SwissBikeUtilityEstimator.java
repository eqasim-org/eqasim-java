package org.eqasim.switzerland.ch.mode_choice.utilities.estimators;

import com.google.inject.Inject;
import org.eqasim.core.components.calibration.VariablesWriter;
import org.eqasim.core.components.calibration.writer.StandardVariablesWriter;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.BikeUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.BikePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.variables.BikeVariables;
import org.eqasim.switzerland.ch.mode_choice.parameters.SwissModeParameters;
import org.eqasim.switzerland.ch.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.ch.mode_choice.utilities.variables.SwissPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SwissBikeUtilityEstimator extends BikeUtilityEstimator {
	private final SwissModeParameters parameters;
	private final SwissPersonPredictor personPredictor;
	private final BikePredictor bikePredictor;
	private final VariablesWriter variablesWriter;

	@Inject
	public SwissBikeUtilityEstimator(SwissModeParameters parameters, SwissPersonPredictor personPredictor,
									 BikePredictor bikePredictor, VariablesWriter variablesWriter) {
		super(parameters, personPredictor.delegate, bikePredictor);

		this.parameters = parameters;
		this.personPredictor = personPredictor;
		this.bikePredictor = bikePredictor;
		this.variablesWriter = variablesWriter;
	}

	protected double estimateRegionalUtility(SwissPersonVariables variables) {
		return (variables.statedPreferenceRegion == 3) ? parameters.swissBike.betaStatedPreferenceRegion3_u : 0.0;
	}

	protected double estimateCantonUtility(Person person) {
		Object cantonObj = person.getAttributes().getAttribute("cantonName");
		if (cantonObj instanceof String canton) {
			return parameters.swissCanton.bike.getOrDefault(canton, 0.0);
		}
		return 0.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		SwissPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += super.estimateUtility(person, trip, elements);
		utility += estimateRegionalUtility(personVariables);
		utility += estimateCantonUtility(person);

		if(variablesWriter.isInitiated()) {
			BikeVariables bikeVariables = bikePredictor.predictVariables(person, trip, elements);
			writeVariablesToCsv(person, trip, bikeVariables, personVariables, utility);
		}
		return utility;
	}

	private void writeVariablesToCsv(Person person, DiscreteModeChoiceTrip trip, BikeVariables bikevariable,
									 SwissPersonVariables personVariables, double utility) {
		double departureTime = trip.getDepartureTime();
		int tripIndex = trip.getIndex();
		String personId = person.getId().toString();
		double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

		Map<String, String> bikeAttributes = new HashMap<>();
		bikeAttributes.put("statedPreferenceRegion", String.valueOf(personVariables.statedPreferenceRegion));
		bikeAttributes.put("travelTime_min", String.valueOf(bikevariable.travelTime_min));
		bikeAttributes.put("age_a", String.valueOf(personVariables.age_a));
		bikeAttributes.put("euclideanDistance_km", String.valueOf(euclideanDistance_km));

		variablesWriter.writeVariables("bike", personId, tripIndex, departureTime, utility, bikeAttributes);
	}
}