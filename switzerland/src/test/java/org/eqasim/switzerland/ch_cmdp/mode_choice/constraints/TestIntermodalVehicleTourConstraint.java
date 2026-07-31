package org.eqasim.switzerland.ch_cmdp.mode_choice.constraints;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.DefaultTourCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.DefaultRoutedTripCandidate;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class TestIntermodalVehicleTourConstraint {
	@Test
	public void testPreCheckRejectsDirectVehicleThatDoesNotReturnHome() {
		IntermodalVehicleTourConstraint constraint = createConstraint();
		List<DiscreteModeChoiceTrip> tour = createHomeWorkHomeTour();

		assertFalse(constraint.validateBeforeEstimation(tour, List.of(TransportMode.bike, TransportMode.walk),
				List.of()));
	}

	@Test
	public void testPreCheckRejectsDirectVehicleAtWrongOrigin() {
		IntermodalVehicleTourConstraint constraint = createConstraint();
		List<DiscreteModeChoiceTrip> tour = createHomeWorkHomeTour();

		assertFalse(constraint.validateBeforeEstimation(tour, List.of(TransportMode.walk, TransportMode.bike),
				List.of()));
	}

	@Test
	public void testPreCheckKeepsPtChainsBecauseIntermodalStopsAreUnknown() {
		IntermodalVehicleTourConstraint constraint = createConstraint();
		List<DiscreteModeChoiceTrip> tour = createHomeWorkHomeTour();

		assertTrue(constraint.validateBeforeEstimation(tour, List.of(TransportMode.pt, TransportMode.walk),
				List.of()));
	}

	@Test
	public void testAllowsReturningIntermodalVehicleAtSameStop() {
		IntermodalVehicleTourConstraint constraint = createConstraint();
		List<DiscreteModeChoiceTrip> tour = createHomeWorkHomeTour();
		DefaultTourCandidate candidate = new DefaultTourCandidate(0.0,
				List.of(createCandidate(createPtTrip(TransportMode.bike, "stop_home", null, true)),
						createCandidate(createPtTrip(null, "stop_home", TransportMode.bike, false))));

		assertTrue(constraint.validateAfterEstimation(tour, candidate, List.of()));
	}

	@Test
	public void testRejectsReturningIntermodalVehicleAtDifferentStop() {
		IntermodalVehicleTourConstraint constraint = createConstraint();
		List<DiscreteModeChoiceTrip> tour = createHomeWorkHomeTour();
		DefaultTourCandidate candidate = new DefaultTourCandidate(0.0,
				List.of(createCandidate(createPtTrip(TransportMode.bike, "stop_home", null, true)),
						createCandidate(createPtTrip(null, "other_stop", TransportMode.bike, false))));

		assertFalse(constraint.validateAfterEstimation(tour, candidate, List.of()));
	}

	@Test
	public void testRejectsPickingUpIntermodalVehicleThatWasNotLeftAtStop() {
		IntermodalVehicleTourConstraint constraint = createConstraint();
		List<DiscreteModeChoiceTrip> tour = createHomeWorkHomeTour();
		DefaultTourCandidate candidate = new DefaultTourCandidate(0.0,
				List.of(createCandidate(createPtTrip(null, "stop_home", null, true)),
						createCandidate(createPtTrip(null, "stop_home", TransportMode.bike, false))));

		assertFalse(constraint.validateAfterEstimation(tour, candidate, List.of()));
	}

	@Test
	public void testRejectsDirectWalkReturnAfterIntermodalVehicleWasLeftAtStop() {
		IntermodalVehicleTourConstraint constraint = createConstraint();
		List<DiscreteModeChoiceTrip> tour = createHomeWorkHomeTour();
		DefaultTourCandidate candidate = new DefaultTourCandidate(0.0,
				List.of(createCandidate(createPtTrip(TransportMode.bike, "stop_home", null, true)),
						createCandidate(TransportMode.pt, List.of(createLeg(TransportMode.walk)))));

		assertFalse(constraint.validateAfterEstimation(tour, candidate, List.of()));
	}

	@Test
	public void testRejectsWalkReturnAfterIntermodalVehicleWasLeftAtStop() {
		IntermodalVehicleTourConstraint constraint = createConstraint();
		List<DiscreteModeChoiceTrip> tour = createHomeWorkHomeTour();
		DefaultTourCandidate candidate = new DefaultTourCandidate(0.0,
				List.of(createCandidate(createPtTrip(TransportMode.bike, "stop_home", null, true)),
						createCandidate(TransportMode.walk, List.of(createLeg(TransportMode.walk)))));

		assertFalse(constraint.validateAfterEstimation(tour, candidate, List.of()));
	}

	@Test
	public void testTracksMainModeVehicleTripsAsWell() {
		IntermodalVehicleTourConstraint constraint = createConstraint();
		List<DiscreteModeChoiceTrip> tour = createHomeWorkHomeTour();
		DefaultTourCandidate candidate = new DefaultTourCandidate(0.0,
				List.of(createCandidate(TransportMode.bike, List.of(createLeg(TransportMode.bike))),
						createCandidate(TransportMode.bike, List.of(createLeg(TransportMode.bike)))));

		assertTrue(constraint.validateAfterEstimation(tour, candidate, List.of()));
	}

	static private IntermodalVehicleTourConstraint createConstraint() {
		return new IntermodalVehicleTourConstraint(List.of(TransportMode.bike), Id.createLinkId("home"));
	}

	static private List<DiscreteModeChoiceTrip> createHomeWorkHomeTour() {
		Activity home = createActivity("home", "home");
		Activity work = createActivity("work", "work");
		Activity homeAgain = createActivity("home", "home");

		return List.of(createTrip(home, work, 0), createTrip(work, homeAgain, 1));
	}

	static private DiscreteModeChoiceTrip createTrip(Activity origin, Activity destination, int index) {
		return new DiscreteModeChoiceTrip(origin, destination, TransportMode.pt, List.of(PopulationUtils.createLeg(TransportMode.pt)),
				0, index, index, origin.getAttributes());
	}

	static private Activity createActivity(String type, String linkId) {
		Activity activity = PopulationUtils.createActivityFromLinkId(type, Id.createLinkId(linkId));
		activity.setEndTime(0.0);
		return activity;
	}

	static private DefaultRoutedTripCandidate createCandidate(List<? extends PlanElement> elements) {
		return new DefaultRoutedTripCandidate(0.0, TransportMode.pt, elements, 0.0);
	}

	static private DefaultRoutedTripCandidate createCandidate(String mode, List<? extends PlanElement> elements) {
		return new DefaultRoutedTripCandidate(0.0, mode, elements, 0.0);
	}

	static private List<? extends PlanElement> createPtTrip(String accessMode, String stopId, String egressMode,
			boolean outbound) {
		Leg pt = PopulationUtils.createLeg(TransportMode.pt);
		Id<TransitStopFacility> vehicleStop = Id.create(stopId, TransitStopFacility.class);
		Id<TransitStopFacility> workStop = Id.create("work_stop", TransitStopFacility.class);
		Id<TransitStopFacility> accessStop = outbound ? vehicleStop : workStop;
		Id<TransitStopFacility> egressStop = outbound ? workStop : vehicleStop;
		pt.setRoute(new DefaultTransitPassengerRoute(Id.createLinkId(accessStop), Id.createLinkId(egressStop), accessStop,
				egressStop, Id.create("line", TransitLine.class),
				Id.create("route", TransitRoute.class)));

		if (accessMode != null) {
			return List.of(createLeg(accessMode), pt, createLeg(TransportMode.walk));
		}

		if (egressMode != null) {
			return List.of(createLeg(TransportMode.walk), pt, createLeg(egressMode));
		}

		return List.of(createLeg(TransportMode.walk), pt, createLeg(TransportMode.walk));
	}

	static private Leg createLeg(String mode) {
		Leg leg = PopulationUtils.createLeg(mode);
		leg.setRoute(RouteUtils.createGenericRouteImpl(Id.create("from_" + mode, Link.class),
				Id.create("to_" + mode, Link.class)));
		return leg;
	}
}
