package org.eqasim.core.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.vehicles.Vehicle;

public class FlowMatricesCombine {
	
	public Map<Id<Vehicle>, ArrayList<Id<Link>>[] > leftLinks = new HashMap<>();
	public Map<Id<Vehicle>, ArrayList<Id<Link>>[] > enteredLinks = new HashMap<>();
	public Map<Id<Vehicle>, ArrayList<Id<Link> [] >[] > linkpairs = new HashMap<>();
	public Map<Id<Link>, Map<Id<Link>, double[]> > flowsonpairs = new HashMap<>();
	public int NUMBER_HOURS = 31;
	public double sample_rate = 0.1;
	
	public FlowMatricesCombine(String events_path, double sample) {
		this.sample_rate = sample;
		// Create an event object
		EventsManager events = EventsUtils.createEventsManager();

		// Create the handler and add it
		FlowMatricesLinkLeaveEventHandler fmll = new FlowMatricesLinkLeaveEventHandler();
		FlowMatricesLinkEnterEventHandler fmel = new FlowMatricesLinkEnterEventHandler();
		events.addHandler(fmll);
		events.addHandler(fmel);
				
		// Create the reader and read the file
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(events_path);
		this.leftLinks = fmll.leftLinks;
		this.enteredLinks = fmel.enteredLinks;
		
		this.convertToLinkPairs();
		this.computeFlowsOnPairs();
	}
	
	@SuppressWarnings("unchecked")
	public void convertToLinkPairs() {
		Set<Id<Vehicle>> all_veh_id = this.enteredLinks.keySet();
		for (Id<Vehicle> idveh : all_veh_id) {
			ArrayList<Id<Link>>[] left = this.leftLinks.get(idveh);
			ArrayList<Id<Link>>[] entered = this.enteredLinks.get(idveh);
			ArrayList<Id<Link> [] >[] linkpairstab = new ArrayList[this.NUMBER_HOURS];
			
			for (int i = 0; i < NUMBER_HOURS; i++) { 
				linkpairstab[i] = new ArrayList<Id<Link>[]>(); 
	        } 
			
			for (int h = 0; h < NUMBER_HOURS ; h ++) {
				ArrayList<Id<Link>> l = left[h];
				ArrayList<Id<Link>> e = entered[h];
				
				for (int j = 0; j < l.size(); j++) {
					Id<Link>[] smalltab = new Id[2];
					smalltab[0] = l.get(j);
					smalltab[1] = e.get(j);
					linkpairstab[h].add(smalltab);
				}
				
			}
			
			this.linkpairs.put(idveh, linkpairstab);
			
		}	
	}
	

	
	public void computeFlowsOnPairs() {
		Set<Id<Vehicle>> all_veh_id = this.linkpairs.keySet();
		for (Id<Vehicle> vehid : all_veh_id) {
			ArrayList<Id<Link> [] >[] pairsbyhour = this.linkpairs.get(vehid);
			
			for (int h = 0; h < this.NUMBER_HOURS; h ++) {
				ArrayList<Id<Link> [] > pairstravelled = pairsbyhour[h];
				
				for (int i = 0; i < pairstravelled.size(); i++) {
					Id<Link> [] pair = pairstravelled.get(i);
					Id<Link> start = pair[0];
					Id<Link> end = pair[1];
					
					if (!this.flowsonpairs.containsKey(start)) {
						Map<Id<Link>, double[]> tabtoadd = new HashMap<Id<Link>, double[]>();
						this.flowsonpairs.put(start, tabtoadd);
					}
					
					Map<Id<Link>, double[]> tabtomodify = this.flowsonpairs.get(start);
					if (! tabtomodify.containsKey(end)) {
						tabtomodify.put(end, new double[this.NUMBER_HOURS]);
					}
					
					tabtomodify.get(end)[h] += (1 / this.sample_rate);
					this.flowsonpairs.put(start, tabtomodify);					
				}
				
			}
		}
	}
	
	
	public static void main(String[] args) {
		@SuppressWarnings("unused")
		String path_to_events = "/home/asallard/Dokumente/Projects/Traffic lights - Zuerich/Simulation results/60it_noTL/ITERS/it.60/60.events.xml.gz";
		FlowMatricesCombine fmc = new FlowMatricesCombine(path_to_events, 0.1);
		fmc.convertToLinkPairs();
		fmc.computeFlowsOnPairs();
		
		Set<Id<Link>> flowmap = fmc.flowsonpairs.keySet();
		
		for (Id<Link> idl : flowmap) {
			
			if (idl == Id.createLinkId("493601")){
				Map<Id<Link>, double[]> flowsonlink = fmc.flowsonpairs.get(idl);
				Set<Id<Link>> destinations = flowsonlink.keySet();
				
				for (int h = 0; h< fmc.NUMBER_HOURS; h++) {
					System.out.println("----------------------------------");
					System.out.println("Hour: " + h);
					for (Id<Link> d: destinations) {
						System.out.print(flowsonlink.get(d)[h] + ", ");
					}
				}
			}
		}
		
		System.out.println("Done!");
		
	}

}
