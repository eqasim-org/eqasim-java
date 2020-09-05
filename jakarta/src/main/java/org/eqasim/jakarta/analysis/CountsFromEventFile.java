package org.eqasim.jakarta.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;


public class CountsFromEventFile {
    public static void main(String[] args) throws IOException {
        String count_xml = args[0]; //path to observed count.xml
        String event_xml = args[1]; //path to output_event.xml.gz
        String matsim_count = args[2]; //output csv
        String networkfile = args[3];
        //Get the Ids from the observed count xml file and store in array
        Counts<Link> counts = new Counts<>();
        MatsimCountsReader countsReader = new MatsimCountsReader(counts );
        countsReader.readFile(count_xml);
        //create an empty array holding a list of Ids you want from the observed count data
        List<Id<Link>> countLocIds = new LinkedList<>();
        for(Count<Link> count: counts.getCounts().values()){
            countLocIds.add(count.getId());
        }
        //To get link freespeed type from network file
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        Network network = scenario.getNetwork();
        new MatsimNetworkReader(network).readFile(networkfile);
        // specify the csv format
        BufferedWriter writer = IOUtils.getBufferedWriter(matsim_count);
        writer.write("loc_id,vehicle_id,time, lancecapacity,freespeed\n");
        //read left link event to capture vehicles leaving a link and get the link id, vehicle and time
        EventsManager events    =   EventsUtils.createEventsManager();
        events.addHandler(new LinkLeaveEventHandler() {
            @Override
            public void handleEvent(LinkLeaveEvent event) {
                Id<Link> linkId = event.getLinkId();
                double time = event.getTime();
                String vehicleId = event.getVehicleId().toString();
                double freeSpeed = 0.0;
                double laneCapacity = 0.0;
                //only write out link ids that ate in observed count
                if (countLocIds.contains(linkId)){
                    Link link = network.getLinks().get(linkId);
                    freeSpeed = link.getFreespeed();
                    laneCapacity = link.getCapacity();
                    try {
                        writer.write(linkId.toString() + "," + vehicleId + ","
                                + time +"," +
                        		laneCapacity + "," +
                                freeSpeed + "\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        new MatsimEventsReader(events).readFile(event_xml);
        writer.flush();
        writer.close();
    }
}
