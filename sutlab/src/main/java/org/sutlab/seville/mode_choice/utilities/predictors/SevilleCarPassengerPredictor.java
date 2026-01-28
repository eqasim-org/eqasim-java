package org.sutlab.seville.mode_choice.utilities.predictors;

import com.google.common.base.Verify;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.router.TripStructureUtils;
import org.sutlab.seville.mode_choice.SevilleModeChoiceModule;
import org.sutlab.seville.mode_choice.utilities.variables.SevilleCarPassengerVariables;

import java.util.List;

public class SevilleCarPassengerPredictor extends CachedVariablePredictor<SevilleCarPassengerVariables> {
	@Override
	public SevilleCarPassengerVariables predict(Person person, DiscreteModeChoiceTrip trip,
												List<? extends PlanElement> elements) {
		double passengerTravelTime_min = 0.0;
		double accessEgressTime_min = 0.0;

		boolean foundCar = false;

		for (Leg leg : TripStructureUtils.getLegs(elements)) {
			if (leg.getMode().equals(SevilleModeChoiceModule.CAR_PASSENGER)) {
				Verify.verify(!foundCar);
				passengerTravelTime_min += leg.getTravelTime().seconds() / 60.0;
			} else if (leg.getMode().equals(TransportMode.walk)) {
				accessEgressTime_min += leg.getTravelTime().seconds() / 60.0;
			} else {
				throw new IllegalStateException("Unexpected mode in passenger chain: " + leg.getMode());
			}
		}

		double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

		return new SevilleCarPassengerVariables(passengerTravelTime_min, euclideanDistance_km, accessEgressTime_min);
	}
}