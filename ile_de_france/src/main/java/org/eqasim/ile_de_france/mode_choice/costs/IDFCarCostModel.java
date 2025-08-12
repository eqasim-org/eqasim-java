package org.eqasim.ile_de_france.mode_choice.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModelWithPreviousTrips;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFCostParameters;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFParkingPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPersonPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFSpatialPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFParkingVariables;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFPersonVariables;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFSpatialVariables;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.DefaultRoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

import com.google.inject.Inject;

public class IDFCarCostModel extends AbstractCostModelWithPreviousTrips {
	private final TimeInterpretation timeInterpretation;
	private final IDFCostParameters costParameters;
	private final IDFPersonPredictor personPredictor;
	private final IDFSpatialPredictor spatialPredictor;
	private final IDFParkingPredictor parkingPredictor;

	@Inject
	public IDFCarCostModel(IDFCostParameters costParameters, IDFPersonPredictor personPredictor,
			IDFSpatialPredictor spatialPredictor, IDFParkingPredictor parkingPredictor,
			TimeInterpretation timeInterpretation) {
		super("car");

		this.costParameters = costParameters;
		this.personPredictor = personPredictor;
		this.spatialPredictor = spatialPredictor;
		this.parkingPredictor = parkingPredictor;
		this.timeInterpretation = timeInterpretation;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements,
			List<TripCandidate> previousTrips) {
		double parkingCost_EUR = calculateParkingCost_EUR(person, trip, elements, previousTrips);

		System.err.println("carcost:parkingCost " + parkingCost_EUR);

		return costParameters.carCost_EUR_km * getInVehicleDistance_km(elements)
				+ parkingCost_EUR;
	}

	public double calculateParkingCost_EUR(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> tripElements, List<TripCandidate> previousTrips) {
		IDFSpatialVariables spatialVariables = spatialPredictor.predictVariables(person, trip, tripElements);
		IDFParkingVariables parkingVariables = parkingPredictor.predictVariables(person, trip, tripElements);
		IDFPersonVariables personVariables = personPredictor.predictVariables(person, trip, tripElements);

		// Origin parking costs
		double originParkingCost_EUR = 0.0;

		boolean isOriginWork = trip.getOriginActivity().getType().equals("work");
		boolean isOriginResident = spatialVariables.originMunicipalityId
				.equals(personVariables.residenceMunicipalityId);

		if (!isOriginWork && !isOriginResident) {
			double originDuration = getOriginDuration(person, trip, previousTrips);
			originParkingCost_EUR = Math.ceil(originDuration / 3600.0)
					* parkingVariables.originParkingTariff_EUR_h;
		}

		// Destination parking costs
		double destinationParkingCost_EUR = 0.0;

		boolean isDestinationWork = trip.getDestinationActivity().getType().equals("work");
		boolean isDestinationResident = spatialVariables.destinationMunicipalityId
				.equals(personVariables.residenceMunicipalityId);

		if (!isDestinationWork && !isDestinationResident) {
			double destinationDuration = getDestinationDuration(person, trip, tripElements);
			destinationParkingCost_EUR = Math.ceil(destinationDuration / 3600.0)
					* parkingVariables.destinationParkingTariff_EUR_h;
		}

		if (originParkingCost_EUR > 0.0) {
			System.err.println("carcost:origin " + originParkingCost_EUR);
		}

		if (destinationParkingCost_EUR > 0.0) {
			System.err.println("carcost:destination " + destinationParkingCost_EUR);
		}

		return originParkingCost_EUR + destinationParkingCost_EUR;
	}

	private final double FIRST_LAST_DURATION = 8.0 * 3600.0;

	private double getOriginDuration(Person person, DiscreteModeChoiceTrip trip, List<TripCandidate> previousTrips) {
		if (previousTrips.size() == 0) {
			return FIRST_LAST_DURATION;
		} else {
			TimeTracker timeTracker = new TimeTracker(timeInterpretation);

			DefaultRoutedTripCandidate previousTrip = (DefaultRoutedTripCandidate) previousTrips
					.get(previousTrips.size() - 1);
			List<? extends PlanElement> precedingElements = previousTrip.getRoutedPlanElements();

			double startTime = ((Leg) precedingElements.get(0)).getDepartureTime().seconds();
			timeTracker.setTime(startTime);
			timeTracker.addElements(precedingElements);

			double arrivalTime = timeTracker.getTime().seconds();
			timeTracker.addActivity(trip.getOriginActivity());
			double departureTime = timeTracker.getTime().seconds();

			return departureTime - arrivalTime;
		}
	}

	private double getDestinationDuration(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> tripElements) {
		List<? extends PlanElement> planElements = person.getSelectedPlan().getPlanElements();
		if (planElements.get(planElements.size() - 1) == trip.getDestinationActivity()) {
			return FIRST_LAST_DURATION;
		} else {
			TimeTracker timeTracker = new TimeTracker(timeInterpretation);
			timeTracker.setTime(trip.getDepartureTime());
			timeTracker.addElements(tripElements);

			double arrivalTime = timeTracker.getTime().seconds();
			timeTracker.addActivity(trip.getDestinationActivity());
			double departureTime = timeTracker.getTime().seconds();

			return departureTime - arrivalTime;
		}
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		throw new IllegalStateException("Use cost calculator with previous costs");
	}
}
