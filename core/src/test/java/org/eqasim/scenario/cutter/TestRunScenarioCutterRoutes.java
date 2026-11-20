package org.eqasim.scenario.cutter;

import static org.junit.Assert.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.eqasim.core.components.transit.departure.DepartureFinder;
import org.eqasim.core.scenario.cutter.RunScenarioCutter;
import org.eqasim.core.scenario.cutter.ScenarioWriter;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.geotools.referencing.CRS;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleUtils;

public class TestRunScenarioCutterRoutes {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    /** Exercise MATSim injection and the actual CLI without unrelated mode-choice datasets. */
    public static class MinimalConfigurator extends EqasimConfigurator {
        public MinimalConfigurator(CommandLine commandLine) { super(commandLine); }
        @Override public void updateConfig(Config config) { }
        @Override public void configureScenario(Scenario scenario) { }
        @Override public void configureController(Controler controller) {
            controller.addOverridingModule(new AbstractModule() {
                @Override public void install() {
                    bind(DepartureFinder.class).toInstance((r, a, e, t) -> {
                        throw new AssertionError("No PT departure is needed in this road-only fixture");
                    });
                }
                @com.google.inject.Provides
                org.matsim.pt.transitSchedule.api.TransitSchedule provideSchedule(Scenario scenario) {
                    return scenario.getTransitSchedule();
                }
            });
        }
    }

    @Test public void skipRoutingWritesValidRoutesAndRepeatedCutPreservesThem() throws Exception {
        Path input = temporary.newFolder("input").toPath();
        Path shape = input.resolve("extent.gpkg");
        writeShape(shape);
        var config = ConfigUtils.createConfig();
        config.transit().setUseTransit(false);
        config.routing().getOrCreateModeRoutingParams("outside").setTeleportedModeSpeed(1000.0);
        config.routing().getOrCreateModeRoutingParams("walk").setTeleportedModeSpeed(1.0);
        config.qsim().setVehiclesSource(org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource.fromVehiclesData);
        config.network().setInputFile("network.xml.gz"); config.plans().setInputFile("population.xml.gz");
        config.facilities().setInputFile("facilities.xml.gz"); config.vehicles().setVehiclesFile("vehicles.xml.gz");
        config.transit().setVehiclesFile("transit_vehicles.xml.gz");
        for (String type : List.of("home", "work", "outside")) {
            var params = new org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams(type);
            params.setScoringThisActivityAtAll(false); config.scoring().addActivityParams(params);
        }
        var scenario = ScenarioUtils.createScenario(config);
        // MATSim's vehicles schema requires a type even for an empty road-only transit fleet.
        var busType = VehicleUtils.createVehicleType(Id.create("bus", org.matsim.vehicles.VehicleType.class));
        busType.setNetworkMode("bus"); scenario.getTransitVehicles().addVehicleType(busType);
        var network = scenario.getNetwork(); var factory = network.getFactory();
        double[] xs = {100, 200, 19800, 19900};
        for (int i = 0; i < xs.length; i++) network.addNode(factory.createNode(Id.createNodeId(i), new Coord(xs[i], 0)));
        for (int i = 0; i < xs.length - 1; i++) {
            for (boolean reverse : List.of(false, true)) {
                var link = factory.createLink(Id.createLinkId(reverse ? "reverse" + i : Integer.toString(i)),
                        network.getNodes().get(Id.createNodeId(reverse ? i + 1 : i)),
                        network.getNodes().get(Id.createNodeId(reverse ? i : i + 1)));
                link.setLength(xs[i + 1] - xs[i]); link.setFreespeed(10); link.setCapacity(1000);
                link.setNumberOfLanes(1); link.setAllowedModes(Set.of("car")); network.addLink(link);
            }
        }
        var person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("person"));
        var plan = PopulationUtils.createPlan(person);
        for (boolean origin : List.of(true, false)) {
            var facility = scenario.getActivityFacilities().getFactory().createActivityFacility(
                    Id.create(origin ? "origin" : "destination", org.matsim.facilities.ActivityFacility.class),
                    new Coord(origin ? -100 : 20100, 0), Id.createLinkId(origin ? 0 : 2));
            facility.addActivityOption(scenario.getActivityFacilities().getFactory().createActivityOption("outside"));
            scenario.getActivityFacilities().addActivityFacility(facility);
            var activity = PopulationUtils.createActivityFromCoordAndLinkId("outside", facility.getCoord(), facility.getLinkId());
            activity.setFacilityId(facility.getId());
            if (origin) {
                activity.setEndTime(100); plan.addActivity(activity);
                plan.addLeg(walk(100, 200, 0, 0));
                plan.addActivity(interaction(100, 0, 300));
                var car = PopulationUtils.createLeg("car"); car.setRoutingMode("car");
                car.setDepartureTime(300); car.setTravelTime(2000);
                var route = (NetworkRoute) new LinkNetworkRouteFactory().createRoute(Id.createLinkId(0), Id.createLinkId(2));
                route.setLinkIds(Id.createLinkId(0), List.of(Id.createLinkId(1)), Id.createLinkId(2));
                route.setVehicleId(Id.createVehicleId("vehicle")); route.setDistance(19800); route.setTravelTime(2000);
                car.setRoute(route); plan.addLeg(car);
                plan.addActivity(interaction(19900, 2, 2300));
                plan.addLeg(walk(2300, 200, 2, 2));
            } else plan.addActivity(activity);
        }
        person.addPlan(plan); scenario.getPopulation().addPerson(person);
        var selected = PopulationUtils.createPlan(person); PopulationUtils.copyFromTo(plan, selected);
        selected.getAttributes().putAttribute("selected-plan-marker", "preserved");
        person.addPlan(selected); person.setSelectedPlan(selected);
        var type = VehicleUtils.createVehicleType(Id.create("car", org.matsim.vehicles.VehicleType.class));
        type.setNetworkMode("car"); scenario.getVehicles().addVehicleType(type);
        scenario.getVehicles().addVehicle(VehicleUtils.createVehicle(Id.createVehicleId("vehicle"), type));
        VehicleUtils.insertVehicleIdsIntoAttributes(person, java.util.Map.of("car", Id.createVehicleId("vehicle")));
        new ScenarioWriter(config, scenario, "").run(input.toFile());
        for (String outputName : List.of("first", "second", "rerouted")) {
            Path output = temporary.getRoot().toPath().resolve(outputName);
            RunScenarioCutter.main(new String[] {"--config-path", input.resolve("config.xml").toString(),
                    "--extent-path", shape.toString(), "--output-path", output.toString(), "--threads", "1",
                    "--skip-routing", Boolean.toString(!outputName.equals("rerouted")),
                    "--eqasim-configurator", MinimalConfigurator.class.getName()});
            var reloaded = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(output.resolve("config.xml").toString()));
            var elements = reloaded.getPopulation().getPersons().get(person.getId()).getSelectedPlan().getPlanElements();
            assertEquals("preserved", reloaded.getPopulation().getPersons().get(person.getId()).getSelectedPlan()
                    .getAttributes().getAttribute("selected-plan-marker"));
            if (outputName.equals("rerouted")) {
                assertTrue(elements.stream().anyMatch(e -> e instanceof Leg leg && leg.getMode().equals("car")));
                continue;
            }
            assertEquals(7, elements.size());
            assertEquals(0, ((Activity) elements.getFirst()).getCoord().getX(), 0);
            assertEquals(20000, ((Activity) elements.getLast()).getCoord().getX(), 0);
            var retainedCar = (Leg) elements.get(3);
            assertEquals("car", retainedCar.getMode()); assertEquals(300, retainedCar.getDepartureTime().seconds(), 0);
            assertEquals(List.of(Id.createLinkId(1)), ((NetworkRoute) retainedCar.getRoute()).getLinkIds());
            assertEquals(Id.createVehicleId("vehicle"), ((NetworkRoute) retainedCar.getRoute()).getVehicleId());
            assertEquals(100, ((Leg) elements.get(1)).getTravelTime().seconds(), 0);
            assertEquals(100, ((Leg) elements.get(5)).getTravelTime().seconds(), 0);
            input = output;
        }
    }

    private Activity interaction(double x, int link, double end) {
        var a = PopulationUtils.createActivityFromCoordAndLinkId("car interaction", new Coord(x, 0), Id.createLinkId(link));
        a.setEndTime(end); a.setMaximumDuration(0); return a;
    }

    private Leg walk(double departure, double duration, int start, int end) {
        var leg = PopulationUtils.createLeg("walk"); leg.setRoutingMode("car");
        leg.setDepartureTime(departure); leg.setTravelTime(duration);
        var route = RouteUtils.createGenericRouteImpl(Id.createLinkId(start), Id.createLinkId(end));
        route.setDistance(200); route.setTravelTime(duration); leg.setRoute(route); return leg;
    }

    private void writeShape(Path path) throws Exception {
        var type = new SimpleFeatureTypeBuilder(); type.setName("extent"); type.setCRS(CRS.decode("EPSG:2056"));
        type.add("geometry", Polygon.class);
        var builder = new SimpleFeatureBuilder(type.buildFeatureType());
        builder.add(new GeometryFactory().createPolygon(new Coordinate[] {new Coordinate(0, -1000),
                new Coordinate(20000, -1000), new Coordinate(20000, 1000), new Coordinate(0, 1000), new Coordinate(0, -1000)}));
        var features = new DefaultFeatureCollection(); features.add(builder.buildFeature("extent"));
        try (var gpkg = new GeoPackage(path.toFile())) {
            gpkg.init(); var entry = new FeatureEntry(); entry.setTableName("extent"); entry.setSrid(2056);
            gpkg.add(entry, features);
        }
    }
}
