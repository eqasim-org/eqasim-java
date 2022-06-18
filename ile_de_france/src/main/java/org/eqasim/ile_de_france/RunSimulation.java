package org.eqasim.ile_de_france;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.misc.InjectorBuilder;
import org.eqasim.core.scenario.routing.PopulationRouter;
import org.eqasim.core.scenario.routing.PopulationRouterModule;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.TeleportedModeParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.algorithms.TripsToLegsAlgorithm;
import org.matsim.core.router.RoutingModeMainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.timing.TimeInterpretationModule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.inject.Injector;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet;

public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException, InterruptedException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes("mode-choice-parameter", "cost-parameter") //
				.build();

		IDFConfigurator configurator = new IDFConfigurator();
		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), configurator.getConfigGroups());
		cmd.applyConfiguration(config);

		List<String> networkModes = new ArrayList<>(config.plansCalcRoute().getNetworkModes());
		networkModes.add("network_walk");
		config.plansCalcRoute().setNetworkModes(networkModes);
	
		// TeleportedModeParams routingParams = new TeleportedModeParams("network_walk");
		// routingParams.setTeleportedModeSpeed(null);
		// routingParams.setTeleportedModeFreespeedFactor(1.0);
		// config.plansCalcRoute().addTeleportedModeParams(routingParams);		
		
		ModeParams networkWalkParams = new ModeParams("network_walk");
		config.planCalcScore().addModeParams(networkWalkParams);

		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
		eqasimConfig.setEstimator("network_walk", "WalkUtilityEstimator");

		SwissRailRaptorConfigGroup srrConfig = (SwissRailRaptorConfigGroup) config.getModules()
				.get(SwissRailRaptorConfigGroup.GROUP);
		srrConfig.setUseIntermodalAccessEgress(true);

		IntermodalAccessEgressParameterSet networkWalkSet = new IntermodalAccessEgressParameterSet();
		networkWalkSet.setMode("network_walk");
		networkWalkSet.setInitialSearchRadius(200.0);
		networkWalkSet.setSearchExtensionRadius(200.0);
		networkWalkSet.setMaxRadius(2000.0);
		srrConfig.addIntermodalAccessEgress(networkWalkSet);

		Scenario scenario = ScenarioUtils.createScenario(config);
		configurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		configurator.adjustScenario(scenario);

		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				List<String> modes = new LinkedList<>();

				for (Trip trip : TripStructureUtils.getTrips(plan)) {
					boolean foundPt = false;

					for (Leg leg : trip.getLegsOnly()) {
						if (leg.getMode().equals("pt")) {
							foundPt = true;
						}
					}

					String routingMode = TripStructureUtils.identifyMainMode(trip.getTripElements());

					if (routingMode.equals("pt") && !foundPt) {
						modes.add("network_walk");
					} else if (routingMode.equals("walk")) {
						modes.add("network_walk");
					} else {
						modes.add(routingMode);
					}
				}

				new TripsToLegsAlgorithm(new RoutingModeMainModeIdentifier()).run(plan);

				int index = 0;

				for (Leg leg : TripStructureUtils.getLegs(plan)) {
					leg.setMode(modes.get(index));
					leg.getAttributes().putAttribute("routingMode", modes.get(index));
					index++;
				}
			}
		}

		System.err.println("Starting to update network");

		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getAllowedModes().contains("car")) {
				Set<String> allowedModes = new HashSet<>(new ArrayList<>(link.getAllowedModes()));
				allowedModes.add("network_walk");
				link.setAllowedModes(allowedModes);
			}
		}

		Network walkNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(scenario.getNetwork()).filter(walkNetwork,
				Collections.singleton("network_walk"));

		for (TransitStopFacility facility : scenario.getTransitSchedule().getFacilities().values()) {
			Link facilityLink = walkNetwork.getLinks().get(facility.getLinkId());

			if (facilityLink == null) {
				facilityLink = NetworkUtils.getNearestLink(walkNetwork, facility.getCoord());
				facility.setLinkId(facilityLink.getId());
			}
		}

		Injector injector = new InjectorBuilder(scenario) //
				.addOverridingModules(configurator.getModules()) //
				.addOverridingModule(new PopulationRouterModule(8, 100, true, Collections.emptySet())) //
				.addOverridingModule(new TimeInterpretationModule()) //
				.build();

		PopulationRouter populationRouter = injector.getInstance(PopulationRouter.class);
		populationRouter.run(scenario.getPopulation());

		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (Trip trip : TripStructureUtils.getTrips(plan)) {
					String routingMode = TripStructureUtils.getRoutingMode(trip.getLegsOnly().get(0));

					if (routingMode.equals("pt")) {
						boolean foundPt = false;

						for (Leg leg : trip.getLegsOnly()) {
							if (leg.getMode().equals("pt")) {
								foundPt = true;
							}
						}

						if (!foundPt) {
							trip.getLegsOnly().get(0).setMode("network_walk");
							trip.getLegsOnly().get(0).getAttributes().putAttribute("routingMode", "network_walk");
							trip.getLegsOnly().get(0).setRoute(null);
						}
					}
				}
			}
		}

		System.err.println("Finished updating network");

		Controler controller = new Controler(scenario);
		configurator.configureController(controller);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new IDFModeChoiceModule(cmd));
		controller.run();
	}
}