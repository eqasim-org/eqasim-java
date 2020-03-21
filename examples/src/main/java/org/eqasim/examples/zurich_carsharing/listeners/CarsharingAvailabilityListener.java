package org.eqasim.examples.zurich_carsharing.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.manager.supply.FreeFloatingVehiclesContainer;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.utils.collections.QuadTree;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class CarsharingAvailabilityListener implements MobsimBeforeSimStepListener, BeforeMobsimListener {

	Map<String, ArrayList<QuadTree<CSVehicle>>> availability = new HashMap<>();
	ArrayList<QuadTree<Double>> rentals = new ArrayList<>();

	Map<String, ArrayList<Map<CSVehicle, Link>>> locations = new HashMap<>();
	@Inject
	private CarsharingSupplyInterface carsahringSupply;
	@Inject
	@Named("carnetwork")
	private Network network;

	@Inject
	Config config;

	@Inject
	private RentalHandlers rentalsHandler;

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {

		if (e.getSimulationTime() % 900.0 == 0.0) {
			for (String company : carsahringSupply.getCompanyNames()) {
				QuadTree<CSVehicle> availVeh = ((FreeFloatingVehiclesContainer) carsahringSupply.getCompany(company)
						.getVehicleContainer("freefloating")).getFfVehicleLocationQuadTree();

				double minx = (1.0D / 0.0D);
				double miny = (1.0D / 0.0D);
				double maxx = (-1.0D / 0.0D);
				double maxy = (-1.0D / 0.0D);

				for (Link l : this.network.getLinks().values()) {
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

				QuadTree<CSVehicle> newQT = new QuadTree<CSVehicle>(minx, miny, maxx, maxy);
				Map<CSVehicle, Link> locationsVehicles = new HashMap<>();

				for (CSVehicle veh : availVeh.values()) {
					Link link = ((FreeFloatingVehiclesContainer) carsahringSupply.getCompany(company)
							.getVehicleContainer("freefloating")).getVehicleLocation(veh);
					if (link == null)
						new RuntimeException("this should not happen");
					newQT.put(link.getCoord().getX(), link.getCoord().getY(), veh);
					locationsVehicles.put(veh, ((FreeFloatingVehiclesContainer) carsahringSupply.getCompany(company)
							.getVehicleContainer("freefloating")).getVehicleLocation(veh));
				}

				if (availability.get(company) != null) {
					availability.get(company).add(newQT);
					locations.get(company).add(locationsVehicles);
				} else {
					ArrayList<QuadTree<CSVehicle>> newArrayList = new ArrayList<>();
					newArrayList.add(newQT);
					availability.put(company, newArrayList);

					ArrayList<Map<CSVehicle, Link>> newLocations = new ArrayList<>();
					newLocations.add(locationsVehicles);
					locations.put(company, newLocations);
				}

			}

			this.rentalsHandler.addQuadTree();
		}

	}

	public Map<String, ArrayList<QuadTree<CSVehicle>>> getAvailability() {
		return availability;
	}

	public ArrayList<QuadTree<Double>> getRentals() {
		return this.rentalsHandler.getRentals();
	}

	public Map<String, ArrayList<Map<CSVehicle, Link>>> getLocations() {
		return locations;
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		availability = new HashMap<>();
		locations = new HashMap<>();
		rentals.clear();
	}

}
