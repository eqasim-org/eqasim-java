package org.eqasim.mode_choice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.misc.InjectorBuilder;
import org.eqasim.core.scenario.config.GenerateConfig;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceModel;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceModel.NoFeasibleChoiceException;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;
import org.matsim.contribs.discrete_mode_choice.modules.AbstractDiscreteModeChoiceExtension;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.Facility;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

public class TestSpecialModeChoiceCases {
	@Test
	public void testOrdinaryTour() throws ConfigurationException, NoFeasibleChoiceException {
		List<DiscreteModeChoiceTrip> trips = new LinkedList<>();
		appendTrip(trips, "home", "A", "work", "B");
		appendTrip(trips, "work", "B", "shop", "C");
		appendTrip(trips, "shop", "C", "home", "A");

		Set<String> chains = findChains(trips, 100);

		assertEquals(2, chains.size());
		assertTrue(chains.contains("walk walk walk"));
		assertTrue(chains.contains("bike bike bike"));
	}

	@Test
	public void testTwoOrdinaryTours() throws ConfigurationException, NoFeasibleChoiceException {
		List<DiscreteModeChoiceTrip> trips = new LinkedList<>();
		appendTrip(trips, "home", "A", "work", "B");
		appendTrip(trips, "work", "B", "shop", "C");
		appendTrip(trips, "shop", "C", "home", "A");
		appendTrip(trips, "home", "A", "leisure", "D");
		appendTrip(trips, "leisure", "D", "home", "A");

		Set<String> chains = findChains(trips, 100);

		assertEquals(4, chains.size());
		assertTrue(chains.contains("walk walk walk walk walk"));
		assertTrue(chains.contains("walk walk walk bike bike"));
		assertTrue(chains.contains("bike bike bike bike bike"));
		assertTrue(chains.contains("bike bike bike walk walk"));
	}

	@Test
	public void testTails() throws ConfigurationException, NoFeasibleChoiceException {
		{
			List<DiscreteModeChoiceTrip> trips = new LinkedList<>();

			appendTrip(trips, "leisure", "D", "shop", "D");
			appendTrip(trips, "shop", "D", "home", "A");
			appendTrip(trips, "home", "A", "work", "B");
			appendTrip(trips, "work", "B", "home", "A");

			Set<String> chains = findChains(trips, 100);

			assertEquals(4, chains.size());
			assertTrue(chains.contains("walk walk walk walk"));
			assertTrue(chains.contains("bike bike walk walk"));
			assertTrue(chains.contains("walk walk bike bike"));
			assertTrue(chains.contains("bike bike bike bike"));
		}

		{
			List<DiscreteModeChoiceTrip> trips = new LinkedList<>();

			appendTrip(trips, "home", "A", "work", "B");
			appendTrip(trips, "work", "B", "home", "A");
			appendTrip(trips, "home", "A", "shop", "D");
			appendTrip(trips, "shop", "D", "leisure", "D");

			Set<String> chains = findChains(trips, 100);

			assertEquals(4, chains.size());
			assertTrue(chains.contains("walk walk walk walk"));
			assertTrue(chains.contains("bike bike walk walk"));
			assertTrue(chains.contains("walk walk bike bike"));
			assertTrue(chains.contains("bike bike bike bike"));
		}
	}

	@Test
	public void testFreeChain() throws ConfigurationException, NoFeasibleChoiceException {
		List<DiscreteModeChoiceTrip> trips = new LinkedList<>();

		appendTrip(trips, "leisure", "D", "shop", "D");
		appendTrip(trips, "shop", "D", "shop", "A");
		appendTrip(trips, "shop", "A", "leisure", "D");

		Set<String> chains = findChains(trips, 100);
		System.out.println(chains);

		assertEquals(4, chains.size());
		assertTrue(chains.contains("walk walk walk"));
		assertTrue(chains.contains("bike bike bike"));
	}

	static private Set<String> findChains(List<DiscreteModeChoiceTrip> trips, int samples)
			throws ConfigurationException, NoFeasibleChoiceException {
		// I) Create configuration that is usually generated for eqasim simulations
		Config config = ConfigUtils.createConfig();
		CommandLine cmd = new CommandLine.Builder(new String[] {}).build();

		new GenerateConfig(cmd, "", 1.0, 1, 1).run(config);

		// Make sure the two relevant options (walk vs. bike) both get zero utility
		DiscreteModeChoiceConfigGroup.getOrCreate(config).setModeAvailability("static");
		EqasimConfigGroup.get(config).setEstimator("walk", EqasimModeChoiceModule.ZERO_ESTIMATOR_NAME);
		EqasimConfigGroup.get(config).setEstimator("bike", EqasimModeChoiceModule.ZERO_ESTIMATOR_NAME);

		// Now create the model
		Scenario scenario = ScenarioUtils.createScenario(config);
		Injector injector = new InjectorBuilder(scenario) //
				.addOverridingModules(EqasimConfigurator.getModules()) //
				.addOverridingModule(new EqasimModeChoiceModule()) //
				.addOverridingModule(new StaticModeAvailabilityModule()) //
				.build();

		DiscreteModeChoiceModel model = injector.getInstance(DiscreteModeChoiceModel.class);

		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		Person person = populationFactory.createPerson(Id.createPersonId("person"));
		Random random = new Random();

		model.chooseModes(person, trips, random);

		Set<String> chains = new HashSet<>();

		for (int i = 0; i < samples; i++) {
			List<String> modes = model.chooseModes(person, trips, random).stream().map(c -> c.getMode())
					.collect(Collectors.toList());

			chains.add(String.join(" ", modes));
		}

		return chains;
	}

	static private void appendTrip(List<DiscreteModeChoiceTrip> trips, String precedingPurpose, String precedingLinkId,
			String followingPurpose, String followingLinkId) {
		Activity originActivity = PopulationUtils.createActivityFromLinkId(precedingPurpose,
				Id.createLinkId(precedingLinkId));
		Activity destinationActivity = PopulationUtils.createActivityFromLinkId(followingPurpose,
				Id.createLinkId(followingLinkId));

		originActivity.setMaximumDuration(3600.0);
		destinationActivity.setMaximumDuration(3600.0);

		DiscreteModeChoiceTrip trip = new DiscreteModeChoiceTrip(originActivity, destinationActivity, "walk",
				Collections.emptyList(), 0, trips.size(), trips.size());
		trips.add(trip);
	}

	static private class StaticModeAvailabilityModule extends AbstractDiscreteModeChoiceExtension {
		@Override
		protected void installExtension() {
			bindModeAvailability("static").toInstance(new StaticModeAvailability());
			bind(ModeParameters.class).toInstance(new ModeParameters());

			addRoutingModuleBinding("walk").to(StaticRoutingModule.class);
			addRoutingModuleBinding("bike").to(StaticRoutingModule.class);
		}
	}

	@Singleton
	static private class StaticRoutingModule implements RoutingModule {
		private final PopulationFactory populationFactory;

		@Inject
		public StaticRoutingModule(PopulationFactory populationFactory) {
			this.populationFactory = populationFactory;
		}

		@Override
		public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime,
				Person person) {
			Leg leg = populationFactory.createLeg("doesn't matter");
			leg.setTravelTime(3600.0);

			return Collections.singletonList(leg);
		}
	}

	static private class StaticModeAvailability implements ModeAvailability {
		@Override
		public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
			return Arrays.asList("bike", "walk");
		}
	}
}
