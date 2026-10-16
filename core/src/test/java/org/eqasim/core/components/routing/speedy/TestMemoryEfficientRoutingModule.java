package org.eqasim.core.components.routing.speedy;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.scenario.ScenarioUtils;

import static org.junit.Assert.*;

public class TestMemoryEfficientRoutingModule {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void switchDefaultsToOriginalAndRejectsTypos() throws Exception {
        assertFalse(MemoryEfficientRoutingModule.isEnabled(command()));
        assertTrue(MemoryEfficientRoutingModule.isEnabled(command("--memory-efficient-routing", "true")));
        assertFalse(MemoryEfficientRoutingModule.isEnabled(command("--memory-efficient-routing", "false")));
        assertThrows(CommandLine.ConfigurationException.class,
                () -> MemoryEfficientRoutingModule.isEnabled(command("--memory-efficient-routing", "typo")));
    }

    @Test public void controllerUsesSharedFactoryAndCompletesSimulation() throws Exception {
        var config = ConfigUtils.createConfig();
        config.controller().setRoutingAlgorithmType(RoutingAlgorithmType.SpeedyALT);
        config.controller().setOutputDirectory(temporary.newFolder("simulation").getAbsolutePath());
        config.controller().setLastIteration(1);
        config.global().setNumberOfThreads(2);
        config.qsim().setNumberOfThreads(1);
        config.qsim().setEndTime(3600);
        config.replanning().addStrategySettings(new org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings()
                .setStrategyName("ReRoute").setWeight(1.0));
        config.scoring().addActivityParams(new ActivityParams("home").setTypicalDuration(3600));
        config.scoring().addActivityParams(new ActivityParams("work").setTypicalDuration(3600));
        var scenario = ScenarioUtils.createScenario(config);
        var network = TestMemoryEfficientSpeedyALT.grid(3, true, 55);
        network.getNodes().values().forEach(scenario.getNetwork()::addNode);
        network.getLinks().values().forEach(scenario.getNetwork()::addLink);
        var links = new java.util.ArrayList<>(network.getLinks().values());
        var factory = scenario.getPopulation().getFactory();
        for (int i = 0; i < 4; i++) {
            var person = factory.createPerson(Id.createPersonId(i));
            var plan = factory.createPlan();
            var home = factory.createActivityFromLinkId("home", links.get(0).getId());
            home.setEndTime(100 + i * 10);
            plan.addActivity(home);
            var leg = factory.createLeg("car");
            leg.setDepartureTime(100 + i * 10);
            leg.setTravelTime(0); // Local PlanRouter deterministically accepts the initial replacement.
            org.matsim.core.router.TripStructureUtils.setRoutingMode(leg, "car");
            plan.addLeg(leg);
            plan.addActivity(factory.createActivityFromLinkId("work", links.get(links.size() - 1).getId()));
            person.addPlan(plan); scenario.getPopulation().addPerson(person);
        }
        var controller = new Controler(scenario);
        controller.addOverridingModule(new MemoryEfficientRoutingModule());
        controller.addOverridingModule(new org.matsim.core.controler.AbstractModule() {
            @Override public void install() {
                bind(org.eqasim.core.components.flow.FlowUtils.class)
                        .toInstance(new org.eqasim.core.components.flow.FlowUtils(scenario));
            }
        });
        controller.run();
        var actual = controller.getInjector().getInstance(LeastCostPathCalculatorFactory.class);
        assertTrue(actual instanceof MemoryEfficientSpeedyALTFactory);
        assertSame(actual, controller.getInjector().getInstance(LeastCostPathCalculatorFactory.class));
        assertFalse(((MemoryEfficientSpeedyALTFactory) actual).getStatistics().isEmpty());
    }

    private static CommandLine command(String... args) throws Exception {
        return new CommandLine.Builder(args).allowOptions(MemoryEfficientRoutingModule.OPTION).build();
    }
}
