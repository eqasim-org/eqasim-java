package org.eqasim.sao_paulo.mode_choice.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.sao_paulo.mode_choice.parameters.SaoPauloCostParameters;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class SaoPauloTaxiCostModel extends AbstractCostModel {
	private final SaoPauloCostParameters costParameters;

	@Inject
	public SaoPauloTaxiCostModel(SaoPauloCostParameters costParameters) {
		super("taxi");

		this.costParameters = costParameters;
	}

	public double getTotalTravelTime(List<? extends PlanElement> elements) {
		double total_time = 0.0;
		String mode = "taxi";

		for (PlanElement element : elements) {
			if (element instanceof Leg) {
				Leg leg = (Leg) element;

				if (leg.getMode().contentEquals(mode)) {
					total_time += (double) leg.getRoute().getTravelTime().seconds() / 60;
				}
			}
		}
		return total_time;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {

		double pick_up_fee = costParameters.taxiPickUpFee_BRL;
		double distance_cost = costParameters.taxiCostPerkm_BRL * getInVehicleDistance_km(elements);
		double time_cost = costParameters.taxiCostPerMin_BRL * getTotalTravelTime(elements);

		return Math.max(pick_up_fee + distance_cost + time_cost, costParameters.taxMinCost_BRL);
	}

}
