package org.eqasim.projects.astra16.mode_choice.predictors;

import java.util.List;

import org.eqasim.automated_vehicles.mode_choice.utilities.predictors.AvPredictor;
import org.eqasim.automated_vehicles.mode_choice.utilities.variables.AvVariables;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class AstraAvPredictor extends AvPredictor {
	@Inject
	public AstraAvPredictor(@Named("av") CostModel costModel) {
		super(costModel);
	}

	@Override
	public AvVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		AvVariables delegateVariables = super.predict(person, trip, elements);

		// Adjust for pickup and dropoff time which otherwise is not considered
		double updatedTravelTime_min = delegateVariables.travelTime_min + 2.0 + 1.0;

		return new AvVariables(updatedTravelTime_min, delegateVariables.cost_MU, delegateVariables.euclideanDistance_km,
				delegateVariables.waitingTime_min, delegateVariables.accessEgressTime_min);
	}
}
