package org.eqasim.core.simulation.routing;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.RaptorStaticConfig;
import ch.sbb.matsim.routing.pt.raptor.RaptorUtils;

public class EqasimRaptorUtils {
	static public final String PT_MODE_PREFIX = "pt:";
	static public final String OTHER_MODE = "other";
	static public final Set<String> CONFIGURED_MODES = new HashSet<>(Arrays.asList("rail", "subway", "tram", "bus"));

	static public RaptorStaticConfig createRaptorStaticConfig(Config config, TransitSchedule schedule,
			EqasimRaptorParameters parameters) {
		RaptorStaticConfig staticConfig = RaptorUtils.createStaticConfig(config);
		staticConfig.setBeelineWalkConnectionDistance(parameters.maximumTransferDistance_m);

		staticConfig.setBeelineWalkSpeed(parameters.walkSpeed_m_s / parameters.walkFactor);
		staticConfig.setBeelineWalkDistanceFactor(parameters.walkFactor);

		staticConfig.setMinimalTransferTime(parameters.minimalTransferTime_s);
		staticConfig.setTransferWalkMargin(parameters.transferWalkMargin_s);

		// Add mode mappings
		staticConfig.setUseModeMappingForPassengers(true);

		Set<String> scheduleModes = new HashSet<>();
		for (TransitLine transitLine : schedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				scheduleModes.add(transitRoute.getTransportMode());
			}
		}

		for (String mode : scheduleModes) {
			String mappedMode = CONFIGURED_MODES.contains(mode) ? mode : OTHER_MODE;
			staticConfig.addModeMappingForPassengers(mode, PT_MODE_PREFIX + mappedMode);
		}

		return staticConfig;
	}

	static public RaptorParameters createRaptorParameters(Config config, TransitSchedule schedule,
			EqasimRaptorParameters parameters) {
		SwissRailRaptorConfigGroup advancedConfig = ConfigUtils.addOrGetModule(config,
				SwissRailRaptorConfigGroup.class);

		Map<String, Double> modeUtilities = new HashMap<>();
		modeUtilities.put(PT_MODE_PREFIX + "rail", parameters.railUtility_h);
		modeUtilities.put(PT_MODE_PREFIX + "subway", parameters.subwayUtility_h);
		modeUtilities.put(PT_MODE_PREFIX + "tram", parameters.tramUtility_h);
		modeUtilities.put(PT_MODE_PREFIX + "bus", parameters.busUtility_h);
		modeUtilities.put("walk", parameters.walkUtility_h);
		modeUtilities.put(PT_MODE_PREFIX + "other", parameters.otherUtility_h);

		RaptorParameters raptorParams = new RaptorParameters(advancedConfig);

		// Waiting
		raptorParams.setMarginalUtilityOfWaitingPt_utl_s(parameters.waitingUtility_h / 3600.0);

		// Transfer
		raptorParams.setTransferPenaltyFixCostPerTransfer(-parameters.transferUtility);
		raptorParams.setTransferPenaltyPerTravelTimeHour(0.0);
		raptorParams.setTransferPenaltyMinimum(-parameters.transferUtility);
		raptorParams.setTransferPenaltyMaximum(-parameters.transferUtility);

		// Direct walk factor
		raptorParams.setDirectWalkFactor(parameters.directWalkFactor);
		raptorParams.setBeelineWalkSpeed(parameters.walkSpeed_m_s / parameters.walkFactor);

		// Modal configuration
		Set<String> modes = new HashSet<>();
		modes.add("walk");
		modes.add(PT_MODE_PREFIX + OTHER_MODE);

		for (TransitLine transitLine : schedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				if (CONFIGURED_MODES.contains(transitRoute.getTransportMode())) {
					modes.add(PT_MODE_PREFIX + transitRoute.getTransportMode());
				}
			}
		}

		for (String mode : modes) {
			double utility = modeUtilities.getOrDefault(mode, modeUtilities.get(PT_MODE_PREFIX + OTHER_MODE));
			raptorParams.setMarginalUtilityOfTravelTime_utl_s(mode, utility / 3600.0);
		}

		// Defaults
		raptorParams.setSearchRadius(config.transitRouter().getSearchRadius());
		raptorParams.setExtensionRadius(config.transitRouter().getExtensionRadius());

		return raptorParams;
	}

	static public void updateScoring(Config config) {
		Set<String> modes = new HashSet<>();
		modes.add(PT_MODE_PREFIX + OTHER_MODE);

		for (String mode : CONFIGURED_MODES) {
			modes.add(PT_MODE_PREFIX + mode);
		}

		for (String mode : modes) {
			if (!config.planCalcScore().getAllModes().contains(mode)) {
				ModeParams modeParams = new ModeParams(mode);
				config.planCalcScore().addModeParams(modeParams);
			}
		}
	}
}
