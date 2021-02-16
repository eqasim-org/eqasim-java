package org.eqasim.vdf.example;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.ile_de_france.IDFConfigurator;
import org.eqasim.ile_de_france.analysis.mode_share.ModeShareModule;
import org.eqasim.ile_de_france.analysis.urban.UrbanAnalysisModule;
import org.eqasim.ile_de_france.flow.calibration.CapacityAdjustment;
import org.eqasim.ile_de_france.grand_paris.PersonUtilityModule;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.eqasim.ile_de_france.mode_choice.epsilon.EpsilonModule;
import org.eqasim.vdf.VDFConfig;
import org.eqasim.vdf.VDFModule;
import org.eqasim.vdf.VDFQSimModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contribs.discrete_mode_choice.modules.SelectorModule;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowOptions("use-epsilon", "convergence-threshold", "flow-path", "fix-modes") //
				.allowPrefixes("mode-choice-parameter", "cost-parameter", "capacity") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), IDFConfigurator.getConfigGroups());
		cmd.applyConfiguration(config);

		Scenario scenario = ScenarioUtils.createScenario(config);
		IDFConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);

		new CapacityAdjustment(cmd).apply(config, scenario.getNetwork());

		Controler controller = new Controler(scenario);
		IDFConfigurator.configureController(controller);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new IDFModeChoiceModule(cmd));
		controller.addOverridingModule(new PersonUtilityModule());
		controller.addOverridingModule(new UrbanAnalysisModule());

		if (cmd.getOption("use-epsilon").map(Boolean::parseBoolean).orElse(false)) {
			controller.addOverridingModule(new EpsilonModule());

			double convergenceThreshold = cmd.getOption("convergence-threshold").map(Double::parseDouble).orElse(0.01);
			controller.addOverridingModule(new ModeShareModule(convergenceThreshold));

			DiscreteModeChoiceConfigGroup dmcConfig = DiscreteModeChoiceConfigGroup.getOrCreate(config);
			dmcConfig.setSelector(SelectorModule.MAXIMUM);

			EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
			eqasimConfig.setEstimator("car", "epsilon_car");
			eqasimConfig.setEstimator("pt", "epsilon_pt");
			eqasimConfig.setEstimator("bike", "epsilon_bike");
			eqasimConfig.setEstimator("walk", "epsilon_walk");
		}

		VDFConfig vdfConfig = new VDFConfig();
		controller.addOverridingModule(new VDFModule(vdfConfig));
		controller.addOverridingQSimModule(new VDFQSimModule());

		config.qsim().setStorageCapFactor(100000);
		config.qsim().setFlowCapFactor(100000);
		config.qsim().setStuckTime(24.0 * 3600.0);
		config.qsim().setTrafficDynamics(TrafficDynamics.queue);
		
		controller.run();
	}
}