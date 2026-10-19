package org.eqasim.core.tools;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.CommandLine;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CleanNetwork {

    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine commandLine = new CommandLine.Builder(args)
                .requireOptions("network-path", "output-path")
                .allowOptions("modes")
                .build();

        Network network = NetworkUtils.createNetwork();
        MatsimNetworkReader reader = new MatsimNetworkReader(network);
        reader.readFile(commandLine.getOptionStrict("network-path"));

        Collection<String> modes = commandLine.getOption("modes").map(m -> Arrays.stream(m.split(",")).collect(Collectors.toSet()))
                .orElse(network.getLinks().values().stream().map(Link::getAllowedModes).flatMap(Collection::stream).collect(Collectors.toSet()));

        // We need to do this for every mode because passing a collection of modes to NetworkUtils.cleanNetwork
        // Ensure the connection in the subnetwork where all links have at least one of the provided modes
        // But we want to have each subnetwork for each mode to be connected
        for(String mode: modes) {
            NetworkUtils.cleanNetwork(network, List.of(mode));
        }

        new NetworkWriter(network).write(commandLine.getOptionStrict("output-path"));
    }
}
