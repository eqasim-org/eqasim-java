package org.eqasim.bavaria.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.bavaria.mode_choice.parameters.BavariaModeParameters;
import org.eqasim.core.simulation.modes.drt.mode_choice.predictors.DrtPredictor;
import org.eqasim.core.simulation.modes.drt.mode_choice.utilities.estimators.DrtUtilityEstimator;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class BavariaDrtUtilityEstimator extends DrtUtilityEstimator {
	private final BavariaModeParameters parameters;

	@Inject
	public BavariaDrtUtilityEstimator(BavariaModeParameters parameters, DrtPredictor predictor) {
		super(parameters, predictor);
		this.parameters = parameters;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double utility = super.estimateUtility(person, trip, elements);

		// customization can go here

		return utility;
	}
}
