package org.eqasim.projects.astra16.service_area;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import ch.ethz.matsim.av.data.AVOperator;
import ch.ethz.matsim.av.network.AVNetworkFilter;

public class ServiceAreaFilter implements AVNetworkFilter {
	private final ServiceArea serviceArea;

	public ServiceAreaFilter(ServiceArea serviceArea) {
		this.serviceArea = serviceArea;
	}

	@Override
	public boolean isAllowed(Id<AVOperator> operatorId, Link link) {
		return serviceArea.covers(link);
	}
}
