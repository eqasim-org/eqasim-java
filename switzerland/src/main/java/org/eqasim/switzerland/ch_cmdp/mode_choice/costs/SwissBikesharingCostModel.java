package org.eqasim.switzerland.ch_cmdp.mode_choice.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissBikesharingCostParameters;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
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
		double travelTime_min = 0.0;
		double distance_km = 0.0;

		for (PlanElement element : elements) {
			if (element instanceof Leg) {
				Leg leg = (Leg) element;

				if (leg.getMode().equals(TransportMode.bike)) {
					travelTime_min += leg.getTravelTime().seconds() / 60.0;
					distance_km += leg.getRoute().getDistance() * 1e-3;
				}
			}
		}

		double cost = parameters.CHF_base + parameters.CHF_min * travelTime_min + parameters.CHF_km * distance_km;
		return Math.max(parameters.CHF_minimum, cost);
    }

}
