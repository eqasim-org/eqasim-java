package org.eqasim.projects.astra16;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.calibration.CalibrationModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.projects.astra16.convergence.ConvergenceModule;
import org.eqasim.projects.astra16.travel_time.SmoothingTravelTimeModule;
import org.eqasim.projects.astra16.travel_time.TravelTimeComparisonModule;
import org.eqasim.switzerland.SwitzerlandConfigurator;
import org.eqasim.switzerland.mode_choice.SwissModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.algorithms.TripsToLegsAlgorithm;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowOptions("clean-routes") //
				.allowPrefixes("av-mode-parameter", "mode-parameter", "cost-parameter") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), AstraConfigurator.getConfigGroups());
		AstraConfigurator.configure(config);
		cmd.applyConfiguration(config);
		AstraConfigurator.adjustOperator(config);

		Scenario scenario = ScenarioUtils.createScenario(config);

		SwitzerlandConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		SwitzerlandConfigurator.adjustScenario(scenario);
		AstraConfigurator.adjustScenario(scenario);
		AstraConfigurator.adjustNetwork(scenario);

		// Travel time calculator
		config.travelTimeCalculator().setAnalyzedModes(new HashSet<>(Arrays.asList("car", "car_passenger", "truck")));
		config.travelTimeCalculator().setFilterModes(true);
		config.travelTimeCalculator().setSeparateModes(false);

		config.plansCalcRoute().setInsertingAccessEgressWalk(false);
		config.plansCalcRoute().setRoutingRandomness(0.0);

		if (cmd.hasOption("clean-routes")) {
			for (Person person : scenario.getPopulation().getPersons().values()) {
				for (Plan plan : person.getPlans()) {
					new TripsToLegsAlgorithm(new MainModeIdentifier() {
						@Override
						public String identifyMainMode(List<? extends PlanElement> tripElements) {
							for (Leg leg : TripStructureUtils.getLegs(tripElements)) {
								if (leg.getMode().equals("access_walk")) {
									return "pt";
								}

								if (leg.getMode().equals("egress_walk")) {
									return "pt";
								}

								if (leg.getMode().equals("transit_walk")) {
									return "pt";
								}

								if (leg.getMode().equals("pt")) {
									return "pt";
								}
							}

							return ((Leg) tripElements.get(0)).getMode();
						}
					}).run(plan);
				}
			}
		}

		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);

		for (Link link : scenario.getNetwork().getLinks().values()) {
			double maximumSpeed = link.getFreespeed();
			boolean isMajor = true;

			for (Link other : link.getToNode().getInLinks().values()) {
				if (other.getCapacity() >= link.getCapacity()) {
					isMajor = false;
				}
			}

			if (!isMajor && link.getToNode().getInLinks().size() > 1) {
				double travelTime = link.getLength() / maximumSpeed;
				travelTime += eqasimConfig.getCrossingPenalty();
				link.setFreespeed(link.getLength() / travelTime);
			}
		}

		// EqasimLinkSpeedCalcilator deactivated!

		Controler controller = new Controler(scenario);
		SwitzerlandConfigurator.configureController(controller);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new SwissModeChoiceModule(cmd));
		controller.addOverridingModule(new CalibrationModule());
		controller.addOverridingModule(new AstraModule(cmd));
		controller.addOverridingModule(new TravelTimeComparisonModule());
		controller.addOverridingModule(new ConvergenceModule());

		AstraConfigurator.configureController(controller, cmd);

		controller.addOverridingModule(new SmoothingTravelTimeModule());

		controller.run();
	}
}
