package org.eqasim.switzerland;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class InsertBikeModeInNetwork {

    public static void main(String[] args) throws IOException {

        // create empty config
        Config config = ConfigUtils.createConfig();

        // create scenario from config
        Scenario scenario = ScenarioUtils.createScenario(config);

        // get network from scenario
        Network network = scenario.getNetwork();

        // instantiate and use network reader to read network file
        MatsimNetworkReader networkReader = new MatsimNetworkReader(network);

        // read path from configurations
        String path = args[0];
        networkReader.readFile(path+"zurich_network.xml");

        for (Link link : network.getLinks().values()) {
            double freespeed = link.getFreespeed();
            Set<String> modes = link.getAllowedModes();
            List<String> updatedModes = new ArrayList<>(modes);
            if (modes.contains("car") && freespeed <= 22.2223) { //g/ tho, some links that car cannot go but bike can, e.g. with buses
                updatedModes.add("bike");
                link.setAllowedModes(Set.copyOf(updatedModes));
            }
        }
        NetworkWriter networkWriter = new NetworkWriter(network);
        networkWriter.write(path+"zurich_network_allow_bike_80.xml");
    }
}
