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
		double travelTime_min = 0.0;
		double accessEgressTime_min = 0.0;

		for (PlanElement element : elements) {
			if (element instanceof Leg) {


				// element.getAttributes().getAttribute("parkingStrategy");

				if (((Leg) element).getMode().equals("car")) {
					travelTime_min += ((Leg) element).getTravelTime().seconds() / 60.0;
				} else if (((Leg) element).getMode().equals("walk")) {
					accessEgressTime_min += ((Leg) element).getTravelTime().seconds() / 60.0;
				}
			}
		}

		double arrivalTime = trip.getDepartureTime() + travelTime_min * 60.0;
		Coord destinationCoord = trip.getDestinationActivity().getCoord();

		double parkingSearchTime_min = parkingListener.getParkingSearchTimeAtCoordAtTime(destinationCoord, arrivalTime) / 60.0;
		double cost_MU = costModel.calculateCost_MU(person, trip, elements);

		// compute parking costs based on destination activity duration
		double actEndTime = parkingListener.getEndTime();
		if (trip.getDestinationActivity().getEndTime().isDefined()) {
			actEndTime = trip.getDestinationActivity().getEndTime().seconds();
		}

		double nextActivityDuration = actEndTime - arrivalTime;
		double parkingCost_MU = nextActivityDuration / 3600.0 * 1.0;

		double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

		return new CorsicaParkingCarVariables(travelTime_min, parkingSearchTime_min, cost_MU, parkingCost_MU, euclideanDistance_km, accessEgressTime_min);
	}

}
