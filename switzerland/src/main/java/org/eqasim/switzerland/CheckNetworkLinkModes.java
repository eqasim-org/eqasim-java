package org.eqasim.switzerland;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CheckNetworkLinkModes {

    public static void main(String[] args) throws IOException {

        // create empty config
        Config config = ConfigUtils.createConfig();

        // create scenario from config
        Scenario scenarioPreClean = ScenarioUtils.createScenario(config);

        // create scenario from config
        Scenario scenarioPostClean = ScenarioUtils.createScenario(config);

        // get network 1 from scenario
        Network networkPreClean = scenarioPreClean.getNetwork();

        // get network 2 from scenario
        Network networkPostClean = scenarioPostClean.getNetwork();

        // instantiate and use network reader 1 to read network file
        MatsimNetworkReader networkReaderPreClean = new MatsimNetworkReader(networkPreClean);

        // instantiate and use network reader 2 to read network file
        MatsimNetworkReader networkReaderPostClean = new MatsimNetworkReader(networkPostClean);

        // read path from configurations
        String path = args[0];

        // read in network file into the 2 Networks
        networkReaderPreClean.readFile(path+"zurich_network_allow_bike_80_gradient.xml");
        networkReaderPostClean.readFile(path+"zurich_network_allow_bike_80_gradient_connected.xml");

        // create counters of bike links original and after cleaning
        Integer countBikePreClean = 0;
        Integer countBikePostClean = 0;
        Integer countBikeCleaned = 0;

        Integer countCarPreClean = 0;
        Integer countCarPostClean = 0;
        Integer countCarCleaned = 0;


        for (Link link : networkPreClean.getLinks().values()) {
            Set<String> allowedModes = link.getAllowedModes();
            if (allowedModes.contains("bike")) {
                countBikePreClean += 1;
            }
            if (allowedModes.contains("car")) {
                countCarPreClean += 1;

            }
        }

        for (Link link : networkPostClean.getLinks().values()) {
            Set<String> allowedModes = link.getAllowedModes();
            if (allowedModes.contains("bike")) {
                countBikePostClean += 1;
            }
            if (allowedModes.contains("car")) {
                countCarPostClean += 1;
            }}


        System.out.println("number of links for bikes pre clean: "+ countBikePreClean);
        System.out.println("number of links for bikes post clean: "+ countBikePostClean);
//        System.out.println("number of links for bikes cleaned: "+ countBikeCleaned);

        System.out.println("number of links for car pre clean: "+ countCarPreClean);
        System.out.println("number of links for car post clean: "+ countCarPostClean);
//        System.out.println("number of links for car cleaned: "+ countCarCleaned);
    }

}

//        number of links for bikes pre clean: 53345
//        number of links for bikes post clean: 40612
//        number of links for car pre clean: 56445
//        number of links for car post clean: 56445

