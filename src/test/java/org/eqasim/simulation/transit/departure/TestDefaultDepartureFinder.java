package org.eqasim.simulation.transit.departure;

import java.util.Arrays;

import org.eqasim.components.transit.departure.DefaultDepartureFinder;
import org.eqasim.components.transit.departure.DepartureFinder;
import org.eqasim.components.transit.departure.DepartureFinder.NoDepartureFoundException;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;

public class TestDefaultDepartureFinder {
	@Test
	public void testDefaultDepartureFinder() throws NoDepartureFoundException {
		TransitScheduleFactory factory = new TransitScheduleFactoryImpl();

		TransitRouteStop stop00 = factory.createTransitRouteStop(null, 0.0, 0.0);
		TransitRouteStop stop45 = factory.createTransitRouteStop(null, 40.0 * 60.0, 45.0 * 60.0);
		TransitRouteStop stop90 = factory.createTransitRouteStop(null, 85.0 * 60.0, 90.0 * 60.0);

		TransitRoute route = factory.createTransitRoute(null, null, Arrays.asList(stop00, stop45, stop90), "pt");

		Departure departureAt08 = factory.createDeparture(Id.create("08:00", Departure.class), 8.0 * 3600.0);
		Departure departureAt09 = factory.createDeparture(Id.create("09:00", Departure.class), 9.0 * 3600.0);
		Departure departureAt10 = factory.createDeparture(Id.create("10:00", Departure.class), 10.0 * 3600.0);
		Departure departureAt11 = factory.createDeparture(Id.create("11:00", Departure.class), 11.0 * 3600.0);

		Arrays.asList(departureAt08, departureAt09, departureAt10, departureAt11).forEach(route::addDeparture);

		DepartureFinder finder = new DefaultDepartureFinder();

		Assert.assertEquals(departureAt08, finder.findDeparture(route, stop00, 7.0 * 3600.0));
		Assert.assertEquals(departureAt08, finder.findDeparture(route, stop00, 8.0 * 3600.0));
		Assert.assertEquals(departureAt09, finder.findDeparture(route, stop00, 8.5 * 3600.0));
		Assert.assertEquals(departureAt10, finder.findDeparture(route, stop00, 9.5 * 3600.0));
		Assert.assertEquals(departureAt11, finder.findDeparture(route, stop00, 10.5 * 3600.0));
		
		boolean exceptionThrown = false;
		try {
			finder.findDeparture(route, stop00, 11.5 * 3600.0);
		} catch (NoDepartureFoundException e) {
			exceptionThrown = true;
		}
		Assert.assertTrue(exceptionThrown);
		
		Assert.assertEquals(departureAt10, finder.findDeparture(route, stop45, 10.5 * 3600.0));
		Assert.assertEquals(departureAt09, finder.findDeparture(route, stop90, 10.0 * 3600.0));

	}
}
