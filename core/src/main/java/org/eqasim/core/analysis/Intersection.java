package org.eqasim.core.analysis;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

public class Intersection {
	Id<Node> intersection_id;
	ArrayList<Id<Link>> incoming_links;
	
	public Intersection(String id, ArrayList<String> links) {
		this.intersection_id = Id.createNodeId(id);
		this.incoming_links = new ArrayList<Id<Link>>(); 
		
		for (int j=0; j< links.size(); j++) {
			String current_link = links.get(j);
			this.incoming_links.add(Id.createLinkId(current_link));
		}
	}
	

}

