package org.eqasim.ile_de_france;

import java.io.File;
import java.util.Optional;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.traffic.EqasimTrafficQSimModule;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.convergence.ConvergenceTerminationCriterion;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.ile_de_france.analysis.counts.CountsModule;
import org.eqasim.ile_de_france.analysis.stuck.StuckAnalysisModule;
import org.eqasim.ile_de_france.analysis.urban.UrbanAnalysisModule;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.eqasim.ile_de_france.mode_choice.epsilon.EpsilonModule;
import org.eqasim.vdf.VDFConfigGroup;
import org.eqasim.vdf.VDFModule;
import org.eqasim.vdf.VDFQSimModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contribs.discrete_mode_choice.modules.SelectorModule;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.TerminationCriterion;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowOptions("count-links", "external-convergence", "signal-input-path", "use-epsilon", "use-vdf",
						"line-switch-utility") //
				.allowPrefixes("mode-choice-parameter", "cost-parameter", OsmNetworkAdjustment.CAPACITY_PREFIX,
						OsmNetworkAdjustment.SPEED_PREFIX) //
				.build();

		boolean useVdf = cmd.getOption("use-vdf").map(Boolean::parseBoolean).orElse(false);
		IDFConfigurator configurator = new IDFConfigurator();

		if (useVdf) {
			configurator.getQSimModules().removeIf(m -> m instanceof EqasimTrafficQSimModule);
		}

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), configurator.getConfigGroups());
		config.addModule(new VDFConfigGroup());
		cmd.applyConfiguration(config);

		Scenario scenario = ScenarioUtils.createScenario(config);
		configurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);

		new OsmNetworkAdjustment(cmd).apply(config, scenario.getNetwork());

		Controler controller = new Controler(scenario);
		configurator.configureController(controller);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new IDFModeChoiceModule(cmd));
		controller.addOverridingModule(new UrbanAnalysisModule());
		controller.addOverridingModule(new StuckAnalysisModule());

		if (cmd.hasOption("line-switch-utility")) {
			double lineSwitchUtility = Double.parseDouble(cmd.getOptionStrict("line-switch-utility"));
			config.planCalcScore().setUtilityOfLineSwitch(lineSwitchUtility);
		}

		if (cmd.hasOption("count-links")) {
			controller.addOverridingModule(new CountsModule(cmd));
		}

		if (cmd.getOption("use-epsilon").map(Boolean::parseBoolean).orElse(true)) {
			controller.addOverridingModule(new EpsilonModule());

			DiscreteModeChoiceConfigGroup dmcConfig = DiscreteModeChoiceConfigGroup.getOrCreate(config);
			dmcConfig.setSelector(SelectorModule.MAXIMUM);

			EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
			eqasimConfig.setEstimator("car", "epsilon_car");
			eqasimConfig.setEstimator("pt", "epsilon_pt");
			eqasimConfig.setEstimator("bike", "epsilon_bike");
			eqasimConfig.setEstimator("walk", "epsilon_walk");
		}

		if (useVdf) {
			controller.addOverridingModule(new VDFModule());
			controller.addOverridingQSimModule(new VDFQSimModule());
		}

		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().to(ModeShareListener.class);
				addControlerListenerBinding().to(ModeShareListener.class);

				bind(ConvergenceTerminationCriterion.class).asEagerSingleton();
				bind(TerminationCriterion.class).to(ConvergenceTerminationCriterion.class);
			}

			@Provides
			@Singleton
			public ModeShareListener provideModeShareListener(OutputDirectoryHierarchy outputHierarchy,
					ConvergenceTerminationCriterion terminationCriterion) {
				Optional<File> signalInputPath = cmd.getOption("signal-input-path").map(p -> new File(p));
				return new ModeShareListener(outputHierarchy, terminationCriterion, signalInputPath);
			}
		});

		controller.run();
	}
}