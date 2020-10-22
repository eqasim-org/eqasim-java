package org.eqasim.projects.astra16.service_area;

import org.matsim.api.core.v01.network.Link;

public class ServiceAreaFilter {
	private final ServiceArea serviceArea;

	public ServiceAreaFilter(ServiceArea serviceArea) {
		this.serviceArea = serviceArea;
	}

	public boolean isAllowed(Link link) {
		return serviceArea.covers(link);
	}
}
