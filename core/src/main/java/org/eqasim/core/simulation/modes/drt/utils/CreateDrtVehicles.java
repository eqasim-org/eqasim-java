package org.eqasim.core.simulation.modes.drt.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.*;
import org.matsim.core.config.CommandLine;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CreateDrtVehicles {
    private final static Logger logger = LogManager.getLogger(CreateDrtVehicles.class);

    public final static long DEFAULT_RANDOM_SEED = 1234;

    public static void main(String[] args) throws CommandLine.ConfigurationException, MalformedURLException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("network-path", "output-vehicles-path", "vehicles-number")
                .allowOptions("vehicles-capacity", "service-begin-time", "service-end-time", "vehicle-id-prefix")
                .allowOptions("random-seed")
                .build();
        int vehiclesNumber = Integer.parseInt(cmd.getOptionStrict("vehicles-number"));
        int vehiclesCapacity = cmd.hasOption("vehicles-capacity") ? Integer.parseInt(cmd.getOptionStrict("vehicles-capacity")) : 4;
        int serviceBeginTime = cmd.hasOption("service-begin-time") ? Integer.parseInt(cmd.getOptionStrict("service-begin-time")) : 0;
        int serviceEndTime = cmd.hasOption("service-end-time") ? Integer.parseInt(cmd.getOptionStrict("service-end-time")) : 24 * 3600;
        long randomSeed = cmd.hasOption("random-seed") ? Long.parseLong(cmd.getOptionStrict("random-seed")) : DEFAULT_RANDOM_SEED;
        String vehicleIdPrefx = cmd.getOption("vehicle-id-prefix").orElse("vehicle_drt_");
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(cmd.getOptionStrict("network-path"));
        List<Id<Link>> linksIds = new ArrayList<>(network.getLinks().keySet());
        Random random = new Random(randomSeed);
        FleetSpecification fleetSpecification = new FleetSpecificationImpl();
        for(int i=0; i<vehiclesNumber; i++) {
            Id<Link> linkId = null;
            while(linkId == null || !network.getLinks().get(linkId).getAllowedModes().contains("car")) {
                linkId = linksIds.get(random.nextInt(linksIds.size()));
            }
            Id<DvrpVehicle> vehicleId = Id.create(vehicleIdPrefx+i, DvrpVehicle.class);
            logger.info("Creating vehicle " + vehicleId.toString() + " on link " + linkId.toString());
            DvrpVehicleSpecification dvrpVehicleSpecification = ImmutableDvrpVehicleSpecification.newBuilder().id(vehicleId).startLinkId(linkId).serviceBeginTime(serviceBeginTime).serviceEndTime(serviceEndTime).capacity(vehiclesCapacity).build();
            fleetSpecification.addVehicleSpecification(dvrpVehicleSpecification);
        }
        new FleetWriter(fleetSpecification.getVehicleSpecifications().values().stream()).write(cmd.getOptionStrict("output-vehicles-path"));
    }
}
