package org.eqasim.ile_de_france.routing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.RaptorStaticConfig;
import ch.sbb.matsim.routing.pt.raptor.RaptorUtils;

public class IDFRaptorUtils {
	static public RaptorStaticConfig createRaptorStaticConfig(Config config) {
		double maximumTransferDistance = 400.0;
		double walkSpeed = 1.33;
		double walkFactor = 1.3;

		SwissRailRaptorConfigGroup srrConfig = ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class);
		srrConfig.setUseModeMappingForPassengers(true);

		// TODO: Consider minimal transfer time
		// TODO: Consider transfer walk margin

		RaptorStaticConfig staticConfig = RaptorUtils.createStaticConfig(config);
		staticConfig.setBeelineWalkConnectionDistance(maximumTransferDistance);

		staticConfig.setBeelineWalkSpeed(walkSpeed / walkFactor);
		staticConfig.setBeelineWalkDistanceFactor(walkFactor);

		return staticConfig;
	}

	static public RaptorParameters createRaptorParameters(Config config, TransitSchedule schedule) {
		SwissRailRaptorConfigGroup advancedConfig = ConfigUtils.addOrGetModule(config,
				SwissRailRaptorConfigGroup.class);

		double utilityOfLineSwitch = -0.26980996526677087;
		double utilityOfWaiting = -1.298754292554342; // per h
		double directWalkFactor = 100.0;
		double walkSpeed = 1.33;
		double walkFactor = 1.3;

		Map<String, Double> modeUtilities = new HashMap<>();
		modeUtilities.put("rail", -0.4543829479956706);
		modeUtilities.put("subway", -0.7715570079250351);
		modeUtilities.put("tram", -1.7608452482684784);
		modeUtilities.put("bus", -1.7447089000006268);
		modeUtilities.put("walk", -1.6352586824349615);
		modeUtilities.put("other", -1.0);

		RaptorParameters raptorParams = new RaptorParameters(advancedConfig);

		// Waiting
		raptorParams.setMarginalUtilityOfWaitingPt_utl_s(utilityOfWaiting / 3600.0);

		// Transfer
		raptorParams.setTransferPenaltyFixCostPerTransfer(-utilityOfLineSwitch);
		raptorParams.setTransferPenaltyPerTravelTimeHour(0.0);
		raptorParams.setTransferPenaltyMinimum(-utilityOfLineSwitch);
		raptorParams.setTransferPenaltyMaximum(-utilityOfLineSwitch);

		// Direct walk factor
		raptorParams.setDirectWalkFactor(directWalkFactor);
		raptorParams.setBeelineWalkSpeed(walkSpeed / walkFactor);

		// Modal configuration
		Set<String> modes = new HashSet<>();

		for (TransitLine transitLine : schedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				modes.add(transitRoute.getTransportMode());
			}
		}

		for (String mode : modes) {
			double utility = modeUtilities.getOrDefault(mode, modeUtilities.get("other"));
			raptorParams.setMarginalUtilityOfTravelTime_utl_s(mode, utility);
		}

		// Defaults
		raptorParams.setSearchRadius(config.transitRouter().getSearchRadius());
		raptorParams.setExtensionRadius(config.transitRouter().getExtensionRadius());

		return raptorParams;
	}
}
