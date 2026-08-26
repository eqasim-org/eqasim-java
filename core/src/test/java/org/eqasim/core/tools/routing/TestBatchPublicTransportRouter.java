package org.eqasim.core.tools.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eqasim.core.tools.routing.BatchPublicTransportRouter.DepartureIndex;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class TestBatchPublicTransportRouter {
	@Test
	public void testDepartureIndex() {
		TransitScheduleFactory factory = new TransitScheduleFactoryImpl();
		TransitStopFacility stopFacility = factory.createTransitStopFacility(
				Id.create("stop", TransitStopFacility.class), new Coord(0.0, 0.0), false);
		TransitRouteStop firstStop = factory.createTransitRouteStop(stopFacility, 10.0, 10.0);
		TransitRouteStop secondStop = factory.createTransitRouteStop(stopFacility, 20.0, 20.0);
		TransitRoute route = factory.createTransitRoute(Id.create("route", TransitRoute.class), null,
				List.of(firstStop, secondStop), "rail");

		Departure lateDeparture = factory.createDeparture(Id.create("late", Departure.class), 200.0);
		Departure earlyDeparture = factory.createDeparture(Id.create("early", Departure.class), 100.0);
		route.addDeparture(lateDeparture);
		route.addDeparture(earlyDeparture);

		DepartureIndex index = new DepartureIndex(route);
		TransitPassengerRoute passengerRoute = mock(TransitPassengerRoute.class);
		when(passengerRoute.getAccessStopId()).thenReturn(stopFacility.getId());

		when(passengerRoute.getBoardingTime()).thenReturn(OptionalTime.defined(110.0));
		assertEquals(earlyDeparture, index.findDeparture(passengerRoute));

		when(passengerRoute.getBoardingTime()).thenReturn(OptionalTime.defined(220.0));
		assertEquals(lateDeparture, index.findDeparture(passengerRoute));

		when(passengerRoute.getBoardingTime()).thenReturn(OptionalTime.defined(111.0));
		assertThrows(IllegalStateException.class, () -> index.findDeparture(passengerRoute));
	}
}
