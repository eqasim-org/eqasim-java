package org.eqasim.core.components;

import java.util.List;

import com.google.inject.Inject;
import org.eqasim.core.components.transit_with_abstract_access.AbstractAccessModuleConfigGroup;
import org.eqasim.core.components.transit_with_abstract_access.routing.TransitWithAbstractAccessRoutingModule;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;

public class EqasimMainModeIdentifier implements MainModeIdentifier {

	private final String ptWithAbstractAccessModeName;

	@Inject
	public EqasimMainModeIdentifier(Config config) {
		if(config != null) {
			this.ptWithAbstractAccessModeName = ((AbstractAccessModuleConfigGroup) config.getModules().get(AbstractAccessModuleConfigGroup.ABSTRACT_ACCESS_GROUP_NAME)).getModeName();
		} else {
			this.ptWithAbstractAccessModeName = null;
		}
	}

	public EqasimMainModeIdentifier(String ptWithAbstractAccessModeName) {
		this.ptWithAbstractAccessModeName = ptWithAbstractAccessModeName;
	}

	@Override
	public String identifyMainMode(List<? extends PlanElement> tripElements) {
		boolean enounteredPt = false;
		for (Leg leg : TripStructureUtils.getLegs(tripElements)) {
			if (!leg.getMode().contains("walk")) {
				if(this.ptWithAbstractAccessModeName != null && leg.getMode().equals(TransportMode.pt)) {
					enounteredPt=true;
					continue;
				}
				if(leg.getMode().equals(TransitWithAbstractAccessRoutingModule.ABSTRACT_ACCESS_LEG_MODE_NAME)) {
					return this.ptWithAbstractAccessModeName;
				}
				return leg.getMode();
			}
		}
		if(enounteredPt) {
			return TransportMode.pt;
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
