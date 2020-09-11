package org.eqasim.core.analysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

public class PrepareInputDataIntersections {
	
	IntersectionsReader ir = new IntersectionsReader();
	Map<Id<Link>, double[] > hourlyCounts = new HashMap<Id<Link>, double[] > ();
	Map<Id<Link>, double[]> capacities = new HashMap<>();	
	Map<Id<Link>, Double> velocities = new HashMap<>();	
	Map<Id<Link>, Double> lanes = new HashMap<>();	
	double samplesize;
	double crossingPenalty;
	
	public static Map<Id<Link>, double[]> parseCSV(String filepath) {
    	Map<Id<Link>, double[]> capacities = new HashMap<Id<Link>, double[]>();
        String line = "";
        String cvsSplitBy = ",";

        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
        	int cpt = 0;

            while ((line = br.readLine()) != null) {
                String[] currentLine = line.split(cvsSplitBy);

                if (cpt >0) {
					Id<Link> lid = Id.createLinkId(currentLine[0]);
					int hour = Integer.parseInt(currentLine[1]);
					if (!capacities.containsKey(lid)) {
						double[] tab = new double[31];
						tab[hour] = Double.parseDouble(currentLine[6]);
						capacities.put(lid, tab);
					} 
					else {
						double[] tab = capacities.get(lid);
						tab[hour] = Double.parseDouble(currentLine[6]);
						capacities.put(lid, tab);
					}
				}
				cpt += 1;

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return capacities;
    }
	
	public void read_capacities_from_CSV(String filepath, Scenario scenario) {
		Map<Id<Link>, double[]> capacities = parseCSV(filepath);
		
		Network net = scenario.getNetwork();
		Map<Id<Link>, ? extends Link> links = net.getLinks();
		
		for (Id<Link> key : links.keySet()) {
			    Link l = links.get(key);
			    if (capacities.containsKey(key)) {
			    	
					this.capacities.put(key, capacities.get(key));
				}
			}
	}
	

	@SuppressWarnings("deprecation")
	public PrepareInputDataIntersections () {

		// Path to configuration file.
		Config config = ConfigUtils.loadConfig("/home/asallard/Dokumente/Projects/Traffic lights - Zuerich/Paris/Log_events_configs/paris_config_CP0.xml");
		//config.controler().setLastIteration(5);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		String sample = scenario.getConfig().findParam("eqasim", "sampleSize");
		this.samplesize = Double.parseDouble(sample); 
		this.crossingPenalty = Double.parseDouble(scenario.getConfig().findParam("eqasim", "crossingPenalty"));

		/** 1. Events **/ 
		// Path to the events file
		String eventFile = "/home/asallard/Dokumente/Projects/Traffic lights - Zuerich/Paris/Log_events_configs/eventsCP0.xml.gz";
				
		// Create an event object
		EventsManager events = EventsUtils.createEventsManager();

		// Create the handler and add it
		HourlyVolumeEventHandler hv = new HourlyVolumeEventHandler();
		events.addHandler(hv);
		
        // Create the reader and read the file
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		this.hourlyCounts = hv.hourlyCounts;
		
		/** 2. Intersections **/
		// Read the intersection file
		String intersectionFile = "/home/asallard/Dokumente/Projects/Traffic lights - Zuerich/Paris/Paris_intersections.xml";
		this.ir.read_xml(intersectionFile);
		
		/** 3. Capacities and velocities **/
		// Access the link capacities
		Network net = scenario.getNetwork();
		Map<Id<Link>, ? extends Link> links = net.getLinks();
		
		System.out.println("Imputing velocities and capacities...");
		for (int k=0; k< ir.intersections.size(); k++ ) {
			Intersection current_intersection = ir.intersections.get(k);
			ArrayList<ArrayList<Id<Link>>> groups = current_intersection.groups;
			
			for (int j=0; j<groups.size(); j++) {
				ArrayList<Id<Link>> group = groups.get(j);
				for (int j2 = 0; j2 < group.size(); j2++) {
					Id<Link> cl = group.get(j2);
					if (! this.capacities.containsKey(cl) ) {
						Link current_link = links.get(cl);
						double[] tab = new double[31];
						for (int j3 = 0; j3 < tab.length; j3 ++) {
							tab[j3] = current_link.getCapacity();
						}
						this.capacities.put(cl, tab);
						this.velocities.put(cl, current_link.getFreespeed());
						this.lanes.put(cl, current_link.getNumberOfLanes());
					}
				
				}
			}
		}
		
		//read_capacities_from_CSV("/home/asallard/Dokumente/Projects/Traffic lights - Zuerich/Simulation results/TT/60it_webster/intersections_webster.csv", scenario);
		
		/** Done! **/
		System.out.println("Events file read!");
	} 
	
	public static void main(String[] args) {
		@SuppressWarnings("unused")
		PrepareInputDataIntersections prep = new PrepareInputDataIntersections();
		
	}
}
