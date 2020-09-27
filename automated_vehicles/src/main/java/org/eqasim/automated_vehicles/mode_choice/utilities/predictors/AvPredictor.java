package org.eqasim.automated_vehicles.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.automated_vehicles.mode_choice.utilities.variables.AvVariables;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.router.TripStructureUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.matsim.av.framework.AVModule;
import ch.ethz.matsim.av.routing.AVRoute;

public class AvPredictor extends CachedVariablePredictor<AvVariables> {
	private CostModel costModel;

	@Inject
	public AvPredictor(@Named("av") CostModel costModel) {
		this.costModel = costModel;
	}

	@Override
	public AvVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double travelTime_min = 0.0;
		double accessEgressTime_min = 0.0;
		double cost_MU = 0.0;
		double waitingTime_min = 0.0;

		for (Leg leg : TripStructureUtils.getLegs(elements)) {
			switch (leg.getMode()) {
			case TransportMode.walk:
				accessEgressTime_min += leg.getTravelTime().seconds() / 60.0;
				break;
			case AVModule.AV_MODE:
				AVRoute route = (AVRoute) leg.getRoute();

				travelTime_min = route.getInVehicleTime() / 60.0;
				waitingTime_min = route.getWaitingTime() / 60.0;

				cost_MU = costModel.calculateCost_MU(person, trip, elements);

				break;
			default:
				throw new IllegalStateException("Encountered unknown mode in AvPredictor: " + leg.getMode());
			}
		}

		double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

		return checkNaN(
				new AvVariables(travelTime_min, cost_MU, euclideanDistance_km, waitingTime_min, accessEgressTime_min));
	}

	public AvVariables checkNaN(AvVariables variables) {
		if (Double.isNaN(variables.travelTime_min) || Double.isNaN(variables.waitingTime_min)) {
			throw new IllegalStateException(
					"NaN values encountered in AVVariables. Is the AV extension set up properly?");
		}

		return variables;
	}
}
