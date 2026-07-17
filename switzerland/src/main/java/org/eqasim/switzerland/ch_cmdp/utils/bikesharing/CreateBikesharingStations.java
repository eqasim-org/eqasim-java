package org.eqasim.switzerland.ch_cmdp.utils.bikesharing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.shared_mobility.io.DefaultSharingServiceSpecification;
import org.matsim.contrib.shared_mobility.io.ImmutableSharingStationSpecification;
import org.matsim.contrib.shared_mobility.io.ImmutableSharingVehicleSpecification;
import org.matsim.contrib.shared_mobility.io.SharingServiceSpecification;
import org.matsim.contrib.shared_mobility.io.SharingServiceWriter;
import org.matsim.contrib.shared_mobility.service.SharingStation;
import org.matsim.contrib.shared_mobility.service.SharingVehicle;
import org.matsim.core.config.CommandLine;
import org.matsim.core.network.NetworkUtils;

public class CreateBikesharingStations {
    private static final String USAGE = """
            Usage:
              CreateBikesharingStations \\
                --network-path network.xml.gz \\
                --output-path bikesharing_supply.xml \\
                --number-of-stations 100 \\
                --bikes-per-station 8 \\
                --parking-spaces-per-station 12 \\
                [--random-seed 0]
            """;

    public static void main(String[] args) throws CommandLine.ConfigurationException {
        if (args.length == 0 || hasHelpOption(args)) {
            System.out.println(USAGE);
            return;
        }

        CommandLine cmd = new CommandLine.Builder(args)
                .requireOptions("network-path", "output-path", "number-of-stations", "bikes-per-station",
                        "parking-spaces-per-station")
                .allowOptions("random-seed")
                .build();
        int numberOfStations = getPositiveInt(cmd, "number-of-stations");
        int bikesPerStation = getPositiveInt(cmd, "bikes-per-station");
        int parkingSpacesPerStation = getPositiveInt(cmd, "parking-spaces-per-station");

        if (parkingSpacesPerStation <= bikesPerStation) {
            throw new CommandLine.ConfigurationException(
                    "parking-spaces-per-station must be larger than bikes-per-station");
        }

        long randomSeed = cmd.hasOption("random-seed") ? Long.parseLong(cmd.getOptionStrict("random-seed")) : 0L;

        Network network = NetworkUtils.readNetwork(cmd.getOptionStrict("network-path"));
        List<Id<Link>> linkIds = new ArrayList<>();
        for (Link link : network.getLinks().values()) {
            if (link.getAllowedModes().contains(TransportMode.bike)) {
                linkIds.add(link.getId());
            }
        }
        if (linkIds.isEmpty()) {
            throw new CommandLine.ConfigurationException("Network contains no links accessible by bike");
        }

        SharingServiceSpecification specification = createSpecification(linkIds, numberOfStations, bikesPerStation,
                parkingSpacesPerStation, randomSeed);
        new SharingServiceWriter(specification).write(cmd.getOptionStrict("output-path"));
    }

    private static SharingServiceSpecification createSpecification(List<Id<Link>> linkIds, int numberOfStations,
            int bikesPerStation, int parkingSpacesPerStation, long randomSeed) {
        SharingServiceSpecification specification = new DefaultSharingServiceSpecification();
        Random random = new Random(randomSeed);

        if (numberOfStations <= linkIds.size()) {
            Collections.shuffle(linkIds, random);
        }

        for (int stationIndex = 0; stationIndex < numberOfStations; stationIndex++) {
            Id<Link> linkId = numberOfStations <= linkIds.size() ? linkIds.get(stationIndex)
                    : linkIds.get(random.nextInt(linkIds.size()));
            Id<SharingStation> stationId = Id.create("station_" + stationIndex, SharingStation.class);

            specification.addStation(ImmutableSharingStationSpecification.newBuilder()
                    .id(stationId)
                    .linkId(linkId)
                    .capacity(parkingSpacesPerStation)
                    .build());

            for (int bikeIndex = 0; bikeIndex < bikesPerStation; bikeIndex++) {
                specification.addVehicle(ImmutableSharingVehicleSpecification.newBuilder()
                        .id(Id.create("bike_" + stationIndex + "_" + bikeIndex, SharingVehicle.class))
                        .startStationId(stationId)
                        .startLinkId(linkId)
                        .build());
            }
        }

        return specification;
    }

    private static int getPositiveInt(CommandLine cmd, String option) throws CommandLine.ConfigurationException {
        int value = Integer.parseInt(cmd.getOptionStrict(option));
        if (value <= 0) {
            throw new CommandLine.ConfigurationException(option + " must be larger than zero");
        }
        return value;
    }

    private static boolean hasHelpOption(String[] args) {
        for (String arg : args) {
            if (arg.equals("-h") || arg.equals("--help")) {
                return true;
            }
        }
        return false;
    }

}
