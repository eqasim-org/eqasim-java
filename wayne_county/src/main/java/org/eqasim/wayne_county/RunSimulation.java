package org.eqasim.wayne_county;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.wayne_county.mode_choice.WayneCountyModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunSimulation {

	public static void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes("mode-parameter", "cost-parameter") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"),
				EqasimConfigurator.getConfigGroups());
		EqasimConfigGroup.get(config).setTripAnalysisInterval(5);
		cmd.applyConfiguration(config);

		Scenario scenario = ScenarioUtils.createScenario(config);

		EqasimConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		for (Person person : scenario.getPopulation().getPersons().values()) {

			Plan plan = person.getPlans().get(0);

			for (PlanElement pe : plan.getPlanElements()) {

				if (pe instanceof Activity) {
					Link link = scenario.getNetwork().getLinks().get(((Activity) pe).getLinkId());
					((Activity) pe).setCoord(link.getCoord());
				}
			}
		}
		EqasimConfigurator.adjustScenario(scenario);

		EqasimConfigGroup eqasimConfig = (EqasimConfigGroup) config.getModules().get(EqasimConfigGroup.GROUP_NAME);

		eqasimConfig.setEstimator("walk", "wcWalkEstimator");
		eqasimConfig.setEstimator("pt", "wcPTEstimator");
		eqasimConfig.setEstimator("car", "wcCarEstimator");

		Controler controller = new Controler(scenario);
		EqasimConfigurator.configureController(controller);
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new WayneCountyModeChoiceModule(cmd));
		controller.addOverridingModule(new EqasimAnalysisModule());
		// controller.addOverridingModule(new CalibrationModule());
		controller.run();

	}

}
