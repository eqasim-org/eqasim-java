package org.eqasim.bavaria.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.bavaria.mode_choice.BavariaModeChoiceModule;
import org.eqasim.bavaria.mode_choice.utilities.variables.BavariaCarPassengerVariables;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.router.TripStructureUtils;

import com.google.common.base.Verify;

public class BavariaCarPassengerPredictor extends CachedVariablePredictor<BavariaCarPassengerVariables> {
	@Override
	public BavariaCarPassengerVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		double passengerTravelTime_min = 0.0;
		double accessEgressTime_min = 0.0;

		boolean foundCar = false;

		for (Leg leg : TripStructureUtils.getLegs(elements)) {
			if (leg.getMode().equals(BavariaModeChoiceModule.CAR_PASSENGER)) {
				Verify.verify(!foundCar);
				passengerTravelTime_min += leg.getTravelTime().seconds() / 60.0;
			} else if (leg.getMode().equals(TransportMode.walk)) {
				accessEgressTime_min += leg.getTravelTime().seconds() / 60.0;
			} else {
				throw new IllegalStateException("Unexpected mode in passenger chain: " + leg.getMode());
			}
		}

		double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

		return new BavariaCarPassengerVariables(passengerTravelTime_min, euclideanDistance_km, accessEgressTime_min);
	}
}