package org.eqasim.ile_de_france;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;

import ch.ethz.matsim.discrete_mode_choice.modules.DiscreteModeChoiceModule;

public class IDFConfigurator extends EqasimConfigurator {
	static public void adjustUrbanCapacity(Scenario scenario, double urbanCapacityFactor) {
		for (Link link : scenario.getNetwork().getLinks().values()) {
			boolean isUrban = (Boolean) link.getAttributes().getAsMap().getOrDefault("isUrban", false);

			if (isUrban) {
				link.setCapacity(link.getCapacity() * urbanCapacityFactor);
			}
		}
	}

	static public void adjustCalibratedConfiguration(Config config) {
		// TODO: Eventually, this should go to the pipeline and directly to the config
		// file!

		// Adjust flow and storage capacity
		config.qsim().setFlowCapFactor(0.045);
		config.qsim().setStorageCapFactor(0.045);

		// Adjust replanning
		for (StrategySettings strategy : config.strategy().getStrategySettings()) {
			if (strategy.getStrategyName().equals(DiscreteModeChoiceModule.STRATEGY_NAME)) {
				strategy.setWeight(0.05);
			}

			if (strategy.getStrategyName().equals("KeepLastSelected")) {
				strategy.setWeight(0.95);
			}
		}

		// Adjust iterations
		config.controler().setLastIteration(60);
	}

	static public void adjustCalibratedScenario(CommandLine cmd, Scenario scenario) {
		double urbanCapacityFactor = cmd.getOption("urban-capacity-factor").map(Double::parseDouble).orElse(0.8);
		adjustUrbanCapacity(scenario, urbanCapacityFactor);
	}

	static public void checkUrbanAttributes(Scenario scenario) {
		boolean foundUrbanActivityAttribute = false;
		boolean foundUrbanLinkAttribute = false;

		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getAttributes().getAttribute("isUrban") != null) {
				foundUrbanLinkAttribute = true;
				break;
			}
		}

		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
				if (element instanceof Activity) {
					if (element.getAttributes().getAttribute("isUrban") != null) {
						foundUrbanActivityAttribute = true;
					}
				}
			}
		}

		if (!foundUrbanActivityAttribute) {
			throw new IllegalStateException(
					"Could not find 'isUrban' attribute in population. Are you using an old version of the pipeline?");
		}

		if (!foundUrbanLinkAttribute) {
			throw new IllegalStateException(
					"Could not find 'isUrban' attribute in network. Are you using an old version of the pipeline?");
		}
	}
}
