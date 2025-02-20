package org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment;

import com.google.common.base.Verify;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.NetworkWideParkingSpaceStore;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingSpace;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;

import java.util.List;

public class ParkingAwareNetworkRoutingModule implements RoutingModule {

    public static final String PARKING_TYPE_ATTR = "parkingType";

    private final String mode;
    private final RoutingModule delegate;
    private final NetworkWideParkingSpaceStore networkWideParkingSpaceStore;
    private final ParkingSpaceAssignmentLogic parkingSpaceAssignmentLogic;

    public ParkingAwareNetworkRoutingModule(String mode, RoutingModule delegate, NetworkWideParkingSpaceStore networkWideParkingSpaceStore, ParkingSpaceAssignmentLogic parkingSpaceAssignmentLogic) {
        this.mode = mode;
        this.delegate = delegate;
        this.networkWideParkingSpaceStore = networkWideParkingSpaceStore;
        this.parkingSpaceAssignmentLogic = parkingSpaceAssignmentLogic;
    }

    @Override
    public List<? extends PlanElement> calcRoute(RoutingRequest request) {
        List<? extends PlanElement> elements = this.delegate.calcRoute(request);
        for(int i = elements.size()-1; i>=0; i--) {
            PlanElement element = elements.get(i);
            if((element instanceof Leg leg) && leg.getMode().equals(this.mode)) {
                Verify.verify(this.mode.equals(leg.getRoutingMode()));
                ParkingSpace parkingSpace = parkingSpaceAssignmentLogic.getUsedParkingSpace(networkWideParkingSpaceStore, request.getPerson(), request.getToFacility().getLinkId());
                Verify.verify(parkingSpace != null, String.format("Parking space not found for person %s at link %s", request.getPerson().getId().toString(), request.getToFacility().getLinkId().toString()));
                leg.getAttributes().putAttribute(PARKING_TYPE_ATTR, parkingSpace.parkingType().id());
            }
        }
        return elements;
    }
}
