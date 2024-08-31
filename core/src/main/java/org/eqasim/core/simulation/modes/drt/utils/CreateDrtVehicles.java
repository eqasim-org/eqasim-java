package org.eqasim.core.simulation.modes.drt.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.extent.ShapeScenarioExtent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.*;
import org.matsim.core.config.CommandLine;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CreateDrtVehicles {
    private final static Logger logger = LogManager.getLogger(CreateDrtVehicles.class);

    public final static long DEFAULT_RANDOM_SEED = 1234;

    public static void main(String[] args) throws CommandLine.ConfigurationException, IOException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("network-path", "output-vehicles-path", "vehicles-number")
                .allowOptions("vehicles-capacity", "service-begin-time", "service-end-time", "vehicle-id-prefix")
                .allowOptions("random-seed")
                .allowOptions("service-area-path", "network-modes")
                .build();
        int vehiclesNumber = Integer.parseInt(cmd.getOptionStrict("vehicles-number"));
        int vehiclesCapacity = cmd.hasOption("vehicles-capacity") ? Integer.parseInt(cmd.getOptionStrict("vehicles-capacity")) : 4;
        int serviceBeginTime = cmd.hasOption("service-begin-time") ? Integer.parseInt(cmd.getOptionStrict("service-begin-time")) : 0;
        int serviceEndTime = cmd.hasOption("service-end-time") ? Integer.parseInt(cmd.getOptionStrict("service-end-time")) : 24 * 3600;
        long randomSeed = cmd.hasOption("random-seed") ? Long.parseLong(cmd.getOptionStrict("random-seed")) : DEFAULT_RANDOM_SEED;
        String vehicleIdPrefx = cmd.getOption("vehicle-id-prefix").orElse("vehicle_drt_");
        String serviceArea = cmd.getOption("service-area-path").orElse(null);
        Set<String> networkModes = cmd.hasOption("network-modes") ? Arrays.stream(cmd.getOptionStrict("network-modes").split(",")).map(String::trim).collect(Collectors.toSet()) : Set.of("car");
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(cmd.getOptionStrict("network-path"));
        Stream<? extends Link> linkStream = network.getLinks().values().stream();
        if(serviceArea != null) {
            ScenarioExtent extent = new ShapeScenarioExtent.Builder(new File(serviceArea), Optional.empty(), Optional.empty()).build();
            linkStream = linkStream.filter(link -> extent.isInside(link.getFromNode().getCoord()) || extent.isInside(link.getToNode().getCoord()));
        }
        List<Id<Link>> linksIds = linkStream.filter(link -> link.getAllowedModes().stream().anyMatch(networkModes::contains)).map(Link::getId).toList();
        if(linksIds.size() == 0) {
            throw new IllegalStateException("No matching links to sample from");
        }
        Random random = new Random(randomSeed);
        FleetSpecification fleetSpecification = new FleetSpecificationImpl();
        for(int i=0; i<vehiclesNumber; i++) {
            Id<Link> linkId = null;
            while(linkId == null || !network.getLinks().get(linkId).getAllowedModes().contains("car")) {
                linkId = linksIds.get(random.nextInt(linksIds.size()));
            }
            Id<DvrpVehicle> vehicleId = Id.create(vehicleIdPrefx+i, DvrpVehicle.class);
            logger.info("Creating vehicle " + vehicleId.toString() + " on link " + linkId);
            DvrpVehicleSpecification dvrpVehicleSpecification = ImmutableDvrpVehicleSpecification.newBuilder().id(vehicleId).startLinkId(linkId).serviceBeginTime(serviceBeginTime).serviceEndTime(serviceEndTime).capacity(vehiclesCapacity).build();
            fleetSpecification.addVehicleSpecification(dvrpVehicleSpecification);
        }
        new FleetWriter(fleetSpecification.getVehicleSpecifications().values().stream()).write(cmd.getOptionStrict("output-vehicles-path"));
    }
}
