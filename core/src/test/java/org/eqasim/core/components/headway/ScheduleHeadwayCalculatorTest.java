package org.eqasim.core.components.headway;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.PopulationUtils;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.common.base.Verify;

public class ScheduleHeadwayCalculatorTest {
	@Test
	public void testDirect() {
		/*-
		 * Build schedule:
		 * - Line A, RouteA (AS1, AS2, AS3, AS4), inter-stop time 1000s, departures @ 0, 2500, 5000
		 */
		TransitSchedule schedule = new ScheduleBuilder() //
				.addRoute( //
						"A", "A", // Line ID, Route ID
						Arrays.asList("AS1", "AS2", "AS3", "AS4"), // Stop IDs
						1000.0, // Travel time between stops
						Arrays.asList(0.0, 2500.0, 5000.0)) // Route departures
				.build();

		/*-
		 * Build trajectory:
		 * - Access 200s
		 * - Travel on route A from AS1 to AS3 (departure 2500, TT 2000)
		 * - Egress 200s
		 */
		LegsBuilder builder = new LegsBuilder(schedule);
		builder.addWalk(200.0);
		assertEquals(200.0, builder.getCurrentTime(), 1e-3);

		builder.addTransit("A", "AS1", "AS3");
		assertEquals(2500.0 + 2000.0, builder.getCurrentTime(), 1e-3);

		builder.addWalk(200.0);
		assertEquals(4700.0, builder.getCurrentTime(), 1e-3);

		/*-
		 * Find next departure:
		 * - Travel on route A from AS1 to AS3 (departure 5000.0, TT 2000.0)		
		 */
		ScheduleHeadwayCalculator calculator = new ScheduleHeadwayCalculator(null, schedule);
		double nextArrivalTime = calculator.calculateNextArrival(builder.getLegs());

		assertEquals(5000.0 + 2000.0 + 200.0, nextArrivalTime, 1e-3);
	}

	@Test
	public void testAlternativeRoute() {
		/*-
		 * Build schedule:
		 * - Line A, Route A1 (AS1, AS2, AS3, AS4), inter-stop time 1000s, departures @ 0, 2500, 5000
		 * - Line A, Route A2 (AS1, AS2, AS3, AS4), inter-stop time 1000s, departures @ 4000, 6000
		 */
		TransitSchedule schedule = new ScheduleBuilder() //
				.addRoute( //
						"A", "A1", // Line ID, Route ID
						Arrays.asList("AS1", "AS2", "AS3", "AS4"), // Stop IDs
						1000.0, // Travel time between stops
						Arrays.asList(0.0, 2500.0, 5000.0)) // Route departures
				.addRoute( //
						"A", "A2", // Line ID, Route ID
						Arrays.asList("AS1", "AS2", "AS3", "AS4"), // Stop IDs
						1000.0, // Travel time between stops
						Arrays.asList(4000.0, 6000.0)) // Route departures
				.build();

		/*-
		 * Build trajectory:
		 * - Access 200s
		 * - Travel on route A1 from AS1 to AS3 (departure 2500, TT 2000)
		 * - Egress 200s
		 */
		LegsBuilder builder = new LegsBuilder(schedule);
		builder.addWalk(200.0);
		assertEquals(200.0, builder.getCurrentTime(), 1e-3);

		builder.addTransit("A1", "AS1", "AS3");
		assertEquals(2500.0 + 2000.0, builder.getCurrentTime(), 1e-3);

		builder.addWalk(200.0);
		assertEquals(4700.0, builder.getCurrentTime(), 1e-3);

		/*-
		 * Find next departure:
		 * - Travel on route A2 from AS1 to AS3 (departure 4000.0, TT 2000.0)		
		 */
		ScheduleHeadwayCalculator calculator = new ScheduleHeadwayCalculator(null, schedule);
		double nextArrivalTime = calculator.calculateNextArrival(builder.getLegs());

		assertEquals(4000.0 + 2000.0 + 200.0, nextArrivalTime, 1e-3);
	}

	@Test
	public void testTransfer() {
		/*-
		 * Build schedule:
		 * - Line A, Route A (AS1, AS2, AS3, AS4), inter-stop time 1000s, departures @ 0, 2500, 5000
		 * - Line B, Route B (BS1, BS2, BS3, BS4), inter-stop time 1000s, departures @ 3600, 5600, 8600, 10600
		 * - Interchange AS3 -> BS2
		 */
		TransitSchedule schedule = new ScheduleBuilder() //
				.addRoute( //
						"A", "A", // Line ID, Route ID
						Arrays.asList("AS1", "AS2", "AS3", "AS4"), // Stop IDs
						1000.0, // Travel time between stops
						Arrays.asList(0.0, 2500.0, 5000.0)) // Route departures
				.addRoute( //
						"B", "B", // Line ID, Route ID
						Arrays.asList("BS1", "BS2", "BS3", "BS4"), // Stop IDs
						1000.0, // Travel time between stops
						Arrays.asList(3600.0, 5600.0, 8600.0, 10600.0)) // Route departures
				.build();

		/*-
		 * Build trajectory:
		 * - Access 200s
		 * - Travel on route A from AS1 to AS3 (departure 2500, TT 2000) -> 4500
		 * - Interchange 50s -> 4550
		 * - Travel on route B from BS2 to BS4 (departure 4600, TT 2000) -> 6600
		 * - Egress 200s
		 */
		LegsBuilder builder = new LegsBuilder(schedule);
		builder.addWalk(200.0);
		assertEquals(200.0, builder.getCurrentTime(), 1e-3);

		builder.addTransit("A", "AS1", "AS3");
		assertEquals(2500.0 + 2000.0, builder.getCurrentTime(), 1e-3);
		
		builder.addWalk(50.0);
		assertEquals(4500.0 + 50.0, builder.getCurrentTime(), 1e-3);
		
		builder.addTransit("B", "BS2", "BS4");
		assertEquals(3600.0 + 1000.0 + 2000.0, builder.getCurrentTime(), 1e-3);

		builder.addWalk(200.0);
		assertEquals(6600.0 + 200.0, builder.getCurrentTime(), 1e-3);

		/*-
		 * Find next departure:
		 * - Travel on route A from AS1 to AS3 (departure 4000.0, TT 2000.0) -> 6000.0
		 * - Travel on route B from BS2 to BS4 (departure 6600.0, TT 2000.0) -> 8000.0		
		 */
		ScheduleHeadwayCalculator calculator = new ScheduleHeadwayCalculator(null, schedule);
		double nextArrivalTime = calculator.calculateNextArrival(builder.getLegs());

		// Departure + going to BS2 + on board + egress
		assertEquals(8600.0 + 1000.0 + 2000.0 + 200.0, nextArrivalTime, 1e-3);
	}

	private class LegsBuilder {
		private final TransitSchedule schedule;

		private final List<Leg> legs = new LinkedList<>();
		private double currentTime = 0.0;

		LegsBuilder(TransitSchedule schedule) {
			this.schedule = schedule;
		}

		Leg addWalk(double duration) {
			Leg leg = PopulationUtils.createLeg("walk");
			leg.setTravelTime(duration);

			legs.add(leg);
			currentTime += duration;

			return leg;
		}

		Leg addTransit(String routeId, String accessStopId, String egressStopId) {
			TransitLine transitLine = null;
			TransitRoute transitRoute = null;

			for (TransitLine candidateLine : schedule.getTransitLines().values()) {
				for (TransitRoute candidateRoute : candidateLine.getRoutes().values()) {
					if (candidateRoute.getId().toString().equals(routeId)) {
						transitLine = candidateLine;
						transitRoute = candidateRoute;
					}
				}
			}

			Verify.verifyNotNull(transitLine);
			Verify.verifyNotNull(transitRoute);

			Id<TransitStopFacility> accessFacilityId = Id.create(accessStopId, TransitStopFacility.class);
			Id<TransitStopFacility> egressFacilityId = Id.create(egressStopId, TransitStopFacility.class);

			TransitStopFacility accessFacility = schedule.getFacilities().get(accessFacilityId);
			TransitStopFacility egressFacility = schedule.getFacilities().get(egressFacilityId);

			TransitRouteStop accessStop = transitRoute.getStop(accessFacility);
			TransitRouteStop egressStop = transitRoute.getStop(egressFacility);

			double accessDepartureOffset = accessStop.getDepartureOffset().seconds();
			double egressArrivalOffset = egressStop.getArrivalOffset().seconds();

			Departure departure = null;

			for (Departure candidate : transitRoute.getDepartures().values()) {
				if (candidate.getDepartureTime() + accessDepartureOffset >= currentTime) {
					departure = candidate;
					break;
				}
			}

			double boardingTime = departure.getDepartureTime() + accessDepartureOffset;
			double arrivalTime = departure.getDepartureTime() + egressArrivalOffset;

			DefaultTransitPassengerRoute passengerRoute = new DefaultTransitPassengerRoute(accessFacility, transitLine,
					transitRoute, egressFacility);
			passengerRoute.setBoardingTime(boardingTime);

			Leg leg = PopulationUtils.createLeg("pt");
			leg.setDepartureTime(currentTime);
			leg.setRoute(passengerRoute);
			leg.setTravelTime(arrivalTime - currentTime);

			legs.add(leg);
			currentTime = arrivalTime;

			return leg;
		}

		double getCurrentTime() {
			return currentTime;
		}

		List<Leg> getLegs() {
			return legs;
		}
	}

	private class ScheduleBuilder {
		private final TransitScheduleFactory factory = new TransitScheduleFactoryImpl();
		private final Set<String> routeIds = new HashSet<>();
		private final TransitSchedule schedule;

		ScheduleBuilder() {
			this.schedule = factory.createTransitSchedule();
		}

		ScheduleBuilder addRoute(String lineId, String routeId, List<String> stopIds, double travelTime,
				List<Double> departures) {
			// Create line if it doesn't exist
			Id<TransitLine> transitLineId = Id.create(lineId, TransitLine.class);
			TransitLine transitLine = schedule.getTransitLines().get(transitLineId);

			if (transitLine == null) {
				transitLine = factory.createTransitLine(transitLineId);
				schedule.addTransitLine(transitLine);
			}

			// Create route
			Verify.verify(routeIds.add(routeId));
			Id<TransitRoute> transitRouteId = Id.create(routeId, TransitRoute.class);

			List<TransitRouteStop> stops = new LinkedList<>();
			for (int k = 0; k < stopIds.size(); k++) {
				Id<TransitStopFacility> facilityId = Id.create(stopIds.get(k), TransitStopFacility.class);

				TransitStopFacility facility = schedule.getFacilities().get(facilityId);

				if (facility == null) {
					facility = factory.createTransitStopFacility(facilityId, null, false);
					schedule.addStopFacility(facility);
				}

				TransitRouteStop stop = factory.createTransitRouteStop(facility, travelTime * k, travelTime * k);
				stops.add(stop);
			}

			TransitRoute transitRoute = factory.createTransitRoute(transitRouteId, null, stops, "pt");
			transitLine.addRoute(transitRoute);

			for (int k = 0; k < departures.size(); k++) {
				Id<Departure> departureId = Id.create("d:" + routeId + ":" + k, Departure.class);
				Departure departure = factory.createDeparture(departureId, departures.get(k));
				transitRoute.addDeparture(departure);
			}

			return this;
		}

		TransitSchedule build() {
			return schedule;
		}
	}
}
