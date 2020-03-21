package org.eqasim.examples.zurich_carsharing.listeners;

import java.util.ArrayList;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.carsharing.events.NoVehicleCarSharingEvent;
import org.matsim.contrib.carsharing.events.StartRentalEvent;
import org.matsim.contrib.carsharing.events.handlers.NoVehicleCarSharingEventHandler;
import org.matsim.contrib.carsharing.events.handlers.StartRentalEventHandler;
import org.matsim.core.utils.collections.QuadTree;

import com.google.inject.Inject;

public class RentalHandlers implements StartRentalEventHandler, NoVehicleCarSharingEventHandler {

	private ArrayList<QuadTree<Double>> rentals;
	private Network network;

	@Inject
	public RentalHandlers(Network network) {
		this.network = network;		
		rentals = new ArrayList<>();
	}
	
	@Override
	public void handleEvent(NoVehicleCarSharingEvent event) {

		Link link = this.network.getLinks().get(event.getOriginLinkId());
		this.rentals.get(this.rentals.size() - 1).put(link.getCoord().getX(), link.getCoord().getY(), event.getTime());
	}

	@Override
	public void handleEvent(StartRentalEvent event) {
		Link link = this.network.getLinks().get(event.getOriginLinkId());
		this.rentals.get(this.rentals.size() - 1).put(link.getCoord().getX(), link.getCoord().getY(), event.getTime());
	}
	
	public void reset() {
		rentals.clear();
	}
	
	public void addQuadTree() {
		double minx = (1.0D / 0.0D);
		double miny = (1.0D / 0.0D);
		double maxx = (-1.0D / 0.0D);
		double maxy = (-1.0D / 0.0D);
		for (Link l : network.getLinks().values()) {
			if (l.getCoord().getX() < minx)
				minx = l.getCoord().getX();
			if (l.getCoord().getY() < miny)
				miny = l.getCoord().getY();
			if (l.getCoord().getX() > maxx)
				maxx = l.getCoord().getX();
			if (l.getCoord().getY() <= maxy)
				continue;
			maxy = l.getCoord().getY();
		}
		minx -= 1.0D;
		miny -= 1.0D;
		maxx += 1.0D;
		maxy += 1.0D;
		
		QuadTree<Double> newQT = new QuadTree<Double>(minx,miny,maxx, maxy);
		this.rentals.add(newQT);
	}

	public ArrayList<QuadTree<Double>> getRentals() {
		return rentals;
	}

}
