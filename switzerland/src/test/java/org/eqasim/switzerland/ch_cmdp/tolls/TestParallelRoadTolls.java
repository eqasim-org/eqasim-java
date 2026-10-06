package org.eqasim.switzerland.ch_cmdp.tolls;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;
import org.eqasim.core.components.travel_disutility.EqasimTravelDisutilityFactory;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import static org.junit.Assert.*;

/**
 * A small, reproducible routing + QSim experiment. Run with:
 * mvn -pl switzerland -am test -Dtest=TestParallelRoadTolls -Dsurefire.failIfNoSpecifiedTests=false
 * Results: switzerland/target/toll-parallel-roads/{route-shares.png,route-statistics.csv}.
 *
 * Roads have equal lengths and capacities but configurable speeds. With routing
 * noise disabled, agents trade time savings against their personal toll sensitivity.
 */
public class TestParallelRoadTolls {
    private static final int AGENTS = 5_000;
    private static final long SEED = 4711L;
    private static final double[] RATES = {0.0, 0.1, 0.2, 0.4, 0.5};
    private static final double LENGTH = 1_000.0;
    // One speed per road, in the same order as RATES. All three links use this speed.
    private static final double[] SPEEDS_KMH = {70.0, 90.0, 110.0, 120.0, 130.0};
    private static final double CONNECTOR_SPEED = 25.0; // m/s, shared origin/destination
    private static final double CAPACITY = 3_600.0;
    private static final double ROUTING_SIGMA = 0.0;
    private static final double TOLL_SIGMA = 0.5;
    private static final double VALUE_OF_TIME = 15.0;
    private static final TravelTime FREE_FLOW = (link, time, person, vehicle) -> link.getLength() / link.getFreespeed();
    private static final Path OUTPUT = Path.of("target", "toll-parallel-roads");

    @Test
    public void attributesAddTheExpectedMonetaryDisutility() throws Exception {
        var scenario = scenario(OUTPUT.resolve("attributes"), true);
        var tolls = new Tolls();
        var costs = new EqasimTollsTravelDisutilityFactory(delegate(0.0), tolls,
                new MarginalCostOfTolls(0.0, VALUE_OF_TIME, SEED)).createTravelDisutility(FREE_FLOW);
        var vehicle = vehicle("check");
        assertEquals(17, scenario.getNetwork().getLinks().size());
        for (int road = 0; road < RATES.length; road++) {
            double total = 0.0;
            for (int segment = 0; segment < 3; segment++) {
                Link link = scenario.getNetwork().getLinks().get(linkId(road, segment));
                assertEquals(LENGTH, link.getLength(), 0.0);
                assertEquals(SPEEDS_KMH[road] / 3.6, link.getFreespeed(), 0.0);
                assertEquals(CAPACITY, link.getCapacity(), 0.0);
                assertEquals(RATES[road] * LENGTH / 1_000.0, tolls.getToll(link), 1e-6);
                double expected = LENGTH / (SPEEDS_KMH[road] / 3.6) + RATES[road] * LENGTH / 1_000.0 * 3_600.0 / VALUE_OF_TIME;
                assertEquals(expected, costs.getLinkTravelDisutility(link, 0, null, vehicle), 1e-5);
                assertEquals(LENGTH / (SPEEDS_KMH[road] / 3.6), costs.getLinkMinimumTravelDisutility(link), 1e-9);
                total += tolls.getToll(link);
            }
            assertEquals(3.0 * RATES[road], total, 1e-6);
        }
    }

    @Test
    public void equalSpeedsWithoutRoutingVariationFavorTheCheapestRoad() throws Exception {
        double[] equalSpeeds = new double[RATES.length];
        Arrays.fill(equalSpeeds, SPEEDS_KMH[0]);
        var scenario = scenario(OUTPUT.resolve("deterministic"), true, equalSpeeds);
        var factory = new EqasimTollsTravelDisutilityFactory(delegate(0.0),
                new Tolls(), new MarginalCostOfTolls(TOLL_SIGMA, VALUE_OF_TIME, SEED));
        int[] counts = routePopulation(scenario, factory);
        double cheapest = Arrays.stream(RATES).min().orElseThrow();
        for (int road = 0; road < RATES.length; road++) {
            if (RATES[road] > cheapest) assertEquals(0, counts[road]);
        }
        assertEquals(AGENTS, Arrays.stream(counts).sum());
    }

    @Test
    public void simulatedTrafficTradesTravelTimeAgainstTolls() throws Exception {
        Counts untolled = simulate("untolled", false);
        Counts tolled = simulate("tolled", true);
        writeResults(untolled, tolled);
        if (ROUTING_SIGMA == 0.0) {
            double fastest = Arrays.stream(SPEEDS_KMH).max().orElseThrow();
            for (int road = 0; road < RATES.length; road++) {
                if (SPEEDS_KMH[road] < fastest) {
                    assertEquals("Without tolls or routing noise, only fastest roads are chosen", 0, untolled.links[road][0]);
                }
            }
        }
        // Arbitrary speed/rate combinations may leave dominated roads unused.
        // Individual minimum-cost choices are checked independently in routePopulation.
    }

    private static Counts simulate(String name, boolean priced) throws Exception {
        Scenario scenario = scenario(OUTPUT.resolve(name), priced);
        var counts = new Counts();
        var controller = new Controler(scenario);
        controller.addOverridingModule(new org.eqasim.switzerland.ch_cmdp.utils.link_stats.DetailedLinkStatisticsModule());
        // Exercise the real module/provider chain with explicit routing parameters.
        controller.addOverridingModule(AbstractModule.override(List.of(new TollsModule()), new AbstractModule() {
            @Override public void install() {
                // This standalone controller does not install Eqasim's FlowModule.
                bind(org.eqasim.core.components.flow.FlowUtils.class).toInstance(
                        new org.eqasim.core.components.flow.FlowUtils(scenario));
                bind(EqasimTravelDisutilityFactory.class).toInstance(delegate(ROUTING_SIGMA));
                addEventHandlerBinding().toInstance(counts);
            }
        }));
        var factory = controller.getInjector().getInstance(EqasimTollsTravelDisutilityFactory.class);
        assertSame("Car routing must use the toll module's factory", factory, controller.getTravelDisutilityFactory());
        int[] planned = routePopulation(scenario, factory);
        controller.run();
        assertEquals("Every car must arrive", AGENTS, counts.arrivals);
        assertEquals("No agents may get stuck", 0, counts.stuck);
        assertEquals("Each vehicle crosses exactly one branch", AGENTS,
                Arrays.stream(counts.links).mapToInt(links -> links[0]).sum());
        for (int road = 0; road < RATES.length; road++) {
            for (int segment = 0; segment < 3; segment++) {
                assertEquals("Simulated link flow must match routed agents", planned[road], counts.links[road][segment]);
            }
        }
        // Check the real final-iteration listener against independently collected events.
        try (var stream = new java.util.zip.GZIPInputStream(Files.newInputStream(
                OUTPUT.resolve(name).resolve("output_detailed_link_statistics.csv.gz")))) {
            var rows = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).lines().skip(1).toList();
            for (int road = 0; road < RATES.length; road++) {
                for (int segment = 0; segment < 3; segment++) {
                    String prefix = "\"" + linkId(road, segment) + "\",";
                    int recorded = rows.stream().filter(row -> row.startsWith(prefix))
                            .mapToInt(row -> Integer.parseInt(row.split(",")[4])).sum();
                    assertEquals("Final-iteration CSV must match link-entry events", counts.links[road][segment], recorded);
                }
            }
        }
        return counts;
    }

    private static EqasimTravelDisutilityFactory delegate(double sigma) {
        return new EqasimTravelDisutilityFactory((link, person, time, cost) -> 0.0, 0.0, sigma, SEED);
    }

    private static Scenario scenario(Path directory, boolean priced) {
        return scenario(directory, priced, SPEEDS_KMH);
    }

    private static Scenario scenario(Path directory, boolean priced, double[] speedsKmh) {
        assertEquals("Supply exactly five road rates", 5, RATES.length);
        assertEquals("Supply one speed for each road", RATES.length, speedsKmh.length);
        for (int road = 0; road < RATES.length; road++) {
            assertTrue("Speeds must be finite and positive", Double.isFinite(speedsKmh[road]) && speedsKmh[road] > 0);
            assertTrue("Tolls must be finite and nonnegative", Double.isFinite(RATES[road]) && RATES[road] >= 0);
        }
        var config = ConfigUtils.createConfig();
        config.global().setRandomSeed(SEED);
        config.global().setNumberOfThreads(1);
        config.controller().setOutputDirectory(directory.toString());
        config.controller().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
        config.controller().setLastIteration(0);
        config.controller().setCreateGraphs(false);
        // This intentionally one-way OD network is not strongly connected.
        config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
        config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.none);
        config.qsim().setNumberOfThreads(1);
        config.qsim().setEndTime(14_400);
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.fromVehiclesData);
        config.scoring().addActivityParams(new ActivityParams("origin").setTypicalDuration(3_600));
        config.scoring().addActivityParams(new ActivityParams("destination").setTypicalDuration(3_600));
        var calibration = NetworkCalibrationConfigGroup.getOrCreate(config);
        calibration.setTollsSigma(TOLL_SIGMA);
        calibration.setTollsValueOfTime(VALUE_OF_TIME);
        Scenario scenario = ScenarioUtils.createScenario(config);
        Network network = scenario.getNetwork();
        Node start = node(network, "start", -100, 0);
        Node split = node(network, "split", 0, 0);
        Node merge = node(network, "merge", 3_000, 0);
        Node end = node(network, "end", 3_100, 0);
        link(network, "origin", start, split, 100, CONNECTOR_SPEED, 0);
        link(network, "destination", merge, end, 100, CONNECTOR_SPEED, 0);
        for (int road = 0; road < RATES.length; road++) {
            // Coordinates are schematic; simulated lengths are explicitly identical.
            Node first = node(network, "r" + road + "a", 1_000, (road - 2) * 200);
            Node second = node(network, "r" + road + "b", 2_000, (road - 2) * 200);
            Node[] nodes = {split, first, second, merge};
            for (int segment = 0; segment < 3; segment++) {
                link(network, linkId(road, segment).toString(), nodes[segment], nodes[segment + 1], LENGTH, speedsKmh[road] / 3.6,
                        priced ? RATES[road] : 0.0);
            }
        }
        return scenario;
    }

    private static int[] routePopulation(Scenario scenario, EqasimTollsTravelDisutilityFactory factory) {
        Network network = scenario.getNetwork();
        var router = new DijkstraFactory().createPathCalculator(network, factory.createTravelDisutility(FREE_FLOW), FREE_FLOW);
        var population = scenario.getPopulation();
        var pf = population.getFactory();
        int[] counts = new int[RATES.length];
        var sensitivity = new MarginalCostOfTolls(TOLL_SIGMA, VALUE_OF_TIME, SEED);
        for (int index = 0; index < AGENTS; index++) {
            String id = "agent-" + index;
            var person = pf.createPerson(Id.createPersonId(id));
            Vehicle vehicle = vehicle(id);
            if (scenario.getVehicles().getVehicleTypes().isEmpty()) scenario.getVehicles().addVehicleType(vehicle.getType());
            scenario.getVehicles().addVehicle(vehicle);
            VehicleUtils.insertVehicleIdsIntoAttributes(person, Map.of(TransportMode.car, vehicle.getId()));
            double departure = 2.0 * index;
            var path = router.calcLeastCostPath(network.getNodes().get(Id.createNodeId("split")),
                    network.getNodes().get(Id.createNodeId("merge")), departure + 100 / CONNECTOR_SPEED, person, vehicle);
            assertEquals(3, path.links.size());
            int road = Integer.parseInt(path.links.getFirst().getId().toString().split("_")[1]);
            for (int segment = 0; segment < 3; segment++) assertEquals(linkId(road, segment), path.links.get(segment).getId());
            if (ROUTING_SIGMA == 0.0) {
                // Independent enumeration: no Dijkstra, no travel-disutility factory.
                double[] costs = new double[RATES.length];
                for (int candidate = 0; candidate < RATES.length; candidate++) {
                    for (int segment = 0; segment < 3; segment++) {
                        Link link = network.getLinks().get(linkId(candidate, segment));
                        double toll = ((Number) link.getAttributes().getAttribute("toll")).doubleValue();
                        costs[candidate] += link.getLength() / link.getFreespeed()
                                + toll * sensitivity.getMarginalCostOfTolls(vehicle);
                    }
                }
                assertEquals("Each agent must choose minimum time + personal toll cost",
                        Arrays.stream(costs).min().orElseThrow(), costs[road], 1e-6);
            }
            counts[road]++;
            var route = RouteUtils.createLinkNetworkRouteImpl(Id.createLinkId("origin"),
                    path.links.stream().map(Link::getId).toList(), Id.createLinkId("destination"));
            route.setVehicleId(vehicle.getId());
            route.setDistance(3 * LENGTH + 200);
            route.setTravelTime(path.travelTime + 200 / CONNECTOR_SPEED);
            var plan = pf.createPlan();
            var origin = pf.createActivityFromLinkId("origin", Id.createLinkId("origin"));
            origin.setEndTime(departure);
            plan.addActivity(origin);
            var leg = pf.createLeg(TransportMode.car);
            leg.setRoute(route);
            plan.addLeg(leg);
            plan.addActivity(pf.createActivityFromLinkId("destination", Id.createLinkId("destination")));
            person.addPlan(plan);
            population.addPerson(person);
        }
        return counts;
    }

    private static Vehicle vehicle(String id) {
        return VehicleUtils.createVehicle(Id.createVehicleId(id), VehicleUtils.createVehicleType(Id.createVehicleTypeId("car")));
    }

    private static Node node(Network network, String id, double x, double y) {
        Node node = network.getFactory().createNode(Id.createNodeId(id), new Coord(x, y));
        network.addNode(node);
        return node;
    }

    private static void link(Network network, String id, Node from, Node to, double length, double speed, double rate) {
        Link link = network.getFactory().createLink(Id.createLinkId(id), from, to);
        link.setLength(length);
        link.setFreespeed(speed);
        link.setCapacity(CAPACITY);
        link.setNumberOfLanes(1);
        link.setAllowedModes(Set.of(TransportMode.car));
        link.getAttributes().putAttribute("toll", rate * length / 1_000.0);
        network.addLink(link);
    }

    private static Id<Link> linkId(int road, int segment) {
        return Id.createLinkId("road_" + road + "_" + segment);
    }

    private static class Counts implements LinkEnterEventHandler, PersonArrivalEventHandler, PersonStuckEventHandler {
        final int[][] links = new int[5][3];
        int arrivals;
        int stuck;
        @Override public void handleEvent(LinkEnterEvent event) {
            String id = event.getLinkId().toString();
            if (id.startsWith("road_")) {
                String[] parts = id.split("_");
                links[Integer.parseInt(parts[1])][Integer.parseInt(parts[2])]++;
            }
        }
        @Override public void handleEvent(PersonArrivalEvent event) {
            if (TransportMode.car.equals(event.getLegMode())) arrivals++;
        }
        @Override public void handleEvent(PersonStuckEvent event) { stuck++; }
        @Override public void reset(int iteration) {
            for (int[] road : links) Arrays.fill(road, 0);
            arrivals = 0;
            stuck = 0;
        }
    }

    private static void writeResults(Counts untolled, Counts tolled) throws Exception {
        StringBuilder csv = new StringBuilder("road,speed_kmh,free_flow_road_seconds,toll_per_km,route_toll,untolled_vehicles,tolled_vehicles,tolled_share,link_1,link_2,link_3\n");
        for (int road = 0; road < RATES.length; road++) {
            csv.append(String.format(Locale.ROOT, "%d,%.2f,%.3f,%.2f,%.2f,%d,%d,%.6f,%d,%d,%d%n", road,
                    SPEEDS_KMH[road], 3 * LENGTH / (SPEEDS_KMH[road] / 3.6),
                    RATES[road], 3 * RATES[road], untolled.links[road][0], tolled.links[road][0],
                    tolled.links[road][0] / (double) AGENTS, tolled.links[road][0], tolled.links[road][1], tolled.links[road][2]));
        }
        Files.writeString(OUTPUT.resolve("route-statistics.csv"), csv);
        plot(untolled, tolled);
        System.out.println("Parallel-road toll results: " + OUTPUT.toAbsolutePath());
        System.out.println(csv);
    }

    private static void plot(Counts untolled, Counts tolled) throws Exception {
        var image = new BufferedImage(1_200, 900, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setColor(new Color(30, 40, 55));
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.drawString("Five roads: faster travel versus higher tolls", 55, 48);
        g.setFont(new Font("SansSerif", Font.PLAIN, 17));
        g.drawString(String.format(Locale.ROOT, "%d agents | 3 x %.1f km per road | %.0f vehicles/h per link",
                AGENTS, LENGTH / 1_000, CAPACITY), 55, 80);
        Color blue = new Color(30, 110, 170);
        Color grey = new Color(190, 201, 214);
        g.setStroke(new BasicStroke(2));
        for (int road = 0; road < 5; road++) {
            int y = 123 + 31 * road;
            g.setColor(blue);
            g.drawPolyline(new int[] {210, 370, 540, 700}, new int[] {185, y, y, 185}, 4);
            for (int x : new int[] {370, 540}) g.fillOval(x - 4, y - 4, 8, 8);
            g.setColor(Color.DARK_GRAY);
            g.drawString(String.format(Locale.ROOT, "%.0f km/h | %.2f / km | %.0f s", SPEEDS_KMH[road], RATES[road],
                    3 * LENGTH / (SPEEDS_KMH[road] / 3.6)), 820, y + 6);
        }
        g.drawLine(80, 185, 210, 185);
        g.drawLine(700, 185, 800, 185);
        g.drawString("Origin", 85, 170);
        g.drawString("Destination", 705, 282);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString("Simulated vehicles per road", 65, 335);
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g.setColor(grey); g.fillRect(680, 320, 20, 16);
        g.setColor(Color.DARK_GRAY); g.drawString("All tolls zero", 710, 334);
        g.setColor(blue); g.fillRect(890, 320, 20, 16);
        g.setColor(Color.DARK_GRAY); g.drawString("Increasing tolls", 920, 334);
        int bottom = 730;
        int height = 340;
        int peak = Math.max(Arrays.stream(tolled.links).mapToInt(row -> row[0]).max().orElseThrow(),
                Arrays.stream(untolled.links).mapToInt(row -> row[0]).max().orElseThrow());
        int max = ((peak / 1_000) + 1) * 1_000;
        for (int tick = 0; tick <= max; tick += 1_000) {
            int y = bottom - tick * height / max;
            g.setColor(new Color(230, 234, 240)); g.drawLine(100, y, 1_120, y);
            g.setColor(Color.DARK_GRAY); g.drawString(Integer.toString(tick), 45, y + 5);
        }
        for (int road = 0; road < 5; road++) {
            int x = 155 + road * 200;
            int baseHeight = untolled.links[road][0] * height / max;
            int tollHeight = tolled.links[road][0] * height / max;
            g.setColor(grey); g.fillRect(x, bottom - baseHeight, 55, baseHeight);
            g.setColor(blue); g.fillRect(x + 60, bottom - tollHeight, 55, tollHeight);
            g.setColor(Color.DARK_GRAY);
            g.drawString(Integer.toString(untolled.links[road][0]), x, bottom - baseHeight - 9);
            g.drawString(Integer.toString(tolled.links[road][0]), x + 60, bottom - tollHeight - 9);
            g.drawString(String.format(Locale.ROOT, "%.1f%%", 100.0 * tolled.links[road][0] / AGENTS), x + 60, bottom - tollHeight - 29);
            g.drawString(String.format(Locale.ROOT, "%.2f", RATES[road]), x + 38, bottom + 28);
        }
        g.drawString("Toll rate (currency / km)", 480, 792);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.drawString("One QSim iteration; counts from link-entry events. Seed: " + SEED, 55, 832);
        g.drawString(String.format(Locale.ROOT, "Routing sigma = %.2f; toll sigma = %.2f; value of time = %.2f currency/h.",
                ROUTING_SIGMA, TOLL_SIGMA, VALUE_OF_TIME), 55, 855);
        g.drawString("Speeds and tolls vary together: traffic need not decrease with price; some roads may be unused.", 55, 878);
        g.dispose();
        ImageIO.write(image, "png", OUTPUT.resolve("route-shares.png").toFile());
    }
}
