package org.eqasim.scenario.preparation;

import java.util.Arrays;
import java.util.HashSet;

import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;

public class CreateDefaultConfig {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("output-path", "prefix") //
				.build();

		Config config = ConfigUtils.createConfig();
		String prefix = cmd.getOptionStrict("prefix");

		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		config.controler().setOutputDirectory("simulation_output");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		config.global().setRandomSeed(1000);
		config.global().setNumberOfThreads(24);

		config.qsim().setEndTime(30.0 * 3600.0);
		config.qsim().setNumberOfThreads(12);

		config.network().setInputFile(prefix + "network.xml.gz");
		config.plans().setInputFile(prefix + "population.xml.gz");
		config.households().setInputFile(prefix + "households.xml.gz");
		config.facilities().setInputFile(prefix + "facilities.xml.gz");
		config.transit().setTransitScheduleFile(prefix + "transit_schedule.xml.gz");
		config.transit().setVehiclesFile(prefix + "transit_vehicles.xml.gz");

		config.transit().setUseTransit(true);

		for (String activityType : Arrays.asList("home", "work", "education", "shop", "leisure", "other",
				"freight_loading", "freight_unloading")) {
			ActivityParams activityParams = new ActivityParams(activityType);
			activityParams.setScoringThisActivityAtAll(false);
			config.planCalcScore().addActivityParams(activityParams);
		}

		for (String mode : Arrays.asList("work", "bike", "pt", "car", "car_passenger", "truck")) {
			ModeParams modeParams = new ModeParams(mode);
			config.planCalcScore().addModeParams(modeParams);
		}

		config.plansCalcRoute().setNetworkModes(Arrays.asList("car", "car_passenger", "truck"));

		ModeRoutingParams bikeRoutingParams = config.plansCalcRoute().getOrCreateModeRoutingParams("bike");
		bikeRoutingParams.setTeleportedModeSpeed(3.3);
		bikeRoutingParams.setBeelineDistanceFactor(1.0);

		ModeRoutingParams walkRoutingParams = config.plansCalcRoute().getOrCreateModeRoutingParams("walk");
		walkRoutingParams.setTeleportedModeSpeed(2.2);
		walkRoutingParams.setBeelineDistanceFactor(1.0);

		config.travelTimeCalculator().setAnalyzedModes(new HashSet<>(Arrays.asList("car", "car_passenger", "truck")));

		config.strategy().clearStrategySettings();

		StrategySettings strategy = new StrategySettings();
		strategy.setStrategyName("BestScore");
		strategy.setWeight(1.0);
		config.strategy().addStrategySettings(strategy);

		cmd.applyConfiguration(config);
		new ConfigWriter(config).write(cmd.getOptionStrict("output-path"));
	}
}
