package org.eqasim.switzerland.ch_cmdp.runtime;

import java.nio.file.*;
import java.util.*;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.scenario.config.RunGenerateConfig;
import org.eqasim.switzerland.ch_cmdp.dsim.*;
import org.eqasim.switzerland.ch_cmdp.transit_dwell.SampledTransitDwellConfigGroup;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.*;
import org.matsim.core.config.groups.*;
import org.matsim.core.controler.*;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.network.*;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.turnRestrictions.DisallowedNextLinks;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.households.*;
import org.matsim.vehicles.VehicleType;
import static org.junit.Assert.*;

public class TestSimulationRuntimeLifecycle {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    public static class TestConfigurator extends org.eqasim.core.simulation.EqasimConfigurator {
        public TestConfigurator(CommandLine command) { super(command); }
    }

    @Test public void generatorIncludesDsimButSelectsQsim() throws Exception {
        var file = temp.newFile("generated.xml");
        RunGenerateConfig.main(new String[]{"--output-path", file.toString(), "--prefix", "switzerland_",
                "--sample-size", "0.1", "--threads", "4", "--eqasim-configurator", TestConfigurator.class.getName()});
        assertTrue(Files.readString(file.toPath()).contains("<module name=\"dsim\""));
        var config = ConfigUtils.loadConfig(file.toString());
        assertEquals("qsim", config.controller().getMobsim()); assertEquals(4, config.dsim().getThreads());
    }

    private CommandLine command(String... args) throws Exception {
        return new CommandLine.Builder(args).allowOptions(DsimIntegration.OPTION, DsimIntegration.SIMULATE_BIKES_OPTION).build();
    }

    @Test public void xmlAndArgumentSelectDsimAndValidateBikeOption() throws Exception {
        var config = ConfigUtils.createConfig();
        assertNull(DsimIntegration.fromCommand(command(), config));
        assertNotNull(DsimIntegration.fromCommand(command("--use-dsim", "true"), config));
        assertThrows(CommandLine.ConfigurationException.class, () ->
                DsimIntegration.fromCommand(command("--dsim-simulate-bikes", "true"), config));
        config.controller().setMobsim("dsim");
        assertNotNull(DsimIntegration.fromCommand(command(), config));
        assertNotNull(DsimIntegration.fromCommand(command("--dsim-simulate-bikes", "true"), config));
        assertNotNull(DsimIntegration.fromCommand(command("--use-dsim", "false"), config));
    }

    @Test public void xmlDsimRetainsEngineAndTeleportationNeverEditsNetwork() throws Exception { run(true, false, false); }
    @Test public void argumentDsimReturnsToQsimAndRestoresIsolatedBikeLayer() throws Exception { run(false, true, false); }
    @Test public void simulationUpdatesToCapacitiesAndConfigSurvivePostprocessing() throws Exception { run(false, true, true); }

    private void run(boolean xmlDsim, boolean isolate, boolean changeCapacity) throws Exception {
        var config = ConfigUtils.createConfig(new EqasimConfigGroup(), new SampledTransitDwellConfigGroup());
        config.controller().setMobsim(xmlDsim ? "dsim" : "qsim");
        config.controller().setOutputDirectory(temp.newFolder().toString());
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        config.controller().setCompressionType(ControllerConfigGroup.CompressionType.gzip);
        config.controller().setCreateGraphs(false); config.controller().setLastIteration(4);
        config.controller().setWriteEventsInterval(0); config.controller().setWritePlansInterval(0);
        config.qsim().setMainModes(Set.of("car", "bike")); config.routing().setNetworkModes(Set.of("car", "bike"));
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.defaultVehicle);
        config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.SeepageQ);
        config.qsim().setNumberOfThreads(1); config.dsim().setThreads(1);
        config.scoring().setWriteExperiencedPlans(true);
        config.replanning().addStrategySettings(new ReplanningConfigGroup.StrategySettings().setStrategyName("KeepLastSelected").setWeight(1));
        var dsim = DsimIntegration.fromCommand(xmlDsim ? command() : command("--use-dsim", "true", "--dsim-simulate-bikes", "true"), config);
        var runtime = new SimulationRuntime(config, dsim, false);
        var scenario = ScenarioUtils.createScenario(config);
        var network = scenario.getNetwork();
        var a = NetworkUtils.createAndAddNode(network, Id.createNodeId("a"), new Coord(0, 0));
        var b = NetworkUtils.createAndAddNode(network, Id.createNodeId("b"), new Coord(100, 0));
        Link road = NetworkUtils.createAndAddLink(network, Id.createLinkId("road"), a, b, 100, 10, 1800, 1);
        Link back = NetworkUtils.createAndAddLink(network, Id.createLinkId("back"), b, a, 100, 10, 1800, 1);
        road.setAllowedModes(Set.of("car", "bike")); back.setAllowedModes(Set.of("car", "bike"));
        var turns = new DisallowedNextLinks(); turns.addDisallowedLinkSequence("bike", List.of(back.getId()));
        NetworkUtils.setDisallowedNextLinks(road, turns); var expectedTurns = turns.copy();
        road.getAttributes().putAttribute("sourceMetadata", "road metadata");
        var pf = scenario.getPopulation().getFactory();
        var person = pf.createPerson(Id.createPersonId("p")); var plan = pf.createPlan();
        plan.addActivity(pf.createActivityFromLinkId("home", road.getId())); person.addPlan(plan);
        person.getAttributes().putAttribute("sourceMetadata", "person metadata"); scenario.getPopulation().addPerson(person);
        var household = scenario.getHouseholds().getFactory().createHousehold(Id.create("h", Household.class));
        household.getMemberIds().add(person.getId()); household.getAttributes().putAttribute("sourceMetadata", "household metadata");
        scenario.getHouseholds().getHouseholds().put(household.getId(), household);
        var bike = scenario.getVehicles().getFactory().createVehicleType(Id.create("bike", VehicleType.class));
        bike.setNetworkMode("bike"); bike.setPcuEquivalents(0.4); scenario.getVehicles().addVehicleType(bike);
        runtime.preprocess(scenario, true);
        assertEquals(isolate ? 4 : 2, network.getLinks().size());
        if (!isolate) { assertEquals(Set.of("car", "bike"), road.getAllowedModes()); assertEquals(expectedTurns, NetworkUtils.getDisallowedNextLinks(road)); }
        // These represent later model updates, not preprocessing, and must not be overwritten.
        config.routing().getOrCreateModeRoutingParams("walk").setTeleportedModeSpeed(1.91);
        if (changeCapacity) {
            bike.setPcuEquivalents(0.6);
        }
        var controller = new Controler(scenario);
        controller.addOverridingModule(new AbstractModule() {
            @Override public void install() {
                bind(Mobsim.class).toInstance(() -> {
                    assertNull(road.getAttributes().getAttribute("sourceMetadata"));
                    road.setFreespeed(9); road.getAttributes().putAttribute("speedFactor", 0.72);
                    person.getAttributes().putAttribute("sourceMetadata", "updated person metadata");
                    household.getAttributes().putAttribute("newMetadata", "new household metadata");
                });
                bind(TerminationCriterion.class).toInstance(new TerminationCriterion() {
                    public boolean mayTerminateAfterIteration(int iteration) { return true; }
                    public boolean doTerminate(int iteration) { return true; }
                });
            }
        });
        runtime.install(controller); controller.run(); runtime.postprocess();
        Path output = Path.of(config.controller().getOutputDirectory());
        var exported = ConfigUtils.loadConfig(output.resolve("output_config.xml").toString());
        assertEquals(xmlDsim ? "dsim" : "qsim", exported.controller().getMobsim());
        assertTrue(exported.getModules().containsKey("dsim"));
        assertTrue(exported.routing().getNetworkModes().contains("bike"));
        assertTrue(exported.transit().isUsingTransitInMobsim());
        assertEquals(QSimConfigGroup.LinkDynamics.SeepageQ, exported.qsim().getLinkDynamics());
        assertTrue(exported.scoring().isWriteExperiencedPlans());
        assertEquals(1.91, exported.routing().getTeleportedModeParams().get("walk").getTeleportedModeSpeed(), 0);
        assertEquals(changeCapacity ? 0.6 : 0.4, bike.getPcuEquivalents(), 0);
        var saved = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(saved.getNetwork()).readFile(output.resolve("output_network.xml.gz").toString());
        new PopulationReader(saved).readFile(output.resolve("output_plans.xml.gz").toString());
        new HouseholdsReaderV10(saved.getHouseholds()).readFile(output.resolve("output_households.xml.gz").toString());
        assertEquals(2, saved.getNetwork().getLinks().size()); assertEquals(2, saved.getNetwork().getNodes().size());
        var savedRoad = saved.getNetwork().getLinks().get(road.getId());
        assertEquals(Set.of("car", "bike"), savedRoad.getAllowedModes()); assertEquals(expectedTurns, NetworkUtils.getDisallowedNextLinks(savedRoad));
        assertEquals("road metadata", savedRoad.getAttributes().getAttribute("sourceMetadata"));
        assertEquals(9, savedRoad.getFreespeed(), 0); assertEquals(0.72, savedRoad.getAttributes().getAttribute("speedFactor"));
        assertEquals("updated person metadata", saved.getPopulation().getPersons().get(person.getId()).getAttributes().getAttribute("sourceMetadata"));
        assertEquals("household metadata", saved.getHouseholds().getHouseholds().get(household.getId()).getAttributes().getAttribute("sourceMetadata"));
        assertEquals("new household metadata", saved.getHouseholds().getHouseholds().get(household.getId()).getAttributes().getAttribute("newMetadata"));
    }

    @Test public void simulationUpdatesToTeleportedBikeParametersSurvive() throws Exception {
        var config = ConfigUtils.createConfig();
        config.routing().setNetworkModes(Set.of("car", "bike"));
        var runtime = new SimulationRuntime(config, null, true);
        runtime.preprocess(ScenarioUtils.createScenario(config), false);
        config.routing().getOrCreateModeRoutingParams("bike").setTeleportedModeSpeed(5.1);
        runtime.postprocess();
        assertEquals(5.1, runtime.exportConfig().routing().getTeleportedModeParams().get("bike").getTeleportedModeSpeed(), 0);
    }

    @Test public void layerRestorationMapsCurrentRoutesAndSkipsDeletedOriginals() {
        var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig()); var network = scenario.getNetwork();
        var a = NetworkUtils.createAndAddNode(network, Id.createNodeId("a"), new Coord(0, 0));
        var b = NetworkUtils.createAndAddNode(network, Id.createNodeId("b"), new Coord(1, 0));
        var road = NetworkUtils.createAndAddLink(network, Id.createLinkId("r"), a, b, 1, 1, 100, 1); road.setAllowedModes(Set.of("bike"));
        var undo = DsimBikeNetworkRestoration.capture(network); DsimBikeNetworkAdapter.adapt(network);
        var copy = network.getLinks().values().stream().filter(l -> DsimBikeNetworkAdapter.isBikeLink(l.getId())).findFirst().orElseThrow();
        var pf = scenario.getPopulation().getFactory(); var person = pf.createPerson(Id.createPersonId("p")); var plan = pf.createPlan();
        var stage = pf.createActivityFromLinkId("bike interaction", copy.getId()); plan.addActivity(stage);
        var leg = pf.createLeg("bike"); var route = RouteUtils.createLinkNetworkRouteImpl(copy.getId(), copy.getId());
        route.setLinkIds(copy.getId(), List.of(copy.getId()), copy.getId()); leg.setRoute(route); plan.addLeg(leg);
        person.addPlan(plan); scenario.getPopulation().addPerson(person);
        undo.restore(scenario); undo.restore(scenario);
        assertEquals(road.getId(), stage.getLinkId()); assertEquals(road.getId(), route.getStartLinkId());
        assertEquals(List.of(road.getId()), route.getLinkIds()); assertEquals(road.getId(), route.getEndLinkId());
        var again = DsimBikeNetworkRestoration.capture(network); DsimBikeNetworkAdapter.adapt(network);
        network.removeLink(road.getId()); again.restore(scenario); assertTrue(network.getLinks().isEmpty());
    }

    @Test public void anExistingBikeLayerIsNotOwnedByThisRun() {
        var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig()); var network = scenario.getNetwork();
        var a = NetworkUtils.createAndAddNode(network, Id.createNodeId("a"), new Coord(0, 0));
        var b = NetworkUtils.createAndAddNode(network, Id.createNodeId("b"), new Coord(1, 0));
        var road = NetworkUtils.createAndAddLink(network, Id.createLinkId("r"), a, b, 1, 1, 100, 1); road.setAllowedModes(Set.of("bike"));
        DsimBikeNetworkAdapter.adapt(network);
        var copy = network.getLinks().values().stream().filter(l -> DsimBikeNetworkAdapter.isBikeLink(l.getId())).findFirst().orElseThrow();
        copy.getAttributes().putAttribute("originalMetadata", "keep");
        var undo = DsimBikeNetworkRestoration.capture(network); DsimBikeNetworkAdapter.adapt(network); undo.restore(scenario);
        assertEquals(2, network.getLinks().size()); assertEquals("keep", copy.getAttributes().getAttribute("originalMetadata"));
    }

    @Test public void cleanupAlsoUndoesCoordinateNormalizationWhileKeepingNewerValues() throws Exception {
        var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        var pf = scenario.getPopulation().getFactory();
        for (String id : List.of("stable", "updated")) {
            var person = pf.createPerson(Id.createPersonId(id)); scenario.getPopulation().addPerson(person);
            person.getAttributes().putAttribute("home_coordinate_x", 200.0);
            person.getAttributes().putAttribute("home_coordinate_y", 300.0);
            person.getAttributes().putAttribute("home_x", 100.0);
        }
        var archive = org.eqasim.switzerland.ch_cmdp.memory.ScenarioAttributeCleaner.cleanAndArchive(scenario, temp.getRoot().toPath());
        var stable = scenario.getPopulation().getPersons().get(Id.createPersonId("stable")).getAttributes();
        var updated = scenario.getPopulation().getPersons().get(Id.createPersonId("updated")).getAttributes();
        assertEquals(200.0, stable.getAttribute("home_x")); assertEquals(300.0, stable.getAttribute("home_y"));
        updated.putAttribute("home_x", 500.0); archive.restore();
        assertEquals(100.0, stable.getAttribute("home_x")); assertFalse(stable.getAsMap().containsKey("home_y"));
        assertEquals(500.0, updated.getAttribute("home_x"));
        assertEquals(200.0, stable.getAttribute("home_coordinate_x"));
    }
}
