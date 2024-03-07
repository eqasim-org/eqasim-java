package org.eqasim.core.components.headway;

import java.util.List;

import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;
import org.matsim.utils.objectattributes.attributable.AttributesUtils;

import ch.sbb.matsim.routing.pt.raptor.RaptorRoute;
import ch.sbb.matsim.routing.pt.raptor.RaptorRoute.RoutePart;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;

public class HeadwayCalculator {
	private final SwissRailRaptor raptor;

	private final double beforeDepartureOffset;
	private final double afterDepartureOffset;

	public HeadwayCalculator(SwissRailRaptor raptor, double beforeDepartureOffset, double afterDepartureOffset) {
		this.raptor = raptor;
		this.beforeDepartureOffset = beforeDepartureOffset;
		this.afterDepartureOffset = afterDepartureOffset;
	}

	public double calculateHeadway_min(Facility originFacility, Facility destinationFacilty, double departureTime) {
		double earliestDepartureTime = departureTime - beforeDepartureOffset;
		double latestDepartureTime = departureTime + afterDepartureOffset;

		List<RaptorRoute> routes = raptor.calcRoutes(originFacility, destinationFacilty, earliestDepartureTime,
				departureTime, latestDepartureTime, null, new AttributesImpl());

		int numberOfPtRoutes = 0;

		for (RaptorRoute route : routes) {
			for (RoutePart part : route.getParts()) {
				if (part.line != null) {
					numberOfPtRoutes++;
					break;
				}
			}
		}
		
		if (numberOfPtRoutes == 0) {
			return Double.POSITIVE_INFINITY;
		} else {
			return  ((beforeDepartureOffset + afterDepartureOffset) / numberOfPtRoutes) / 60.0;
		}		
	}
}
