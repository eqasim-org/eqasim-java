package org.eqasim.core.analysis;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

public class Intersection {
	
	public enum IntersectionTag {
	    TS,
	    NTS
	}
	
	Id<Node> intersection_id;
	ArrayList<Id<Link>> incoming_links;
	ArrayList<ArrayList<Id<Link>>> groups;
	IntersectionTag tag;
	
	
	public Intersection(String id, ArrayList<Id<Link>> links, ArrayList<ArrayList<Id<Link>>> groups, String label) {
		this.intersection_id = Id.createNodeId(id);
		this.incoming_links = new ArrayList<Id<Link>>(); 
		this.groups = new ArrayList<ArrayList<Id<Link>>>();
		
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
	

}

