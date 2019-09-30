package org.eqasim.projects.dynamic_av.waiting_time;

import org.eqasim.projects.dynamic_av.service_area.OperatingArea;
import org.eqasim.projects.dynamic_av.service_area.Zone;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import ch.ethz.matsim.av.config.operator.OperatorConfig;
import ch.ethz.matsim.av.waiting_time.StandardWaitingTimeFactory;
import ch.ethz.matsim.av.waiting_time.WaitingTime;
import ch.ethz.matsim.av.waiting_time.WaitingTimeFactory;

@Singleton
public class ProjectWaitingTimeFactory implements WaitingTimeFactory {
	static public final String TYPE = "Project";

	private final StandardWaitingTimeFactory delegate;
	private final OperatingArea operatingArea;

	@Inject
	public ProjectWaitingTimeFactory(StandardWaitingTimeFactory delegate, OperatingArea operatingArea) {
		this.delegate = delegate;
		this.operatingArea = operatingArea;
	}

	@Override
	public WaitingTime createWaitingTime(OperatorConfig operatorConfig, Network network) {
		for (Zone zone : operatingArea.getZones()) {
			for (Id<Link> linkId : zone.getLinkIds()) {
				Link link = network.getLinks().get(linkId);

				if (link != null) {
					link.getAttributes().putAttribute("waitingTimeGroupIndex", zone.getIndex());
				}
			}
		}

		return delegate.createWaitingTime(operatorConfig, network);
	}
}
