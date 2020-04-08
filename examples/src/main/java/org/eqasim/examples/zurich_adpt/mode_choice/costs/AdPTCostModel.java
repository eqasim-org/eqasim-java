package org.eqasim.examples.zurich_adpt.mode_choice.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class AdPTCostModel extends AbstractCostModel {

	@Inject
	public AdPTCostModel(String mode) {
		super(mode);
		// TODO Auto-generated constructor stub
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		// TODO Auto-generated method stub
		return 0;
	}

}
