package org.eqasim.core.simulation.vdf.travel_time;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.traffic.CrossingPenalty;
import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.simulation.vdf.VDFScope;
import org.eqasim.core.simulation.vdf.travel_time.function.VolumeDelayFunction;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

import com.google.common.base.Verify;

public class VDFTravelTime implements TravelTime {
	private final VDFScope scope;

	private final double minimumSpeed;
	private final double capacityFactor;
	private final double samplingRate;
	private final CrossingPenalty crossingPenalty;

	private final Network network;
	private final VolumeDelayFunction vdf;
	private final ScenarioExtent updateAreaExtent;

	private final IdMap<Link, double[]> travelTimes = new IdMap<>(Link.class);

	private final Logger logger = LogManager.getLogger(VDFTravelTime.class);

	public VDFTravelTime(VDFScope scope, double minimumSpeed, double capacityFactor, double samplingRate,
			Network network, VolumeDelayFunction vdf, CrossingPenalty crossingPenalty) {
		this(scope, minimumSpeed, capacityFactor, samplingRate, network, vdf, crossingPenalty, null);
	}

	public VDFTravelTime(VDFScope scope, double minimumSpeed, double capacityFactor, double samplingRate,
			Network network, VolumeDelayFunction vdf, CrossingPenalty crossingPenalty,
			ScenarioExtent updateAreaExtent) {
		this.scope = scope;
		this.network = network;
		this.vdf = vdf;
		this.updateAreaExtent = updateAreaExtent;
		this.minimumSpeed = minimumSpeed;
		this.capacityFactor = capacityFactor;
		this.samplingRate = samplingRate;
		this.crossingPenalty = crossingPenalty;

		for (Link link : network.getLinks().values()) {
			double travelTime = Math.max(1.0,
					Math.min(link.getLength() / minimumSpeed, link.getLength() / link.getFreespeed())) +
					crossingPenalty.calculateCrossingPenalty(link);
			double[] initialTimes = new double[scope.getIntervals()];
			Arrays.fill(initialTimes, travelTime);
			travelTimes.put(link.getId(), initialTimes);
		}
	}

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		int i = scope.getIntervalIndex(time);
		Id<Vehicle> vehicleId = vehicle==null? null:vehicle.getId();
		double delay = crossingPenalty.calculateCrossingPenalty(link, time, vehicleId);
		return travelTimes.get(link.getId())[i] + delay;
	}

	public void update(IdMap<Link, double[]> counts) {
		update(counts, false);
	}

	public void update(IdMap<Link, double[]> counts, boolean forceUpdateAllLinks) {
		logger.info("Updating VDF travel times ...");

		for (Map.Entry<Id<Link>, double[]> entry : counts.entrySet()) {
			Link link = network.getLinks().get(entry.getKey());
			if (link == null) {
				continue;
			}

			if (updateAreaExtent != null && !forceUpdateAllLinks) {
				if (!updateAreaExtent.isInside(link.getFromNode().getCoord())
						|| !updateAreaExtent.isInside(link.getToNode().getCoord())) {
					continue;
				}
			}

			double[] linkCounts = entry.getValue();
			double[] linkTravelTimes = travelTimes.get(entry.getKey());

			for (int i = 0; i < scope.getIntervals(); i++) {
				double time = scope.getStartTime() + (i + 0.5) * scope.getIntervalTime();

				// Pass per interval
				double flow = linkCounts[i] / samplingRate;
				double capacity = capacityFactor * scope.getIntervalTime() * link.getCapacity(time)
						/ network.getCapacityPeriod();

				// TODO: maybe we should remove this way of getting the travel times, because crossing penalties are not considered here
				double travelTime = Math.max(1.0,
						Math.min(link.getLength() / minimumSpeed, vdf.calculateTravelTime(time, flow, capacity, link))
				);

				linkTravelTimes[i] = travelTime;
			}
		}
	}

	public void write(File outputFile) {
		try {
			DataOutputStream outputStream = new DataOutputStream(
					IOUtils.getOutputStream(outputFile.toURI().toURL(), false));
			outputStream.writeInt(travelTimes.size());
			outputStream.writeInt(scope.getIntervals());
			for (Map.Entry<Id<Link>, double[]> entry : travelTimes.entrySet()) {
				outputStream.writeUTF(entry.getKey().toString());
				for (double d : entry.getValue()) {
					outputStream.writeDouble(d);
				}
			}
			outputStream.flush();
			outputStream.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void readFrom(URL inputFile) {
		try {
			DataInputStream dataInputStream = new DataInputStream(IOUtils.getInputStream(inputFile));
			Verify.verify(dataInputStream.readInt() == travelTimes.size());
			Verify.verify(dataInputStream.readInt() == scope.getIntervals());
			for (int i = 0; i < travelTimes.size(); i++) {
				Id<Link> linkId = Id.createLinkId(dataInputStream.readUTF());
				for (int j = 0; j < scope.getIntervals(); j++) {
					double travelTime = dataInputStream.readDouble();
					travelTimes.get(linkId)[j] = travelTime;
				}
			}
			Verify.verify(dataInputStream.available() == 0);
			dataInputStream.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
