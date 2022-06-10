package org.eqasim.core.components;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;

import java.util.List;

import static org.matsim.core.router.TripStructureUtils.StageActivityHandling.ExcludeStageActivities;

public class EqasimMainModeIdentifier implements MainModeIdentifier {
	@Override
	public String identifyMainMode(List<? extends PlanElement> tripElements) {



		if (TripStructureUtils.getActivities(tripElements, ExcludeStageActivities) .size()!=0) {
			for (Activity act : TripStructureUtils.getActivities(tripElements, ExcludeStageActivities)) {
				if (act.getType().equals("PTSharing_Interaction") || act.getType().equals("SharingPT_Interaction")) {
					return "SharingPT";
				}
			}

		}
		if (tripElements.size()>7) {
			String meh="xd";
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
