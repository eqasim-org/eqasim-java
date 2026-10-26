package org.eqasim.switzerland.ch_cmdp.routing.pt_cache;

import java.util.*;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.eqasim.core.components.raptor.EqasimRaptorConfigGroup;
import org.eqasim.core.components.raptor.EqasimRaptorModule;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.*;

import static org.junit.Assert.*;

public class TestPtRouteCacheIntegration {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void realRouterRetainsTransferCostsAndRestoresBoardingInformationFromDisk() throws Exception { check(true, 0); }
    @Test public void memoryTemplatesRetainRealTransitRoutesAndBoardingInformation() throws Exception { check(true, 1); }
    @Test public void disabledModuleLeavesTheExistingRouterActive() throws Exception { check(false, 0); }

    private void check(boolean enabled, int memoryMb) throws Exception {
        var raptor = new SwissRailRaptorConfigGroup();
        raptor.setIntermodalLegOnlyHandling(SwissRailRaptorConfigGroup.IntermodalLegOnlyHandling.forbid);
        raptor.addModeToModeTransferPenalty(new SwissRailRaptorConfigGroup.ModeToModeTransferPenalty("bus", "tram", 0.2));
        var settings = new PtRouteCacheConfigGroup(); settings.enabled = enabled; settings.memoryMb = memoryMb;
        var config = ConfigUtils.createConfig(raptor, settings, new EqasimRaptorConfigGroup());
        config.controller().setOutputDirectory(temporary.newFolder().getAbsolutePath());
        config.transit().setUseTransit(true);
        config.transitRouter().setSearchRadius(100); config.transitRouter().setExtensionRadius(100);
        var scenario = ScenarioUtils.createScenario(config);
        var network = scenario.getNetwork();
        for (int i = 0; i < 4; i++) NetworkUtils.createAndAddNode(network, Id.createNodeId(i), new Coord(1000 * i, 0));
        var schedule = scenario.getTransitSchedule(); var f = schedule.getFactory();
        var stops = new ArrayList<TransitStopFacility>();
        for (int i = 0; i < 3; i++) {
            var link = NetworkUtils.createAndAddLink(network, Id.createLinkId(i), network.getNodes().get(Id.createNodeId(i)),
                    network.getNodes().get(Id.createNodeId(i + 1)), 1000, 10, 1000, 1);
            link.setAllowedModes(Set.of("car", "bus", "tram"));
            var stop = f.createTransitStopFacility(Id.create("s" + i, TransitStopFacility.class), new Coord(500 + 1000 * i, 0), false);
            stop.setLinkId(link.getId()); schedule.addStopFacility(stop); stops.add(stop);
        }
        NetworkUtils.createAndAddLink(network, Id.createLinkId("return"), network.getNodes().get(Id.createNodeId(3)),
                network.getNodes().get(Id.createNodeId(0)), 3000, 10, 1000, 1);
        for (int i = 0; i < 2; i++) {
            var route = f.createTransitRoute(Id.create("r" + i, TransitRoute.class),
                    RouteUtils.createLinkNetworkRouteImpl(stops.get(i).getLinkId(), List.of(), stops.get(i + 1).getLinkId()),
                    List.of(f.createTransitRouteStop(stops.get(i), 0, 0), f.createTransitRouteStop(stops.get(i + 1), 100, 100)),
                    i == 0 ? "bus" : "tram");
            route.addDeparture(f.createDeparture(Id.create("d" + i, Departure.class), 100 + 200 * i));
            var line = f.createTransitLine(Id.create("l" + i, TransitLine.class));
            line.addRoute(route); schedule.addTransitLine(line);
        }
        var controller = new Controler(scenario);
        controller.addOverridingModule(new SwissRailRaptorModule());
        controller.addOverridingModule(new EqasimRaptorModule());
        controller.addOverridingModule(new PtRouteCacheModule());
        controller.addOverridingModule(new AbstractModule() {
            @Override public void install() {
                bind(org.eqasim.core.components.flow.FlowUtils.class).toInstance(new org.eqasim.core.components.flow.FlowUtils(scenario));
            }
        });
        var injector = controller.getInjector();
        var router = injector.getInstance(TripRouter.class);
        var from = FacilitiesUtils.wrapLinkAndCoord(network.getLinks().get(Id.createLinkId(0)), stops.get(0).getCoord());
        var to = FacilitiesUtils.wrapLinkAndCoord(network.getLinks().get(Id.createLinkId(2)), stops.get(2).getCoord());
        var person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("pt-person"));
        var request = DefaultRoutingRequest.withoutAttributes(from, to, 0, person);
        var first = router.calcRoute("pt", from, to, 0, person, request.getAttributes());
        var second = router.calcRoute("pt", from, to, 0, person, request.getAttributes());
        assertArrayEquals(PtItineraryCodec.encode(first), PtItineraryCodec.encode(second));
        var pt = second.stream().filter(Leg.class::isInstance).map(Leg.class::cast).map(Leg::getRoute)
                .filter(TransitPassengerRoute.class::isInstance).map(TransitPassengerRoute.class::cast).toList();
        assertEquals(2, pt.size()); assertEquals("r0", pt.get(0).getRouteId().toString());
        assertEquals("r1", pt.get(1).getRouteId().toString());
        assertTrue(pt.get(0).getBoardingTime().isDefined());
        if (enabled) {
            var cache = injector.getInstance(PtRouteCache.class);
            assertEquals(1, cache.statistics().misses());
            assertEquals(memoryMb == 0 ? 1 : 0, cache.statistics().diskHits());
            assertEquals(memoryMb == 0 ? 0 : 1, cache.statistics().memoryHits());
            assertNotSame(first.getFirst(), second.getFirst()); cache.close();
        }
    }
}
