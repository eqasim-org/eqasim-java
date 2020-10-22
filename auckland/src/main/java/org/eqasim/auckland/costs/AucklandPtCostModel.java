package org.eqasim.auckland.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class AucklandPtCostModel implements CostModel {
	static public final String NAME = "AucklandPtCostModel";

	private final AucklandCostParameters parameters;

	@Inject
	public AucklandPtCostModel(AucklandCostParameters parameters) {
		this.parameters = parameters;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		return parameters.ptBaseFare_MU;
	}
}
