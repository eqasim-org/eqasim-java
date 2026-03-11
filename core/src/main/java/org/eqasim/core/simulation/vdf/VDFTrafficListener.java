package org.eqasim.core.simulation.vdf;

import java.util.Set;

import org.eqasim.core.simulation.vdf.handlers.VDFTrafficHandler;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.vehicles.Vehicle;

public class VDFTrafficListener
        implements VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, LinkEnterEventHandler {
    private final VDFTrafficHandler handler;
    private final FlowEquivalentProvider flowEquivalentProvider;

    private final Set<String> trackedModes;
    private final IdSet<Vehicle> activeVehicles = new IdSet<>(Vehicle.class);

    public VDFTrafficListener(VDFTrafficHandler handler, FlowEquivalentProvider flowEquivalentProvider,
            Set<String> trackedModes) {
        this.handler = handler;
        this.trackedModes = trackedModes;
        this.flowEquivalentProvider = flowEquivalentProvider;
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        if (trackedModes.contains(event.getNetworkMode())) {
            activeVehicles.add(event.getVehicleId());
        }
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        activeVehicles.remove(event.getVehicleId());
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        if (activeVehicles.contains(event.getVehicleId())) {
            handler.processEnterLink(event.getTime(), event.getLinkId(),
                    flowEquivalentProvider.getFlowEquivalent(event.getVehicleId()));
        }
    }
}
