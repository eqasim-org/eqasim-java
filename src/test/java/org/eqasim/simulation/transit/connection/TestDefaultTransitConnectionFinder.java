package org.eqasim.simulation.transit.connection;

import java.util.Arrays;

import org.eqasim.simulation.transit.connection.TransitConnectionFinder.NoConnectionFoundException;
import org.eqasim.simulation.transit.departure.DefaultDepartureFinder;
import org.eqasim.simulation.transit.departure.DepartureFinder;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class TestDefaultTransitConnectionFinder {
	@Test
	public void testASM52() throws NoConnectionFoundException {
		DepartureFinder departureFinder = new DefaultDepartureFinder();
		TransitConnectionFinder finder = new DefaultTransitConnectionFinder(departureFinder);

		/*
		 * Schedule based on Line 52 (Aare Seeland Mobil)in Switzerland Note that there
		 * is a loop starting at stop 12, for a couple of stations the bus is going the
		 * same route.
		 */

		TransitScheduleFactory factory = new TransitScheduleFactoryImpl();

		TransitStopFacility facility1 = factory
				.createTransitStopFacility(Id.create("Thörigen, Post", TransitStopFacility.class), null, false);
		TransitStopFacility facility2 = factory
				.createTransitStopFacility(Id.create("Bleienbach, Post", TransitStopFacility.class), null, false);
		TransitStopFacility facility3 = factory
				.createTransitStopFacility(Id.create("Bleienbach, Unterdorf", TransitStopFacility.class), null, false);
		TransitStopFacility facility4 = factory
				.createTransitStopFacility(Id.create("Langenthal, Dennli", TransitStopFacility.class), null, false);
		TransitStopFacility facility5 = factory
				.createTransitStopFacility(Id.create("Langenthal, Sängeli", TransitStopFacility.class), null, false);
		TransitStopFacility facility6 = factory.createTransitStopFacility(
				Id.create("Langenthal Süd, Bahnhof", TransitStopFacility.class), null, false);
		TransitStopFacility facility7 = factory
				.createTransitStopFacility(Id.create("Langenthal, Neuhüsli", TransitStopFacility.class), null, false);
		TransitStopFacility facility8 = factory.createTransitStopFacility(
				Id.create("Langenthal, Bleichestrasse", TransitStopFacility.class), null, false);
		TransitStopFacility facility9 = factory
				.createTransitStopFacility(Id.create("Langenthal, Löwenplatz", TransitStopFacility.class), null, false);
		TransitStopFacility facility10 = factory.createTransitStopFacility(
				Id.create("Langenthal, Tell/Kantonalbank", TransitStopFacility.class), null, false);
		TransitStopFacility facility11 = factory.createTransitStopFacility(
				Id.create("Langenthal, Wiesenstrasse", TransitStopFacility.class), null, false);
		TransitStopFacility facility12 = factory
				.createTransitStopFacility(Id.create("Langenthal, Bahnhof", TransitStopFacility.class), null, false);
		TransitStopFacility facility13 = factory.createTransitStopFacility(
				Id.create("Langenthal, Schoren-Eisbahn", TransitStopFacility.class), null, false);
		TransitStopFacility facility14 = factory.createTransitStopFacility(
				Id.create("Thunstetten, Schorenmatte", TransitStopFacility.class), null, false);
		TransitStopFacility facility15 = factory
				.createTransitStopFacility(Id.create("Thunstetten, Wischberg", TransitStopFacility.class), null, false);
		TransitStopFacility facility16 = factory
				.createTransitStopFacility(Id.create("Thunstetten, Schloss", TransitStopFacility.class), null, false);

		TransitRouteStop stop1 = factory.createTransitRouteStop(facility1, 2.0 * 60.0, 2.0 * 60.0);
		TransitRouteStop stop2 = factory.createTransitRouteStop(facility2, 5.0 * 60.0, 5.0 * 60.0);
		TransitRouteStop stop3 = factory.createTransitRouteStop(facility3, 6.0 * 60.0, 6.0 * 60.0);
		TransitRouteStop stop4 = factory.createTransitRouteStop(facility4, 8.0 * 60.0, 8.0 * 60.0);
		TransitRouteStop stop5 = factory.createTransitRouteStop(facility5, 8.0 * 60.0, 8.0 * 60.0);
		TransitRouteStop stop6 = factory.createTransitRouteStop(facility6, 8.0 * 60.0, 8.0 * 60.0);
		TransitRouteStop stop7 = factory.createTransitRouteStop(facility7, 9.0 * 60.0, 9.0 * 60.0);
		TransitRouteStop stop8 = factory.createTransitRouteStop(facility8, 9.0 * 60.0, 9.0 * 60.0);
		TransitRouteStop stop9 = factory.createTransitRouteStop(facility9, 10.0 * 60.0, 10.0 * 60.0);
		TransitRouteStop stop10 = factory.createTransitRouteStop(facility10, 12.0 * 60.0, 12.0 * 60.0);
		TransitRouteStop stop11 = factory.createTransitRouteStop(facility11, 13.0 * 60.0, 13.0 * 60.0);
		TransitRouteStop stop12 = factory.createTransitRouteStop(facility12, 18.0 * 60.0, 18.0 * 60.0);
		TransitRouteStop stop13 = factory.createTransitRouteStop(facility11, 19.0 * 60.0, 19.0 * 60.0);
		TransitRouteStop stop14 = factory.createTransitRouteStop(facility10, 20.0 * 60.0, 20.0 * 60.0);
		TransitRouteStop stop15 = factory.createTransitRouteStop(facility9, 22.0 * 60.0, 22.0 * 60.0);
		TransitRouteStop stop16 = factory.createTransitRouteStop(facility8, 23.0 * 60.0, 23.0 * 60.0);
		TransitRouteStop stop17 = factory.createTransitRouteStop(facility7, 24.0 * 60.0, 24.0 * 60.0);
		TransitRouteStop stop18 = factory.createTransitRouteStop(facility6, 25.0 * 60.0, 25.0 * 60.0);
		TransitRouteStop stop19 = factory.createTransitRouteStop(facility13, 26.0 * 60.0, 26.0 * 60.0);
		TransitRouteStop stop20 = factory.createTransitRouteStop(facility14, 27.0 * 60.0, 27.0 * 60.0);
		TransitRouteStop stop21 = factory.createTransitRouteStop(facility15, 28.0 * 60.0, 28.0 * 60.0);
		TransitRouteStop stop22 = factory.createTransitRouteStop(facility16, 32.0 * 60.0, 32.0 * 60.0);

		Departure departure5 = factory.createDeparture(Id.create("05:47", Departure.class), 5.0 * 3600.0 + 47.0 * 60.0);
		Departure departure7 = factory.createDeparture(Id.create("07:17", Departure.class), 7.0 * 3600.0 + 17.0 * 60.0);
		Departure departure8 = factory.createDeparture(Id.create("08:17", Departure.class), 8.0 * 3600.0 + 17.0 * 60.0);
		Departure departure9 = factory.createDeparture(Id.create("09:17", Departure.class), 9.0 * 3600.0 + 17.0 * 60.0);
		Departure departure10 = factory.createDeparture(Id.create("10:17", Departure.class),
				10.0 * 3600.0 + 17.0 * 60.0);
		Departure departure11 = factory.createDeparture(Id.create("11:17", Departure.class),
				11.0 * 3600.0 + 17.0 * 60.0);
		Departure departure12 = factory.createDeparture(Id.create("12:17", Departure.class),
				12.0 * 3600.0 + 17.0 * 60.0);
		Departure departure13 = factory.createDeparture(Id.create("13:17", Departure.class),
				13.0 * 3600.0 + 17.0 * 60.0);
		Departure departure14 = factory.createDeparture(Id.create("14:17", Departure.class),
				14.0 * 3600.0 + 17.0 * 60.0);
		Departure departure15 = factory.createDeparture(Id.create("15:17", Departure.class),
				15.0 * 3600.0 + 17.0 * 60.0);
		Departure departure16 = factory.createDeparture(Id.create("16:17", Departure.class),
				16.0 * 3600.0 + 17.0 * 60.0);
		Departure departure17 = factory.createDeparture(Id.create("17:17", Departure.class),
				17.0 * 3600.0 + 17.0 * 60.0);
		Departure departure18 = factory.createDeparture(Id.create("18:17", Departure.class),
				18.0 * 3600.0 + 17.0 * 60.0);
		Departure departure19 = factory.createDeparture(Id.create("19:17", Departure.class),
				19.0 * 3600.0 + 17.0 * 60.0);
		Departure departure20 = factory.createDeparture(Id.create("20:17", Departure.class),
				20.0 * 3600.0 + 17.0 * 60.0);

		TransitRoute transitRoute = factory
				.createTransitRoute(Id.create("test", TransitRoute.class), null,
						Arrays.asList(stop1, stop2, stop3, stop4, stop5, stop6, stop7, stop8, stop9, stop10, stop11,
								stop12, stop13, stop14, stop15, stop16, stop17, stop18, stop19, stop20, stop21, stop22),
						"bus");

		Arrays.asList(departure5, departure7, departure8, departure9, departure10, departure11, departure12,
				departure13, departure14, departure15, departure16, departure17, departure18, departure19, departure20)
				.forEach(departure -> transitRoute.addDeparture(departure));

		// Case 1: Simple

		double departureTime = 9.0 * 3600.0 + 20.0 * 60.0; // (2min waiting time)
		double totalTravelTime = 4.0 * 60.0 + 2.0 * 60.0;

		TransitConnection connection = finder.findConnection(departureTime, totalTravelTime,
				Id.create("Bleienbach, Post", TransitStopFacility.class),
				Id.create("Langenthal, Bleichestrasse", TransitStopFacility.class), transitRoute);

		Assert.assertSame(stop2, connection.getAccessStop());
		Assert.assertSame(stop8, connection.getEgressStop());
		Assert.assertSame(departure9, connection.getDeparture());
		Assert.assertEquals(4.0 * 60.0, connection.getInVehicleTime(), 1e-3);
		Assert.assertEquals(2.0 * 60.0, connection.getWaitingTime(), 1e-3);

		// Case 2:

		departureTime = 13.0 * 3600.0 + 18.0 * 60.0; // (4min waiting time)
		totalTravelTime = 8.0 * 60.0 + 4.0 * 60.0;

		connection = finder.findConnection(departureTime, totalTravelTime,
				Id.create("Bleienbach, Post", TransitStopFacility.class),
				Id.create("Langenthal, Wiesenstrasse", TransitStopFacility.class), transitRoute);

		Assert.assertSame(stop2, connection.getAccessStop());
		Assert.assertSame(stop11, connection.getEgressStop());
		Assert.assertSame(departure13, connection.getDeparture());
		Assert.assertEquals(8.0 * 60.0, connection.getInVehicleTime(), 1e-3);
		Assert.assertEquals(4.0 * 60.0, connection.getWaitingTime(), 1e-3);

		// Case 2a (before loop):

		departureTime = 14.0 * 3600.0 + 17.0 * 60.0 + (13.0 - 3.0) * 60.0; // (3min waiting time)
		totalTravelTime = 19.0 * 60.0 + 3.0 * 60.0;

		connection = finder.findConnection(departureTime, totalTravelTime,
				Id.create("Langenthal, Wiesenstrasse", TransitStopFacility.class),
				Id.create("Thunstetten, Schloss", TransitStopFacility.class), transitRoute);

		Assert.assertSame(stop11, connection.getAccessStop());
		Assert.assertSame(stop22, connection.getEgressStop());
		Assert.assertSame(departure14, connection.getDeparture());
		Assert.assertEquals(19.0 * 60.0, connection.getInVehicleTime(), 1e-3);
		Assert.assertEquals(3.0 * 60.0, connection.getWaitingTime(), 1e-3);

		// Case 2b (after loop):

		departureTime = 14.0 * 3600.0 + 17.0 * 60.0 + (19.0 - 3.0) * 60.0; // (3min waiting time)
		totalTravelTime = 13.0 * 60.0 + 3.0 * 60.0;

		connection = finder.findConnection(departureTime, totalTravelTime,
				Id.create("Langenthal, Wiesenstrasse", TransitStopFacility.class),
				Id.create("Thunstetten, Schloss", TransitStopFacility.class), transitRoute);

		Assert.assertSame(stop13, connection.getAccessStop());
		Assert.assertSame(stop22, connection.getEgressStop());
		Assert.assertSame(departure14, connection.getDeparture());
		Assert.assertEquals(13.0 * 60.0, connection.getInVehicleTime(), 1e-3);
		Assert.assertEquals(3.0 * 60.0, connection.getWaitingTime(), 1e-3);
	}

	@Test
	public void testLeysin170() throws NoConnectionFoundException {
		DepartureFinder departureFinder = new DefaultDepartureFinder();
		TransitConnectionFinder finder = new DefaultTransitConnectionFinder(departureFinder);

		/*
		 * Schedule based on Line 170 in Leysin, Switzerland. Note that there are
		 * several loops in the route, but also a repetition of a sequence of stops
		 * (e.g. watch out for Place du Marché and Télécabine Berneuse).
		 */

		TransitScheduleFactory factory = new TransitScheduleFactoryImpl();

		TransitStopFacility facility1 = factory
				.createTransitStopFacility(Id.create("Patinoire-camping", TransitStopFacility.class), null, false);
		TransitStopFacility facility2 = factory
				.createTransitStopFacility(Id.create("En Crettaz", TransitStopFacility.class), null, false);
		TransitStopFacility facility3 = factory
				.createTransitStopFacility(Id.create("Place de la Tour d'Ai", TransitStopFacility.class), null, false);
		TransitStopFacility facility4 = factory
				.createTransitStopFacility(Id.create("Collège", TransitStopFacility.class), null, false);
		TransitStopFacility facility5 = factory
				.createTransitStopFacility(Id.create("La Croisée", TransitStopFacility.class), null, false);
		TransitStopFacility facility6 = factory
				.createTransitStopFacility(Id.create("Place du Marché", TransitStopFacility.class), null, false);
		TransitStopFacility facility6r = factory.createTransitStopFacility(
				Id.create("Place du Marché (reverse stop)", TransitStopFacility.class), null, false);
		TransitStopFacility facility7 = factory
				.createTransitStopFacility(Id.create("Télécabine Berneuse", TransitStopFacility.class), null, false);
		TransitStopFacility facility7r = factory.createTransitStopFacility(
				Id.create("Télécabine Berneuse (reverse stop)", TransitStopFacility.class), null, false);
		TransitStopFacility facility8 = factory
				.createTransitStopFacility(Id.create("Fabiola", TransitStopFacility.class), null, false);
		TransitStopFacility facility9 = factory
				.createTransitStopFacility(Id.create("Les Esserts-Fontaine", TransitStopFacility.class), null, false);
		TransitStopFacility facility10 = factory
				.createTransitStopFacility(Id.create("Les Alpes", TransitStopFacility.class), null, false);
		TransitStopFacility facility11 = factory
				.createTransitStopFacility(Id.create("Vermont (gare)", TransitStopFacility.class), null, false);
		TransitStopFacility facility12 = factory
				.createTransitStopFacility(Id.create("American School", TransitStopFacility.class), null, false);
		TransitStopFacility facility13 = factory
				.createTransitStopFacility(Id.create("Esplanade-Kumon", TransitStopFacility.class), null, false);
		TransitStopFacility facility14 = factory
				.createTransitStopFacility(Id.create("Feydey, gare", TransitStopFacility.class), null, false);
		TransitStopFacility facility15 = factory
				.createTransitStopFacility(Id.create("Grand Hôtel (parking)", TransitStopFacility.class), null, false);
		TransitStopFacility facility16 = factory
				.createTransitStopFacility(Id.create("Grand Hôtel (virage)", TransitStopFacility.class), null, false);
		TransitStopFacility facility17 = factory.createTransitStopFacility(
				Id.create("Ecole ski-Central Résid", TransitStopFacility.class), null, false);

		TransitRouteStop stop1 = factory.createTransitRouteStop(facility1, 0.0 * 60.0, 0.0 * 60.0);
		TransitRouteStop stop2 = factory.createTransitRouteStop(facility2, 4.0 * 60.0, 4.0 * 60.0);
		TransitRouteStop stop3 = factory.createTransitRouteStop(facility3, 7.0 * 60.0, 7.0 * 60.0);
		TransitRouteStop stop4 = factory.createTransitRouteStop(facility4, 10.0 * 60.0, 10.0 * 60.0);
		TransitRouteStop stop5 = factory.createTransitRouteStop(facility5, 11.0 * 60.0, 11.0 * 60.0);
		TransitRouteStop stop6 = factory.createTransitRouteStop(facility6r, 13.0 * 60.0, 13.0 * 60.0);
		TransitRouteStop stop7 = factory.createTransitRouteStop(facility7, 15.0 * 60.0, 15.0 * 60.0);
		TransitRouteStop stop8 = factory.createTransitRouteStop(facility8, 18.0 * 60.0, 18.0 * 60.0);
		TransitRouteStop stop9 = factory.createTransitRouteStop(facility9, 19.0 * 60.0, 19.0 * 60.0);
		TransitRouteStop stop10 = factory.createTransitRouteStop(facility10, 20.0 * 60.0, 20.0 * 60.0);
		TransitRouteStop stop11 = factory.createTransitRouteStop(facility7r, 21.0 * 60.0, 21.0 * 60.0);
		TransitRouteStop stop12 = factory.createTransitRouteStop(facility6, 23.0 * 60.0, 23.0 * 60.0);
		TransitRouteStop stop13 = factory.createTransitRouteStop(facility11, 24.0 * 60.0, 24.0 * 60.0);
		TransitRouteStop stop14 = factory.createTransitRouteStop(facility12, 26.0 * 60.0, 26.0 * 60.0);
		TransitRouteStop stop15 = factory.createTransitRouteStop(facility13, 27.0 * 60.0, 27.0 * 60.0);
		TransitRouteStop stop16 = factory.createTransitRouteStop(facility14, 30.0 * 60.0, 30.0 * 60.0);
		TransitRouteStop stop17 = factory.createTransitRouteStop(facility15, 32.0 * 60.0, 32.0 * 60.0);
		TransitRouteStop stop18 = factory.createTransitRouteStop(facility16, 33.0 * 60.0, 33.0 * 60.0);
		TransitRouteStop stop19 = factory.createTransitRouteStop(facility14, 34.0 * 60.0, 34.0 * 60.0);
		TransitRouteStop stop20 = factory.createTransitRouteStop(facility17, 36.0 * 60.0, 36.0 * 60.0);
		TransitRouteStop stop21 = factory.createTransitRouteStop(facility7, 39.0 * 60.0, 39.0 * 60.0);
		TransitRouteStop stop22 = factory.createTransitRouteStop(facility6, 41.0 * 60.0, 41.0 * 60.0);
		TransitRouteStop stop23 = factory.createTransitRouteStop(facility3, 43.0 * 60.0, 43.0 * 60.0);
		TransitRouteStop stop24 = factory.createTransitRouteStop(facility1, 44.0 * 46.0, 44.0 * 60.0);
		TransitRouteStop stop25 = factory.createTransitRouteStop(facility2, 46.0 * 60.0, 46.0 * 60.0);
		TransitRouteStop stop26 = factory.createTransitRouteStop(facility1, 50.0 * 60.0, 50.0 * 60.0);

		Departure departure8 = factory.createDeparture(Id.create("08:20", Departure.class), 8.0 * 3600.0 + 20.0 * 60.0);
		Departure departure9 = factory.createDeparture(Id.create("09:20", Departure.class), 9.0 * 3600.0 + 20.0 * 60.0);
		Departure departure10 = factory.createDeparture(Id.create("10:20", Departure.class),
				10.0 * 3600.0 + 20.0 * 60.0);
		Departure departure11 = factory.createDeparture(Id.create("11:20", Departure.class),
				11.0 * 3600.0 + 20.0 * 60.0);
		Departure departure12 = factory.createDeparture(Id.create("12:20", Departure.class),
				12.0 * 3600.0 + 20.0 * 60.0);
		Departure departure13 = factory.createDeparture(Id.create("13:20", Departure.class),
				13.0 * 3600.0 + 20.0 * 60.0);
		Departure departure14 = factory.createDeparture(Id.create("14:20", Departure.class),
				14.0 * 3600.0 + 20.0 * 60.0);
		Departure departure15 = factory.createDeparture(Id.create("15:20", Departure.class),
				15.0 * 3600.0 + 20.0 * 60.0);
		Departure departure16 = factory.createDeparture(Id.create("16:20", Departure.class),
				16.0 * 3600.0 + 20.0 * 60.0);
		Departure departure17 = factory.createDeparture(Id.create("17:20", Departure.class),
				17.0 * 3600.0 + 20.0 * 60.0);

		TransitRoute transitRoute = factory.createTransitRoute(Id.create("test", TransitRoute.class), null,
				Arrays.asList(stop1, stop2, stop3, stop4, stop5, stop6, stop7, stop8, stop9, stop10, stop11, stop12,
						stop13, stop14, stop15, stop16, stop17, stop18, stop19, stop20, stop21, stop22, stop23, stop24,
						stop25, stop26),
				"bus");

		Arrays.asList(departure8, departure9, departure10, departure11, departure12, departure13, departure14,
				departure15, departure16, departure17).forEach(departure -> transitRoute.addDeparture(departure));

		// Case 1: The connection before the loop is meant

		double departureTime = 9.0 * 3600.0 + 35.0 * 60.0 - 2.0 * 60.0; // (2min waiting time)
		double totalTravelTime = 8.0 * 60.0 + 2.0 * 60.0;

		TransitConnection connection = finder.findConnection(departureTime, totalTravelTime,
				Id.create("Télécabine Berneuse", TransitStopFacility.class),
				Id.create("Place du Marché", TransitStopFacility.class), transitRoute);

		Assert.assertSame(stop7, connection.getAccessStop());
		Assert.assertSame(stop12, connection.getEgressStop());
		Assert.assertSame(departure9, connection.getDeparture());
		Assert.assertEquals(8.0 * 60.0, connection.getInVehicleTime(), 1e-3);
		Assert.assertEquals(2.0 * 60.0, connection.getWaitingTime(), 1e-3);

		// Case 2: The connection after the loop is meant (shorter driving time)

		departureTime = 12.0 * 3600.0 + 58.0 * 60.0;
		totalTravelTime = 2.0 * 60.0 + 1.0 * 60.0;

		connection = finder.findConnection(departureTime, totalTravelTime,
				Id.create("Télécabine Berneuse", TransitStopFacility.class),
				Id.create("Place du Marché", TransitStopFacility.class), transitRoute);

		Assert.assertSame(stop21, connection.getAccessStop());
		Assert.assertSame(stop22, connection.getEgressStop());
		Assert.assertSame(departure12, connection.getDeparture());
		Assert.assertEquals(2.0 * 60.0, connection.getInVehicleTime(), 1e-3);
		Assert.assertEquals(1.0 * 60.0, connection.getWaitingTime(), 1e-3);
	}
}
