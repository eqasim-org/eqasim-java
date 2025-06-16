package org.eqasim.core.scenario.freeflow;

import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RunAdaptFreespeed {
    static public void main(String[] args)
            throws ConfigurationException, JacksonException, DatabindException, IOException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("input-path", "output-path", "configuration-path")
                .build();

        String inputPath = cmd.getOptionStrict("input-path");
        String outputPath = cmd.getOptionStrict("output-path");
        String settingsPath = cmd.getOptionStrict("settings-path");

        ObjectMapper objectMapper = new ObjectMapper();
        FreeflowConfiguration configuration = objectMapper.readValue(new File(settingsPath),
                FreeflowConfiguration.class);

        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(inputPath);

        FreeflowConfigurator.create(network).apply(network, configuration);

        new NetworkWriter(network).write(outputPath);
    }
}
