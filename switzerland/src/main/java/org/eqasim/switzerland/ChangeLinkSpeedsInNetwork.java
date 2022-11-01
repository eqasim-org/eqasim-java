package org.eqasim.switzerland;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ChangeLinkSpeedsInNetwork {

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
        networkReader.readFile(path+"zurich_network_allow_bike_80_gradient.xml");

        List<String> linkInfoList = new ArrayList<>();

        for (Link link : network.getLinks().values()) {
            double freespeed = link.getFreespeed();
            Set<String> modes = link.getAllowedModes();
            boolean hasOsmHighway = link.getAttributes().getAsMap().containsKey("osm:way:highway");
            String osmHighway = "";
            if (hasOsmHighway){
                osmHighway = link.getAttributes().getAttribute("osm:way:highway").toString();
            }
            boolean isImportantCarRoad = (osmHighway.contains("motorway"))|(osmHighway.contains("trunk"))|(osmHighway.contains("primary"))|(osmHighway.contains("secondary"));
            if (modes.contains("bike") && !isImportantCarRoad && (freespeed > 8.33334)) { //g/ tho, some links that car cannot go but bike can, e.g. with buses
                link.setFreespeed(8.33333);

                String linkInfo = link.getId().toString() + "," + osmHighway + "," + freespeed + "," + link.getLength();
                linkInfoList.add(linkInfo);
            }
        }
        NetworkWriter networkWriter = new NetworkWriter(network);
        networkWriter.write(path+"zurich_network_allow_bike_80_gradient_policy02_30.xml");

        try {
            FileWriter fw = new FileWriter(path+"p02_link_changes.csv");
            fw.write("link_id,osm_highway,original_freespeed,link_length");
            fw.write("\n");

            Iterator itr = linkInfoList.iterator();
            while(itr.hasNext()) {
                String element = (String) itr.next();
                fw.write(element);
                fw.write("\n");
            }
            // DONT FORGET TO FLUSH
            fw.flush();
            fw.close();}
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
