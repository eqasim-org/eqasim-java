package org.eqasim.switzerland.ch_cmdp.calibration;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.fast_calibration.AlphaCalibratorConfig;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissCmdpModeParameters;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

public class TestKeepCantonAlphaCalibrator {
    @Rule public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void snapshotsEachSelectedCantonWithTheSameTripAndPersonFilters() throws Exception {
        var f = new Fixture("keep_canton:geneva;vaud");
        f.add("geneva", "car");
        f.add("geneva", "pt");
        f.add("geneva", "bike").person.getAttributes().putAttribute("isFreight", true);
        f.add("geneva", "bike").person.getAttributes().putAttribute("isCrossBorder", true);
        var zero = f.add("geneva", "bike");
        zero.destination.setCoord(zero.origin.getCoord());
        f.add("geneva", "bike").origin.setType("outside");
        f.add("geneva", "bike").destination.setType("outside");
        f.add("geneva", "truck");
        f.add("geneva", "bike").origin.setCoord(null);
        f.add("vaud", "walk");
        f.add("vaud", "bike");
        f.add("bern", "car");
        var calibrator = f.create();
        var initial = calibrator.getInitialShares();
        assertEquals(2, initial.size());
        assertEquals(0.5, initial.get("geneva").get("car"), 0);
        assertEquals(0.5, initial.get("geneva").get("pt"), 0);
        assertEquals(0, initial.get("geneva").get("bike"), 0);
        assertEquals(0.5, initial.get("vaud").get("walk"), 0);
        assertEquals(0.5, initial.get("vaud").get("bike"), 0);
        assertThrows(UnsupportedOperationException.class, () -> initial.get("geneva").put("car", 1.0));
        assertThrows(UnsupportedOperationException.class, () -> initial.clear());
        var snapshot = Path.of(f.output.getOutputFilename("keep_canton_initial_shares.csv"));
        assertTrue(Files.readString(snapshot).contains("geneva,2,0.5,0.5"));
    }

    @Test
    public void adjustsOnlySelectedCantonAndConfiguredModesWhileKeepingNonzeroPtReference() throws Exception {
        var f = new Fixture("keep_canton:geneva;vaud");
        f.config.setCalibratedModes("car,pt");
        f.parameters.swissCanton.car.put("geneva", 2.0);
        f.parameters.swissCanton.pt.put("geneva", 0.7);
        f.parameters.swissCanton.walk.put("geneva", 4.0);
        f.parameters.car.alpha_u = 3.0;
        f.add("geneva", "car");
        f.add("geneva", "car");
        var changed = f.add("geneva", "pt");
        f.add("geneva", "pt");
        f.add("vaud", "car");
        f.add("vaud", "pt");
        var unselected = f.add("bern", "pt");
        var calibrator = f.create();
        f.iteration(calibrator, 0);
        changed.leg.setMode("car");
        unselected.leg.setMode("car");
        f.iteration(calibrator, 1);
        assertEquals(2.0 - 0.5 * Math.log(3), f.parameters.swissCanton.car.get("geneva"), 1e-12);
        assertEquals(0.7, f.parameters.swissCanton.pt.get("geneva"), 0);
        assertEquals(4.0, f.parameters.swissCanton.walk.get("geneva"), 0);
        assertEquals(0, f.parameters.swissCanton.car.get("vaud"), 0);
        assertEquals(0, f.parameters.swissCanton.car.get("bern"), 0);
        assertEquals(3.0, f.parameters.car.alpha_u, 0);
        assertEquals(0.5, calibrator.getInitialShares().get("geneva").get("car"), 0);
        // Full-population measurements do not depend on utility-tracking flags.
        assertNull(changed.plan.getAttributes().getAttribute("createdLastIteration"));
        assertNull(changed.plan.getAttributes().getAttribute("utility"));
        changed.leg.setMode("pt");
        double previous = f.parameters.swissCanton.car.get("geneva");
        f.iteration(calibrator, 2);
        assertEquals(previous, f.parameters.swissCanton.car.get("geneva"), 0);
    }

    @Test
    public void unchangedSharesAndZeroShareModesLeaveConstantsStableAndFinite() throws Exception {
        var f = new Fixture("keep_canton:geneva");
        f.parameters.swissCanton.pt.put("geneva", 1.5);
        f.parameters.swissCanton.car.put("geneva", -0.2);
        f.add("geneva", "car");
        var changed = f.add("geneva", "pt");
        var calibrator = f.create();
        f.iteration(calibrator, 1);
        assertEquals(-0.2, f.parameters.swissCanton.car.get("geneva"), 0);
        assertEquals(1.5, f.parameters.swissCanton.pt.get("geneva"), 0);
        changed.leg.setMode("bike");
        f.iteration(calibrator, 2);
        for (var map : List.of(f.parameters.swissCanton.car, f.parameters.swissCanton.pt,
                f.parameters.swissCanton.walk, f.parameters.swissCanton.bike, f.parameters.swissCanton.cp)) {
            assertTrue(Double.isFinite(map.get("geneva")));
        }
        assertTrue(f.parameters.swissCanton.bike.get("geneva") < 0);
    }

    @Test
    public void rejectsEmptyAndUnrepresentedCantonSelections() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> KeepCantonAlphaCalibrator.parseCantons("keep_canton: ; "));
        assertThrows(IllegalArgumentException.class, () -> KeepCantonAlphaCalibrator.parseCantons("canton"));
        assertEquals(List.of("geneva", "vaud"), List.copyOf(
                KeepCantonAlphaCalibrator.parseCantons(" KEEP_CANTON:geneva; vaud;geneva; ")));
        var f = new Fixture("keep_canton:misspelled");
        f.add("geneva", "car");
        assertTrue(assertThrows(IllegalArgumentException.class, f::create).getMessage().contains("misspelled"));
    }

    @Test
    public void noCurrentEligibleTripsSkipsUpdateAndFinalParametersArePortable() throws Exception {
        var f = new Fixture("keep_canton:geneva");
        var trip = f.add("geneva", "car");
        var calibrator = f.create();
        trip.person.getAttributes().putAttribute("isFreight", true);
        f.iteration(calibrator, 1);
        assertEquals(0, f.parameters.swissCanton.car.get("geneva"), 0);
        calibrator.notifyIterationEnds(new IterationEndsEvent(null, 1, true));
        Path yaml = Path.of(f.output.getOutputFilenameWithOutputPrefix("mode_parameters.yml"));
        assertTrue(Files.exists(yaml));
        assertEquals(yaml.getFileName().toString(), f.eqasim.getModeParametersPath());
        var before = Files.readAllBytes(yaml);
        calibrator.notifyShutdown(new ShutdownEvent(null, false, 1, null));
        assertArrayEquals(before, Files.readAllBytes(yaml));
    }

    @Test
    public void nonzeroFirstIterationIsOnlyTheInputBaseline() throws Exception {
        var f = new Fixture("keep_canton:geneva");
        f.scenario.getConfig().controller().setFirstIteration(10);
        f.add("geneva", "car");
        var changed = f.add("geneva", "pt");
        var calibrator = f.create();
        changed.leg.setMode("car");
        f.iteration(calibrator, 10);
        assertEquals(0, f.parameters.swissCanton.car.get("geneva"), 0);
        f.iteration(calibrator, 11);
        assertTrue(f.parameters.swissCanton.car.get("geneva") < 0);
    }

    @Test
    public void feedbackRestoresInitialSharesAfterAnExternalUtilityChange() throws Exception {
        var f = new Fixture("keep_canton:geneva");
        f.config.setCalibratedModes("car,pt");
        var population = new java.util.ArrayList<Entry>();
        for (int i = 0; i < 200; i++) population.add(f.add("geneva", i < 100 ? "car" : "pt"));
        var calibrator = f.create();
        int carCount = 0;
        for (int iteration = 1; iteration <= 12; iteration++) {
            // A synthetic logit response to a +1 external car utility shock,
            // plus the current calibrated canton constant.
            double carUtility = 1 + f.parameters.swissCanton.car.get("geneva");
            carCount = (int) Math.round(200 / (1 + Math.exp(-carUtility)));
            for (int i = 0; i < population.size(); i++) population.get(i).leg.setMode(i < carCount ? "car" : "pt");
            f.iteration(calibrator, iteration);
        }
        assertEquals(0.5, carCount / 200.0, 0.01);
        assertEquals(0.5, calibrator.getInitialShares().get("geneva").get("car"), 0);
    }

    @Test
    public void guiceAndControllerEventsUseOneSnapshotAndPersistBeforeShutdown() throws Exception {
        var f = new Fixture("keep_canton:geneva");
        f.add("geneva", "car");
        var changed = f.add("geneva", "pt");
        var injector = com.google.inject.Guice.createInjector(new com.google.inject.AbstractModule() {
            @Override protected void configure() {
                bind(Scenario.class).toInstance(f.scenario);
                bind(OutputDirectoryHierarchy.class).toInstance(f.output);
                bind(SwissCmdpModeParameters.class).toInstance(f.parameters);
                bind(TripListConverter.class).toInstance(f.converter);
                bind(AlphaCalibratorConfig.class).toInstance(f.config);
                bind(EqasimConfigGroup.class).toInstance(f.eqasim);
                bind(org.eqasim.core.components.fast_calibration.FastCalibration.class)
                        .to(KeepCantonAlphaCalibrator.class).asEagerSingleton();
            }
        });
        var calibration = injector.getInstance(org.eqasim.core.components.fast_calibration.FastCalibration.class);
        assertSame(calibration, injector.getInstance(KeepCantonAlphaCalibrator.class));
        var listeners = new org.matsim.core.controler.ControllerListenerManagerImpl();
        listeners.addControllerListener(calibration);
        Files.createDirectories(Path.of(f.output.getIterationPath(0)));
        listeners.fireControllerIterationStartsEvent(0, false);
        changed.leg.setMode("car");
        Files.createDirectories(Path.of(f.output.getIterationPath(1)));
        listeners.fireControllerIterationStartsEvent(1, true);
        assertTrue(f.parameters.swissCanton.car.get("geneva") < 0);
        listeners.fireControllerIterationEndsEvent(1, true);
        assertNotNull(f.eqasim.getModeParametersPath());
        assertTrue(Files.exists(Path.of(f.output.getOutputFilenameWithOutputPrefix("mode_parameters.yml"))));
    }

    private class Fixture {
        final AlphaCalibratorConfig config = new AlphaCalibratorConfig();
        final EqasimConfigGroup eqasim = new EqasimConfigGroup();
        final Scenario scenario;
        final SwissCmdpModeParameters parameters = new SwissCmdpModeParameters();
        final TripListConverter converter = mock(TripListConverter.class);
        final OutputDirectoryHierarchy output;

        Fixture(String level) throws Exception {
            config.setActivate(true);
            config.setLevel(level);
            config.setBeta(0.5);
            config.setFilePath("unused-and-missing.csv");
            var mainConfig = ConfigUtils.createConfig(config, eqasim);
            mainConfig.controller().setOutputDirectory(temp.getRoot().toPath().resolve("run-" + System.nanoTime()).toString());
            scenario = ScenarioUtils.createScenario(mainConfig);
            output = new OutputDirectoryHierarchy(mainConfig);
        }

        Entry add(String canton, String mode) {
            var factory = scenario.getPopulation().getFactory();
            var person = factory.createPerson(Id.createPersonId(scenario.getPopulation().getPersons().size()));
            person.getAttributes().putAttribute("cantonName", canton);
            var plan = factory.createPlan();
            var origin = factory.createActivityFromCoord("home", new Coord(0, 0));
            var destination = factory.createActivityFromCoord("work", new Coord(100, 0));
            var leg = factory.createLeg(mode);
            plan.addActivity(origin);
            plan.addLeg(leg);
            plan.addActivity(destination);
            person.addPlan(plan);
            scenario.getPopulation().addPerson(person);
            when(converter.convert(plan)).thenAnswer(invocation -> List.of(new DiscreteModeChoiceTrip(
                    origin, destination, leg.getMode(), List.of(leg), 0, 0, 0, new AttributesImpl())));
            return new Entry(person, plan, origin, destination, leg);
        }

        KeepCantonAlphaCalibrator create() {
            return new KeepCantonAlphaCalibrator(scenario, output, parameters, converter, config, eqasim);
        }

        void iteration(KeepCantonAlphaCalibrator calibrator, int iteration) throws Exception {
            Files.createDirectories(Path.of(output.getIterationPath(iteration)));
            calibrator.notifyIterationStarts(new IterationStartsEvent(null, iteration, false));
        }
    }

    private record Entry(Person person, Plan plan, Activity origin, Activity destination, Leg leg) { }
}
