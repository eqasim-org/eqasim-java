package org.eqasim.simulation.transit.departure;

import java.util.Arrays;

import org.eqasim.core.components.transit.departure.DefaultDepartureFinder;
import org.eqasim.core.components.transit.departure.DepartureFinder;
import org.eqasim.core.components.transit.departure.DepartureFinder.NoDepartureFoundException;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class TestDefaultDepartureFinder {
	@Test
	public void testDefaultDepartureFinder() throws NoDepartureFoundException {
		TransitScheduleFactory factory = new TransitScheduleFactoryImpl();

		TransitStopFacility facility00 = factory.createTransitStopFacility(Id.create("f00", TransitStopFacility.class),
				new Coord(0.0, 0.0), false);
		TransitRouteStop stop00 = factory.createTransitRouteStop(facility00, 0.0, 0.0);

		TransitStopFacility facility45 = factory.createTransitStopFacility(Id.create("f45", TransitStopFacility.class),
				new Coord(0.0, 0.0), false);
		TransitRouteStop stop45 = factory.createTransitRouteStop(facility45, 40.0 * 60.0, 45.0 * 60.0);

		TransitStopFacility facility90 = factory.createTransitStopFacility(Id.create("f90", TransitStopFacility.class),
				new Coord(0.0, 0.0), false);
		TransitRouteStop stop90 = factory.createTransitRouteStop(facility90, 85.0 * 60.0, 90.0 * 60.0);

		TransitRoute route = factory.createTransitRoute(null, null, Arrays.asList(stop00, stop45, stop90), "pt");

		Departure departureAt08 = factory.createDeparture(Id.create("08:00", Departure.class), 8.0 * 3600.0);
		Departure departureAt09 = factory.createDeparture(Id.create("09:00", Departure.class), 9.0 * 3600.0);
		Departure departureAt10 = factory.createDeparture(Id.create("10:00", Departure.class), 10.0 * 3600.0);
		Departure departureAt11 = factory.createDeparture(Id.create("11:00", Departure.class), 11.0 * 3600.0);

		Arrays.asList(departureAt08, departureAt09, departureAt10, departureAt11).forEach(route::addDeparture);

		DepartureFinder finder = new DefaultDepartureFinder();

		Assert.assertEquals(departureAt08,
				finder.findNextDeparture(route, facility00.getId(), facility90.getId(), 7.0 * 3600.0).departure);
		Assert.assertEquals(departureAt08,
				finder.findNextDeparture(route, facility00.getId(), facility90.getId(), 8.0 * 3600.0).departure);
		Assert.assertEquals(departureAt09,
				finder.findNextDeparture(route, facility00.getId(), facility90.getId(), 8.5 * 3600.0).departure);
		Assert.assertEquals(departureAt10,
				finder.findNextDeparture(route, facility00.getId(), facility90.getId(), 9.5 * 3600.0).departure);
		Assert.assertEquals(departureAt11,
				finder.findNextDeparture(route, facility00.getId(), facility90.getId(), 10.5 * 3600.0).departure);

		boolean exceptionThrown = false;
		try {
			finder.findNextDeparture(route, facility00.getId(), facility90.getId(), 11.5 * 3600.0);
		} catch (NoDepartureFoundException e) {
			exceptionThrown = true;
		}
		Assert.assertTrue(exceptionThrown);

		Assert.assertEquals(departureAt10,
				finder.findNextDeparture(route, facility45.getId(), facility90.getId(), 10.5 * 3600.0).departure);
		Assert.assertEquals(departureAt09,
				finder.findNextDeparture(route, facility90.getId(), facility90.getId(), 10.0 * 3600.0).departure);
	}

	@Test
	public void testDepartureFinderWithMultiStop() throws NoDepartureFoundException {
		TransitScheduleFactory factory = new TransitScheduleFactoryImpl();

		TransitStopFacility facilityA = factory.createTransitStopFacility(Id.create("A", TransitStopFacility.class),
				new Coord(0.0, 0.0), false);

		TransitStopFacility facilityB = factory.createTransitStopFacility(Id.create("B", TransitStopFacility.class),
				new Coord(0.0, 0.0), false);

		TransitStopFacility facilityC = factory.createTransitStopFacility(Id.create("C", TransitStopFacility.class),
				new Coord(0.0, 0.0), false);

		// A -> B -> A -> C
		// 0min -> 10min -> 20min -> 30min

		TransitRouteStop stop00 = factory.createTransitRouteStop(facilityA, 0.0, 0.0);
		TransitRouteStop stop10 = factory.createTransitRouteStop(facilityB, 10.0, 10.0);
		TransitRouteStop stop20 = factory.createTransitRouteStop(facilityA, 20.0, 20.0);
		TransitRouteStop stop30 = factory.createTransitRouteStop(facilityC, 30.0, 30.0);

		{
			TransitRoute route = factory.createTransitRoute(null, null, Arrays.asList(stop00, stop10, stop20, stop30),
					"pt");

			Departure departure = factory.createDeparture(Id.create("dep100", Departure.class), 100.0);
			route.addDeparture(departure);

			DepartureFinder finder = new DefaultDepartureFinder();

			Assert.assertEquals(stop00,
					finder.findNextDeparture(route, facilityA.getId(), facilityC.getId(), 0.0).stop);
			Assert.assertEquals(stop20,
					finder.findNextDeparture(route, facilityA.getId(), facilityC.getId(), 111.0).stop);
			Assert.assertEquals(stop20,
					finder.findNextDeparture(route, facilityA.getId(), facilityC.getId(), 111.0).stop);
		}

		{
			TransitRoute route = factory.createTransitRoute(null, null, Arrays.asList(stop00, stop10, stop20, stop30),
					"pt");

			// Departure 1: A (100), B (110), A (120), C (130)
			// Departure 2: A (200), B (210), A (220), C (230)

			// Departure time 115 -> so closest departure on this route is at the second A
			// However, we want to go to B, so we need to take the next departure and choose
			// the first A!

			Departure departure100 = factory.createDeparture(Id.create("dep100", Departure.class), 100.0);
			route.addDeparture(departure100);

			Departure departure200 = factory.createDeparture(Id.create("dep200", Departure.class), 200.0);
			route.addDeparture(departure200);

			DepartureFinder finder = new DefaultDepartureFinder();

			Assert.assertEquals(stop00,
					finder.findNextDeparture(route, facilityA.getId(), facilityC.getId(), 0.0).stop);
			Assert.assertEquals(departure100,
					finder.findNextDeparture(route, facilityA.getId(), facilityC.getId(), 0.0).departure);

			Assert.assertEquals(stop00,
					finder.findNextDeparture(route, facilityA.getId(), facilityB.getId(), 0.0).stop);
			Assert.assertEquals(departure100,
					finder.findNextDeparture(route, facilityA.getId(), facilityB.getId(), 0.0).departure);

			Assert.assertEquals(stop20,
					finder.findNextDeparture(route, facilityA.getId(), facilityC.getId(), 115.0).stop);
			Assert.assertEquals(departure100,
					finder.findNextDeparture(route, facilityA.getId(), facilityC.getId(), 115.0).departure);

			Assert.assertEquals(stop00,
					finder.findNextDeparture(route, facilityA.getId(), facilityB.getId(), 115.0).stop);
			Assert.assertEquals(departure200,
					finder.findNextDeparture(route, facilityA.getId(), facilityB.getId(), 115.0).departure);
		}
	}
}
