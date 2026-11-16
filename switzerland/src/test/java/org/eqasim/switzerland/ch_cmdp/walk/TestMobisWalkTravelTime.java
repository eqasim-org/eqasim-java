package org.eqasim.switzerland.ch_cmdp.walk;

import static org.junit.Assert.*;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.*;
import org.matsim.core.config.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.FacilitiesUtils;

public class TestMobisWalkTravelTime {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void matchesOriginalPythonEvaluator() {
        var model = new MobisWalkTravelTime(1.4299713498762905, 3.1589037650039673,
                0.869597698757856, 0.002, 1.3106159889893567);
        // Reference values executed from the original model.py travel_time() with ESTIMATED_PARAMETERS.
        assertEquals(916.5330404016421, model.travelTime(1000, 0), 1e-9);
        assertEquals(1236.3462992226946, model.travelTime(1000, 100), 1e-9);
        assertEquals(1158.9905237717735, model.travelTime(1000, -100), 1e-9);
        assertEquals(916.5330404016421, model.travelTime(1000, 2.6212319779787134), 1e-9);
        assertEquals(1.4299713498762905, model.speed(.002), 0);
        assertEquals(1.4299713498762905, model.speed(-.002), 0);
        assertEquals(0, model.travelTime(0, 0), 0);
        assertThrows(IllegalArgumentException.class, () -> model.travelTime(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new MobisWalkTravelTime(0, 3, 1, .002, 1.3));
    }

    @Test public void usesActualEndpointXYAndNearestNodeZNotNodeDistanceOrEndpointZ() {
        var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        NetworkUtils.createAndAddNode(scenario.getNetwork(), Id.createNodeId("a"), new Coord(-1000, 0, 400));
        NetworkUtils.createAndAddNode(scenario.getNetwork(), Id.createNodeId("b"), new Coord(2000, 0, 500));
        var calculator = new WalkTravelTimeCalculator(scenario, new MobisWalkTravelTime());
        var from = new Coord(0, 0, 99999); var to = new Coord(1000, 0, -99999);
        assertEquals(1236.6182236978348, calculator.calculate(from, to, null, 0, 999), 1e-9);
        assertEquals(1159.2333238664232, calculator.calculate(to, from, null, 0, 999), 1e-9);
        assertEquals(0, calculator.calculate(from, from, null, 0, 999), 0);
    }

    @Test public void missingNodeElevationsUseFittedFlatSpeed() {
        var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        var calculator = new WalkTravelTimeCalculator(scenario, new MobisWalkTravelTime());
        assertEquals(916.7832167832169, calculator.calculate(new Coord(0, 0), new Coord(1000, 0), null, 0, 12), 1e-9);
        NetworkUtils.createAndAddNode(scenario.getNetwork(), Id.createNodeId(0), new Coord(0, 0));
        NetworkUtils.createAndAddNode(scenario.getNetwork(), Id.createNodeId(1), new Coord(1000, 0));
        calculator = new WalkTravelTimeCalculator(scenario, new MobisWalkTravelTime());
        assertEquals(916.7832167832169, calculator.calculate(new Coord(0, 0), new Coord(1000, 0), null, 0, 12), 1e-9);
    }

    @Test public void coincidentNearestNodeAndZeroElevationUseFlatSpeed() {
        var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        NetworkUtils.createAndAddNode(scenario.getNetwork(), Id.createNodeId("sea"), new Coord(0, 0, 0));
        var calculator = new WalkTravelTimeCalculator(scenario, new MobisWalkTravelTime());
        assertEquals(new MobisWalkTravelTime().travelTime(1000, 0),
                calculator.calculate(new Coord(0, 0), new Coord(1000, 0), null, 0, 12), 1e-9);
        NetworkUtils.createAndAddNode(scenario.getNetwork(), Id.createNodeId("hill"), new Coord(1000, 0, 100));
        calculator = new WalkTravelTimeCalculator(scenario, new MobisWalkTravelTime());
        assertEquals(916.7832167832169, calculator.calculate(new Coord(0, 0), new Coord(1000, 0), null, 0, 12), 1e-9);
        assertEquals(916.7832167832169, calculator.calculate(new Coord(1000, 0), new Coord(0, 0), null, 0, 12), 1e-9);
    }

    @Test public void selectionPersistsInConfigAndInstalledModuleUsesModel() throws Exception {
        var config = ConfigUtils.createConfig(new WalkConfigGroup());
        assertEquals("mobis", ConfigUtils.addOrGetModule(config, WalkConfigGroup.class).getModel());
        var settings = ConfigUtils.addOrGetModule(config, WalkConfigGroup.class);
        settings.setModel("mobis");
        settings.setV0MetersPerSecond(2);
        settings.setK(1.1);
        settings.setUphillPenalty(.4);
        settings.setFlatGrade(.01);
        settings.setDetourFactor(1.2);
        var path = temporary.newFile("walking.xml");
        ConfigUtils.writeConfig(config, path.toString());
        var restored = ConfigUtils.loadConfig(path.toString(), new WalkConfigGroup());
        var loaded = ConfigUtils.addOrGetModule(restored, WalkConfigGroup.class);
        assertEquals("mobis", loaded.getModel());
        assertEquals(2, loaded.getV0MetersPerSecond(), 0);
        assertEquals(1.1, loaded.getK(), 0);
        assertEquals(.4, loaded.getUphillPenalty(), 0);
        assertEquals(.01, loaded.getFlatGrade(), 0);
        assertEquals(1.2, loaded.getDetourFactor(), 0);
        assertThrows(IllegalArgumentException.class, () -> loaded.setModel("invalid"));
        assertThrows(IllegalArgumentException.class, () -> loaded.setK(-1));
        assertThrows(IllegalArgumentException.class, () -> loaded.setDetourFactor(.9));
        restored.controller().setOutputDirectory(temporary.newFolder("output").toString());
        var scenario = ScenarioUtils.createScenario(restored);
        var a = NetworkUtils.createAndAddNode(scenario.getNetwork(), Id.createNodeId("a"), new Coord(0, 0, 400));
        var b = NetworkUtils.createAndAddNode(scenario.getNetwork(), Id.createNodeId("b"), new Coord(1000, 0, 500));
        var link = NetworkUtils.createAndAddLink(scenario.getNetwork(), Id.createLinkId("ab"), a, b, 1000, 10, 1000, 1);
        var controller = new Controler(scenario);
        controller.addOverridingModule(new WalkModule());
        var injector = controller.getInjector();
        assertTrue(injector.getInstance(WalkTravelTime.class) instanceof MobisWalkTravelTime);
        var router = injector.getInstance(Key.get(RoutingModule.class, Names.named("walk")));
        var request = DefaultRoutingRequest.withoutAttributes(FacilitiesUtils.wrapLinkAndCoord(link, new Coord(0, 0)),
                FacilitiesUtils.wrapLinkAndCoord(link, new Coord(1000, 0)), 123, null);
        var leg = (org.matsim.api.core.v01.population.Leg) router.calcRoute(request).getFirst();
        assertEquals(669.7668422753228, leg.getTravelTime().seconds(), 1e-9);
        assertEquals(leg.getTravelTime().seconds(), leg.getRoute().getTravelTime().seconds(), 0);
    }
}
