package org.eqasim.vdf.travel_time;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.vdf.VDFScope;
import org.eqasim.vdf.travel_time.function.VolumeDelayFunction;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class VDFTravelTime implements TravelTime {
	private final VDFScope scope;

	private final double minimumSpeed;
	private final double capacityFactor;
	private final double samplingRate;
	private final double crossingPenalty;

	private final Network network;
	private final VolumeDelayFunction vdf;

	private final IdMap<Link, List<Double>> travelTimes = new IdMap<>(Link.class);

	private final Logger logger = LogManager.getLogger(VDFTravelTime.class);

	public VDFTravelTime(VDFScope scope, double minimumSpeed, double capacityFacotor, double samplingRate,
			Network network, VolumeDelayFunction vdf, double crossingPenalty) {
		this.scope = scope;
		this.network = network;
		this.vdf = vdf;
		this.minimumSpeed = minimumSpeed;
		this.capacityFactor = capacityFacotor;
		this.samplingRate = samplingRate;
		this.crossingPenalty = crossingPenalty;

		for (Link link : network.getLinks().values()) {
			double travelTime = Math.max(1.0,
					Math.min(link.getLength() / minimumSpeed, link.getLength() / link.getFreespeed()));
			travelTimes.put(link.getId(), new ArrayList<>(
					Collections.nCopies(scope.getIntervals(), considerCrossingPenalty(link, travelTime))));
		}
	}

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		int i = scope.getIntervalIndex(time);
		return travelTimes.get(link.getId()).get(i);
	}

	public void update(IdMap<Link, List<Double>> counts) {
		logger.info(String.format("Updating VDFTravelTime ..."));

		long totalCount = counts.size() * scope.getIntervals();
		long nonFreespeedCount = 0;

		for (Map.Entry<Id<Link>, List<Double>> entry : counts.entrySet()) {
			Link link = network.getLinks().get(entry.getKey());

			List<Double> linkCounts = entry.getValue();
			List<Double> linkTravelTimes = travelTimes.get(entry.getKey());

			for (int i = 0; i < scope.getIntervals(); i++) {
				double time = scope.getStartTime() + i * scope.getIntervalTime();

				// Pass per interval
				double flow = linkCounts.get(i) / samplingRate;
				double capacity = capacityFactor * scope.getIntervalTime() * link.getCapacity(time)
						/ network.getCapacityPeriod();

				double travelTime = Math.max(1.0,
						Math.min(link.getLength() / minimumSpeed, vdf.getTravelTime(time, flow, capacity, link)));
				linkTravelTimes.set(i, considerCrossingPenalty(link, travelTime));

				if (travelTime > link.getLength() / link.getFreespeed()) {
					nonFreespeedCount += 1;
				}
			}
		}

		logger.info(String.format("  Done: %d/%d are slower than freespeed", nonFreespeedCount, totalCount));
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
}
