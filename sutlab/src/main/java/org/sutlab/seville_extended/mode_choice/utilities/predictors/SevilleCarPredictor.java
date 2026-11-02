package org.sutlab.seville_extended.mode_choice.utilities.predictors;

import com.google.common.base.Verify;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.router.TripStructureUtils;
import org.sutlab.seville_extended.mode_choice.utilities.variables.SevilleCarVariables;

import java.util.List;

public class SevilleCarPredictor extends CachedVariablePredictor<SevilleCarVariables> {
	private final CostModel costModel;

	@Inject
	public SevilleCarPredictor(@Named("car") CostModel costModel) {
		this.costModel = costModel;
	}

	@Override
	public SevilleCarVariables predict(Person person, DiscreteModeChoiceTrip trip,
									   List<? extends PlanElement> elements) {
		double carTravelTime_min = 0.0;
		double accessEgressTime_min = 0.0;
		double routeDistance_km = 0.0;

		boolean foundCar = false;

		for (Leg leg : TripStructureUtils.getLegs(elements)) {
			if (leg.getMode().equals(TransportMode.car)) {
				Verify.verify(!foundCar);
				carTravelTime_min += leg.getTravelTime().seconds() / 60.0;
				routeDistance_km += leg.getRoute().getDistance() * 1e-3;
			} else if (leg.getMode().equals(TransportMode.walk)) {
				accessEgressTime_min += leg.getTravelTime().seconds() / 60.0;
			} else {
				throw new IllegalStateException("Unexpected mode in car chain: " + leg.getMode());
			}
		}

		double cost_MU = costModel.calculateCost_MU(person, trip, elements);

		// Mean speed of the car route, driving time taken as at least 1 min
		double speed_kmh = routeDistance_km / (Math.max(1.0, carTravelTime_min) / 60.0);

		return new SevilleCarVariables(carTravelTime_min, accessEgressTime_min, cost_MU, routeDistance_km, speed_kmh);
	}
}
