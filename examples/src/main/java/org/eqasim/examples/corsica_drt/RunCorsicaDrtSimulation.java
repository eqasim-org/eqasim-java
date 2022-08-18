package org.eqasim.examples.corsica_drt;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.transit.EqasimTransitQSimModule;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.examples.corsica_drt.analysis.DvrpAnalsisModule;
import org.eqasim.examples.corsica_drt.mode_choice.CorsicaDrtModeAvailability;
import org.eqasim.examples.corsica_drt.rejections.RejectionConstraint;
import org.eqasim.examples.corsica_drt.rejections.RejectionModule;
import org.eqasim.ile_de_france.IDFConfigurator;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.insertion.selective.SelectiveInsertionSearchParams;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigGroup.OperationalScheme;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.common.io.Resources;

/**
 * This is an example run script that runs the Corsica test scenario with an
 * on-demand vehicle fleet using DRT.
 * 
 * The scenario files for the Corisca scenario are located in the resources of
 * the ile_de_france module and the additional fleet definition file is located
 * in the resources of the examples module.
 */
public class RunCorsicaDrtSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.allowOptions("use-rejection-constraint") //
				.allowPrefixes("mode-parameter", "cost-parameter") //
				.build();
		URL configUrl = Resources.getResource("corsica/corsica_config.xml");

		IDFConfigurator configurator = new IDFConfigurator();
		Config config = ConfigUtils.loadConfig(configUrl, configurator.getConfigGroups());

		config.controler().setLastIteration(2);
		config.qsim().setFlowCapFactor(1e9);
		config.qsim().setStorageCapFactor(1e9);

		{ // Configure DVRP
			DvrpConfigGroup dvrpConfig = new DvrpConfigGroup();
			config.addModule(dvrpConfig);
		}

		MultiModeDrtConfigGroup multiModeDrtConfig = new MultiModeDrtConfigGroup();

		{ // Configure DRT
			config.addModule(multiModeDrtConfig);

			DrtConfigGroup drtConfig = new DrtConfigGroup();
			drtConfig.setMode("drt");
			drtConfig.setOperationalScheme(OperationalScheme.door2door);
			drtConfig.setStopDuration(15.0);
			drtConfig.setMaxWaitTime(600.0);
			drtConfig.setMaxTravelTimeAlpha(1.5);
			drtConfig.setMaxTravelTimeBeta(300.0);
			drtConfig.setVehiclesFile(Resources.getResource("corsica_drt/drt_vehicles.xml").toString());

			DrtInsertionSearchParams searchParams = new SelectiveInsertionSearchParams();
			drtConfig.addDrtInsertionSearchParams(searchParams);

			multiModeDrtConfig.addDrtConfig(drtConfig);
			DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfig, config.planCalcScore(), config.plansCalcRoute());

			// Additional requirements
			config.qsim().setStartTime(0.0);
			config.qsim().setSimStarttimeInterpretation(StarttimeInterpretation.onlyUseStarttime);
		}

		cmd.applyConfiguration(config);

		{ // Add the DRT mode to the choice model
			DiscreteModeChoiceConfigGroup dmcConfig = DiscreteModeChoiceConfigGroup.getOrCreate(config);

			// Add DRT to the available modes
			dmcConfig.setModeAvailability(CorsicaDrtModeAvailability.NAME);

			// Add DRT to cached modes
			Set<String> cachedModes = new HashSet<>();
			cachedModes.addAll(dmcConfig.getCachedModes());
			cachedModes.add("drt");
			dmcConfig.setCachedModes(cachedModes);

			// Set up choice model
			EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
			eqasimConfig.setCostModel("drt", "drt");
			eqasimConfig.setEstimator("drt", "drt");

			// Add rejection constraint
			if (cmd.getOption("use-rejection-constraint").map(Boolean::parseBoolean).orElse(false)) {
				Set<String> tripConstraints = new HashSet<>(dmcConfig.getTripConstraints());
				tripConstraints.add(RejectionConstraint.NAME);
				dmcConfig.setTripConstraints(tripConstraints);
			}

			// Set analysis interval
			eqasimConfig.setAnalysisInterval(1);
		}

		{ // Set up some defaults for MATSim scoring
			ModeParams modeParams = new ModeParams("drt");
			config.planCalcScore().addModeParams(modeParams);
		}

		Scenario scenario = ScenarioUtils.createScenario(config);
		configurator.configureScenario(scenario);

		{ // Add DRT route factory
			scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DrtRoute.class,
					new DrtRouteFactory());
		}

		ScenarioUtils.loadScenario(scenario);

		Controler controller = new Controler(scenario);
		configurator.configureController(controller);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new IDFModeChoiceModule(cmd));

		{ // Configure controller for DRT
			controller.addOverridingModule(new DvrpModule());
			controller.addOverridingModule(new MultiModeDrtModule());

			controller.configureQSimComponents(components -> {
				DvrpQSimComponents.activateAllModes(multiModeDrtConfig).configure(components);

				// Need to re-do this as now it is combined with DRT
				EqasimTransitQSimModule.configure(components, config);
			});
		}

		{ // Add overrides for Corsica + DRT
			controller.addOverridingModule(new CorsicaDrtModule(cmd));
			controller.addOverridingModule(new RejectionModule(Arrays.asList("drt")));
			controller.addOverridingModule(new DvrpAnalsisModule());
		}

		controller.run();
	}
}
