package org.eqasim.core.simulation.modes.drt.analysis.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEventHandler;

public class VehicleRegistry implements TaskStartedEventHandler {
	private final IdMap<DvrpVehicle, String> vehicleModes = new IdMap<>(DvrpVehicle.class);

	@Override
	public void handleEvent(TaskStartedEvent event) {
		vehicleModes.computeIfAbsent(event.getDvrpVehicleId(), id -> event.getDvrpMode());
	}

	public boolean isFleet(Id<?> id) {
		return vehicleModes.containsKey(Id.create(id, DvrpVehicle.class));
	}

	public String getMode(Id<?> id) {
		return vehicleModes.get(Id.create(id, DvrpVehicle.class));
	}
}
