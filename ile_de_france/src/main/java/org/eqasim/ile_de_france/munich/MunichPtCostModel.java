package org.eqasim.ile_de_france.munich;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPersonPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFPersonVariables;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.inject.Inject;

public class MunichPtCostModel implements CostModel {
	private final TransitSchedule schedule;
	private final TimeInterpretation timeInterpretation;
	private final IDFPersonPredictor personPredictor;

	@Inject
	public MunichPtCostModel(TransitSchedule schedule, TimeInterpretation timeInterpretation,
			IDFPersonPredictor personPredictor) {
		this.schedule = schedule;
		this.timeInterpretation = timeInterpretation;
		this.personPredictor = personPredictor;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		IDFPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);

		if (personVariables.hasSubscription) {
			return 0.0;
		}

		Id<TransitStopFacility> firstFacilityId = null;
		Id<TransitStopFacility> lastFacilityId = null;

		TimeTracker timeTracker = new TimeTracker(timeInterpretation);
		timeTracker.setTime(trip.getDepartureTime());

		for (var element : elements) {
			if (element instanceof Leg leg && leg.getRoute() instanceof TransitPassengerRoute route) {
				if (firstFacilityId == null) {
					firstFacilityId = route.getAccessStopId();
				}

				lastFacilityId = route.getEgressStopId();
			}

			timeTracker.addElement(element);
		}

		double travelTime = timeTracker.getTime().seconds() - trip.getDepartureTime();

		if (travelTime <= 3600.0) {
			// maybe short distance ticket

			boolean isValid = true;
			int stopCount = 0;

			for (Leg leg : TripStructureUtils.getLegs(elements)) {
				if (leg.getRoute() instanceof TransitPassengerRoute route) {
					TransitLine transitLine = schedule.getTransitLines().get(route.getLineId());
					TransitRoute transitRoute = transitLine.getRoutes().get(route.getRouteId());

					if (transitRoute.getTransportMode().equals("subway")
							|| transitRoute.getTransportMode().equals("bus")) {
						TransitStopFacility accessFacility = schedule.getFacilities().get(route.getAccessStopId());
						TransitStopFacility egressFacility = schedule.getFacilities().get(route.getEgressStopId());

						TransitRouteStop accessStop = transitRoute.getStop(accessFacility);
						TransitRouteStop egressStop = transitRoute.getStop(egressFacility);

						int accessStopIndex = transitRoute.getStops().indexOf(accessStop);
						int egressStopIndex = transitRoute.getStops().indexOf(egressStop);

						stopCount += egressStopIndex - accessStopIndex;

						if (stopCount > 4) {
							isValid = false;
							break;
						}
					} else {
						isValid = false;
						break;
					}
				}
			}

			if (isValid) {
				return shortPrice;
			}
		}

		TransitStopFacility firstFacility = schedule.getFacilities().get(firstFacilityId);
		TransitStopFacility lastFacility = schedule.getFacilities().get(lastFacilityId);

		Integer firstMinimumZone = (Integer) firstFacility.getAttributes().getAttribute("minimumZone");
		Integer firstMaximumZone = (Integer) firstFacility.getAttributes().getAttribute("maximumZone");

		Integer lastMinimumZone = (Integer) lastFacility.getAttributes().getAttribute("minimumZone");
		Integer lastMaximumZone = (Integer) lastFacility.getAttributes().getAttribute("maximumZone");

		if (firstMinimumZone != null && lastMinimumZone != null) {
			// we have a zonal trip
			return getZonalPrice(firstMinimumZone, firstMaximumZone, lastMinimumZone, lastMaximumZone);
		} else {
			// something is outside of any zone
			return basePrice_h * Math.max(1.0, Math.ceil(travelTime / 3600.0));
		}
	}

	private final static double shortPrice = 1.9;
	private final static double basePrice_h = 8.0;

	private final static double[] prices = new double[] { //
			3.9, 5.8, 7.7, 9.7, 11.6, 13.6, 15.4, 17.1, 18.8, 20.5, 22.2, 25.5 //
	};

	private double getZonalPrice(int firstMinimumZone, int firstMaximumZone, int lastMinimumZone, int lastMaximumZone) {
		int difference = Integer.MAX_VALUE;
		difference = Math.min(difference, Math.abs(firstMinimumZone - lastMinimumZone));
		difference = Math.min(difference, Math.abs(firstMinimumZone - lastMaximumZone));
		difference = Math.min(difference, Math.abs(firstMaximumZone - lastMinimumZone));
		difference = Math.min(difference, Math.abs(firstMaximumZone - lastMaximumZone));

		boolean includesM = false;
		includesM |= firstMinimumZone == 0;
		includesM |= firstMaximumZone == 0;
		includesM |= lastMinimumZone == 0;
		includesM |= lastMaximumZone == 0;

		if (includesM) {
			return prices[difference];
		} else {
			return prices[Math.max(0, difference - 1)];
		}
	}
}
