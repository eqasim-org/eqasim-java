package org.eqasim.switzerland;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ReadNetworkToCSV {
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

    List<String> linkInfoList = new ArrayList<>();

        for (Link link : network.getLinks().values()) {
            String linkId = link.getId().toString();
            Object osmWayId = link.getAttributes().getAttribute("osm:way:id");
            String osmWayIdString = "";
            if (osmWayId == null){
                osmWayIdString = "nan";
            }
            else{osmWayIdString = osmWayId.toString();}

            double fromNodeX = link.getFromNode().getCoord().getX();
            double fromNodeY = link.getFromNode().getCoord().getY();
            double toNodeX = link.getToNode().getCoord().getX();
            double toNodeY = link.getToNode().getCoord().getY();


            String linkInfo = linkId + "," + osmWayIdString + "," + fromNodeX + "," + fromNodeY + ","+ toNodeX + "," + toNodeY;
            linkInfoList.add(linkInfo);
        }

        try {
            FileWriter fw = new FileWriter(path+"zurich_network.csv");
            fw.write("matsim_link_id,osm_way_id,from_X,from_Y,to_X,to_Y");
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
    }}



