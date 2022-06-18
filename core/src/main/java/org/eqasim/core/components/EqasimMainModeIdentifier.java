package org.eqasim.core.components;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;

import java.util.List;

import static org.matsim.core.router.TripStructureUtils.StageActivityHandling.ExcludeStageActivities;
import static org.matsim.core.router.TripStructureUtils.StageActivityHandling.StagesAsNormalActivities;

public class EqasimMainModeIdentifier implements MainModeIdentifier , AnalysisMainModeIdentifier {
	@Override
	public String identifyMainMode(List<? extends PlanElement> tripElements) {


		if (TripStructureUtils.getActivities(tripElements, StagesAsNormalActivities) .size()!=0) {
			for (Activity act : TripStructureUtils.getActivities(tripElements,StagesAsNormalActivities)) {
				if (act.getType().equals("PTSharing interaction") || act.getType().equals("SharingPT interaction")) {
					return "SharingPT";
				}
			}

		}

		for (Leg leg : TripStructureUtils.getLegs(tripElements)) {
				if(leg.getMode().contains("eScooter")){
					return"eScooter";
				}
			if(leg.getMode().contains("Shared-Bike")){
				return"SharedBike";
			}
				if (!leg.getMode().contains("walk")) {
					return leg.getMode();
				}
			}

			String singleLegMode = TripStructureUtils.getLegs(tripElements).get(0).getMode();

			switch (singleLegMode) {
				case TransportMode.transit_walk:
				case TransportMode.non_network_walk:
					return TransportMode.pt;
				default:
					return TransportMode.walk;
			}
		}

}
