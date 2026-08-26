package org.eqasim.core.components.flow;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import static org.junit.Assert.assertEquals;

public class TestVehiclePcuLookup {
    @Test
    public void classifiesVehiclesOnceAndReturnsPcuByIndex() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        Id<Vehicle> carId = addVehicle(scenario, "car-without-colon", "car", 1.0, false);
        Id<Vehicle> truckId = addVehicle(scenario, "person:truck", "truck", 2.5, false);
        Id<Vehicle> bikeId = addVehicle(scenario, "person:bike", "bike", 1.0, false);
        Id<Vehicle> busId = addVehicle(scenario, "bus-1", "bus", 3.0, true);
        Id<Vehicle> railId = addVehicle(scenario, "rail-1", "rail", 8.0, true);

        VehiclePcuLookup lookup = new VehiclePcuLookup(scenario);

        assertEquals(1.0F, lookup.getPcu(carId), 0.0F);
        assertEquals(2.5F, lookup.getPcu(truckId), 0.0F);
        assertEquals(0.0F, lookup.getPcu(bikeId), 0.0F);
        assertEquals(3.0F, lookup.getPcu(busId), 0.0F);
        assertEquals(0.0F, lookup.getPcu(railId), 0.0F);
        assertEquals(0.0F, lookup.getPcu(Id.createVehicleId("unknown")), 0.0F);
    }

    private Id<Vehicle> addVehicle(Scenario scenario, String vehicleIdValue, String typeIdValue,
                                   double pcu, boolean transit) {
        var vehicles = transit ? scenario.getTransitVehicles() : scenario.getVehicles();
        Id<VehicleType> typeId = Id.create(typeIdValue, VehicleType.class);
        VehicleType type = vehicles.getFactory().createVehicleType(typeId);
        type.setPcuEquivalents(pcu);
        vehicles.addVehicleType(type);

        Id<Vehicle> vehicleId = Id.createVehicleId(vehicleIdValue);
        vehicles.addVehicle(vehicles.getFactory().createVehicle(vehicleId, type));
        return vehicleId;
    }
}
