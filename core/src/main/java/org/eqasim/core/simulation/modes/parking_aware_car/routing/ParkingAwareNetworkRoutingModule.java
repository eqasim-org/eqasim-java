package org.eqasim.core.simulation.modes.parking_aware_car.routing;

import com.google.common.base.Verify;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.NetworkWideParkingSpaceStore;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingSpace;
import org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.ParkingSpaceAssignmentLogic;
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
                ParkingSpace parkingSpace = parkingSpaceAssignmentLogic.getUsedParkingSpace(networkWideParkingSpaceStore, request.getPerson().getId(), leg.getRoute().getEndLinkId());
                Verify.verify(parkingSpace != null, String.format("Parking space not found for person %s at link %s", request.getPerson().getId().toString(), request.getToFacility().getLinkId().toString()));
                leg.getAttributes().putAttribute(PARKING_TYPE_ATTR, parkingSpace.parkingType().id());
                if(!parkingSpace.parkingType().id().equals(networkWideParkingSpaceStore.getFallBackParkingType().id())) {
                    request.getPerson().getAttributes().putAttribute(ParkingAwareMultimodalLinkChooser.LAST_CAR_LOCATION_ATTRIBUTE_NAME, leg.getRoute().getEndLinkId());
                }
            }
        }
        return elements;
    }
}
