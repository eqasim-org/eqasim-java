package org.eqasim.projects.astra16.waiting_time;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import org.eqasim.projects.astra16.service_area.ServiceArea;
import org.eqasim.projects.astra16.service_area.ServiceAreaZone;
import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.facilities.Facility;

import ch.ethz.matsim.av.config.operator.WaitingTimeConfig;
import ch.ethz.matsim.av.waiting_time.WaitingTime;

public class WaitingTimeWriter {
	private final WaitingTime waitingTime;
	private final WaitingTimeConfig config;

	private final Map<Integer, Link> queryLinks = new HashMap<>();

	public WaitingTimeWriter(WaitingTime waitingTime, ServiceArea serviceArea, Network network,
			WaitingTimeConfig config) {
		this.waitingTime = waitingTime;
		this.config = config;

		for (ServiceAreaZone zone : serviceArea.getZones()) {
			Coordinate interiorPoint = zone.getGeometry().getInteriorPoint().getCoordinate();
			Link closestLink = NetworkUtils.getNearestLink(network,
					new Coord(interiorPoint.getX(), interiorPoint.getY()));
			queryLinks.put(zone.getIndex(), closestLink);
		}
	}

	public void write(File path) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)));
		writer.write(String.join(";", new String[] { "groupIndex", "time", "waitingTime" }) + "\n");

		for (Map.Entry<Integer, Link> query : queryLinks.entrySet()) {
			double time = config.getEstimationStartTime();

			while (time < config.getEstimationEndTime()) {
				double queryTime = time + config.getEstimationInterval() * 0.5;
				Facility queryFacility = new LinkWrapperFacility(query.getValue());

				double waitingTimeValue = waitingTime.getWaitingTime(queryFacility, queryTime);

				writer.write(String.join(";", new String[] { //
						String.valueOf(query.getKey()), //
						String.valueOf(time), //
						String.valueOf(waitingTimeValue) //
				}) + "\n");

				time += config.getEstimationInterval();
			}

		}

		writer.close();
	}
}
