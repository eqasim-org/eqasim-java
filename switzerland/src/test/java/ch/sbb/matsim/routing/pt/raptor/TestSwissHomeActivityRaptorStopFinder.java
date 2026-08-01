package ch.sbb.matsim.routing.pt.raptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eqasim.switzerland.ch_cmdp.config.SwissIntermodalAccessEgressConfigGroup;
import org.eqasim.switzerland.ch_cmdp.routing.IntermodalVehicleRoutingAttributes;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

public class TestSwissHomeActivityRaptorStopFinder {
	@Test
	public void testFiltersRequiredEgressModeAndStop() {
		SwissIntermodalAccessEgressConfigGroup config = new SwissIntermodalAccessEgressConfigGroup();
		config.setRestrictVehicleToHomeActivity(false);
		SwissHomeActivityRaptorStopFinder finder = new SwissHomeActivityRaptorStopFinder(new FakeStopFinder(), config);

		Attributes attributes = new AttributesImpl();
		attributes.putAttribute(IntermodalVehicleRoutingAttributes.REQUIRED_EGRESS_MODE, TransportMode.bike);
		attributes.putAttribute(IntermodalVehicleRoutingAttributes.REQUIRED_EGRESS_STOP_ID, "required_stop");

		List<InitialStop> stops = finder.findStops(null, null, null, 0.0, attributes, null, null,
				RaptorStopFinder.Direction.EGRESS);

		assertEquals(1, stops.size());
		assertEquals("required_stop", stops.get(0).stop.getId().toString());
	}

	@Test
	public void testFiltersForbiddenAccessMode() {
		SwissIntermodalAccessEgressConfigGroup config = new SwissIntermodalAccessEgressConfigGroup();
		config.setRestrictVehicleToHomeActivity(false);
		SwissHomeActivityRaptorStopFinder finder = new SwissHomeActivityRaptorStopFinder(new FakeStopFinder(), config);

		Attributes attributes = new AttributesImpl();
		attributes.putAttribute(IntermodalVehicleRoutingAttributes.FORBIDDEN_ACCESS_MODE, TransportMode.bike);

		List<InitialStop> stops = finder.findStops(null, null, null, 0.0, attributes, null, null,
				RaptorStopFinder.Direction.ACCESS);

		assertEquals(2, stops.size());
		assertFalse(containsMode(stops, TransportMode.bike));
		assertTrue(containsMode(stops, TransportMode.walk));
		assertTrue(containsMode(stops, TransportMode.car));
	}

	@Test
	public void testFiltersMultipleForbiddenAccessModes() {
		SwissIntermodalAccessEgressConfigGroup config = new SwissIntermodalAccessEgressConfigGroup();
		config.setRestrictVehicleToHomeActivity(false);
		SwissHomeActivityRaptorStopFinder finder = new SwissHomeActivityRaptorStopFinder(new FakeStopFinder(), config);

		Attributes attributes = new AttributesImpl();
		attributes.putAttribute(IntermodalVehicleRoutingAttributes.FORBIDDEN_ACCESS_MODE,
				TransportMode.bike + "," + TransportMode.car);

		List<InitialStop> stops = finder.findStops(null, null, null, 0.0, attributes, null, null,
				RaptorStopFinder.Direction.ACCESS);

		assertEquals(1, stops.size());
		assertEquals(TransportMode.walk, ((Leg) stops.get(0).planElements.get(0)).getMode());
	}

	@Test
	public void testRestrictsConfiguredIntermodalVehicleModesAwayFromHome() {
		SwissIntermodalAccessEgressConfigGroup config = new SwissIntermodalAccessEgressConfigGroup();
		config.setRestrictedIntermodalAccessEgressModes(TransportMode.bike + "," + TransportMode.car);
		SwissHomeActivityRaptorStopFinder finder = new SwissHomeActivityRaptorStopFinder(new FakeStopFinder(), config);

		List<InitialStop> stops = finder.findStops(null, null, null, 0.0, null, null, null,
				RaptorStopFinder.Direction.ACCESS);

		assertEquals(1, stops.size());
		assertEquals(TransportMode.walk, ((Leg) stops.get(0).planElements.get(0)).getMode());
	}

	static private class FakeStopFinder implements RaptorStopFinder {
		@Override
		public List<InitialStop> findStops(Facility fromFacility, Facility toFacility, Person person,
				double departureTime, Attributes routingAttributes, RaptorParameters parameters, SwissRailRaptorData data,
				Direction type) {
			return List.of(new InitialStop(createStop("required_stop"), 0.0, 0.0, List.of(createLeg(TransportMode.bike))),
					new InitialStop(createStop("required_stop"), 0.0, 0.0, List.of(createLeg(TransportMode.walk))),
					new InitialStop(createStop("other_stop"), 0.0, 0.0, List.of(createLeg(TransportMode.bike))),
					new InitialStop(createStop("other_stop"), 0.0, 0.0, List.of(createLeg(TransportMode.car))));
		}
	}

	static private TransitStopFacility createStop(String id) {
		TransitScheduleFactory factory = new TransitScheduleFactoryImpl();
		return factory.createTransitStopFacility(Id.create(id, TransitStopFacility.class), new Coord(0.0, 0.0), false);
	}

	static private PlanElement createLeg(String mode) {
		Leg leg = PopulationUtils.createLeg(mode);
		leg.setTravelTime(0.0);
		return leg;
	}

	static private boolean containsMode(List<InitialStop> stops, String mode) {
		for (InitialStop stop : stops) {
			if (mode.equals(((Leg) stop.planElements.get(0)).getMode())) {
				return true;
			}
		}

		return false;
	}
}
