package org.eqasim.san_francisco.bike.network;

import org.eqasim.san_francisco.bike.reader.BikeInfo;
import org.eqasim.san_francisco.bike.reader.BikeInfoCSVReader;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;

import java.io.IOException;
import java.util.Map;

public class RunBikeNetworkFixer {
    public static void main(String[] args) throws CommandLine.ConfigurationException, IOException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("input-path", "bike-lane-csv", "output-path") //
                .build();

        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(cmd.getOptionStrict("input-path"));

        Map<Id<Link>, BikeInfo> bikeInfoMap = new BikeInfoCSVReader().read(cmd.getOptionStrict("bike-lane-csv"));

        new BikeNetworkFixer().addBikeLaneInfo(network, bikeInfoMap);

        new NetworkWriter(network).write(cmd.getOptionStrict("output-path"));

    }
}
