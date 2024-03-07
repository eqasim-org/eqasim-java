package org.eqasim.ile_de_france;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.traffic.EqasimTrafficQSimModule;
import org.eqasim.core.components.transit.EqasimTransitQSimModule;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.convergence.ConvergenceTerminationCriterion;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.ile_de_france.analysis.counts.CountsModule;
import org.eqasim.ile_de_france.analysis.delay.DelayAnalysisModule;
import org.eqasim.ile_de_france.analysis.stuck.StuckAnalysisModule;
import org.eqasim.ile_de_france.analysis.urban.UrbanAnalysisModule;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.eqasim.ile_de_france.parking.ParkingModule;
import org.eqasim.ile_de_france.routing.IDFRaptorModule;
import org.eqasim.ile_de_france.routing.IDFRaptorUtils;
import org.eqasim.ile_de_france.scenario.RunAdaptConfig;
import org.eqasim.vdf.VDFConfigGroup;
import org.eqasim.vdf.VDFModule;
import org.eqasim.vdf.VDFQSimModule;
import org.eqasim.vdf.engine.VDFEngineConfigGroup;
import org.eqasim.vdf.engine.VDFEngineModule;
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
				.allowOptions("counts-path", "external-convergence", "signal-input-path", "use-epsilon", "use-vdf",
						"use-counts-calibration", "use-vdf-engine", "vdf-generate-network-events",
						"line-switch-utility", "cost-model") //
				.allowPrefixes("mode-choice-parameter", "cost-parameter", OsmNetworkAdjustment.CAPACITY_PREFIX,
						OsmNetworkAdjustment.SPEED_PREFIX, "raptor") //
				.build();

		boolean useVdf = cmd.getOption("use-vdf").map(Boolean::parseBoolean).orElse(false);
		IDFConfigurator configurator = new IDFConfigurator();

		if (useVdf) {
			configurator.getQSimModules().removeIf(m -> m instanceof EqasimTrafficQSimModule);
		}

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), configurator.getConfigGroups());
		config.addModule(new VDFConfigGroup());
		cmd.applyConfiguration(config);

		{
			// Avoid logging errors when using TripsAndLegsCSV
			config.controler().setWriteTripsInterval(0);
		}

		Scenario scenario = ScenarioUtils.createScenario(config);
		configurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		configurator.adjustScenario(scenario);

		{
			config.planCalcScore().setMarginalUtlOfWaiting_utils_hr(-1.0);
			IDFRaptorUtils.updateScoring(config);
		}

		new OsmNetworkAdjustment(cmd).apply(config, scenario.getNetwork());

		RunAdaptConfig.adaptEstimators(config);
		RunAdaptConfig.adaptConstraints(config);

		Controler controller = new Controler(scenario);
		configurator.configureController(controller);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new IDFModeChoiceModule(cmd));
		controller.addOverridingModule(new UrbanAnalysisModule());
		controller.addOverridingModule(new StuckAnalysisModule());
		controller.addOverridingModule(new DelayAnalysisModule());

		if (cmd.hasOption("line-switch-utility")) {
			double lineSwitchUtility = Double.parseDouble(cmd.getOptionStrict("line-switch-utility"));
			config.planCalcScore().setUtilityOfLineSwitch(lineSwitchUtility);
		}

		if (cmd.hasOption("counts-path")) {
			boolean useCountsCalibration = cmd.getOption("use-counts-calibration").map(Boolean::parseBoolean)
					.orElse(false);
			controller.addOverridingModule(new CountsModule(cmd, useCountsCalibration));
		}

		if (cmd.getOption("use-epsilon").map(Boolean::parseBoolean).orElse(true)) {
			DiscreteModeChoiceConfigGroup dmcConfig = DiscreteModeChoiceConfigGroup.getOrCreate(config);
			dmcConfig.setSelector(SelectorModule.MAXIMUM);

			EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
			eqasimConfig.setEstimator("car", "epsilon_car");
			eqasimConfig.setEstimator("pt", "epsilon_pt");
			eqasimConfig.setEstimator("bike", "epsilon_bike");
			eqasimConfig.setEstimator("walk", "epsilon_walk");
			eqasimConfig.setEstimator("car_passenger", "epsilon_car_passenger");
		}

		if (useVdf) {
			controller.addOverridingModule(new VDFModule());
			controller.addOverridingQSimModule(new VDFQSimModule());

			config.qsim().setFlowCapFactor(1e9);
			config.qsim().setStorageCapFactor(1e9);
		}

		boolean useVdfEngine = cmd.getOption("use-vdf-engine").map(Boolean::parseBoolean).orElse(false);
		if (useVdfEngine) {
			Set<String> mainModes = new HashSet<>(config.qsim().getMainModes());
			mainModes.remove("car");
			config.qsim().setMainModes(mainModes);
			
			VDFEngineConfigGroup vdfEngineConfig = new VDFEngineConfigGroup();
			config.addModule(vdfEngineConfig);

			vdfEngineConfig.setGenerateNetworkEvents(
					cmd.getOption("vdf-generate-network-events").map(Boolean::parseBoolean).orElse(true));
			vdfEngineConfig.setModes(Collections.singleton("car"));

			controller.addOverridingModule(new VDFEngineModule());

			controller.configureQSimComponents(cfg -> {
				EqasimTransitQSimModule.configure(cfg, controller.getConfig());
				cfg.addNamedComponent(VDFEngineModule.COMPONENT_NAME);
			});
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

		controller.addOverridingModule(new ParkingModule(3.0));
		controller.addOverridingModule(new IDFRaptorModule(cmd));

		controller.run();
	}
}