package org.eqasim.switzerland;


import org.locationtech.jts.geom.LineString;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.GeometryUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CheckNetworkConnected {

    public static void main(String[] args) throws IOException {

        // create empty config
        Config config = ConfigUtils.createConfig();

        // create scenario from config
        Scenario scenarioOriginal = ScenarioUtils.createScenario(config);

        // create scenario from config
        Scenario scenarioBike = ScenarioUtils.createScenario(config);

        // get network 1 from scenario
        Network networkOriginal = scenarioOriginal.getNetwork();

        // get network 2 from scenario
        Network networkBike = scenarioBike.getNetwork();

        // instantiate and use network reader 1 to read network file
        MatsimNetworkReader networkReaderOriginal = new MatsimNetworkReader(networkOriginal);

        // instantiate and use network reader 2 to read network file
        MatsimNetworkReader networkReaderBike = new MatsimNetworkReader(networkBike);

        // read path from configurations
        String path = args[0];

        // read in network file into the 2 Networks
        networkReaderBike.readFile(path+"zurich_network_allow_bike_80_gradient.xml");
        networkReaderOriginal.readFile(path+"zurich_network_allow_bike_80_gradient.xml");

        // Clean networkBike
        MultimodalNetworkCleaner networkCleaner = new MultimodalNetworkCleaner(networkBike);
        Set<String> modes = new HashSet<>();
        modes.add("bike");
        networkCleaner.run(modes);

        // create counters of bike links original and after cleaning
        Integer countBikeOriginal = 0;
        Integer countBikeCleaned = 0;

        // loop through all links in the original network
        for (Link link : networkOriginal.getLinks().values()) {
            // get allowed modes of link
            Set<String> allowedModes = link.getAllowedModes();
            // for links with bike
            if (allowedModes.contains("bike")) {
                // count as bike link
                countBikeOriginal += 1;
                // if the cleaned networkBike contains this bike link, continue
                if (networkBike.getLinks().containsKey(link.getId())) { //this is always true
                    continue;
                    // if it does not contain it, that means it was removed i.e. it is not a connected bike link
                } else {
                    // remove bike from allowed modes
                    List<String> updatedModes = new ArrayList<>(allowedModes);
                    updatedModes.remove("bike");
                    link.setAllowedModes(Set.copyOf(updatedModes));
                    // count how many times the link is removed
                    countBikeCleaned += 1;
                }
            }
            }
        System.out.println("number of links for bikes original: "+ countBikeOriginal);
        System.out.println("number of links for bikes cleaned: "+ countBikeCleaned);

        NetworkWriter networkWriter = new NetworkWriter(networkOriginal);
        networkWriter.write(path+"zurich_network_allow_bike_80_gradient_connected.xml");
    }

        }

// this script is wrong, the MultimodalNetworkCleaner removes the modes on the links, not the links themselves