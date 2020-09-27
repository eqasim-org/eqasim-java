package org.eqasim.core.components;

import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;

public class EqasimMainModeIdentifier implements MainModeIdentifier {
	@Override
	public String identifyMainMode(List<? extends PlanElement> tripElements) {
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
