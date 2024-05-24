package org.eqasim.core.components.raptor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.RaptorUtils;

public class EqasimRaptorUtils {
	private final static Logger logger = LogManager.getLogger(EqasimRaptorUtils.class);

	static public RaptorParameters createParameters(Config config, EqasimRaptorConfigGroup raptorConfig,
			TransitSchedule schedule) {
		RaptorParameters parameters = RaptorUtils.createParameters(config);

		// use transportMode from TransitRoute in schedule
		parameters.setUseTransportModeUtilities(true);

		parameters.setMarginalUtilityOfTravelTime_utl_s("rail", raptorConfig.travelTimeRail_u_h / 3600.0);
		parameters.setMarginalUtilityOfTravelTime_utl_s("subway", raptorConfig.travelTimeSubway_u_h / 3600.0);
		parameters.setMarginalUtilityOfTravelTime_utl_s("bus", raptorConfig.travelTimeBus_u_h / 3600.0);
		parameters.setMarginalUtilityOfTravelTime_utl_s("tram", raptorConfig.travelTimeTram_u_h / 3600.0);

		parameters.setTransferPenaltyFixCostPerTransfer(-raptorConfig.perTransfer_u);

		parameters.setMarginalUtilityOfWaitingPt_utl_s(raptorConfig.waitTime_u_h / 3600.0);
		parameters.setMarginalUtilityOfTravelTime_utl_s("walk", raptorConfig.walkTime_u_h / 3600.0);

		Set<String> requiredModes = new HashSet<>();

		for (TransitLine transitLine : schedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				requiredModes.add(transitRoute.getTransportMode());
			}
		}

		requiredModes.removeAll(Arrays.asList("rail", "subway", "bus", "tram", "walk"));

		if (requiredModes.size() > 0) {
			logger.warn("Setting 'other' routing cost for following modes: " + String.join(", ", requiredModes));

			for (String mode : requiredModes) {
				parameters.setMarginalUtilityOfTravelTime_utl_s(mode, raptorConfig.travelTimeOther_u_h / 3600.0);
			}
		}

		return parameters;
	}
}
