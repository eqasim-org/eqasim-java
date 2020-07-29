package org.eqasim.core.analysis;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class ComputeDelayTrafficLights {
	
	IntersectionsReader ir = new IntersectionsReader();
	Map<Id<Link>, double[] > hourlyCounts = new HashMap<Id<Link>, double[] > ();
	Map<Id<Link>, double[]> capacities = new HashMap<>();	
	Map<Id<Link>, Double> velocities = new HashMap<>();	
	Map<Id<Link>, Double> lanes = new HashMap<>();	
	double samplesize;
	double crossingPenalty;
	final static double INTERSECTION_CAPACITY = 2000;
	final static double LOST_TIME_PER_LINK = 2.0;
	
	public ComputeDelayTrafficLights(PrepareInputDataIntersections p) {
		this.hourlyCounts = p.hourlyCounts;
		this.ir = p.ir;
		this.capacities = p.capacities;
		this.velocities = p.velocities;
		this.samplesize = p.samplesize;
		this.crossingPenalty = p.crossingPenalty;
		this.lanes = p.lanes;
	}
	
	public int get_numberoflinks(ArrayList<ArrayList<Id<Link>>> groups) {
		int gsize = 0;
		
		for (int g=0; g<groups.size(); g++) {
			ArrayList<Id<Link>> group = groups.get(g);
			
			gsize += group.size();
		}
		
		return gsize;
	}
		
	public double[] optimal_cycle_length(Intersection intersection, int hour ) {
		ArrayList<ArrayList<Id<Link>>> groups = intersection.groups;
		double  ratio = 0.0;
		
		int numberoflinks = this.get_numberoflinks(groups);
		
		double total_lost_time = LOST_TIME_PER_LINK * numberoflinks;
		
		for (int k=0; k<groups.size(); k++) {
			ArrayList<Id<Link>> group = groups.get(k);
			double flow = 0.0;
			double capacities = 0.0;
			for (int k2=0; k2<group.size(); k2++) {
				Id<Link> idlink = group.get(k2);
				double[] count = this.hourlyCounts.get(idlink);
				if (!( count == null)){
				    flow += (Math.max(5, count[hour]) / 0.1 ) / this.lanes.get(idlink);
				}
				else {
					flow += (5.0 / this.samplesize) / this.lanes.get(idlink);
				}
				capacities += 1800;
			}
		    ratio += flow  / capacities;
		}
		
		System.out.println(ratio );
		//double maxratio = Collections.max(ratio);
		
		double optimal_cycle_length = 0;
		if (ratio < 1) {
			optimal_cycle_length =  (1.5 * total_lost_time + 5) / (1 - ratio); 
		}
		else {
			int groupssize = this.get_numberoflinks(groups);
			if (groupssize <= 3) {
				optimal_cycle_length = 60;
			}
			else {
				optimal_cycle_length = 90;
			}
		}
		
		double cycleLength = Math.max(40, Math.min(120, optimal_cycle_length));
	    double[] cycle_and_lost = new double[2];
	    cycle_and_lost[0] = cycleLength;
	    cycle_and_lost[1] = total_lost_time;
	    
		
		return cycle_and_lost;
	}
	
	public void flows(String pathToCSV) {
		try {
			FileWriter csvWriter = new FileWriter(pathToCSV);
			
			csvWriter.append("intersection");
			for (int k= 0; k<31; k++) {
				csvWriter.append(",");
				csvWriter.append(String.valueOf(k));
			}	
			csvWriter.append("\n");
			
			for (int k= 0; k < this.ir.intersections.size(); k++) {
				Intersection ir = this.ir.intersections.get(k);
				csvWriter.append(String.valueOf(k));
				csvWriter.append(",");
				ArrayList<ArrayList<Id<Link>>> groups = ir.groups;
				
				for (int h = 0; h< 31; h++) {
					double flow = 0;
					
					for (int l = 0; l< groups.size(); l++) {
						ArrayList<Id<Link>> group = groups.get(l);
						
						for (int g = 0; g < group.size(); g++) {
							Id<Link> idl = group.get(g);
							double[] counts = this.hourlyCounts.get(idl);
							if (!(counts == null)) {
				    			flow += Math.max(5, counts[h]) / this.samplesize ;
						        
				    		}
							else{
								flow += 5 / this.samplesize;
							}
						}
					}
					csvWriter.append(String.valueOf(flow));
					csvWriter.append(",");
					
				}
				csvWriter.append("\n");
			}
			
			csvWriter.flush();
			csvWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	
	}
	
	public List<Map<Id<Link>, double[]>> compute_all_delays_webster(boolean change ){
		Map<Id<Link>, double[]> delays = new HashMap<Id<Link>, double[]>();
		Map<Id<Link>, double[]> opts = new HashMap<Id<Link>, double[]>();
		Map<Id<Link>, double[]> greens = new HashMap<Id<Link>, double[]>();
		Map<Id<Link>, double[]> caps = new HashMap<Id<Link>, double[]>();
		Map<Id<Link>, double[]> linkflows = new HashMap<Id<Link>, double[]>();
		
		
		for (int k=0; k<this.ir.intersections.size(); k++) {
			Intersection intersection = this.ir.intersections.get(k);
			ArrayList<ArrayList<Id<Link>>> groups = intersection.groups;
			
			for (int hour=0; hour<31; hour++) {
				
				double[] data = optimal_cycle_length(intersection,hour);
				double opt_cycle_length = data[0];
				double lost_time = data[1];
				double eff_green_time = opt_cycle_length - lost_time;
				
			    double total_flow = 0;
			    double[] flowsongroups = new double[groups.size()];			    
			    Map<Id<Link>, Double> flowonlinks = new HashMap<Id<Link>, Double>();
			
			    for (int l=0; l<groups.size(); l++) {
			    	
			    	ArrayList<Id<Link>> group = groups.get(l);
			    	double groupflow = 0;
			    	
			    	for (int g=0; g<group.size(); g++) {
			    		Id<Link> idl = group.get(g);
			    		double[] counts = this.hourlyCounts.get(idl);
			    		double flow = 5 / this.samplesize;
			    		if (!(counts == null)) {
			    			flow = Math.max(5, counts[hour]) / this.samplesize ;
					        
			    		}
			    		total_flow += flow;
				        groupflow += flow;
				    	flowonlinks.put(idl, flow);
			    	}
			    	flowsongroups[l] = groupflow;
			    }
			    
			    for (int l=0; l<groups.size(); l++) {
			    	System.out.println("--------------");
			    	ArrayList<Id<Link>> group = groups.get(l);
			    	
			    	double group_flow = flowsongroups[l];
			    	double green_time = eff_green_time * group_flow / total_flow;
			    	double green_ratio = green_time / opt_cycle_length;
			    	
			    	for (int g=0; g<group.size(); g++) {			    		
			    		Id<Link> idlink = group.get(g);
			    		double link_flow = flowonlinks.get(idlink);
			    		double link_capacity = Math.max(200, INTERSECTION_CAPACITY * green_ratio) * this.lanes.get(idlink);
			    		if ( change) {
			    			link_capacity = Math.max(200, INTERSECTION_CAPACITY * green_ratio) * this.lanes.get(idlink);
			    			double[] tab = this.capacities.get(idlink);
			    			tab[hour] = link_capacity;
			    			this.capacities.put(idlink, tab);
			    		}
			    		double saturation = (link_flow / this.lanes.get(idlink)) / INTERSECTION_CAPACITY;
			    		//double saturation = link_flow / 1800;
			    		double delay1 = (opt_cycle_length * Math.pow(1 - green_ratio, 2))/(2 * (1 - green_ratio * saturation));
				    	double delay2 = Math.pow(saturation, 2) / (2 * (link_flow/3600) * (1 - saturation));
				    	double delay3 = 0.65 * Math.pow(opt_cycle_length / Math.pow((link_flow/3600), 2), 1/3) * Math.pow(saturation, 2 + 5 * green_ratio);
			    		
				    	double delay = Math.max(delay1 + delay2 - delay3, this.crossingPenalty );
				    	if (delay>100) {
				    		System.out.println("Alert " + delay1 + " " + delay2 + " " + delay3);
				    	}
				    	/*System.out.println("--------------");
				    	System.out.println("Green ratio " + green_ratio);
				    	System.out.println("Opt. length " + opt_cycle_length);
				    	System.out.println("capacity" + this.capacities.get(idlink)[hour]);
				    	System.out.println("flow" + link_flow);
				    	System.out.println( delay1 + " " + delay2 + " " + delay3);*/
				    	
				    	
				    	if (hour % 24 <= 4 || hour % 24 >= 23) {
				    		delay = this.crossingPenalty;
				    	}
				    		
				    	
				    	if (!delays.containsKey(idlink)) {
				    		delays.put(idlink, new double[31]);
				    		delays.get(idlink)[hour] = delay;
				    	}
				    	else {
				    		delays.get(idlink)[hour] = delay;
				    	}
				    	
				    	if (!opts.containsKey(idlink)) {
				    		opts.put(idlink, new double[31]);
				    		opts.get(idlink)[hour] = opt_cycle_length;
				    	}
				    	else {
				    		opts.get(idlink)[hour] = opt_cycle_length;
				    	}
				    	
				    	if (!greens.containsKey(idlink)) {
				    		greens.put(idlink, new double[31]);
				    		greens.get(idlink)[hour] = green_time;
				    	}
				    	else {
				    		greens.get(idlink)[hour] = green_time;
				    	}
				    	if (!caps.containsKey(idlink)) {
				    		caps.put(idlink, new double[31]);
				    		caps.get(idlink)[hour] = link_capacity;
				    	}
				    	else {
				    		caps.get(idlink)[hour] = link_capacity;
				    	}
				    	
				    	
				    	if (!linkflows.containsKey(idlink)) {
				    		linkflows.put(idlink, new double[31]);
				    		linkflows.get(idlink)[hour] = link_flow;
				    	}
				    	else {
				    		linkflows.get(idlink)[hour] = link_flow;
				    	}
			    	}
			    	
			    }
			}
		}
		
		List<Map<Id<Link>, double[]>> ret = new ArrayList<Map<Id<Link>, double[]>>();
		ret.add(0, delays);
		ret.add(1, greens);
		ret.add(2, opts);
		ret.add(3, caps);
		ret.add(4, linkflows);
		
		return ret;
	}
	
	public List<Map<Id<Link>, double[]>> compute_all_delays_heuristic(){
		Map<Id<Link>, double[]> delays = new HashMap<Id<Link>, double[]>();
		Map<Id<Link>, double[]> opts = new HashMap<Id<Link>, double[]>();
		Map<Id<Link>, double[]> greens = new HashMap<Id<Link>, double[]>();
		Map<Id<Link>, double[]> caps = new HashMap<Id<Link>, double[]>();
		Map<Id<Link>, double[]> linkflows = new HashMap<Id<Link>, double[]>();
		
		
		for (int k=0; k<this.ir.intersections.size(); k++) {
			Intersection intersection = this.ir.intersections.get(k);
			ArrayList<ArrayList<Id<Link>>> groups = intersection.groups;
		    
		    int nblinks = this.get_numberoflinks(groups);
		    double cycle_length = 90;
		    if (nblinks <= 3) {
		    	cycle_length = 60;
		    }
			
			for (int hour=0; hour<31; hour++) {
				
			    double[] flowsongroups = new double[groups.size()];			    
			    Map<Id<Link>, Double> flowonlinks = new HashMap<Id<Link>, Double>();
			    
			    double total_speed = 0;
			    double[] speedsongroups = new double[groups.size()];			    
			    Map<Id<Link>, Double> speedonlinks = new HashMap<Id<Link>, Double>();
			    double total_flow = 0;
			    for (int l=0; l<groups.size(); l++) {
			    	
			    	ArrayList<Id<Link>> group = groups.get(l);
			    	double groupflow = 0;
			    	double groupspeed = 0;
			    	
			    	for (int g=0; g<group.size(); g++) {
			    		Id<Link> idl = group.get(g);
			    		double[] counts = this.hourlyCounts.get(idl);
			    		double flow = 5 / this.samplesize;
			    		double speed = this.velocities.get(idl);
			    		if (!(counts == null)) {
			    			flow = Math.max(5, counts[hour]) / this.samplesize ;
					        
			    		}
				        groupflow += flow;
				        total_flow += flow;
				    	flowonlinks.put(idl, flow);
				    	total_speed += speed;
				        groupspeed += speed;
				    	speedonlinks.put(idl, speed);
			    	}
			    	flowsongroups[l] = groupflow;
			    	speedsongroups[l] = groupspeed;
			    }
			    
			    for (int l=0; l<groups.size(); l++) {
			    	ArrayList<Id<Link>> group = groups.get(l);
			    	
			    	//double group_speed = speedsongroups[l];
			    	double green_time = cycle_length * 0.9 * flowsongroups[l] / total_flow;
			    	double green_ratio = green_time / cycle_length;
			    	
			    	for (int g=0; g<group.size(); g++) {			    		
			    		Id<Link> idlink = group.get(g);
			    		System.out.println(idlink + " " + l +", "+ green_ratio);
			    		double link_flow = flowonlinks.get(idlink);
			    		double link_capacity = INTERSECTION_CAPACITY * green_ratio / group.size() * this.lanes.get(idlink);
			    		double[] tab = this.capacities.get(idlink);
			    		tab[hour] = link_capacity;
			    		this.capacities.put(idlink, tab);
			    		double saturation = Math.min(link_flow / link_capacity, 0.99);
			    		
			    		double delay1 = (cycle_length * Math.pow(1 - green_ratio, 2))/(2 * (1 - green_ratio * saturation));
				    	double delay2 = Math.pow(saturation, 2) / (2 * link_flow * (1 - saturation));
				    	double delay3 = 0.65 * Math.pow(cycle_length / Math.pow(link_flow, 2), 1/3) * Math.pow(saturation, 2 + 5 * green_ratio);
			    		
				    	double delay = Math.max(delay1 + delay2 + delay3, this.crossingPenalty );
				    	if (delay>100) {
				    		System.out.println("Alert " + delay1 + " " + delay2 + " " + delay3);
				    	}
				    	
				    	if (hour % 24 <= 4 || hour % 24 >= 23) {
				    		delay = this.crossingPenalty;
				    	}
				    		
				    	
				    	if (!delays.containsKey(idlink)) {
				    		delays.put(idlink, new double[31]);
				    		delays.get(idlink)[hour] = delay;
				    	}
				    	else {
				    		delays.get(idlink)[hour] = delay;
				    	}
				    	
				    	if (!opts.containsKey(idlink)) {
				    		opts.put(idlink, new double[31]);
				    		opts.get(idlink)[hour] = cycle_length;
				    	}
				    	else {
				    		opts.get(idlink)[hour] = cycle_length;
				    	}
				    	
				    	if (!greens.containsKey(idlink)) {
				    		greens.put(idlink, new double[31]);
				    		greens.get(idlink)[hour] = green_time;
				    	}
				    	else {
				    		greens.get(idlink)[hour] = green_time;
				    	}
				    	
				    	if (!caps.containsKey(idlink)) {
				    		caps.put(idlink, new double[31]);
				    		caps.get(idlink)[hour] = link_capacity;
				    	}
				    	else {
				    		caps.get(idlink)[hour] = link_capacity;
				    	}
				    	
				    	
				    	if (!linkflows.containsKey(idlink)) {
				    		linkflows.put(idlink, new double[31]);
				    		linkflows.get(idlink)[hour] = link_flow;
				    	}
				    	else {
				    		linkflows.get(idlink)[hour] = link_flow;
				    	}
			    	}
			    	
			    }
			}
		}
		
		List<Map<Id<Link>, double[]>> ret = new ArrayList<Map<Id<Link>, double[]>>();
		ret.add(0, delays);
		ret.add(1, greens);
		ret.add(2, opts);
		ret.add(3, caps);
		ret.add(4, linkflows);
		
		return ret;
	}
	
	
	public void writeCSV_webster(String pathToCSV) {
		try {
			FileWriter csvWriter = new FileWriter(pathToCSV);
			List<Map<Id<Link>, double[]>> results = this.compute_all_delays_webster(false);
			Map<Id<Link>, double[]> delays = results.get(0);
			Map<Id<Link>, double[]> greens = results.get(1);
			Map<Id<Link>, double[]> opts = results.get(2);
			Map<Id<Link>, double[]> caps = results.get(3);
			Map<Id<Link>, double[]> flows = results.get(4);
			
			csvWriter.append("Link_ID");
			csvWriter.append(",");
			csvWriter.append("Hour");
			csvWriter.append(",");
			csvWriter.append("Delay");
			csvWriter.append(",");
			csvWriter.append("Optimal Length");
			csvWriter.append(",");
			csvWriter.append("Green time");
			csvWriter.append(",");
			csvWriter.append("Capacity");
			csvWriter.append(",");
			csvWriter.append("Flow");
			csvWriter.append("\n");
			
			for (Entry<Id<Link>, double[]> entry : delays.entrySet()) {
				Id<Link> idLink = entry.getKey();
				double[] delays_link = entry.getValue();
				double[] opt_link = opts.get(idLink);
				double[] green_link = greens.get(idLink);
				double[] flow_link = flows.get(idLink);
				double[] cap_link = caps.get(idLink);
				
				for (int l = 0; l<delays_link.length; l++) {
					csvWriter.append(idLink.toString());
					csvWriter.append(",");
					csvWriter.append(Integer.toString(l));
					csvWriter.append(",");
					double d = delays_link[l];
					double o = opt_link[l];
					double g = green_link[l];
					double f = flow_link[l];
					double c = cap_link[l];
					if (d==0) {
						System.out.println("Alert 0 " + l);
					}
					csvWriter.append(Double.toString(d));
					csvWriter.append(",");
					csvWriter.append(Double.toString(o));
					csvWriter.append(",");
					csvWriter.append(Double.toString(g));
					csvWriter.append(",");
					csvWriter.append(Double.toString(c));
					csvWriter.append(",");
					csvWriter.append(Double.toString(f));
					csvWriter.append("\n");
				}
			}
			
			csvWriter.flush();
			csvWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void writeCSV_heuristic(String pathToCSV) {
		try {
			FileWriter csvWriter = new FileWriter(pathToCSV);
			List<Map<Id<Link>, double[]>> results = this.compute_all_delays_heuristic();
			Map<Id<Link>, double[]> delays = results.get(0);
			Map<Id<Link>, double[]> greens = results.get(1);
			Map<Id<Link>, double[]> opts = results.get(2);
			Map<Id<Link>, double[]> caps = results.get(3);
			Map<Id<Link>, double[]> flows = results.get(4);
			
			csvWriter.append("Link_ID");
			csvWriter.append(",");
			csvWriter.append("Hour");
			csvWriter.append(",");
			csvWriter.append("Delay");
			csvWriter.append(",");
			csvWriter.append("Optimal Length");
			csvWriter.append(",");
			csvWriter.append("Green time");
			csvWriter.append(",");
			csvWriter.append("Capacity");
			csvWriter.append(",");
			csvWriter.append("Flow");
			csvWriter.append("\n");
			
			for (Entry<Id<Link>, double[]> entry : delays.entrySet()) {
				Id<Link> idLink = entry.getKey();
				double[] delays_link = entry.getValue();
				double[] opt_link = opts.get(idLink);
				double[] green_link = greens.get(idLink);
				double[] flow_link = flows.get(idLink);
				double[] cap_link = caps.get(idLink);
				
				for (int l = 0; l<delays_link.length; l++) {
					csvWriter.append(idLink.toString());
					csvWriter.append(",");
					csvWriter.append(Integer.toString(l));
					csvWriter.append(",");
					double d = delays_link[l];
					double o = opt_link[l];
					double g = green_link[l];
					double f = flow_link[l];
					double c = cap_link[l];
					if (d==0) {
						System.out.println("Alert 0 " + l);
					}
					csvWriter.append(Double.toString(d));
					csvWriter.append(",");
					csvWriter.append(Double.toString(o));
					csvWriter.append(",");
					csvWriter.append(Double.toString(g));
					csvWriter.append(",");
					csvWriter.append(Double.toString(c));
					csvWriter.append(",");
					csvWriter.append(Double.toString(f));
					csvWriter.append("\n");
				}
			}
			
			csvWriter.flush();
			csvWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void main(String[] args) {
		PrepareInputDataIntersections p = new PrepareInputDataIntersections();
		ComputeDelayTrafficLights delay = new ComputeDelayTrafficLights(p);
		//delay.flows("/home/asallard/Dokumente/Projects/Traffic lights - Zuerich/flowsintersection");
		delay.writeCSV_webster("/home/asallard/Dokumente/Projects/Traffic lights - Zuerich/Simulation results/TT/essais2/delays_opt_length.csv");
	}

}
