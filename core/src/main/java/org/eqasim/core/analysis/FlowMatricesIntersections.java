package org.eqasim.core.analysis;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class FlowMatricesIntersections {
	public Map<Id<Link>, Map<Id<Link>, double[]> > flowsonpairs = new HashMap<>();
	IntersectionsReader ir = new IntersectionsReader(true);
	double samplesize;
	
	public FlowMatricesIntersections(String events_path, double sample_rate) {
		// 1: Compute all flows
		FlowMatricesCombine fmc = new FlowMatricesCombine(events_path, sample_rate);
		Map<Id<Link>, Map<Id<Link>, double[]> > all_flows = fmc.flowsonpairs;
		
		// 2: Import intersections
		String intersectionFile = "/home/asallard/Dokumente/Projects/Traffic lights - Zuerich/Intersections/Zurich_intersections.xml";
		this.ir.read_xml(intersectionFile);
		
		// 3: Only select flows on intersections
		for (Map.Entry<Id<Link>, Map<Id<Link>, double[]>> entry : all_flows.entrySet()) {
			Id<Link> idl = entry.getKey();
			System.out.println(idl);
			for (int j = 0; j < ir.intersectionsFlows.size(); j ++) {
				IntersectionWithFlows iwf = this.ir.intersectionsFlows.get(j);
				if (iwf.incoming_links.contains(idl)) {
					iwf.addInFlow(idl, entry.getValue());
				}
			}
		}
		
		// 3.5 Fix the flows
		ArrayList<IntersectionWithFlows> iwfss = this.ir.intersectionsFlows;
		for (int i = 0; i < iwfss.size(); i++) {
			IntersectionWithFlows iwf = iwfss.get(i);
			for (int j = 0; j < iwf.incoming_links.size(); j++) {
				for (int k = 0; k < iwf.outgoing_links.size(); k++) {
					if (!iwf.flows.containsKey(iwf.incoming_links.get(j))) {
						iwf.flows.put(iwf.incoming_links.get(j), new HashMap<Id<Link>, double[]>());
					}
					if (! iwf.flows.get(iwf.incoming_links.get(j)).containsKey(iwf.outgoing_links.get(k))) {
						double[] taab = new double[31];
						iwf.flows.get(iwf.incoming_links.get(j)).put(iwf.outgoing_links.get(k), taab);
					}
					for (int l = 0; l < 31; l++) {
						iwf.flows.get(iwf.incoming_links.get(j)).get(iwf.outgoing_links.get(k))[l] = 
								Math.max(iwf.flows.get(iwf.incoming_links.get(j)).get(iwf.outgoing_links.get(k))[l], 0);
					}
				}
			}
		}
		
		
		// 4. Check
		ArrayList<IntersectionWithFlows> iwfs = this.ir.intersectionsFlows;
		for (int i = 0; i < iwfs.size(); i++) {
			IntersectionWithFlows iwf = iwfs.get(i);
			
			System.out.println("------------------------------------");
			System.out.println("Node id: "+ iwf.intersection_id);
			System.out.print("Incoming links: ");
			for (int k = 0; k < iwf.incoming_links.size(); k++) {
				System.out.print(iwf.incoming_links.get(k) + ", ");
			}
			System.out.println(" ");
			System.out.print("Outgoing links: ");
			for (int k = 0; k < iwf.outgoing_links.size(); k++) {
				System.out.print(iwf.outgoing_links.get(k) + ", ");
			}
			System.out.println(" ");
			if (iwf.outgoing_links.size() > 0) {
				System.out.println(" ");
				System.out.print("Incoming flows at 8: ");
				for (int k = 0; k < iwf.incoming_links.size(); k++) {
					Id<Link> id = iwf.incoming_links.get(k);
					Map<Id<Link>, double[]> obs_flows = iwf.flows.get(id);
					for (Id<Link> out : obs_flows.keySet()) {
						System.out.print(out + ": " + obs_flows.get(out)[9] + " ");
					}
				}
				System.out.println(" ");
			}
		}
		
		// Path to configuration file.
		Config config = ConfigUtils.loadConfig("/home/asallard/Dokumente/Projects/Traffic lights - Zuerich/Zurich_10pct_Aurore/zurich_config.xml");
		//config.controler().setLastIteration(5);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		String sample = scenario.getConfig().findParam("eqasim", "sampleSize");
		this.samplesize = Double.parseDouble(sample); 
		
		// Access the link capacities
		Network net = scenario.getNetwork();
		Map<Id<Link>, ? extends Link> links = net.getLinks();
		System.out.println("Imputing velocities and capacities...");
		for (int k=0; k< ir.intersectionsFlows.size(); k++ ) {
			IntersectionWithFlows current_intersection = ir.intersectionsFlows.get(k);
			for (Id<Link> idlink : current_intersection.incoming_links) {
				Link l = links.get(idlink);
				current_intersection.capacities.put(idlink, l.getCapacity());
				current_intersection.lanes.put(idlink, l.getNumberOfLanes());
			}
			for (Id<Link> idlink : current_intersection.outgoing_links) {
				Link l = links.get(idlink);
				current_intersection.capacities.put(idlink, l.getCapacity());
				current_intersection.lanes.put(idlink, l.getNumberOfLanes());
			}
		}
	}
	
	public void proceed_all_NTS_intersections() {
		for (int k=0; k< ir.intersectionsFlows.size(); k++ ) {
			IntersectionWithFlows current_intersection = ir.intersectionsFlows.get(k);
			Map<Id<Link>, ArrayList<Double>> allcap = new HashMap<Id<Link>, ArrayList<Double>>(); 
			for (int h = 0 ; h < 31; h++) {
				Map<Id<Link>, Double> excap = current_intersection.tampere_algorithm(h);
				for (Id<Link> inl : excap.keySet()) {
					if (! allcap.containsKey(inl)) {
						ArrayList<Double> alvalues = new ArrayList<Double>();
						allcap.put(inl, alvalues);
					}
					ArrayList<Double> ald = allcap.get(inl);
					ald.add(excap.get(inl));
					allcap.put(inl, ald);
				}
			}
			for (Id<Link> inlink : current_intersection.incoming_links) {
				ArrayList<Double> caps = allcap.get(inlink);
				current_intersection.output_capacities.put(inlink, Collections.max(caps));
			}
			
		}
	}
	
	public void write_csv() {
		try {
			FileWriter csvWriter = new FileWriter("/home/asallard/Dokumente/Projects/Node_models/NTS_intersections.csv");
			csvWriter.append("LinkID");
			csvWriter.append(",");
			csvWriter.append("Previous capacity");
			csvWriter.append(",");
			csvWriter.append("Updated capacity");
			csvWriter.append("\n");
			
			for (int k = 0; k < this.ir.intersectionsFlows.size(); k++) {
				IntersectionWithFlows current_intersection = ir.intersectionsFlows.get(k);
				for (Id<Link> idlink : current_intersection.incoming_links) {
					double oldcap = current_intersection.capacities.get(idlink);
					double newcap = current_intersection.output_capacities.get(idlink);
					newcap = Math.max(newcap, 600.0);
					csvWriter.append(idlink.toString());
					csvWriter.append(",");
					csvWriter.append(((Double)oldcap).toString());
					csvWriter.append(",");
					csvWriter.append(((Double)newcap).toString());
					csvWriter.append("\n");
				}
				
			}
			csvWriter.flush();
			csvWriter.close();
		} catch (IOException e) {
		}
		
		
	}
	
	public static void main(String[] args) {
		String path_to_events = "/home/asallard/Dokumente/Projects/Traffic lights - Zuerich/Simulation results/Last try/CP3_Webster/ITERS/it.60/60.events.xml.gz";
		FlowMatricesIntersections fmi = new FlowMatricesIntersections(path_to_events, 0.1);
		fmi.proceed_all_NTS_intersections();
		fmi.write_csv();
	}

}
