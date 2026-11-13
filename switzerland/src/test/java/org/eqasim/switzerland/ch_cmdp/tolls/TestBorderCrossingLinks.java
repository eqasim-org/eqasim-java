package org.eqasim.switzerland.ch_cmdp.tolls;

import static org.junit.Assert.*;
import java.util.List;
import org.eqasim.core.components.travel_disutility.EqasimTravelDisutilityFactory;
import org.eqasim.switzerland.ch_cmdp.memory.ScenarioAttributeCleaner;
import org.eqasim.switzerland.ch_cmdp.trucks.TruckTravelDisutilityFactory;
import org.junit.Test;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;

public class TestBorderCrossingLinks {
    private static final double BORDER = borderCrossingLinks.DISUTILITY_SECONDS;
    private static Link link(Network network, String id) {
        if (network.getNodes().isEmpty()) {
            NetworkUtils.createAndAddNode(network, Id.createNodeId(0), new Coord(0, 0));
            NetworkUtils.createAndAddNode(network, Id.createNodeId(1), new Coord(1000, 0));
        }
        var link = NetworkUtils.createAndAddLink(network, Id.createLinkId(id), network.getNodes().get(Id.createNodeId(0)),
                network.getNodes().get(Id.createNodeId(1)), 1000, 10, 1000, 1);
        link.getAttributes().putAttribute("osm:way:highway", "secondary");
        return link;
    }

    @Test public void preprocessingKeepsOnlyMarkedIdsAndLookupWorksOnNetworkCopies() {
        var network = NetworkUtils.createNetwork();
        var marked = link(network, "marked"); marked.getAttributes().putAttribute("ch_border_crossing", true);
        var text = link(network, "text"); text.getAttributes().putAttribute("ch_border_crossing", "true");
        var unmarked = link(network, "false"); unmarked.getAttributes().putAttribute("ch_border_crossing", false);
        var absent = link(network, "absent");
        var borders = new borderCrossingLinks(network);
        assertEquals(2, borders.getLinkIds().size());
        assertEquals(BORDER, borders.getDisutility(marked), 0); assertEquals(BORDER, borders.getDisutility(text), 0);
        assertEquals(0, borders.getDisutility(unmarked), 0); assertEquals(0, borders.getDisutility(absent), 0);
        // Filtered/copy networks use different Link objects with the same immutable IDs.
        var copy = link(NetworkUtils.createNetwork(), "marked");
        assertEquals(BORDER, borders.getDisutility(copy), 0);
        marked.getAttributes().removeAttribute("ch_border_crossing");
        assertEquals(BORDER, borders.getDisutility(marked), 0);
        assertThrows(UnsupportedOperationException.class, () -> borders.getLinkIds().clear());
    }

    @Test public void carAndTruckAddFixedBorderCostOutsideRandomnessAndTolls() {
        var network = NetworkUtils.createNetwork(); var link = link(network, "border");
        link.getAttributes().putAttribute("ch_border_crossing", true); link.getAttributes().putAttribute("toll", 7.0);
        var marked = new borderCrossingLinks(network); var empty = new borderCrossingLinks(List.of());
        var marginal = new MarginalCostOfTolls(0, 3600, 4711); var tolls = new Tolls();
        var carBase = new EqasimTravelDisutilityFactory((l, p, t, c) -> 50, 2, .3, 4711);
        TravelTime time = (l, t, p, v) -> 200;
        var car = new EqasimTollsTravelDisutilityFactory(carBase, tolls, marginal, marked).createTravelDisutility(time);
        var carWithout = new EqasimTollsTravelDisutilityFactory(carBase, tolls, marginal, empty).createTravelDisutility(time);
        var truck = new TruckTravelDisutilityFactory(2, .3, 4711, tolls, marginal, marked).createTravelDisutility(time);
        var truckWithout = new TruckTravelDisutilityFactory(2, .3, 4711, tolls, marginal, empty).createTravelDisutility(time);
        for (int i = 0; i < 100; i++) {
            var person = org.matsim.core.population.PopulationUtils.getFactory().createPerson(Id.createPersonId(i));
            assertEquals(BORDER, car.getLinkTravelDisutility(link, 900, person, null) - carWithout.getLinkTravelDisutility(link, 900, person, null), 1e-9);
            assertEquals(BORDER, truck.getLinkTravelDisutility(link, 900, person, null) - truckWithout.getLinkTravelDisutility(link, 900, person, null), 1e-9);
        }
        assertEquals(BORDER, car.getLinkMinimumTravelDisutility(link) - carWithout.getLinkMinimumTravelDisutility(link), 0);
        assertEquals(BORDER, truck.getLinkMinimumTravelDisutility(link) - truckWithout.getLinkMinimumTravelDisutility(link), 0);
        assertEquals(200, time.getLinkTravelTime(link, 900, null, null), 0);
    }

    @Test public void scenarioCleanupPreservesBorderMarkerBeforeIndexConstruction() {
        var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig()); var link = link(scenario.getNetwork(), "border");
        link.getAttributes().putAttribute("ch_border_crossing", true);
        ScenarioAttributeCleaner.clean(scenario);
        assertEquals(Boolean.TRUE, link.getAttributes().getAttribute("ch_border_crossing"));
        assertEquals(BORDER, new borderCrossingLinks(scenario.getNetwork()).getDisutility(link), 0);
    }

    @Test public void simulationModulesShareOneIndexForBothModes() throws Exception {
        var settings = new org.eqasim.core.components.config.EqasimConfigGroup();
        settings.setRoutingDistanceUtility(0); settings.setSigmaRoutingRandomness(0);
        var config = ConfigUtils.createConfig(settings);
        config.routing().setNetworkModes(List.of("car", "truck"));
        config.controller().setOutputDirectory(java.nio.file.Files.createTempDirectory("border-index-test").toString());
        var scenario = ScenarioUtils.createScenario(config); var link = link(scenario.getNetwork(), "border");
        link.getAttributes().putAttribute("ch_border_crossing", true);
        link.getAttributes().putAttribute("osm:way:highway", "motorway");
        var controller = new org.matsim.core.controler.Controler(scenario);
        controller.addOverridingModule(new org.matsim.core.controler.AbstractModule() {
            @Override public void install() {
                bind(EqasimTravelDisutilityFactory.class).toInstance(new EqasimTravelDisutilityFactory((l, p, t, base) -> 0, 0));
            }
        });
        controller.addOverridingModule(new TollsModule());
        controller.addOverridingModule(new org.eqasim.switzerland.ch_cmdp.trucks.TruckRoutingModule());
        var injector = controller.getInjector();
        var borders = injector.getInstance(borderCrossingLinks.class);
        assertSame(borders, injector.getInstance(borderCrossingLinks.class));
        link.getAttributes().removeAttribute("ch_border_crossing");
        var factories = injector.getInstance(com.google.inject.Key.get(new com.google.inject.TypeLiteral<java.util.Map<String,
                org.matsim.core.router.costcalculators.TravelDisutilityFactory>>() {}));
        TravelTime time = (l, t, p, v) -> 100;
        assertEquals(100 + BORDER, factories.get("car").createTravelDisutility(time).getLinkTravelDisutility(link, 0, null, null), 0);
        assertEquals(100 + BORDER, factories.get("truck").createTravelDisutility(time).getLinkTravelDisutility(link, 0, null, null), 0);
    }
}
