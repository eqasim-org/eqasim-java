package org.eqasim.core.components.network_calibration.freespeed_calibration;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eqasim.core.components.network_calibration.LinkCategorizer;
import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;
import org.eqasim.core.components.network_calibration.NetworkCalibrationUtils;
import org.eqasim.core.components.network_calibration.cost_calibration.PenaltiesAdapter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

/** Uses MATSim's real final network writer to verify listener ordering and restart safety. */
public class TestFreespeedNetworkOutput {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    private static final double BASE_SPEED = 10;
    private static final double INITIAL_FACTOR = 0.8;

    @Test
    public void fixedFactorsAreAppliedExactlyOnceAcrossRepeatedRestarts() throws Exception {
        Network network = network();
        for (int run = 0; run < 3; run++) {
            network = run(network, false, null).network();
            assertNetwork(network, INITIAL_FACTOR);
        }
    }

    @Test
    public void calibratedFactorsAreStoredBeforeFinalNetworkIsWritten() throws Exception {
        Result result = run(network(), true, null);
        assertNotEquals(INITIAL_FACTOR, result.factor(), 1e-9);
        assertNetwork(result.network(), result.factor());
        assertNetwork(run(result.network(), false, null).network(), result.factor());
    }

    @Test
    public void csvOverridesArePersistedAndReusableWithoutTheCsv() throws Exception {
        Path csv = temp.newFile("factors.csv").toPath();
        Files.writeString(csv, "category;municipalityType;specialRegion;factor\n2;urban;0;1.1\n");
        Result result = run(network(), false, csv);
        assertNetwork(result.network(), 1.1);
        Files.delete(csv);
        assertNetwork(run(result.network(), false, null).network(), 1.1);
    }

    private Result run(Network input, boolean calibrating, Path csv) throws Exception {
        var calibration = new NetworkCalibrationConfigGroup();
        calibration.setActivate(true);
        calibration.setObjective("freespeed");
        calibration.setCalibrate(calibrating ? "freespeed" : "");
        var freespeed = calibration.getFreeSpeedCalibrationConfigGroup();
        freespeed.setWarmupIterations(1);
        freespeed.setUpdateInterval(1);
        freespeed.setMinTripsPerGroup(1);
        freespeed.setObservedTripsFile("unused.csv");
        if (!calibrating && csv == null) freespeed.setSpecialRegionPath("missing-region.geojson");
        if (csv != null) freespeed.setFactorsFile(csv.toString());
        var config = ConfigUtils.createConfig(calibration);
        Path output = temp.getRoot().toPath().resolve("run-" + System.nanoTime());
        config.controller().setOutputDirectory(output.toString());
        config.controller().setLastIteration(1);
        config.controller().setCreateGraphs(false);
        config.controller().setWriteEventsInterval(0);
        config.controller().setWritePlansInterval(0);
        config.controller().setCompressionType(org.matsim.core.config.groups.ControllerConfigGroup.CompressionType.gzip);
        var scenario = ScenarioUtils.createScenario(config);
        input.getNodes().values().forEach(scenario.getNetwork()::addNode);
        input.getLinks().values().forEach(scenario.getNetwork()::addLink);
        Network network = scenario.getNetwork();
        var categorizer = new LinkCategorizer(network, calibration);
        var manager = new FreespeedFactorManager(calibration, freespeed);
        var key = categorizer.getFreespeedCalibrationKey(network.getLinks().get(Id.createLinkId("road")));
        var trips = mock(TripsHandler.class);
        var stats = new FreespeedFactorManager.GroupStats(freespeed);
        stats.addStat(20, 10, 1, 100, BASE_SPEED);
        when(trips.routeTrips()).thenReturn(Map.of(key, stats));
        List<Double> simulatedSpeeds = new ArrayList<>();
        var controller = new Controler(scenario);
        controller.addOverridingModule(new AbstractModule() {
            @Override public void install() {
                // Exercise the controller lifecycle and actual output writer, without traffic.
                bind(Mobsim.class).toInstance(() -> simulatedSpeeds.add(
                        network.getLinks().get(Id.createLinkId("road")).getFreespeed()));
                addControllerListenerBinding().to(FreespeedAdapter.class);
            }

            @Provides @Singleton
            FreespeedAdapter adapter(OutputDirectoryHierarchy io) {
                return new FreespeedAdapter(network, calibration, freespeed, io, categorizer,
                        manager, mock(PenaltiesAdapter.class), trips);
            }
        });
        controller.run();
        assertEquals(2, simulatedSpeeds.size());
        double finalFactor = !calibrating && csv == null
                ? NetworkCalibrationUtils.readDoubleAttribute(network.getLinks().get(Id.createLinkId("road")),
                        NetworkCalibrationUtils.SPEED_FACTOR_ATTRIBUTE).orElse(1.0)
                : manager.getFactor(key);
        assertEquals(BASE_SPEED * finalFactor, simulatedSpeeds.getLast(), 1e-9);
        assertEquals(BASE_SPEED, network.getLinks().get(Id.createLinkId("road")).getFreespeed(), 0);
        if (calibrating) verify(trips).routeTrips();
        else verifyNoInteractions(trips);
        Network saved = NetworkUtils.createNetwork();
        new MatsimNetworkReader(saved).readFile(output.resolve("output_network.xml.gz").toString());
        return new Result(saved, finalFactor);
    }

    private static Network network() {
        Network network = NetworkUtils.createNetwork();
        var factory = network.getFactory();
        var a = factory.createNode(Id.createNodeId("a"), new Coord(0, 0));
        var b = factory.createNode(Id.createNodeId("b"), new Coord(100, 0));
        network.addNode(a);
        network.addNode(b);
        var road = factory.createLink(Id.createLinkId("road"), a, b);
        road.setLength(100);
        road.setFreespeed(BASE_SPEED);
        road.setCapacity(1000);
        road.setNumberOfLanes(1);
        road.setAllowedModes(Set.of("car"));
        road.getAttributes().putAttribute("osm:way:highway", "primary");
        road.getAttributes().putAttribute("municipalityType", "urban");
        road.getAttributes().putAttribute(NetworkCalibrationUtils.SPEED_FACTOR_ATTRIBUTE, INITIAL_FACTOR);
        network.addLink(road);
        var reverse = factory.createLink(Id.createLinkId("reverse"), b, a);
        reverse.setLength(100);
        reverse.setFreespeed(17);
        reverse.setCapacity(1000);
        reverse.setNumberOfLanes(1);
        reverse.setAllowedModes(Set.of("car"));
        reverse.getAttributes().putAttribute("osm:way:highway", "primary");
        reverse.getAttributes().putAttribute("municipalityType", "urban");
        reverse.getAttributes().putAttribute(NetworkCalibrationUtils.SPEED_FACTOR_ATTRIBUTE, INITIAL_FACTOR);
        network.addLink(reverse);
        var bike = factory.createLink(Id.createLinkId("bike"), b, a);
        bike.setLength(100);
        bike.setFreespeed(4);
        bike.setCapacity(1000);
        bike.setNumberOfLanes(1);
        bike.setAllowedModes(Set.of("bike"));
        network.addLink(bike);
        return network;
    }

    private static void assertNetwork(Network network, double factor) {
        var road = network.getLinks().get(Id.createLinkId("road"));
        assertEquals(BASE_SPEED, road.getFreespeed(), 0);
        assertEquals(factor, NetworkCalibrationUtils.readDoubleAttribute(
                road, NetworkCalibrationUtils.SPEED_FACTOR_ATTRIBUTE).orElseThrow(), 1e-9);
        var reverse = network.getLinks().get(Id.createLinkId("reverse"));
        assertEquals(17, reverse.getFreespeed(), 0);
        assertEquals(factor, NetworkCalibrationUtils.readDoubleAttribute(
                reverse, NetworkCalibrationUtils.SPEED_FACTOR_ATTRIBUTE).orElseThrow(), 1e-9);
        var bike = network.getLinks().get(Id.createLinkId("bike"));
        assertEquals(4, bike.getFreespeed(), 0);
        assertNull(bike.getAttributes().getAttribute(NetworkCalibrationUtils.SPEED_FACTOR_ATTRIBUTE));
    }

    private record Result(Network network, double factor) { }
}
