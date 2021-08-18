package org.eqasim.core.scenario.cutter;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Optional;

import org.eqasim.core.components.transit.events.PublicTransitEvent;
import org.eqasim.core.components.transit.events.PublicTransitEventHandler;
import org.eqasim.core.components.transit.events.PublicTransitEventMapper;
import org.eqasim.core.misc.InjectorBuilder;
import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.extent.ShapeScenarioExtent;
import org.eqasim.core.scenario.cutter.facilities.CleanHomeFacilities;
import org.eqasim.core.scenario.cutter.facilities.FacilitiesCutter;
import org.eqasim.core.scenario.cutter.network.MinimumNetworkFinder;
import org.eqasim.core.scenario.cutter.network.NetworkCutter;
import org.eqasim.core.scenario.cutter.network.RoadNetwork;
import org.eqasim.core.scenario.cutter.outside.OutsideActivityAdapter;
import org.eqasim.core.scenario.cutter.population.CleanHouseholds;
import org.eqasim.core.scenario.cutter.population.PopulationCutter;
import org.eqasim.core.scenario.cutter.population.PopulationCutterModule;
import org.eqasim.core.scenario.cutter.population.RemoveEmptyPlans;
import org.eqasim.core.scenario.cutter.transit.DefaultStopSequenceCrossingPointFinder;
import org.eqasim.core.scenario.cutter.transit.StopSequenceCrossingPointFinder;
import org.eqasim.core.scenario.cutter.transit.TransitScheduleCutter;
import org.eqasim.core.scenario.cutter.transit.TransitVehiclesCutter;
import org.eqasim.core.scenario.routing.PopulationRouter;
import org.eqasim.core.scenario.routing.PopulationRouterModule;
import org.eqasim.core.scenario.validation.ScenarioValidator;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.ControlerListenerManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.scoring.EventsToLegs.LegHandler;
import org.matsim.core.scoring.ExperiencedPlansModule;
import org.matsim.core.scoring.ExperiencedPlansService;
import org.matsim.core.scoring.PersonExperiencedLeg;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;

import com.google.inject.Injector;

public class RunScenarioCutter {
	static public void main(String[] args)
			throws ConfigurationException, MalformedURLException, IOException, InterruptedException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path", "output-path", "extent-path") //
				.allowOptions("threads", "prefix", "extent-attribute", "extent-value", "events-path", "plans-path") //
				.build();

		// Load some configuration
		String prefix = cmd.getOption("prefix").orElse("");
		int numberOfThreads = cmd.getOption("threads").map(Integer::parseInt)
				.orElse(Runtime.getRuntime().availableProcessors());

		File outputDirectory = new File(cmd.getOptionStrict("output-path")).getAbsoluteFile();
		ScenarioWriter.checkOutputDirectory(outputDirectory);

		// Load scenario extent
		File extentPath = new File(cmd.getOptionStrict("extent-path"));
		Optional<String> extentAttribute = cmd.getOption("extent-attribute");
		Optional<String> extentValue = cmd.getOption("extent-value");
		ScenarioExtent extent = new ShapeScenarioExtent.Builder(extentPath, extentAttribute, extentValue).build();

		// Load scenario
		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"),
				EqasimConfigurator.getConfigGroups());
		cmd.applyConfiguration(config);

		Scenario scenario = ScenarioUtils.createScenario(config);
		EqasimConfigurator.configureScenario(scenario);

		Optional<String> eventsPath = cmd.getOption("events-path");
		Optional<String> plansPath = cmd.getOption("plans-path");

		if (eventsPath.isPresent() && plansPath.isPresent()) {
			throw new IllegalStateException("Only one of events-path or plans-path can be provided.");
		}

		if (plansPath.isPresent()) {
			File plansFile = new File(plansPath.get());

			if (!plansFile.exists()) {
				throw new IllegalStateException("Plans file does not exist: " + plansPath);
			} else {
				config.plans().setInputFile(plansFile.getAbsolutePath());
			}
		}

		ScenarioUtils.loadScenario(scenario);

		if (eventsPath.isPresent()) {
			Injector eventsProcessorInjector = new InjectorBuilder(scenario) //
					.addOverridingModules(EqasimConfigurator.getModules()) //
					.addOverridingModule(new ExperiencedPlansModule()) //
					.addOverridingModule(new AbstractModule() {
						@Override
						public void install() {
							bind(ControlerListenerManager.class).toInstance(new ControlerListenerManagerImpl());
						}

					}).build();

			ExperiencedPlansService experiencedPlansService = eventsProcessorInjector
					.getInstance(ExperiencedPlansService.class);

			ControlerListenerManagerImpl controlerListenerManager = (ControlerListenerManagerImpl) eventsProcessorInjector
					.getInstance(ControlerListenerManager.class);
			controlerListenerManager.fireControlerIterationStartsEvent(0, false);

			EventsManager eventsManager = eventsProcessorInjector.getInstance(EventsManager.class);
			MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
			reader.addCustomEventMapper(PublicTransitEvent.TYPE, new PublicTransitEventMapper());

			IdMap<Person, PublicTransitEvent> transitEvents = new IdMap<>(Person.class);

			eventsManager.addHandler(new PublicTransitEventHandler() {
				@Override
				public void handleEvent(PublicTransitEvent event) {
					transitEvents.put(event.getPersonId(), event);
				}
			});

			eventsProcessorInjector.getInstance(EventsToLegs.class).addLegHandler(new LegHandler() {
				@Override
				public void handleLeg(PersonExperiencedLeg leg) {
					if (leg.getLeg().getMode().equals(TransportMode.pt)) {
						PublicTransitEvent transitEvent = transitEvents.remove(leg.getAgentId());

						DefaultTransitPassengerRoute route = new DefaultTransitPassengerRoute(
								leg.getLeg().getRoute().getStartLinkId(), leg.getLeg().getRoute().getEndLinkId(),
								transitEvent.getAccessStopId(), transitEvent.getEgressStopId(),
								transitEvent.getTransitLineId(), transitEvent.getTransitRouteId());

						route.setDistance(leg.getLeg().getRoute().getDistance());
						route.setTravelTime(leg.getLeg().getRoute().getTravelTime().seconds());

						leg.getLeg().setRoute(route);
					}
				}
			});

			reader.readFile(eventsPath.get());

			Population population = scenario.getPopulation();

			for (Id<Person> personId : new ArrayList<>(population.getPersons().keySet())) {
				Person person = population.getPersons().get(personId);
				Plan recordedPlan = experiencedPlansService.getExperiencedPlans().get(personId);

				if (recordedPlan == null) {
					population.removePerson(personId);
				} else {
					new ArrayList<>(person.getPlans()).forEach(person::removePlan);
					person.addPlan(recordedPlan);

					for (PlanElement element : recordedPlan.getPlanElements()) {
						if (element instanceof Activity) {
							Activity activity = (Activity) element;

							if (!TripStructureUtils.isStageActivityType(activity.getType())) {
								ActivityFacility facility = scenario.getActivityFacilities().getFacilities()
										.get(activity.getFacilityId());
								activity.setCoord(facility.getCoord());
							}
						}
					}
				}
			}
		}

		// Check validity before cutting
		ScenarioValidator scenarioValidator = new ScenarioValidator();
		scenarioValidator.checkScenario(scenario);

		// Prepare road network
		RoadNetwork roadNetwork = new RoadNetwork(scenario.getNetwork());

		// Cut population
		Injector populationCutterInjector = new InjectorBuilder(scenario) //
				.addOverridingModules(EqasimConfigurator.getModules()) //
				.addOverridingModule(new PopulationCutterModule(extent, numberOfThreads, 40)) //
				.build();

		PopulationCutter populationCutter = populationCutterInjector.getInstance(PopulationCutter.class);
		populationCutter.run(scenario.getPopulation());

		// ... and remove empty plans
		RemoveEmptyPlans removeEmptyPlans = new RemoveEmptyPlans();
		removeEmptyPlans.run(scenario.getPopulation());

		// ... and make outside activities consistent
		OutsideActivityAdapter outsideActivityAdapter = new OutsideActivityAdapter(roadNetwork);
		outsideActivityAdapter.run(scenario.getPopulation(), scenario.getActivityFacilities());

		// ... and make households consistent
		CleanHouseholds cleanHouseholds = new CleanHouseholds(scenario.getPopulation());
		cleanHouseholds.run(scenario.getHouseholds());

		// Cut transit
		StopSequenceCrossingPointFinder stopSequenceCrossingPointFinder = new DefaultStopSequenceCrossingPointFinder(
				extent);
		TransitScheduleCutter transitScheduleCutter = new TransitScheduleCutter(extent,
				stopSequenceCrossingPointFinder);
		transitScheduleCutter.run(scenario.getTransitSchedule());

		TransitVehiclesCutter transitVehiclesCutter = new TransitVehiclesCutter(scenario.getTransitSchedule());
		transitVehiclesCutter.run(scenario.getTransitVehicles());

		// Cut facilities
		CleanHomeFacilities cleanHomeFacilities = new CleanHomeFacilities(scenario.getPopulation());
		cleanHomeFacilities.run(scenario.getActivityFacilities());

		FacilitiesCutter facilitiesCutter = new FacilitiesCutter(extent, scenario.getPopulation());
		facilitiesCutter.run(scenario.getActivityFacilities(), true);

		// Cut network
		MinimumNetworkFinder minimumNetworkFinder = new MinimumNetworkFinder(extent, roadNetwork, numberOfThreads, 20);
		NetworkCutter networkCutter = new NetworkCutter(extent, scenario, minimumNetworkFinder);
		networkCutter.run(scenario.getNetwork());

		// "Cut" config
		// (we need to reload it, because it has become locked at this point)
		config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), EqasimConfigurator.getConfigGroups());
		cmd.applyConfiguration(config);
		ConfigCutter configCutter = new ConfigCutter(prefix);
		configCutter.run(config);

		// Final routing
		Injector routingInjector = new InjectorBuilder(scenario) //
				.addOverridingModules(EqasimConfigurator.getModules()) //
				.addOverridingModule(new PopulationRouterModule(numberOfThreads, 100, false)) //
				.build();

		PopulationRouter router = routingInjector.getInstance(PopulationRouter.class);
		router.run(scenario.getPopulation());

		// Check validity after cutting
		scenarioValidator.checkScenario(scenario);

		// Write scenario
		ScenarioWriter scenarioWriter = new ScenarioWriter(config, scenario, prefix);
		scenarioWriter.run(outputDirectory);
	}
}
