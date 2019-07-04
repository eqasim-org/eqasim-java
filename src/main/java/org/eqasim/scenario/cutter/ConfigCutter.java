package org.eqasim.scenario.cutter;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;

public class ConfigCutter {
	private final String prefix;
	
	public ConfigCutter(String prefix) {
		this.prefix = prefix;
	}
	
	public void run(Config config) {
		config.plans().setInputFile(prefix + "population.xml.gz");
		config.facilities().setInputFile(prefix + "facilities.xml.gz");
		config.network().setInputFile(prefix + "network.xml.gz");
		config.households().setInputFile(prefix + "households.xml.gz");
		config.transit().setTransitScheduleFile(prefix + "transit_schedule.xml.gz");
		config.transit().setVehiclesFile(prefix + "transit_vehicles.xml.gz");

		ModeRoutingParams outsideModeRoutingParams = config.plansCalcRoute().getOrCreateModeRoutingParams("outside");
		outsideModeRoutingParams.setBeelineDistanceFactor(1.0);
		outsideModeRoutingParams.setTeleportedModeSpeed(1e6);

		ModeParams outsideModeScoringParams = config.planCalcScore().getOrCreateModeParams("outside");
		outsideModeScoringParams.setConstant(0.0);
		outsideModeScoringParams.setMarginalUtilityOfDistance(0.0);
		outsideModeScoringParams.setMarginalUtilityOfTraveling(0.0);
		outsideModeScoringParams.setMonetaryDistanceRate(0.0);

		ActivityParams outsideActivityParams = new ActivityParams("outside");
		outsideActivityParams.setTypicalDuration(1.0);
		outsideActivityParams.setScoringThisActivityAtAll(false);
		config.planCalcScore().addActivityParams(outsideActivityParams);
	}
}
