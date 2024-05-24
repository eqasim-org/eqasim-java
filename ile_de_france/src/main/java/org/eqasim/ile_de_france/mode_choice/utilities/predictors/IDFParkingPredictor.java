package org.eqasim.ile_de_france.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFParkingVariables;
import org.eqasim.ile_de_france.parking.ParkingInformation;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class IDFParkingPredictor extends CachedVariablePredictor<IDFParkingVariables> {
	private final static double LAST_DURATION = 8.0 * 3600.0;

	private final ParkingInformation parkingInformation;
	private final TimeInterpretation timeInterpretation;

	@Inject
	public IDFParkingPredictor(ParkingInformation parkingInformation, TimeInterpretation timeInterpretation) {
		this.parkingInformation = parkingInformation;
		this.timeInterpretation = timeInterpretation;
	}

	@Override
	protected IDFParkingVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		double parkingPressure = parkingInformation.getParkingPressure(trip.getDestinationActivity().getLinkId());
		double parkingCost_EUR_h = parkingInformation.getParkingCost_EUR_h(trip.getDestinationActivity(), person);

		TimeTracker timeTracker = new TimeTracker(timeInterpretation);
		timeTracker.setTime(trip.getDepartureTime());
		timeTracker.addElements(elements);

		double startTime = timeTracker.getTime().seconds();

		Activity lastActivity = (Activity) person.getSelectedPlan().getPlanElements()
				.get(person.getSelectedPlan().getPlanElements().size() - 1);

		if (trip.getDestinationActivity() == lastActivity) {
			timeTracker.addDuration(LAST_DURATION);
		} else {
			timeTracker.addActivity(trip.getDestinationActivity());
		}

		double endTime = timeTracker.getTime().seconds();
		double duration_h = (endTime - startTime) / 3600.0;

		double parkingCost_EUR = parkingCost_EUR_h * duration_h;
		return new IDFParkingVariables(parkingPressure, parkingCost_EUR);
	}
}
