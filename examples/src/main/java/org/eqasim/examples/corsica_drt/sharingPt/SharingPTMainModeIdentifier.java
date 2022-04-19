package org.eqasim.examples.corsica_drt.sharingPt;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;

import java.util.List;

public class SharingPTMainModeIdentifier implements MainModeIdentifier {
	@Override
	public String identifyMainMode(List<? extends PlanElement> tripElements) {

		// Iterate through activities and searchs it there is sharing-pt interaction
		// Based on Asize Diallo car -pt Implementation

		for(Activity tempAct: TripStructureUtils.getActivities(tripElements,null)){
			if(tempAct.getType().equals("SharingPT_Interacion")||tempAct.getType().equals("PTSharing_Interaction")){
				return"sharing_pt";
			}
		}

		for (Leg leg : TripStructureUtils.getLegs(tripElements)) {

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
