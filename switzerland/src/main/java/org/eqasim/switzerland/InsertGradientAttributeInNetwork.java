package org.eqasim.switzerland;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class InsertGradientAttributeInNetwork {
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

        // read network
        networkReader.readFile(path+"zurich_network_allow_bike_80.xml");

        // read csv of gradients
        BufferedReader reader = new BufferedReader(new FileReader(path+"zurich_network_grad.csv"));

        // create a dict to contain link id and gradient
        Dictionary link2GradientDict = new Hashtable();

        // read the first line of csv (headers)
        reader.readLine();
        String s = reader.readLine();
        while (s != null) {
            // read into string array, separated by comma
            String[] arr = s.split(",");
            // define id
            String linkId = arr[1];
            // define gradient
            double gradient = Double.parseDouble(arr[8]);
            link2GradientDict.put(linkId,gradient);
            s = reader.readLine();} // this is to move the reader to the next line of the csv
        System.out.println(link2GradientDict.size());

        for (Link link : network.getLinks().values()) {
            String linkId = link.getId().toString();
            if (((Hashtable) link2GradientDict).containsKey(linkId)){
                Double gradient = (Double) link2GradientDict.get(linkId);
            link.getAttributes().putAttribute("gradient",gradient);
        }
            else{
                continue;
            }
        }
        NetworkWriter networkWriter = new NetworkWriter(network);
        networkWriter.write(path+"zurich_network_allow_bike_80_gradient.xml");
            }
}




