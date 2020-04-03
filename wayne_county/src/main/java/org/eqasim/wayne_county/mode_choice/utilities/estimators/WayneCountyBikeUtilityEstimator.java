package org.eqasim.wayne_county.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.BikeUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.BikePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.BikeVariables;
import org.eqasim.wayne_county.mode_choice.parameters.WayneCountyModeParameters;
import org.eqasim.wayne_county.mode_choice.utilities.predictors.WayneCountyPersonPredictor;
import org.eqasim.wayne_county.mode_choice.utilities.variables.WayneCountyPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class WayneCountyBikeUtilityEstimator extends BikeUtilityEstimator {
	private final WayneCountyModeParameters parameters;
	private final WayneCountyPersonPredictor predictor;
	private BikePredictor bikePredictor;

	@Inject
	public WayneCountyBikeUtilityEstimator(WayneCountyModeParameters parameters, PersonPredictor personPredictor, 
			BikePredictor bikePredictor, WayneCountyPersonPredictor predictor) {
		super(parameters, personPredictor, bikePredictor);

		this.parameters = parameters;
		this.predictor = predictor;
		this.bikePredictor = bikePredictor;
	}
	
	protected double estimateConstantUtility(WayneCountyPersonVariables variables) {
		switch (variables.hhlIncomeClass) {
		case 1:
			return parameters.wcBike.alpha_low_income;
		case 2:
			return parameters.wcBike.alpha_medium_income;
		case 3:
			return parameters.wcBike.alpha_high_income;
		default:
			return 2;
		}
	}

	protected double estimateTravelTime(BikeVariables variables) {
		return parameters.wcBike.beta_time_min * variables.travelTime_min;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		WayneCountyPersonVariables variables = predictor.predictVariables(person, trip, elements);
		BikeVariables variables_bike = bikePredictor.predictVariables(person, trip, elements);

		double utility = 0.0;
		
		utility += estimateConstantUtility(variables);
		utility += estimateTravelTime(variables_bike);
		
		return utility;
	}
}
