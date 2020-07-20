package org.eqasim.jakarta.mode_choice.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.jakarta.mode_choice.parameters.JakartaCostParameters;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class JakartaMcodtCostModel extends AbstractCostModel {
	private final JakartaCostParameters costParameters;

	@Inject
	public JakartaMcodtCostModel(JakartaCostParameters costParameters) {
		super("mcodt");

		this.costParameters = costParameters;
	}
	
	public double getTotalTravelTime(List<? extends PlanElement> elements) {
		double total_time = 0.0;
		String mode = "mcodt";
		
		for (PlanElement element : elements) {
			if (element instanceof Leg) {
				Leg leg = (Leg) element;

				if (leg.getMode().contentEquals(mode)) {
					total_time += (double) leg.getRoute().getTravelTime() / 60;
				}
			}
		}
		return total_time;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		
		double pick_up_fee = costParameters.mcodtPickUpFee_KIDR;
		double distance_cost = costParameters.mcodtCostPerkm_KIDR * getInVehicleDistance_km(elements);
		//double time_cost = costParameters.taxiCostPerMin_BRL * getTotalTravelTime(elements);
		
		return Math.max(pick_up_fee + distance_cost, costParameters.mcodtMinCost_KIDR);
	}

}
