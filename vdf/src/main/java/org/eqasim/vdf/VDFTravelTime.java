package org.eqasim.vdf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eqasim.vdf.function.VolumeDelayFunction;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class VDFTravelTime implements TravelTime {
	private final double startTime;
	private final double interval;
	private final int numberOfIntervals;
	private final double minimumSpeed;

	private final Network network;
	private final VolumeDelayFunction vdf;

	private final IdMap<Link, List<Double>> travelTimes = new IdMap<>(Link.class);

	public VDFTravelTime(double startTime, double interval, int numberOfIntervals, double minimumSpeed, Network network,
			VolumeDelayFunction vdf) {
		this.startTime = startTime;
		this.interval = interval;
		this.numberOfIntervals = numberOfIntervals;
		this.network = network;
		this.vdf = vdf;
		this.minimumSpeed = minimumSpeed;

		for (Link link : network.getLinks().values()) {
			double travelTime = Math.max(1.0,
					Math.max(link.getLength() / minimumSpeed, link.getLength() / link.getFreespeed()));
			travelTimes.put(link.getId(), new ArrayList<>(Collections.nCopies(numberOfIntervals, travelTime)));
		}
	}

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		int i = getInterval(time);
		return travelTimes.get(link.getId()).get(i);
	}

	public void update(IdMap<Link, List<Integer>> counts) {
		for (Map.Entry<Id<Link>, List<Integer>> entry : counts.entrySet()) {
			Link link = network.getLinks().get(entry.getKey());

			List<Integer> linkCounts = entry.getValue();
			List<Double> linkTravelTimes = travelTimes.get(entry.getKey());

			for (int i = 0; i < numberOfIntervals; i++) {
				double time = startTime + i * interval;

				// Pass per hour, which is more familiar choice
				double flow = 3600.0 * linkCounts.get(i) / interval;
				double capacity = 3600.0 * link.getCapacity(time) / network.getCapacityPeriod();

				double travelTime = Math.max(1.0,
						Math.max(link.getLength() / minimumSpeed, vdf.getTravelTime(time, flow, capacity, link)));
				linkTravelTimes.set(i, travelTime);
			}
		}
	}

	public int getNumberOfIntervals() {
		return numberOfIntervals;
	}

	public int getInterval(double time) {
		return Math.min(Math.max(0, (int) Math.floor((time - startTime) / interval)), numberOfIntervals - 1);
	}
}
