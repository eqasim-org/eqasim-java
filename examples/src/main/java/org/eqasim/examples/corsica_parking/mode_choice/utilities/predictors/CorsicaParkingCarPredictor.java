package org.eqasim.examples.corsica_parking.mode_choice.utilities.predictors;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.examples.corsica_parking.components.parking.ParkingListener;
import org.eqasim.examples.corsica_parking.mode_choice.utilities.variables.CorsicaParkingCarVariables;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class CorsicaParkingCarPredictor extends CachedVariablePredictor<CorsicaParkingCarVariables> {
	private final CostModel costModel;
	private final ModeParameters parameters;
	private final ParkingListener parkingListener;

	@Inject
	public CorsicaParkingCarPredictor(ModeParameters parameters, @Named("car") CostModel costModel,
									  ParkingListener parkingListener) {
		this.costModel = costModel;
		this.parameters = parameters;
		this.parkingListener = parkingListener;
	}

	@Override
	public CorsicaParkingCarVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		if (elements.size() > 5) {
			throw new IllegalStateException("Car trips should at most contain 5 stages: 3 legs (walk, car, walk) and 2 interaction activities.");
		}

		double travelTime_min = 0.0;
		double travelCost_MU = 0.0;
		double accessEgressTime_min = 0.0;
		double euclideanDistance_km = 0.0;
		double parkingSearchTime_min = 0.0;
		double parkingCost_MU = 0.0;

		// case of single walk leg
		if (elements.size() == 1) {
			Leg leg = (Leg) elements.get(0);
			accessEgressTime_min = leg.getTravelTime().seconds() / 60.0;
		}

		// case of car leg surrounded by 2 walk legs
		else if (elements.size() == 5) {
			Leg accessWalk = (Leg) elements.get(0);
			Leg carLeg = (Leg) elements.get(2);
			Leg egressWalk = (Leg) elements.get(4);

			accessEgressTime_min = (accessWalk.getTravelTime().seconds() + egressWalk.getTravelTime().seconds()) / 60.0;
			travelTime_min = carLeg.getTravelTime().seconds() / 60.0;
			travelCost_MU = costModel.calculateCost_MU(person, trip, elements);
			euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);
		}

		else {
			throw new IllegalStateException("Car trip contains " + elements.size() + " legs! Not sure what to do here.");
		}

		// Determine whether we have dedicated parking
		boolean hasParking = false;
		Activity destinationActivity = trip.getDestinationActivity();
		if (destinationActivity.getType().equals("home") | destinationActivity.getType().equals("work")) {
			hasParking = Boolean.parseBoolean(destinationActivity.getAttributes().getAttribute("parkingAvailable").toString());
		}

		// if they do not have parking, they need to search and pay for it
		if (!hasParking) {
			// estimate parking search time
			double arrivalTime = trip.getDepartureTime() + travelTime_min * 60.0;
			Coord destinationCoord = trip.getDestinationActivity().getCoord();
			parkingSearchTime_min = parkingListener.getParkingSearchTimeAtCoordAtTime(destinationCoord, arrivalTime) / 60.0;

			// compute parking costs based on destination activity duration
			double actEndTime = parkingListener.getEndTime();
			if (trip.getDestinationActivity().getEndTime().isDefined()) {
				actEndTime = trip.getDestinationActivity().getEndTime().seconds();
			}
			double nextActivityDuration = actEndTime - arrivalTime;
			parkingCost_MU = nextActivityDuration / 3600.0 * 1.0;
		}

		return new CorsicaParkingCarVariables(travelTime_min, parkingSearchTime_min,  travelCost_MU, parkingCost_MU,
				euclideanDistance_km, accessEgressTime_min);

	}
}
