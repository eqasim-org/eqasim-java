package org.eqasim.core.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

public class IntersectionWithFlows {

	public enum IntersectionTag {
	    TS,
	    NTS
	}
	
	Id<Node> intersection_id;
	ArrayList<Id<Link>> incoming_links;
	ArrayList<Id<Link>> outgoing_links;
	Map<Id<Link>, Map<Id<Link>, double[]>> flows;
	ArrayList<ArrayList<Id<Link>>> groups;
	IntersectionTag tag;
	Map<Id<Link>, Double > capacities = new HashMap<>();
	Map<Id<Link>, Double > lanes = new HashMap<>();
	Map<Id<Link>, Double > output_capacities = new HashMap<>();
	
	
	public IntersectionWithFlows(String id, ArrayList<Id<Link>> links,
			ArrayList<ArrayList<Id<Link>>> groups, String label) {
		this.intersection_id = Id.createNodeId(id);
		this.incoming_links = new ArrayList<Id<Link>>(); 
		this.outgoing_links = new ArrayList<Id<Link>>(); 
		this.groups = new ArrayList<ArrayList<Id<Link>>>();
		this.flows = new HashMap<Id<Link>, Map<Id<Link>, double[]>>();
		
		if (label == "TS") {
			this.tag = IntersectionTag.TS;
		}
		else if (label == "NTS") {
			this.tag = IntersectionTag.NTS;
		}
		else {
			System.err.println("Non valid type for intersection " + id);
		}
		
		for (int j=0; j< groups.size(); j++) {
			ArrayList<Id<Link>> current_group = groups.get(j);
			this.groups.add(current_group);
		}
		
		for (int j=0; j < links.size(); j++) {
			this.incoming_links.add(links.get(j));
		}
	}
	
	public void addInFlow(Id<Link> idl, Map<Id<Link>, double[]> flow_out) {
		this.flows.put(idl, flow_out);
		Set<Id<Link>> set_out_links = flow_out.keySet();
		
		for (Id<Link> o : set_out_links) {
			if (!this.outgoing_links.contains(o)) {
				this.outgoing_links.add(o);
			}
		}
	}
	
	public Map<Id<Link>, Double > tampere_algorithm(){
		Map<Id<Link>, Double > exact_cap = new HashMap<Id<Link>, Double >();
		Map<Id<Link>, Map<Id<Link>, Double>> input_demand = new HashMap<Id<Link>, Map<Id<Link>, Double>>();
		Map<Id<Link>, Double> entry_capacities = new HashMap<Id<Link>, Double>();
		Map<Id<Link>, Double> exit_capacities = new HashMap<Id<Link>, Double>();
		
		for (Id<Link> inlink : this.incoming_links) {
			entry_capacities.put(inlink, this.capacities.get(inlink));
		}
		
		for (Id<Link> outlink : this.outgoing_links) {
			exit_capacities.put(outlink, this.capacities.get(outlink));
		}
		
		Map<Id<Link>, Double> totals = new HashMap<Id<Link>, Double>();
		for (Id<Link> inlink : this.incoming_links) {
			Map<Id<Link>, Double> outtab = new HashMap<Id<Link>, Double>();
			double total = 0;
			for (Id<Link> outlink : this.outgoing_links) {
				double[] day_flows = this.flows.get(inlink).get(outlink);
				double total_link = 0;
				for (int n = 0; n < day_flows.length; n++) {
					total_link += day_flows[n];
					total += day_flows[n];
				}
				outtab.put(outlink, total);
			}
			input_demand.put(inlink, outtab);
			totals.put(inlink, total);
		}
		for (Id<Link> inlink : this.incoming_links) {
			Map<Id<Link>, Double> outtab = input_demand.get(inlink);
			for (Id<Link> outlink : outtab.keySet()) {
				double t = outtab.get(outlink);
				t = t / totals.get(inlink) * entry_capacities.get(inlink);
				outtab.put(outlink, t);
			}
			input_demand.put(inlink, outtab);
		}
		
		TampereUtils tu = new TampereUtils(input_demand, entry_capacities, exit_capacities);
		tu.solve_intersection();
		
		Map<Id<Link>, Map<Id<Link>, Double>> result = tu.Q_entry;
		for (Id<Link> inlink : result.keySet()) {
			Map<Id<Link>, Double> thislink = result.get(inlink);
			double vtl = thislink.values().stream().mapToDouble(Double::doubleValue).sum();
			exact_cap.put(inlink, vtl);
		}
		
		return exact_cap;
	}
	

}

