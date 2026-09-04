package org.eqasim.ile_de_france.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFParkingVariables;
import org.eqasim.ile_de_france.parking.ParkingPressure;
import org.eqasim.ile_de_france.parking.ParkingTariff;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class IDFParkingPredictor extends CachedVariablePredictor<IDFParkingVariables> {
	private final ParkingPressure parkingPressure;
	private final ParkingTariff parkingTariff;

	@Inject
	public IDFParkingPredictor(ParkingPressure parkingPressure, ParkingTariff parkingTariff) {
		this.parkingPressure = parkingPressure;
		this.parkingTariff = parkingTariff;
	}

	@Override
	protected IDFParkingVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		double originParkingPressure = parkingPressure.getParkingPressure(
				trip.getOriginActivity().getLinkId());

		double destinationParkingPressure = parkingPressure.getParkingPressure(
				trip.getDestinationActivity().getLinkId());

		double parkingPressure = originParkingPressure + destinationParkingPressure;

		double originParkingTariff_EUR_h = parkingTariff.getParkingTariff(trip.getOriginActivity().getLinkId());
		double destinationParkingTariff_EUR_h = parkingTariff
				.getParkingTariff(trip.getDestinationActivity().getLinkId());

		return new IDFParkingVariables( //
				parkingPressure, //
				originParkingTariff_EUR_h, //
				destinationParkingTariff_EUR_h);
	}
}
