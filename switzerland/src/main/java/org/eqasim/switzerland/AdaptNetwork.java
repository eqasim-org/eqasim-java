package org.eqasim.switzerland;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class AdaptNetwork {
	
	public static Map<Id<Link>, Double> parseCSV(String filepath) {
		Map<Id<Link>, List<Double>> tabs = new HashMap<Id<Link>, List<Double>>();
    	Map<Id<Link>, Double> capacities = new HashMap<Id<Link>, Double>();
        String line = "";
        String cvsSplitBy = ",";

        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
        	int cpt = 0;

            while ((line = br.readLine()) != null) {
                String[] currentLine = line.split(cvsSplitBy);

                if (cpt > 0) {
					Id<Link> lid = Id.createLinkId(currentLine[0]);
					double cap = Double.parseDouble(currentLine[5]);
					if (!tabs.containsKey(lid)) {
						tabs.put(lid, new ArrayList<Double>());
					} 
					List<Double> l = tabs.get(lid);
					l.add(cap);
					tabs.put(lid, l);
				}
				cpt += 1;

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        
        Iterator<Entry<Id<Link>, List<Double>>> it = tabs.entrySet().iterator();
        while (it.hasNext()) {
			Map.Entry<Id<Link>, List<Double>> pair = (Map.Entry<Id<Link>, List<Double>>)it.next();
        	Id<Link> idlink = pair.getKey();
        	List<Double> l = pair.getValue();
        	double total = 0.0;
        	for (double d : l) {
        		total += d;
        	}
        	double mean = total / l.size();
        	capacities.put(idlink, mean);
        	it.remove();
        }
        
        return capacities;
    }
	
	public static void read_capacities_from_CSV(String filepath, Scenario scenario) {
		Map<Id<Link>, Double> capacities = parseCSV(filepath);
		
		Network net = scenario.getNetwork();
		Map<Id<Link>, ? extends Link> links = net.getLinks();
		
		for (Id<Link> key : links.keySet()) {
			    Link l = links.get(key);
			    //if (capacities.containsKey(key)) {
			    if (true) {
			    	System.out.println("Before" + l.getCapacity());
					l.setCapacity(1800.0 * l.getNumberOfLanes());
					System.out.println("After" + l.getCapacity());
				}
			}
	}
	
	public AdaptNetwork(String networkpath, String csvpath, String outputpath) {
		Config config = ConfigUtils.createConfig();
		
		Scenario scenario = ScenarioUtils.createMutableScenario(config);
		
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario.getNetwork());
		
		netReader.readFile(networkpath);
		read_capacities_from_CSV(csvpath, scenario);
		NetworkWriter netWriter = new NetworkWriter(scenario.getNetwork());
		netWriter.writeV2(outputpath);
	}
	
	public static void main(String[] args) {
		String networkpath = args[0];
		String csvpath = args[1];
		String outputpath = args[2];
		
		@SuppressWarnings("unused")
		AdaptNetwork an = new AdaptNetwork(networkpath,csvpath, outputpath);
	}

}
