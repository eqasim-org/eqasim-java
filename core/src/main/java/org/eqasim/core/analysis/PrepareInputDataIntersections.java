package org.eqasim.core.analysis;

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
	Map<Id<Link>, Double> capacities = new HashMap<>();	
	double samplesize;
	

	public PrepareInputDataIntersections () {

		// Path to configuration file.
		Config config = ConfigUtils.loadConfig("/home/asallard/Dokumente/Projects/Traffic lights - Zuerich/Zurich_10pct_Aurore/zurich_config.xml");
		config.controler().setLastIteration(5);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		String sample = scenario.getConfig().findParam("eqasim", "sampleSize");
		this.samplesize = Double.parseDouble(sample); 

		/** 1. Events **/ 
		// Path to the events file
		String eventFile = "/home/asallard/Dokumente/Projects/Traffic lights - Zuerich/Zurich_10pct_Aurore/40.events.xml.gz";
				
		// Create an event object
		EventsManager events = EventsUtils.createEventsManager();

		// Create the handler and add it
		HourlyVolumeEventHandler hv = new HourlyVolumeEventHandler();
		events.addHandler(hv);
		
        // Create the reader and read the file
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		hv.writeChart_AllLinks("/home/asallard/Dokumente/Projects/Traffic lights - Zuerich/output_link_");
		this.hourlyCounts = hv.hourlyCounts;
		
		/** 2. Intersections **/
		// Read the intersection file
		String intersectionFile = "/home/asallard/Dokumente/Projects/Traffic lights - Zuerich/Traffic_Light_output.xml";
		this.ir.read_xml(intersectionFile);
		
		/** 3. Capacities **/
		// Access the link capacities
		Network net = scenario.getNetwork();
		Map<Id<Link>, ? extends Link> links = net.getLinks();
		
		for (int k=0; k< ir.intersections.size(); k++ ) {
			Intersection current_intersection = ir.intersections.get(k);
			ArrayList<Id<Link>> incoming_links = current_intersection.incoming_links;
			
			for (int j=0; j<incoming_links.size(); j++) {
				Id<Link> current_link_id = incoming_links.get(j);
				if (! this.capacities.containsKey(current_link_id) ) {
					Link current_link = links.get(current_link_id);
					this.capacities.put(current_link_id, current_link.getCapacity());
				}
			}
		}
		
		/** Done! **/
		System.out.println("Events file read!");
	} 
	
	public static void main(String[] args) {
		PrepareInputDataIntersections prep = new PrepareInputDataIntersections();
		
	}
}
