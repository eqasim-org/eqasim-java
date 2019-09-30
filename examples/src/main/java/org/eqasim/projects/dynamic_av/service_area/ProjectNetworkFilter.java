package org.eqasim.projects.dynamic_av.service_area;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import ch.ethz.matsim.av.data.AVOperator;
import ch.ethz.matsim.av.network.AVNetworkFilter;

public class ProjectNetworkFilter implements AVNetworkFilter {
	private final OperatingArea operatingArea;

	public ProjectNetworkFilter(OperatingArea operatingArea) {
		this.operatingArea = operatingArea;
	}

	@Override
	public boolean isAllowed(Id<AVOperator> operatorId, Link link) {
		return operatingArea.covers(link.getId());
	}
}
