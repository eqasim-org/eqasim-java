package org.eqasim.core.components.network_calibration;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.network_calibration.Processors.CountsProcessor;
import org.eqasim.core.components.network_calibration.Processors.FlowProcessor;
import org.eqasim.core.components.network_calibration.cost_calibration.*;
import org.eqasim.core.components.network_calibration.freespeed_calibration.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup.CompressionType;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class TestFixedRegionalCalibration {
    @Rule public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void regionalResultsSurviveOutputAndRestartsWithoutRegionFiles() throws Exception {
        Path region = region();
        Network saved = run(network(), true, region);
        assertSavedValues(saved);
        Files.delete(region);
        // An unchanged config may still name files that are unavailable on the new server.
        saved = run(saved, false, region);
        assertSavedValues(saved);
        // Removing the region paths altogether must give the same result.
        assertSavedValues(run(saved, false, null));
    }

    @Test
    public void onlyTheCalibratedComponentNeedsItsRegionalFiles() throws Exception {
        Path region = region();
        Network network = network();
        for (String objective : new String[] {"freespeed", "penalty"}) {
            var config = configuration(false, null);
            config.setCalibrate(objective);
            config.getFreeSpeedCalibrationConfigGroup().setSpecialRegionPath(
                    objective.equals("freespeed") ? region.toString() : "missing-speed-region.geojson");
            config.getCostCalibrationConfigGroup().setSpecialRegionPath(
                    objective.equals("penalty") ? region.toString() : "missing-penalty-region.geojson");
            var categorizer = new LinkCategorizer(network, config);
            var inside = network.getLinks().get(Id.createLinkId("0"));
            assertEquals(objective.equals("freespeed") ? 1 : 0, categorizer.getFreespeedSpecialRegionId(inside));
            assertEquals(objective.equals("penalty") ? 1 : 0, categorizer.getPenaltiesSpecialRegionId(inside));
        }
    }

    private Network run(Network input, boolean calibrating, Path region) throws Exception {
        var calibration = configuration(calibrating, region);
        var speedConfig = calibration.getFreeSpeedCalibrationConfigGroup();
        speedConfig.setObservedTripsFile("unused.csv");
        speedConfig.setWarmupIterations(10);
        var costConfig = calibration.getCostCalibrationConfigGroup();
        costConfig.setWarmupIterations(10);
        var config = ConfigUtils.createConfig(calibration);
        Path output = temp.getRoot().toPath().resolve("run-" + System.nanoTime());
        config.controller().setOutputDirectory(output.toString());
        config.controller().setLastIteration(0);
        config.controller().setCreateGraphs(false);
        config.controller().setWriteEventsInterval(0);
        config.controller().setCompressionType(CompressionType.gzip);
        var scenario = ScenarioUtils.createScenario(config);
        input.getNodes().values().forEach(scenario.getNetwork()::addNode);
        input.getLinks().values().forEach(scenario.getNetwork()::addLink);
        var network = scenario.getNetwork();
        var categorizer = new LinkCategorizer(network, calibration);
        var keys = new PenaltyKeyManager(calibration, costConfig, network, categorizer);
        var penalties = spy(new PenaltyManager(calibration, costConfig));
        var factors = spy(new FreespeedFactorManager(calibration, speedConfig));
        var controller = new Controler(scenario);
        controller.addOverridingModule(new AbstractModule() {
            @Override public void install() {
                addControllerListenerBinding().to(FreespeedAdapter.class);
                addControllerListenerBinding().to(PenaltiesAdapter.class);
            }

            @Provides @Singleton
            PenaltiesAdapter penaltyAdapter(OutputDirectoryHierarchy io) {
                var adapter = new PenaltiesAdapter(network,
                        () -> { assertTrue(calibrating); return mock(CountsProcessor.class); },
                        () -> { assertTrue(calibrating); return mock(FlowProcessor.class); },
                        calibration, costConfig, io, new EqasimConfigGroup(), categorizer, keys, penalties);
                if (calibrating) {
                    // A calibration result distinct from the original empty link attributes.
                    penalties.setPenalty(new PenaltyGroupKey(2, true, 1), 0.2);
                    penalties.setPenalty(new PenaltyGroupKey(2, true, 0), 0.05);
                }
                return adapter;
            }

            @Provides @Singleton
            FreespeedAdapter speedAdapter(OutputDirectoryHierarchy io, PenaltiesAdapter penaltyAdapter) {
                return new FreespeedAdapter(network, calibration, speedConfig, io, categorizer,
                        factors, penaltyAdapter, mock(TripsHandler.class));
            }

            @Provides
            Mobsim mobsim(PenaltiesAdapter adapter) {
                return () -> {
                    for (var link : network.getLinks().values()) {
                        boolean inside = link.getId().toString().equals("0");
                        assertEquals(10 * (inside ? 0.7 : 0.9), link.getFreespeed(), 1e-9);
                        assertEquals(link.getLength() / link.getFreespeed() * (inside ? 0.2 : 0.05),
                                adapter.computePenalty(link), 1e-9);
                    }
                };
            }
        });
        controller.run();
        if (!calibrating) {
            verify(penalties, never()).loadInitialPenalties(any());
            verify(penalties, never()).getPenalty(any());
            verify(factors, never()).loadFactors(any());
            verify(factors, never()).getFactor(any());
        }
        var saved = NetworkUtils.createNetwork();
        new MatsimNetworkReader(saved).readFile(output.resolve("output_network.xml.gz").toString());
        return saved;
    }

    private static NetworkCalibrationConfigGroup configuration(boolean calibrating, Path region) {
        var config = new NetworkCalibrationConfigGroup();
        config.setActivate(true);
        config.setObjective("penalty,freespeed");
        config.setCalibrate(calibrating ? "penalty,freespeed" : "");
        if (region != null) {
            config.getCostCalibrationConfigGroup().setSpecialRegionPath(region.toString());
            config.getFreeSpeedCalibrationConfigGroup().setSpecialRegionPath(region.toString());
        }
        return config;
    }

    private Path region() throws Exception {
        Path path = temp.newFile("region-" + System.nanoTime() + ".geojson").toPath();
        Files.writeString(path, """
                {"type":"FeatureCollection","features":[{"type":"Feature","properties":{},
                "geometry":{"type":"Polygon","coordinates":[[[-10,-10],[110,-10],[110,10],[-10,10],[-10,-10]]]}}]}
                """);
        return path;
    }

    private static Network network() {
        var network = NetworkUtils.createNetwork();
        Coord[] coords = {new Coord(0, 0), new Coord(100, 0), new Coord(100, 100), new Coord(0, 100)};
        for (int i = 0; i < 4; i++) NetworkUtils.createAndAddNode(network, Id.createNodeId(i), coords[i]);
        for (int i = 0; i < 4; i++) {
            var link = NetworkUtils.createAndAddLink(network, Id.createLinkId(i),
                    network.getNodes().get(Id.createNodeId(i)), network.getNodes().get(Id.createNodeId((i + 1) % 4)),
                    100, 10, 1000, 1);
            link.setAllowedModes(Set.of("car"));
            link.getAttributes().putAttribute("osm:way:highway", "primary");
            link.getAttributes().putAttribute("municipalityType", "urban");
            link.getAttributes().putAttribute("speedFactor", i == 0 ? 0.7 : 0.9);
        }
        return network;
    }

    private static void assertSavedValues(Network network) {
        for (var link : network.getLinks().values()) {
            boolean inside = link.getId().toString().equals("0");
            assertEquals(10, link.getFreespeed(), 0);
            assertEquals(inside ? 0.7 : 0.9, NetworkCalibrationUtils.readDoubleAttribute(link, "speedFactor").orElseThrow(), 1e-9);
            assertEquals(inside ? 0.2 : 0.05, NetworkCalibrationUtils.readDoubleAttribute(link, "penalty").orElseThrow(), 1e-9);
        }
    }
}
