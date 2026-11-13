package org.eqasim.switzerland.ch.mode_choice.costs;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.switzerland.ch.mode_choice.SwissModeChoiceModule;
import org.eqasim.switzerland.ch.mode_choice.parameters.SwissCostParameters;
import org.eqasim.switzerland.ch.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.RegionalSubscription;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.Authority;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.NetworkOfDistances;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.ZonalRegistry;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.Zone;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPtRoutePredictor;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

/**
 * The PT price of the ch scenario: a ZVV ride A - B - C - D (Swiss stops 8500001 to 8500004) (zones 110, 110, 121, 122; 110 counts twice), priced with
 * the bundled pricing description. A regional subscription covers the zones of RegionalSubscriptionProducts, and no
 * longer the trips within ptRegionalRadius_km of home: every traveller here lives at the origin.
 */
public class TestChPtCostModel {
    private static final Authority ZVV = new Authority("ZVV", 2, "ZoneBased");
    private static final String[] STOPS = { "8500001", "8500002", "8500003", "8500004" };

    private final TransitSchedule schedule = schedule();
    private final ZonalRegistry registry = new ZonalRegistry(List.of(ZVV), List.of(
            new Zone(ZVV, "110", List.of("8500001", "8500002")),
            new Zone(ZVV, "121", List.of("8500003")),
            new Zone(ZVV, "122", List.of("8500004"))));

    private final SwissPtCostModel model = new SwissPtCostModel(SwissCostParameters.buildDefault(),
            new SwissPersonPredictor(new PersonPredictor()), new SwissPtRoutePredictor(schedule, registry, new NetworkOfDistances()),
            SwissModeChoiceModule.createDefaultPtStageCostCalculator());

    private static TransitSchedule schedule() {
        TransitSchedule schedule = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getTransitSchedule();
        TransitScheduleFactory factory = schedule.getFactory();
        List<TransitRouteStop> stops = new ArrayList<>();
        for (int i = 0; i < STOPS.length; i++) {
            TransitStopFacility facility = factory.createTransitStopFacility(Id.create(STOPS[i], TransitStopFacility.class),
                    new Coord(1000 * i, 0), false);
            facility.setName(STOPS[i]);
            schedule.addStopFacility(facility);
            stops.add(factory.createTransitRouteStop(facility, i * 60.0, i * 60.0));
        }
        Id<Link> link = Id.createLinkId("l");
        TransitRoute route = factory.createTransitRoute(Id.create("route", TransitRoute.class),
                RouteUtils.createLinkNetworkRouteImpl(link, link), stops, "bus");
        TransitLine line = factory.createTransitLine(Id.create("line", TransitLine.class));
        line.addRoute(route);
        schedule.addTransitLine(line);
        return schedule;
    }

    private double price(Boolean ptHasVerbund, String zones) {
        Map<String, Object> attributes = new HashMap<>();
        if (ptHasVerbund != null) attributes.put("ptHasVerbund", ptHasVerbund);
        if (zones != null) attributes.put(RegionalSubscription.ATTRIBUTE, zones);
        return price(attributes);
    }

    /** The price for a person of 30 living at the origin, with these attributes. */
    private double price(Map<String, Object> attributes) {
        Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId("p"));
        person.getAttributes().putAttribute("age", 30);
        person.getAttributes().putAttribute("home_x", 0.0);
        person.getAttributes().putAttribute("home_y", 0.0);
        attributes.forEach(person.getAttributes()::putAttribute);

        DefaultTransitPassengerRoute route = new DefaultTransitPassengerRoute(Id.createLinkId("l"), Id.createLinkId("l"),
                Id.create("8500001", TransitStopFacility.class), Id.create("8500004", TransitStopFacility.class),
                Id.create("line", TransitLine.class), Id.create("route", TransitRoute.class));
        route.setDistance(3000.0);
        Leg leg = PopulationUtils.createLeg("pt");
        leg.setDepartureTime(8 * 3600.0);
        leg.setTravelTime(600.0);
        leg.setRoute(route);

        Activity origin = PopulationUtils.createActivityFromCoord("home", new Coord(0, 0));
        Activity destination = PopulationUtils.createActivityFromCoord("work", new Coord(3000, 0));
        var trip = new DiscreteModeChoiceTrip(origin, destination, "pt", List.of(leg), 0, 0, 0, new AttributesImpl());
        return model.calculateCost_MU(person, trip, List.of(leg));
    }

    @Test
    public void tripIsPricedOnTheZonesOutsideTheSubscription() {
        assertEquals(9.40, price(null, null), 1e-9);                         // 110 (twice), 121, 122: 4 zones
        assertEquals(9.40, price(true, null), 1e-9);                         // a Verbundabo without its zones
        assertEquals(9.40, price(true, "ZVV:151"), 1e-9);                    // other zones, although close to home
        assertEquals(4.70, price(true, "ZVV:110"), 1e-9);                    // B - D in 121 and 122
        assertEquals(0.00, price(true, "ZVV:110,121,122"), 1e-9);
        assertEquals(9.40, price(null, "ZVV:110,121,122"), 1e-9);            // the zones alone are not a Verbundabo (as ch_cmdp)
    }

    @Test
    public void subscriptionsOfTheCurrentAndOlderPopulations() {
        // the subscriptions attribute of the current populations (as ch_cmdp): none, GA, HT, VA, VA+HT
        assertEquals(9.40, price(Map.of("subscriptions", "none")), 1e-9);
        assertEquals(0.00, price(Map.of("subscriptions", "GA")), 1e-9);
        assertEquals(4.70, price(Map.of("subscriptions", "HT")), 1e-9);              // ZVV half fare, 4 zones
        assertEquals(4.70, price(Map.of("subscriptions", "VA", RegionalSubscription.ATTRIBUTE, "ZVV:110")), 1e-9);
        assertEquals(3.30, price(Map.of("subscriptions", "VA+HT", RegionalSubscription.ATTRIBUTE, "ZVV:110")), 1e-9);

        // the boolean attributes of the older populations, when there is no subscriptions attribute
        assertEquals(0.00, price(Map.of("ptHasGA", true)), 1e-9);
        assertEquals(4.70, price(Map.of("ptHasHalbtax", true)), 1e-9);
        assertEquals(9.40, price(Map.of("ptHasGA", false, "ptHasHalbtax", false)), 1e-9);

        // the subscriptions attribute wins over the older ones
        assertEquals(9.40, price(Map.of("subscriptions", "none", "ptHasGA", true)), 1e-9);
    }
}
