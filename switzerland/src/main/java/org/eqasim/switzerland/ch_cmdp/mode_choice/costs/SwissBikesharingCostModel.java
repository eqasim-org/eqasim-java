package org.eqasim.switzerland.ch_cmdp.mode_choice.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissBikesharingCostParameters;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class SwissBikesharingCostModel extends AbstractCostModel {
	private final SwissBikesharingCostParameters parameters;

    @Inject
	public SwissBikesharingCostModel(SwissBikesharingCostParameters costParameters) {
		super("bikesharing");
		this.parameters = costParameters;
	}

    @Override
    public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        double cost = 0.0;
        return cost;
    }

}
