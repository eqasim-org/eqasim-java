package org.eqasim.san_francisco.mode_choice.utilities.estimators;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.BikeUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.BikePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.BikeVariables;
import org.eqasim.san_francisco.mode_choice.parameters.SanFranciscoModeParameters;
import org.eqasim.san_francisco.mode_choice.utilities.predictors.SanFranciscoBikePredictor;
import org.eqasim.san_francisco.mode_choice.utilities.predictors.SanFranciscoPersonPredictor;
import org.eqasim.san_francisco.mode_choice.utilities.variables.SanFranciscoPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import java.util.List;

public class SanFranciscoBikeUtilityEstimator extends BikeUtilityEstimator {
	private final SanFranciscoModeParameters parameters;
	private final SanFranciscoPersonPredictor sanFranciscoPersonPredictor;
	private SanFranciscoBikePredictor sanFranciscoBikePredictor;

	@Inject
	public SanFranciscoBikeUtilityEstimator(SanFranciscoModeParameters parameters, PersonPredictor personPredictor,
											BikePredictor bikePredictor,
											SanFranciscoBikePredictor sanFranciscoBikePredictor,
											SanFranciscoPersonPredictor sanFranciscoPersonPredictor) {
		super(parameters, personPredictor, bikePredictor);

		this.parameters = parameters;
		this.sanFranciscoPersonPredictor = sanFranciscoPersonPredictor;
		this.sanFranciscoBikePredictor = sanFranciscoBikePredictor;
	}

	protected double estimateRegionalUtility(SanFranciscoPersonVariables variables) {
		return (variables.cityTrip) ? parameters.sfBike.alpha_bike_city : 0.0;
	}

	protected double estimateTravelTime(BikeVariables variables) {
		return parameters.sfBike.vot_min * variables.travelTime_min;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		SanFranciscoPersonVariables variables = sanFranciscoPersonPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		BikeVariables variables_bike = sanFranciscoBikePredictor.predictVariables(person, trip, elements);

		utility += estimateConstantUtility();
		utility += estimateTravelTime(variables_bike) * (parameters.sfAvgHHLIncome.avg_hhl_income / variables.hhlIncome)
				* parameters.betaCost_u_MU;
		utility += estimateRegionalUtility(variables);

		return utility;
	}
}
