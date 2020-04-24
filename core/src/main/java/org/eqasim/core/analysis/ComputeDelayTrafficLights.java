package org.eqasim.core.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class ComputeDelayTrafficLights {
	
	IntersectionsReader ir = new IntersectionsReader();
	Map<Id<Link>, double[] > hourlyCounts = new HashMap<Id<Link>, double[] > ();
	Map<Id<Link>, Double> capacities = new HashMap<>();	
	double samplesize;
	
	public ComputeDelayTrafficLights(PrepareInputDataIntersections p) {
		this.hourlyCounts = p.hourlyCounts;
		this.ir = p.ir;
		this.capacities = p.capacities;
		this.samplesize = p.samplesize;
	}
	
	final static double LOST_TIME_PER_LINK = 2.0;
	
	public void resizeCounts() {
		Map<Id<Link>, double[] > hourlyCounts = this.hourlyCounts;
		Map<Id<Link>, double[] > newhourlyCounts = new HashMap<Id<Link>, double[] >();
		
		Iterator<Entry<Id<Link>, double[]>> it = hourlyCounts.entrySet().iterator();
	    while (it.hasNext()) {
	    	Entry<Id<Link>, double[]> pair = it.next();
	    	double[] counts = pair.getValue();
	    	Id<Link> idlink = pair.getKey();
	        
	    	double[] newcounts = new double[counts.length];
	    	for (int k=0; k<counts.length; k++) {
	    		newcounts[k] = counts[k] / this.samplesize;
	    	}
	    	
	    	newhourlyCounts.put(idlink, newcounts);
	        it.remove(); // avoids a ConcurrentModificationException
	    }
	    
	    this.hourlyCounts = newhourlyCounts;
	}
	
	public double optimal_effective_green_time(Intersection intersection, int hour) {
		
		ArrayList<Id<Link>> links = intersection.incoming_links;
		ArrayList<Double>  ratio = new ArrayList<Double>();
		
		double total_lost_time = LOST_TIME_PER_LINK * links.size();
		
		for (int k=0; k<links.size(); k++) {
			Id<Link> idlink = links.get(k);
			double[] count = this.hourlyCounts.get(idlink);
			double flow = 0.0;
			if (!( count == null)){
			    flow = Math.max(5, count[hour]);
			    
			}
			else {
				flow = 5;
			}
			double capacity = this.capacities.get(links.get(k));
		    ratio.add(flow / capacity);
		}
		
		double maxratio = Collections.max(ratio);
		
		double optimal_cycle_length = 0;
		if (maxratio < 1) {
			optimal_cycle_length =  (1.5 * total_lost_time + 5) / (1 - maxratio); 
		}
		else {
			optimal_cycle_length = 60;
		}
		
		return Math.max(20, Math.min(120, optimal_cycle_length - total_lost_time));
	}
	
	public Map<Id<Link>, double[]> compute_all_delays(){
		Map<Id<Link>, double[]> delays = new HashMap<Id<Link>, double[]>();
		
		this.resizeCounts();
		
		for (int k=0; k<this.ir.intersections.size(); k++) {
			Intersection intersection = this.ir.intersections.get(k);
			
			for (int hour=0; hour<30; hour++) {
				double optimal_length = optimal_effective_green_time(intersection,hour);
			
			    ArrayList<Id<Link>> links = intersection.incoming_links;
			    double total_flow = 0;
			    double[] flows = new double[links.size()];
			
			    for (int l=0; l<links.size(); l++) {
			    	double[] counts = this.hourlyCounts.get(links.get(l));
			    	if (!(counts == null)) {
				        double flow = Math.max(5, counts[hour]);
				        flows[l] = flow;
				        total_flow += flow;
			    	}
			    	else {
			    		flows[l] = 5;
				        total_flow += 5;
			    	}
			    }
			    
			    for (int l=0; l<links.size(); l++) {
			    	Id<Link> idlink = links.get(l);
			    	double link_flow = flows[l];
			    	double green_time = Math.max(4, optimal_length * link_flow / total_flow);
			    	double capacity = this.capacities.get(idlink);
			    	double saturation = link_flow / capacity;
			    	double green_ratio = green_time / optimal_length;
			    	
			    	double delay1 = (optimal_length * Math.pow(1 - green_ratio, 2))/(2 * (1 - green_ratio * saturation));

			    	double delay2 = Math.pow(saturation, 2) / (2 * link_flow * (1 - saturation));
			    	double delay3 = 0.65 * Math.pow(optimal_length / Math.pow(link_flow, 2), 1/3) * Math.pow(saturation,2 + 5 * green_ratio);

			    	
			    	double delay = Math.min(20, delay1 + delay2 + delay3);
			    	System.out.println(delay);
			    	
			    	if (!delays.containsKey(idlink)) {
			    		delays.put(idlink, new double[30]);
			    	}
			    	else {
			    		delays.get(idlink)[hour] = delay;
			    	}
			    }
			}
		}
		
		return delays;
	}
	
	public static void main(String[] args) {
		PrepareInputDataIntersections p = new PrepareInputDataIntersections();
		ComputeDelayTrafficLights delay = new ComputeDelayTrafficLights(p);
		delay.compute_all_delays();
	}

}
