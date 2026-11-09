package org.eqasim.core.components.scoring;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.io.IOUtils;

import static org.junit.Assert.*;

public class TestAnalysisOnlyScoringModule {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void defaultSwitchAndExplicitOptOut() throws Exception {
        assertTrue(AnalysisOnlyScoringModule.isEnabled(command()));
        assertFalse(AnalysisOnlyScoringModule.isEnabled(command("--analysis-only-scoring", "false")));
        assertThrows(CommandLine.ConfigurationException.class,
                () -> AnalysisOnlyScoringModule.isEnabled(command("--analysis-only-scoring", "typo")));
    }

    @Test public void protectsScoreDependentStrategiesEvenAtZeroWeight() {
        var config = config();
        assertEquals(java.util.Optional.empty(), AnalysisOnlyScoringModule.incompatibility(config));
        config.replanning().addStrategySettings(new StrategySettings().setStrategyName("ChangeExpBeta").setWeight(0));
        assertTrue(AnalysisOnlyScoringModule.incompatibility(config).isPresent());
    }

    @Test public void protectsPlanSelectionScoreOutputsAndSmoothing() {
        var config = config();
        config.replanning().setMaxAgentPlanMemorySize(2);
        assertTrue(AnalysisOnlyScoringModule.incompatibility(config).isPresent());
        config = config();
        config.replanning().setPlanSelectorForRemoval("WorstPlanSelector");
        assertTrue(AnalysisOnlyScoringModule.incompatibility(config).isPresent());
        config = config();
        config.scoring().setWriteExperiencedPlans(true);
        assertTrue(AnalysisOnlyScoringModule.incompatibility(config).isPresent());
        config = config();
        config.scoring().addParam("writeScoreExplanations", "true");
        assertTrue(AnalysisOnlyScoringModule.incompatibility(config).isPresent());
        config = config();
        config.scoring().setLearningRate(0.5);
        assertTrue(AnalysisOnlyScoringModule.incompatibility(config).isPresent());
        assertTrue(AnalysisOnlyScoringModule.incompatibility(ConfigUtils.createConfig()).isPresent());
    }

    @Test public void preservesTravelStatisticsAndExportsAcrossIterations() throws Exception {
        // Paired real controllers exercise event reconstruction, final activities and scoring lifecycle.
        // Car and teleported walk travellers have two trips each, so activity/leg exports are nontrivial.
        var baseline = run(false, false, "baseline");
        var optimized = run(true, false, "optimized");
        var expected = exports(baseline);
        var actual = exports(optimized);
        assertEquals(expected.keySet(), actual.keySet());
        assertTrue(expected.keySet().toString(), expected.keySet().stream().anyMatch(s -> s.contains("trips")));
        assertTrue(expected.keySet().toString(), expected.keySet().stream().anyMatch(s -> s.contains("legs")));
        assertTrue(expected.keySet().toString(), expected.keySet().stream().anyMatch(s -> s.contains("activities")));
        assertTrue(expected.keySet().toString(), expected.keySet().stream().anyMatch(s -> s.contains("traveldistance")));
        assertTrue(expected.keySet().toString(), expected.keySet().stream().anyMatch(s -> s.contains("linkstats")));
        for (var name : expected.keySet()) assertEquals(name, expected.get(name), actual.get(name));
    }

    @Test public void unsafeConfigurationActuallyRetainsConventionalScoring() throws Exception {
        run(true, true, "fallback");
    }

    private Path run(boolean optimized, boolean fallback, String name) throws Exception {
        var config = config();
        var directory = temporary.newFolder(name).toPath();
        config.controller().setOutputDirectory(directory.toString());
        config.controller().setLastIteration(2);
        config.controller().setWriteTripsInterval(1);
        config.controller().setWriteEventsInterval(1);
        config.controller().setCreateGraphsInterval(0);
        config.linkStats().setWriteLinkStatsInterval(2);
        config.linkStats().setAverageLinkStatsOverIterations(2);
        config.global().setNumberOfThreads(1);
        config.qsim().setNumberOfThreads(1);
        config.qsim().setEndTime(3600);
        config.scoring().addActivityParams(new ActivityParams("home").setTypicalDuration(3600));
        config.scoring().addActivityParams(new ActivityParams("work").setTypicalDuration(1200));
        if (fallback) config.scoring().setWriteExperiencedPlans(true);
        var scenario = ScenarioUtils.createScenario(config);
        var network = scenario.getNetwork();
        for (int i = 0; i < 4; i++)
            NetworkUtils.createAndAddNode(network, Id.createNodeId(i), new Coord(i % 2 * 100, i / 2 * 100));
        int[][] edges = {{0,1},{1,3},{3,2},{2,0},{1,0},{3,1},{2,3},{0,2}};
        for (int i = 0; i < edges.length; i++)
            NetworkUtils.createAndAddLink(network, Id.createLinkId(i),
                    network.getNodes().get(Id.createNodeId(edges[i][0])),
                    network.getNodes().get(Id.createNodeId(edges[i][1])), 100, 10, 900, 1);
        var factory = scenario.getPopulation().getFactory();
        for (int i = 0; i < 6; i++) {
            var person = factory.createPerson(Id.createPersonId(i));
            var plan = factory.createPlan();
            var home = factory.createActivityFromLinkId("home", Id.createLinkId(0));
            home.setEndTime(100 + i * 10);
            plan.addActivity(home);
            String mode = i % 2 == 0 ? "car" : "walk";
            var outward = factory.createLeg(mode);
            outward.setDepartureTime(100 + i * 10);
            outward.setTravelTime(0);
            TripStructureUtils.setRoutingMode(outward, mode);
            plan.addLeg(outward);
            var work = factory.createActivityFromLinkId("work", Id.createLinkId(2));
            work.setEndTime(1800 + i * 10);
            plan.addActivity(work);
            var inward = factory.createLeg(mode);
            inward.setDepartureTime(1800 + i * 10);
            inward.setTravelTime(0);
            TripStructureUtils.setRoutingMode(inward, mode);
            plan.addLeg(inward);
            plan.addActivity(factory.createActivityFromLinkId("home", Id.createLinkId(0)));
            person.addPlan(plan);
            scenario.getPopulation().addPerson(person);
        }
        var controller = new Controler(scenario);
        controller.addOverridingModule(new org.matsim.contribs.discrete_mode_choice.modules.DiscreteModeChoiceModule());
        controller.addOverridingModule(new AbstractModule() {
            @Override public void install() {
                bind(org.eqasim.core.components.flow.FlowUtils.class)
                        .toInstance(new org.eqasim.core.components.flow.FlowUtils(scenario));
            }
        });
        if (optimized) controller.addOverridingModule(new AnalysisOnlyScoringModule());
        controller.run();
        boolean analysisOnly = optimized && !fallback;
        assertEquals(analysisOnly, controller.getInjector().getInstance(ScoringFunctionFactory.class)
                instanceof AnalysisOnlyScoringModule.Factory);
        var type = Class.forName("org.matsim.core.scoring.ScoringFunctionsForPopulation");
        var scorer = controller.getInjector().getInstance(type);
        for (String fieldName : List.of("agentScorers", "partialScores", "tripRecords", "vehicles2Drivers")) {
            var field = type.getDeclaredField(fieldName);
            field.setAccessible(true);
            if (analysisOnly) assertNull(fieldName, field.get(scorer));
            else assertNotNull(fieldName, field.get(scorer));
        }
        for (var person : scenario.getPopulation().getPersons().values()) {
            if (analysisOnly) assertEquals(0.0, person.getSelectedPlan().getScore(), 0);
            else assertNotEquals(0.0, person.getSelectedPlan().getScore(), 0);
        }
        // Experienced records outlive population membership. Finalization must remain
        // valid without a per-person scorer registry after population changes as well.
        scenario.getPopulation().removePerson(Id.createPersonId(0));
        controller.getInjector().getInstance(org.matsim.core.scoring.ExperiencedPlansService.class).finishIteration();
        return directory;
    }

    private Map<String, String> exports(Path root) throws Exception {
        var result = new TreeMap<String, String>();
        try (var paths = Files.walk(root)) {
            for (var path : paths.filter(Files::isRegularFile).toList()) {
                String name = path.getFileName().toString().toLowerCase();
                if (!(name.contains("trips") || name.contains("legs") || name.contains("activities")
                        || name.contains("traveldistance") || name.contains("ph_modestats") || name.contains("pkm_modestats")
                        || name.contains("linkstats") || name.contains("utilities") || name.endsWith("events.xml.gz"))
                        || name.endsWith(".png")) continue;
                try (var reader = IOUtils.getBufferedReader(path.toString())) {
                    // Events carry a creation-date comment; event content must be identical.
                    result.put(root.relativize(path).toString(), reader.lines()
                            .filter(line -> !line.startsWith("<!--")).collect(java.util.stream.Collectors.joining("\n")));
                }
            }
        }
        System.out.println("Compared travel exports: " + result.keySet());
        return result;
    }

    private static Config config() {
        var dmc = new DiscreteModeChoiceConfigGroup();
        dmc.setEnforceSinglePlan(true);
        dmc.setTripEstimator("MATSimTripScoring");
        dmc.setTourEstimator("Cumulative");
        dmc.setPerformReroute(false);
        dmc.getCarModeAvailabilityConfig().setAvailableModes(List.of("car", "walk"));
        var config = ConfigUtils.createConfig(dmc);
        config.replanning().setMaxAgentPlanMemorySize(1);
        config.replanning().setPlanSelectorForRemoval("NonSelectedPlanSelector");
        config.replanning().addStrategySettings(new StrategySettings().setStrategyName("ReRoute").setWeight(0.25));
        config.replanning().addStrategySettings(new StrategySettings().setStrategyName("KeepLastSelected").setWeight(0.25));
        config.replanning().addStrategySettings(new StrategySettings().setStrategyName("DiscreteModeChoice").setWeight(0.5));
        config.scoring().setLearningRate(1);
        config.scoring().setWriteExperiencedPlans(false);
        config.scoring().addParam("writeScoreExplanations", "false");
        return config;
    }

    private static CommandLine command(String... args) throws Exception {
        return new CommandLine.Builder(args).allowOptions(AnalysisOnlyScoringModule.OPTION).build();
    }
}
