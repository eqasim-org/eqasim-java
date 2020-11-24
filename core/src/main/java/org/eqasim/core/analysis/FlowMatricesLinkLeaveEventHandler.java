package org.eqasim.core.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

public class FlowMatricesLinkLeaveEventHandler implements LinkLeaveEventHandler {
	
	public Map<Id<Vehicle>, ArrayList<Id<Link>>[] > leftLinks = new HashMap<>();
	private static int NUMBER_HOURS = 31;

	@SuppressWarnings("unchecked")
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Vehicle> idveh = event.getVehicleId();
		Id<Link> idlink = event.getLinkId();
		double time = event.getTime();
		int hour = (int) (time / 3600);
		
		if (!this.leftLinks.containsKey(idveh)) {
			ArrayList<Id<Link>>[] tablinkid = new ArrayList[NUMBER_HOURS]; 
			for (int i = 0; i < NUMBER_HOURS; i++) { 
				tablinkid[i] = new ArrayList<Id<Link>>(); 
	        } 
			this.leftLinks.put(idveh, tablinkid);	
		}
		
		ArrayList<Id<Link>>[] tablinkid = this.leftLinks.get(idveh);
		if (hour < 31) {
			tablinkid[hour].add(idlink);
		}
	}

}
