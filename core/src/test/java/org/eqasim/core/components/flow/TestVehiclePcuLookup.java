package org.eqasim.core.components.flow;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import java.util.List;
import java.util.Map;
import org.matsim.vehicles.Vehicles;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.google.inject.AbstractModule;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.Injector;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.core.controler.OutputDirectoryHierarchy;

public class TestVehiclePcuLookup {
    @Test
    public void regularAndTransitVehiclesShareOneIndexSpace() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Id<Vehicle> car = addVehicle(scenario, "index-check-car", "car", 1.0, false);
        Id<Vehicle> bus = addVehicle(scenario, "index-check-bus", "bus", 3.0, true);
        assertNotEquals(car.index(), bus.index());
        VehiclePcuLookup lookup = new VehiclePcuLookup(scenario, new FlowUtils(scenario));
        assertEquals(1.0F, lookup.getPcu(car), 0.0F);
        assertEquals(3.0F, lookup.getPcu(bus), 0.0F);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void initializationRejectsAnIndexCollisionAcrossCollections() {
        Scenario scenario = mock(Scenario.class);
        Vehicles regular = mock(Vehicles.class);
        Vehicles transit = mock(Vehicles.class);
        when(scenario.getVehicles()).thenReturn(regular);
        when(scenario.getTransitVehicles()).thenReturn(transit);
        Id<Vehicle> car = Id.createVehicleId("index-collision-car");
        Id<Vehicle> bus = mock(Id.class);
        when(bus.index()).thenReturn(car.index());
        when(bus.toString()).thenReturn("index-collision-bus");
        when(regular.getVehicles()).thenReturn(Map.of(car, mock(Vehicle.class)));
        when(transit.getVehicles()).thenReturn(Map.of(bus, mock(Vehicle.class)));
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new VehiclePcuLookup(scenario, mock(FlowUtils.class)));
        assertTrue(error.getMessage().contains("share index " + car.index()));
    }

    @Test
    public void sameVehicleIdInBothCollectionsIsNotAnIndexCollision() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Id<Vehicle> regular = addVehicle(scenario, "shared-vehicle", "car", 1.0, false);
        Id<Vehicle> transit = addVehicle(scenario, "shared-vehicle", "bus", 3.0, true);
        assertEquals(regular.index(), transit.index());
        VehiclePcuLookup lookup = new VehiclePcuLookup(scenario, new FlowUtils(scenario));
        assertEquals(3.0F, lookup.getPcu(regular), 0.0F);
    }

    @Test
    public void classifiesVehiclesOnceAndReturnsPcuByIndex() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        Id<Vehicle> carId = addVehicle(scenario, "bus-bike-car", "car", 1.0, false);
        Id<Vehicle> truckId = addVehicle(scenario, "person:truck", "truck", 2.5, false);
        Id<Vehicle> bikeId = addVehicle(scenario, "vehicle-42", "bike", 1.0, false);
        Id<Vehicle> busId = addVehicle(scenario, "vehicle-17", "pt", 3.0, true);
        Id<Vehicle> railId = addVehicle(scenario, "bus-named-train", "rail", 8.0, true);

        addRoute(scenario, "bus", busId);
        addRoute(scenario, "rail", railId);

        VehiclePcuLookup lookup = new VehiclePcuLookup(scenario, new FlowUtils(scenario));

        assertEquals(1.0F, lookup.getPcu(carId), 0.0F);
        assertEquals(2.5F, lookup.getPcu(truckId), 0.0F);
        assertEquals(0.0F, lookup.getPcu(bikeId), 0.0F);
        assertEquals(3.0F, lookup.getPcu(busId), 0.0F);
        assertEquals(0.0F, lookup.getPcu(railId), 0.0F);
        assertEquals(0.0F, lookup.getPcu(Id.createVehicleId("unknown")), 0.0F);
    }

    @Test
    public void countWeightsUseVehicleClassRatherThanPcu() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Id<Vehicle> bike = addVehicle(scenario, "42", "bike", 2.0, false);
        Id<Vehicle> bus = addVehicle(scenario, "17", "pt", 3.0, true);
        Id<Vehicle> smallCar = addVehicle(scenario, "bus-bike", "car", 0.2, false);
        Id<Vehicle> truck = addVehicle(scenario, "truck", "truck", 2.5, false);
        addRoute(scenario, "bus", bus);
        FlowUtils utils = new FlowUtils(scenario);
        assertEquals(0.0F, utils.getCountValue(bike, 0.1), 0.0F);
        assertEquals(0.1F, utils.getCountValue(bus, 0.1), 0.0F);
        assertEquals(1.0F, utils.getCountValue(smallCar, 0.1), 0.0F);
        assertEquals(1.0F, utils.getCountValue(truck, 0.1), 0.0F);
        assertFalse(utils.isBus(smallCar));
        assertFalse(utils.isBike(smallCar));
    }

    @Test
    public void dailyCountsAndPcuFlowsUseSeparateWeights() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Id<Vehicle> bus = addVehicle(scenario, "17", "bus", 3.0, true);
        Id<Vehicle> car = addVehicle(scenario, "bus-bike", "car", 0.2, false);
        Id<Vehicle> bike = addVehicle(scenario, "42", "bike", 1.0, false);
        Id<Vehicle> passenger = addVehicle(scenario, "43", "car_passenger", 1.0, false);
        var network = scenario.getNetwork();
        var factory = network.getFactory();
        var from = factory.createNode(Id.createNodeId("from"), new Coord(0, 0));
        var to = factory.createNode(Id.createNodeId("to"), new Coord(100, 0));
        network.addNode(from);
        network.addNode(to);
        var link = factory.createLink(Id.createLinkId("road"), from, to);
        network.addLink(link);
        FlowUtils utils = new FlowUtils(scenario);
        FlowConfigGroup config = new FlowConfigGroup();
        FlowBinManager bins = new FlowBinManager(config);
        LinkFlowCounter counter = new LinkFlowCounter(network, new FlowDataSet(network, bins, config.getBeta()),
                bins, mock(OutputDirectoryHierarchy.class), config, new VehiclePcuLookup(scenario, utils), 0.1, utils);
        for (Id<Vehicle> id : List.of(bus, car, bike, passenger)) {
            counter.handleEvent(new LinkEnterEvent(100.0, id, link.getId()));
        }
        assertEquals(3.2, counter.getLinkCounts(link.getId(), 100.0), 1e-6);
        assertEquals(1.1, counter.getDailyCounts(link.getId()), 1e-6);
    }

    @Test
    public void injectedFlowUtilsIsSharedAndClassificationIsCached() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        FlowConfigGroup flowConfig = new FlowConfigGroup();
        flowConfig.setActivate(false);
        scenario.getConfig().addModule(flowConfig);
        scenario.getConfig().addModule(new EqasimConfigGroup());
        Id<Vehicle> bus = addVehicle(scenario, "17", "pt", 3.0, true);
        addRoute(scenario, "bus", bus);
        // Use MATSim's injector, which requires explicit bindings, and the production module.
        var injector = Injector.createInjector(scenario.getConfig(), new FlowModule(), new AbstractModule() {
            @Override
            protected void configure() {
                bind(Scenario.class).toInstance(scenario);
                bind(Network.class).toInstance(scenario.getNetwork());
                bind(OutputDirectoryHierarchy.class).toInstance(mock(OutputDirectoryHierarchy.class));
            }
        });
        FlowUtils first = injector.getInstance(FlowUtils.class);
        assertSame(first, injector.getInstance(FlowUtils.class));
        scenario.getTransitSchedule().getTransitLines().values().iterator().next()
                .getRoutes().values().iterator().next().setTransportMode("rail");
        assertTrue(first.isBus(bus));
    }

    private void addRoute(Scenario scenario, String mode, Id<Vehicle> vehicleId) {
        var schedule = scenario.getTransitSchedule();
        var factory = schedule.getFactory();
        var line = factory.createTransitLine(Id.create(mode, TransitLine.class));
        var route = factory.createTransitRoute(Id.create(mode, TransitRoute.class), null, List.of(), mode);
        var departure = factory.createDeparture(Id.create(mode, Departure.class), 0.0);
        departure.setVehicleId(vehicleId);
        route.addDeparture(departure);
        line.addRoute(route);
        schedule.addTransitLine(line);
    }

    private Id<Vehicle> addVehicle(Scenario scenario, String vehicleIdValue, String typeIdValue,
                                   double pcu, boolean transit) {
        var vehicles = transit ? scenario.getTransitVehicles() : scenario.getVehicles();
        Id<VehicleType> typeId = Id.create(typeIdValue, VehicleType.class);
        VehicleType type = vehicles.getFactory().createVehicleType(typeId);
        type.setPcuEquivalents(pcu);
        type.setNetworkMode(typeIdValue);
        vehicles.addVehicleType(type);

        Id<Vehicle> vehicleId = Id.createVehicleId(vehicleIdValue);
        vehicles.addVehicle(vehicles.getFactory().createVehicle(vehicleId, type));
        return vehicleId;
    }
}
