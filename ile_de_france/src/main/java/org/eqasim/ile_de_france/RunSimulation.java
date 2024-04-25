package org.eqasim.ile_de_france;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.raptor.EqasimRaptorConfigGroup;
import org.eqasim.core.components.traffic.EqasimTrafficQSimModule;
import org.eqasim.core.components.transit.EqasimTransitQSimModule;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.ile_de_france.analysis.counts.CountsModule;
import org.eqasim.ile_de_france.analysis.delay.DelayAnalysisModule;
import org.eqasim.ile_de_france.analysis.urban.UrbanAnalysisModule;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.eqasim.ile_de_france.parking.ParkingModule;
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
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.config.TransitRouterConfigGroup;

public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowOptions("counts-path", "use-epsilon", "use-vdf", "use-vdf-engine", "vdf-generate-network-events",
						"cost-model") //
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
		configurator.addOptionalConfigGroups(config);

		{
			/// ADJUSTMENTS AFTER INLINING MODAL ROUTING

			TransitRouterConfigGroup trrConfig = (TransitRouterConfigGroup) config.getModules()
					.get(TransitRouterConfigGroup.GROUP_NAME);
			trrConfig.setAdditionalTransferTime(60.0);
			trrConfig.setMaxBeelineWalkConnectionDistance(400.0);
			trrConfig.setDirectWalkFactor(100.0);

			EqasimRaptorConfigGroup raptorConfig = (EqasimRaptorConfigGroup) config.getModules()
					.get(EqasimRaptorConfigGroup.GROUP_NAME);
			raptorConfig.perTransfer_u = -0.26980996526677087;
			raptorConfig.waitTime_u_h = -1.298754292554342;
			raptorConfig.walkTime_u_h = -1.6352586824349615;
			raptorConfig.travelTimeOther_u_h = -1.0;
			raptorConfig.travelTimeRail_u_h = -0.4543829479956706;
			raptorConfig.travelTimeSubway_u_h = -0.7715570079250351;
			raptorConfig.travelTimeTram_u_h = -1.7608452482684784;
			raptorConfig.travelTimeBus_u_h = -1.7447089000006268;

			// changed from previous version
			// letting walkSpeed_m_s at 1.2 (in config), previously we used 1.33 for pt
			// routing
		}

		cmd.applyConfiguration(config);

		{
			// Avoid logging errors when using TripsAndLegsCSV
			config.controller().setWriteTripsInterval(0);
		}

		Scenario scenario = ScenarioUtils.createScenario(config);
		configurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		configurator.adjustScenario(scenario);

		new OsmNetworkAdjustment(cmd).apply(config, scenario.getNetwork());

		RunAdaptConfig.adaptEstimators(config);
		RunAdaptConfig.adaptConstraints(config);

		Controler controller = new Controler(scenario);
		configurator.configureController(controller);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new IDFModeChoiceModule(cmd));
		controller.addOverridingModule(new UrbanAnalysisModule());
		controller.addOverridingModule(new DelayAnalysisModule());

		if (cmd.hasOption("counts-path")) {
			controller.addOverridingModule(new CountsModule(cmd));
		}

		if (cmd.getOption("use-epsilon").map(Boolean::parseBoolean).orElse(true)) {
			DiscreteModeChoiceConfigGroup dmcConfig = DiscreteModeChoiceConfigGroup.getOrCreate(config);
			dmcConfig.setSelector(SelectorModule.MAXIMUM);

			EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);

			eqasimConfig.setEstimator("car", "epsilon_" + IDFModeChoiceModule.CAR_ESTIMATOR_NAME);
			eqasimConfig.setEstimator("pt", "epsilon_" + IDFModeChoiceModule.PT_ESTIMATOR_NAME);
			eqasimConfig.setEstimator("bike", "epsilon_" + IDFModeChoiceModule.BIKE_ESTIMATOR_NAME);
			eqasimConfig.setEstimator("walk", "epsilon_" + EqasimModeChoiceModule.WALK_ESTIMATOR_NAME);
			eqasimConfig.setEstimator("car_passenger", "epsilon_" + IDFModeChoiceModule.PASSENGER_ESTIMATOR_NAME);
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

		controller.addOverridingModule(new ParkingModule(3.0));

		controller.run();
	}
}