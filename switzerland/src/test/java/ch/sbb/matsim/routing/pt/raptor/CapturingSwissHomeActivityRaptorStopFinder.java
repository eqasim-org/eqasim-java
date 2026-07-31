package ch.sbb.matsim.routing.pt.raptor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eqasim.switzerland.ch_cmdp.config.SwissIntermodalAccessEgressConfigGroup;
import org.eqasim.switzerland.ch_cmdp.routing.IntermodalVehicleRoutingAttributes;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.Attributes;

public class CapturingSwissHomeActivityRaptorStopFinder extends SwissHomeActivityRaptorStopFinder {
	private final Map<String, Object> requiredEgressModes = new LinkedHashMap<>();
	private final List<Object> forbiddenAccessModes = new ArrayList<>();

	public CapturingSwissHomeActivityRaptorStopFinder(DefaultRaptorStopFinder delegate,
			SwissIntermodalAccessEgressConfigGroup config) {
		super(delegate, config);
	}

	@Override
	public List<InitialStop> findStops(Facility fromFacility, Facility toFacility, Person person, double departureTime,
			Attributes routingAttributes, RaptorParameters parameters, SwissRailRaptorData data, Direction type) {
		if (routingAttributes != null) {
			Object forbiddenAccessMode = routingAttributes
					.getAttribute(IntermodalVehicleRoutingAttributes.FORBIDDEN_ACCESS_MODE);
			if (type == Direction.ACCESS && forbiddenAccessMode != null) {
				forbiddenAccessModes.add(forbiddenAccessMode);
			}

			Object requiredEgressMode = routingAttributes
					.getAttribute(IntermodalVehicleRoutingAttributes.REQUIRED_EGRESS_MODE);
			Object requiredEgressStopId = routingAttributes
					.getAttribute(IntermodalVehicleRoutingAttributes.REQUIRED_EGRESS_STOP_ID);
			if (type == Direction.EGRESS && requiredEgressMode != null && requiredEgressStopId != null) {
				requiredEgressModes.put(requiredEgressStopId.toString(), requiredEgressMode);
			}
		}

		return super.findStops(fromFacility, toFacility, person, departureTime, routingAttributes, parameters, data, type);
	}

	public Map<String, Object> getRequiredEgressModes() {
		return requiredEgressModes;
	}

	public List<Object> getForbiddenAccessModes() {
		return forbiddenAccessModes;
	}
}
