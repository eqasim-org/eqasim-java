package org.eqasim.sao_paulo.mode_choice.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.costs.AbstractCostModel;
import org.eqasim.sao_paulo.mode_choice.utilities.predictors.SaoPauloPersonPredictor;
import org.eqasim.sao_paulo.mode_choice.utilities.variables.SaoPauloPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class SaoPauloPtCostModel extends AbstractCostModel {
	private final SaoPauloPersonPredictor personPredictor;

	@Inject
	public SaoPauloPtCostModel(SaoPauloPersonPredictor personPredictor) {
		super("pt");

		this.personPredictor = personPredictor;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		SaoPauloPersonVariables personVariables = personPredictor.predictVariables(person);

		if (personVariables.hasSubscription) {
			return 0.0;
		}

		return 3.8;
	}
}
