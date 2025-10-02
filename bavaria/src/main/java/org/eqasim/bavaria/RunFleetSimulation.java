package org.eqasim.bavaria;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsSetImpl;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.insertion.extensive.ExtensiveInsertionSearchParams;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrixParams;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.EndtimeInterpretation;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

public class RunFleetSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("population-path", "network-path", "fleet-path", "output-path")
				.build();

		double simulationStartTime = 0.0;
		double simulationEndTime = 24.0 * 3600.0;

		Config config = ConfigUtils.createConfig();

		config.plans().setInputFile(cmd.getOptionStrict("population-path"));
		config.network().setInputFile(cmd.getOptionStrict("network-path"));

		config.controller().setOutputDirectory(cmd.getOptionStrict("output-path"));
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		ActivityParams genericActivity = new ActivityParams("generic");
		genericActivity.setScoringThisActivityAtAll(false);
		config.scoring().addActivityParams(genericActivity);

		ModeParams modeParams = new ModeParams("drt");
		config.scoring().addModeParams(modeParams);

		config.controller().setLastIteration(0);

		config.qsim().setStartTime(simulationStartTime);
		config.qsim().setSimStarttimeInterpretation(StarttimeInterpretation.onlyUseStarttime);

		config.qsim().setEndTime(simulationEndTime);
		config.qsim().setSimEndtimeInterpretation(EndtimeInterpretation.onlyUseEndtime);

		config.qsim().setFlowCapFactor(1e9);
		config.qsim().setStorageCapFactor(1e9);

		DvrpConfigGroup dvrpConfig = new DvrpConfigGroup();
		config.addModule(dvrpConfig);

		SquareGridZoneSystemParams zoneSystemParams = new SquareGridZoneSystemParams();
		zoneSystemParams.setCellSize(100.0);

		DvrpTravelTimeMatrixParams matrixParams = DvrpConfigGroup.get(config).getTravelTimeMatrixParams();
		matrixParams.addParameterSet(zoneSystemParams);

		MultiModeDrtConfigGroup multiModeDrtConfig = new MultiModeDrtConfigGroup();
		config.addModule(multiModeDrtConfig);

		DrtConfigGroup drtConfig = new DrtConfigGroup();
		multiModeDrtConfig.addParameterSet(drtConfig);

		drtConfig.setMode("drt");
		drtConfig.setStopDuration(30.0);
		drtConfig.setVehiclesFile(cmd.getOptionStrict("fleet-path"));

		DrtInsertionSearchParams insertionParams = new ExtensiveInsertionSearchParams();
		drtConfig.addParameterSet(insertionParams);

		DrtOptimizationConstraintsSetImpl constraints = drtConfig.addOrGetDrtOptimizationConstraintsParams()
				.addOrGetDefaultDrtOptimizationConstraintsSet();
		constraints.setMaxWaitTime(300.0);
		constraints.setMaxDetourBeta(1.0);
		constraints.setMaxDetourAlpha(600.0);

		Scenario scenario = ScenarioUtils.createScenario(config);
		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DrtRoute.class,
				new DrtRouteFactory());
		ScenarioUtils.loadScenario(scenario);

		Controler controller = new Controler(scenario);
		controller.configureQSimComponents(DvrpQSimComponents.activateModes("drt"));
		controller.addOverridingModule(new DvrpModule());
		controller.addOverridingModule(new MultiModeDrtModule());
		controller.run();
	}
}