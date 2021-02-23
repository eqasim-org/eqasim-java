package org.eqasim.ile_de_france;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.ile_de_france.analysis.mode_share.ModeShareModule;
import org.eqasim.ile_de_france.analysis.urban.UrbanAnalysisModule;
import org.eqasim.ile_de_france.flow.analysis.FlowModule;
import org.eqasim.ile_de_france.flow.calibration.CapacityAdjustment;
import org.eqasim.ile_de_france.grand_paris.PersonUtilityModule;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.eqasim.ile_de_france.mode_choice.epsilon.EpsilonModule;
import org.eqasim.ile_de_france.travel_time.TravelTimeComparisonModule;
import org.eqasim.vdf.VDFConfig;
import org.eqasim.vdf.VDFModule;
import org.eqasim.vdf.VDFQSimModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contribs.discrete_mode_choice.modules.DiscreteModeChoiceModule;
import org.matsim.contribs.discrete_mode_choice.modules.SelectorModule;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;

public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowOptions("use-epsilon", "mode-convergence-threshold", "travel-time-convergence-threshold",
						"flow-path", "fix-modes", "use-vdf") //
				.allowPrefixes("mode-choice-parameter", "cost-parameter", "capacity") //
				.build();

		boolean useVdf = cmd.getOption("use-vdf").map(Boolean::parseBoolean).orElse(false);

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), IDFConfigurator.getConfigGroups());
		cmd.applyConfiguration(config);

		Scenario scenario = ScenarioUtils.createScenario(config);
		IDFConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);

		new CapacityAdjustment(cmd).apply(config, scenario.getNetwork());

		Controler controller = new Controler(scenario);
		IDFConfigurator.configureController(controller, !useVdf);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new IDFModeChoiceModule(cmd));
		controller.addOverridingModule(new PersonUtilityModule());
		controller.addOverridingModule(new UrbanAnalysisModule());

		if (cmd.getOption("use-epsilon").map(Boolean::parseBoolean).orElse(false)) {
			controller.addOverridingModule(new EpsilonModule());

			double convergenceThreshold = cmd.getOption("mode-convergence-threshold").map(Double::parseDouble)
					.orElse(0.01);
			controller.addOverridingModule(new ModeShareModule(convergenceThreshold));

			DiscreteModeChoiceConfigGroup dmcConfig = DiscreteModeChoiceConfigGroup.getOrCreate(config);
			dmcConfig.setSelector(SelectorModule.MAXIMUM);

			EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
			eqasimConfig.setEstimator("car", "epsilon_car");
			eqasimConfig.setEstimator("pt", "epsilon_pt");
			eqasimConfig.setEstimator("bike", "epsilon_bike");
			eqasimConfig.setEstimator("walk", "epsilon_walk");
		}

		if (cmd.hasOption("flow-path")) {
			controller.addOverridingModule(new FlowModule(cmd.getOptionStrict("flow-path")));
		}

		if (cmd.getOption("fix-modes").map(Boolean::parseBoolean).orElse(false)) {
			for (StrategySettings strategy : config.strategy().getStrategySettings()) {
				if (strategy.getStrategyName().equals(DiscreteModeChoiceModule.STRATEGY_NAME)) {
					strategy.setStrategyName(DefaultStrategy.ReRoute);
				}
			}

			if (cmd.getOption("use-epsilon").map(Boolean::parseBoolean).orElse(false)) {
				throw new IllegalStateException("Cannot be used in combination");
			}

			DiscreteModeChoiceConfigGroup.getOrCreate(config).setEnforceSinglePlan(false);
		}

		double comparisonStartTime = 0.0;
		double comparisonEndTime = config.travelTimeCalculator().getMaxTime();

		if (useVdf) {
			VDFConfig vdfConfig = new VDFConfig();
			controller.addOverridingModule(new VDFModule(vdfConfig));
			controller.addOverridingQSimModule(new VDFQSimModule());

			config.qsim().setStorageCapFactor(100000);
			config.qsim().setFlowCapFactor(100000);
			config.qsim().setStuckTime(24.0 * 3600.0);
			config.qsim().setTrafficDynamics(TrafficDynamics.queue);

			// config.controler().setWriteEventsInterval(20);
			// config.controler().setWritePlansInterval(20);

			comparisonStartTime = vdfConfig.startTime;
			comparisonEndTime = vdfConfig.endTime;
		}

		double convergenceThreshold = cmd.getOption("travel-time-convergence-threshold").map(Double::parseDouble)
				.orElse(0.2);
		controller.addOverridingModule(
				new TravelTimeComparisonModule(300.0, comparisonStartTime, comparisonEndTime, convergenceThreshold, 0));

		config.linkStats().setWriteLinkStatsInterval(0);
		config.controler().setWriteTripsInterval(0);

		controller.run();
	}
}