package org.eqasim.sao_paulo;

import java.util.Random;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.sao_paulo.mode_choice.SaoPauloModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;

public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes("mode-parameter", "cost-parameter", "already-equilibrium") //
				.build();

		EqasimConfigurator configurator = new EqasimConfigurator();
		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), configurator.getConfigGroups());
		EqasimConfigGroup.get(config).setAnalysisInterval(1);
		cmd.applyConfiguration(config);

		Scenario scenario = ScenarioUtils.createScenario(config);
		configurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		configurator.adjustScenario(scenario);
		// the first check is for backwards compatibility
		// it will be removed with the next synthesis release
		if (!cmd.getOption("already-equilibrium").isPresent()
				|| !Boolean.parseBoolean(cmd.getOption("already-equilibrium").get())) {
			Random random = new Random(0);

			for (Person person : scenario.getPopulation().getPersons().values()) {
				for (Plan plan : person.getPlans()) {
					for (Trip trip : TripStructureUtils.getTrips(plan)) {
						if (trip.getTripElements().size() == 1) {
							Leg leg = (Leg) trip.getTripElements().get(0);
							if (leg.getMode().equals("car")) {
								if (random.nextDouble() < 0.5) {
									leg.setMode("walk");
									leg.setRoute(null);
								}
							}
						}
					}
				}
			}
		}
		EqasimConfigGroup eqasimConfig = (EqasimConfigGroup) config.getModules().get(EqasimConfigGroup.GROUP_NAME);
		eqasimConfig.setEstimator("walk", "spWalkEstimator");
		eqasimConfig.setEstimator("pt", "spPTEstimator");
		eqasimConfig.setEstimator("car", "spCarEstimator");
		eqasimConfig.setEstimator("taxi", "spTaxiEstimator");

		Controler controller = new Controler(scenario);
		configurator.configureController(controller);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new SaoPauloModeChoiceModule(cmd));
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.run();
	}
}
