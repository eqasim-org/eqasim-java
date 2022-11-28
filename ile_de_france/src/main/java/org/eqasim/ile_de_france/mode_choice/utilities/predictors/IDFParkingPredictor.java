package org.eqasim.ile_de_france.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFParkingVariables;
import org.eqasim.ile_de_france.parking.ParkingInformation;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.replanning.time_interpreter.TimeInterpreter;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class IDFParkingPredictor extends CachedVariablePredictor<IDFParkingVariables> {
	private final ParkingInformation parkingInformation;
	private final TimeInterpreter.Factory timeInterpreterFactory;

	@Inject
	public IDFParkingPredictor(ParkingInformation parkingInformation, TimeInterpreter.Factory timeInterpreterFactory) {
		this.parkingInformation = parkingInformation;
		this.timeInterpreterFactory = timeInterpreterFactory;
	}

	@Override
	protected IDFParkingVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		double parkingPressure = parkingInformation.getParkingPressure(trip.getDestinationActivity().getLinkId());
		double parkingCost_EUR_h = parkingInformation.getParkingCost_EUR_h(trip.getDestinationActivity(), person);

		// TODO: Not exactly as in choice model!

		TimeInterpreter timeInterpreter = timeInterpreterFactory.createTimeInterpreter();
		timeInterpreter.setTime(trip.getDepartureTime());
		timeInterpreter.addPlanElements(elements);

		double startTime = timeInterpreter.getCurrentTime();
		timeInterpreter.addActivity(trip.getDestinationActivity());

		double endTime = timeInterpreter.getCurrentTime();
		double duration_h = (endTime - startTime) / 3600.0;

		double parkingCost_EUR = parkingCost_EUR_h * duration_h;
		return new IDFParkingVariables(parkingPressure, parkingCost_EUR);
	}
}
