package org.eqasim.core.simulation.mode_choice.utilities.predictors;

import com.google.common.base.Verify;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.utilities.variables.MotorcycleVariables;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.router.TripStructureUtils;

import java.util.List;

public class MotorcyclePredictor extends CachedVariablePredictor<MotorcycleVariables> {
	private final CostModel costModel;
	private final ModeParameters parameters;

	@Inject
	public MotorcyclePredictor(ModeParameters parameters, @Named("motorcycle") CostModel costModel) {
		this.costModel = costModel;
		this.parameters = parameters;
	}

	@Override
	public MotorcycleVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double motorcycleTravelTime_min = parameters.motorcycle.constantParkingSearchPenalty_min;
		double accessEgressTime_min = parameters.motorcycle.additionalAccessEgressWalkTime_min;

		boolean foundCar = false;

		for (Leg leg : TripStructureUtils.getLegs(elements)) {
			if (leg.getMode().equals(TransportMode.motorcycle)) {
				Verify.verify(!foundCar);
				motorcycleTravelTime_min += leg.getTravelTime().seconds() / 60.0;
			} else if (leg.getMode().equals(TransportMode.walk)) {
				accessEgressTime_min += leg.getTravelTime().seconds() / 60.0;
			} else {
				throw new IllegalStateException("Unexpected mode in car chain: " + leg.getMode());
			}
		}

		double cost_MU = costModel.calculateCost_MU(person, trip, elements);
		double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

		return new MotorcycleVariables(motorcycleTravelTime_min, cost_MU, euclideanDistance_km, accessEgressTime_min);
	}
}
