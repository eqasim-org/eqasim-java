package org.eqasim.projects.dynamic_av.service_area;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class ProjectNetworkConfigurator {
	private final OperatingArea operatingArea;

	public ProjectNetworkConfigurator(OperatingArea operatingArea) {
		this.operatingArea = operatingArea;
	}

	public void apply(Network network) {
		for (Link link : network.getLinks().values()) {
			for (Zone zone : operatingArea.getZones()) {
				if (zone.covers(link)) {
					link.getAttributes().putAttribute("waitingTimeGroupIndex", zone.getIndex());
				}
			}
		}
	}
}
