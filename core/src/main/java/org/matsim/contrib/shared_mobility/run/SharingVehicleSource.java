package org.matsim.contrib.shared_mobility.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.shared_mobility.io.SharingServiceSpecification;
import org.matsim.contrib.shared_mobility.io.SharingVehicleSpecification;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

/**
 * @author steffenaxer
 */
public class SharingVehicleSource implements AgentSource {
    private QSim qsim;
    private SharingServiceSpecification specification;

    public SharingVehicleSource(QSim qSim, SharingServiceSpecification specification) {
        this.qsim = qSim;
        this.specification = specification;
    }

    @Override
    public void insertAgentsIntoMobsim() {

        Vehicles vehicles = this.qsim.getScenario().getVehicles();
        VehiclesFactory factory = this.qsim.getScenario().getVehicles().getFactory();

        for (SharingVehicleSpecification veh : specification.getVehicles()) {
            Id<Link> startLink = veh.getStartLinkId().get();
            Id<Vehicle> vehId = Id.createVehicleId(veh.getId().toString());
            Vehicle basicVehicle = vehicles.getVehicles().get(vehId);
            if (basicVehicle == null) {
                VehicleType vehicleType = getOrCreateVehicleType(vehicles, factory, vehId);
                basicVehicle = factory.createVehicle(vehId, vehicleType);
                vehicles.addVehicle(basicVehicle);
            }
            VehicleUtils.setInitialLinkId(basicVehicle, startLink);
            QVehicleImpl qvehicle = new QVehicleImpl(basicVehicle);
            qvehicle.setCurrentLink(this.qsim.getScenario().getNetwork().getLinks().get(startLink));
            qsim.addParkedVehicle(qvehicle, startLink);
        }
    }

    private static VehicleType getOrCreateVehicleType(Vehicles vehicles, VehiclesFactory factory, Id<Vehicle> vehicleId) {
        if (vehicleId.toString().contains(TransportMode.bike)) {
            Id<VehicleType> vehicleTypeId = Id.create("default_" + TransportMode.bike, VehicleType.class);
            VehicleType vehicleType = vehicles.getVehicleTypes().get(vehicleTypeId);
            if (vehicleType == null) {
                vehicleType = factory.createVehicleType(vehicleTypeId);
                vehicleType.setNetworkMode(TransportMode.bike);
                vehicles.addVehicleType(vehicleType);
            }
            return vehicleType;
        }

        Id<VehicleType> vehicleTypeId = Id.create("defaultVehicleType", VehicleType.class);
        VehicleType vehicleType = vehicles.getVehicleTypes().get(vehicleTypeId);
        if (vehicleType == null) {
            vehicleType = VehicleUtils.createDefaultVehicleType();
            vehicles.addVehicleType(vehicleType);
        }
        return vehicleType;
    }
}
