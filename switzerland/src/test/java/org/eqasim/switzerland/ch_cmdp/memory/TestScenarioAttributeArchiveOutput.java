package org.eqasim.switzerland.ch_cmdp.memory;

import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.*;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.households.*;
import static org.junit.Assert.*;

/** Exercises the actual MATSim core writer, including normal early termination before configured lastIteration. */
public class TestScenarioAttributeArchiveOutput {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    @Test public void finalOutputsContainArchivedAttributesAndPreserveRuntimeValues() throws Exception { run(false); }
    @Test public void earlyTerminationAlsoRestoresBeforeTheCoreWriter() throws Exception { run(true); }

    private void run(boolean early) throws Exception {
        var config = ConfigUtils.createConfig();
        Path output = temp.getRoot().toPath().resolve("simulation_output");
        config.controller().setOutputDirectory(output.toString());
        config.controller().setLastIteration(early ? 5 : 1);
        config.controller().setWriteEventsInterval(0); config.controller().setWritePlansInterval(0);
        config.controller().setCreateGraphs(false); config.scoring().setWriteExperiencedPlans(false);
        var strategy = new org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings();
        strategy.setStrategyName("KeepLastSelected"); strategy.setWeight(1.0);
        config.replanning().addStrategySettings(strategy);
        config.controller().setCompressionType(ControllerConfigGroup.CompressionType.gzip);
        var scenario = ScenarioUtils.createScenario(config);
        var network = scenario.getNetwork();
        var a = NetworkUtils.createAndAddNode(network, Id.createNodeId("a"), new Coord(0, 0));
        var b = NetworkUtils.createAndAddNode(network, Id.createNodeId("b"), new Coord(100, 0));
        var link = NetworkUtils.createAndAddLink(network, Id.createLinkId("road"), a, b, 100, 10, 1800, 1);
        NetworkUtils.createAndAddLink(network, Id.createLinkId("return"), b, a, 100, 10, 1800, 1);
        link.getAttributes().putAttribute("sourceMetadata", "original road metadata"); link.getAttributes().putAttribute("speedFactor", 0.8);
        var person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("person"));
        person.getAttributes().putAttribute("sourceMetadata", 7); person.getAttributes().putAttribute("runtimeMetadata", "original");
        person.getAttributes().putAttribute("carASC", 1.0);
        var plan = scenario.getPopulation().getFactory().createPlan();
        plan.addActivity(scenario.getPopulation().getFactory().createActivityFromCoord("home", a.getCoord()));
        person.addPlan(plan); scenario.getPopulation().addPerson(person);
        var household = scenario.getHouseholds().getFactory().createHousehold(Id.create("household", Household.class));
        household.getMemberIds().add(person.getId()); household.getAttributes().putAttribute("sourceMetadata", "original household metadata");
        scenario.getHouseholds().getHouseholds().put(household.getId(), household);
        var archive = ScenarioAttributeCleaner.cleanAndArchive(scenario, temp.getRoot().toPath()); Path file = archive.file();
        assertTrue(Files.exists(file));
        var calls = new AtomicInteger(); var controller = new Controler(scenario);
        controller.addOverridingModule(new AbstractModule() {
            @Override public void install() {
                bind(Mobsim.class).toInstance(() -> {
                    assertNull(person.getAttributes().getAttribute("sourceMetadata"));
                    assertNull(link.getAttributes().getAttribute("sourceMetadata")); assertTrue(household.getAttributes().isEmpty());
                    assertTrue(Files.exists(file));
                    if (calls.incrementAndGet() == 2) {
                        person.getAttributes().putAttribute("runtimeMetadata", "updated");
                        person.getAttributes().putAttribute("carASC", 3.0); link.getAttributes().putAttribute("speedFactor", 0.9);
                    }
                });
                if (early) bind(TerminationCriterion.class).toInstance(new TerminationCriterion() {
                    public boolean mayTerminateAfterIteration(int iteration) { return iteration >= 1; }
                    public boolean doTerminate(int iteration) { return iteration >= 1; }
                });
            }
        });
        archive.install(controller); controller.run();
        assertEquals(2, calls.get()); assertFalse(Files.exists(file));
        var saved = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(saved.getNetwork()).readFile(output.resolve("output_network.xml.gz").toString());
        new PopulationReader(saved).readFile(output.resolve("output_plans.xml.gz").toString());
        new HouseholdsReaderV10(saved.getHouseholds()).readFile(output.resolve("output_households.xml.gz").toString());
        var savedPerson = saved.getPopulation().getPersons().get(person.getId());
        assertEquals(7, savedPerson.getAttributes().getAttribute("sourceMetadata"));
        assertEquals("updated", savedPerson.getAttributes().getAttribute("runtimeMetadata"));
        assertEquals(3.0, savedPerson.getAttributes().getAttribute("carASC"));
        assertEquals("original road metadata", saved.getNetwork().getLinks().get(link.getId()).getAttributes().getAttribute("sourceMetadata"));
        assertEquals(0.9, saved.getNetwork().getLinks().get(link.getId()).getAttributes().getAttribute("speedFactor"));
        assertEquals("original household metadata", saved.getHouseholds().getHouseholds().get(household.getId()).getAttributes().getAttribute("sourceMetadata"));
    }
}
