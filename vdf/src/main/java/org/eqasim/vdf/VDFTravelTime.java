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
	private final double flowCapacityFactor;
	private final double crossingPenalty;

	private final Network network;
	private final VolumeDelayFunction vdf;

	private final IdMap<Link, List<Double>> travelTimes = new IdMap<>(Link.class);

	public VDFTravelTime(double startTime, double interval, int numberOfIntervals, double minimumSpeed,
			double flowCapacityFacotor, Network network, VolumeDelayFunction vdf, double crossingPenalty) {
		this.startTime = startTime;
		this.interval = interval;
		this.numberOfIntervals = numberOfIntervals;
		this.network = network;
		this.vdf = vdf;
		this.minimumSpeed = minimumSpeed;
		this.flowCapacityFactor = flowCapacityFacotor;
		this.crossingPenalty = crossingPenalty;

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

	public void update(IdMap<Link, List<Double>> counts) {
		for (Map.Entry<Id<Link>, List<Double>> entry : counts.entrySet()) {
			Link link = network.getLinks().get(entry.getKey());

			List<Double> linkCounts = entry.getValue();
			List<Double> linkTravelTimes = travelTimes.get(entry.getKey());

			for (int i = 0; i < numberOfIntervals; i++) {
				double time = startTime + i * interval;

				// Pass per interval
				double flow = linkCounts.get(i) / flowCapacityFactor;
				double capacity = interval * link.getCapacity(time) / network.getCapacityPeriod();

				double travelTime = Math.max(1.0,
						Math.max(link.getLength() / minimumSpeed, vdf.getTravelTime(time, flow, capacity, link)));
				linkTravelTimes.set(i, considerCrossingPenalty(link, travelTime));
			}
		}
	}

	private double considerCrossingPenalty(Link link, double baseTravelTime) {
		boolean isMajor = true;

		for (Link other : link.getToNode().getInLinks().values()) {
			if (other.getCapacity() >= link.getCapacity()) {
				isMajor = false;
			}
		}

		if (isMajor || link.getToNode().getInLinks().size() == 1) {
			return baseTravelTime;
		} else {
			return baseTravelTime + crossingPenalty;
		}
	}

	public int getNumberOfIntervals() {
		return numberOfIntervals;
	}

	public int getInterval(double time) {
		return Math.min(Math.max(0, (int) Math.floor((time - startTime) / interval)), numberOfIntervals - 1);
	}
}
