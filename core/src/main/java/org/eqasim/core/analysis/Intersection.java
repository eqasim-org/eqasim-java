package org.eqasim.core.analysis;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

public class Intersection {
	Id<Node> intersection_id;
	//ArrayList<Id<Link>> incoming_links;
	ArrayList<ArrayList<Id<Link>>> groups;
	
	public Intersection(String id, ArrayList<ArrayList<Id<Link>>> groups) {
		this.intersection_id = Id.createNodeId(id);
		//this.incoming_links = new ArrayList<Id<Link>>(); 
		this.groups = new ArrayList<ArrayList<Id<Link>>>();
		
		for (int j=0; j< groups.size(); j++) {
			ArrayList<Id<Link>> current_group = groups.get(j);
			this.groups.add(current_group);
		}
	}
	

}

