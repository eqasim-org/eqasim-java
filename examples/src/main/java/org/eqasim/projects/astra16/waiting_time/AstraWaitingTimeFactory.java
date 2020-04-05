package org.eqasim.projects.astra16.waiting_time;

import org.eqasim.projects.astra16.service_area.ServiceArea;
import org.eqasim.projects.astra16.service_area.ServiceAreaZone;
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
public class AstraWaitingTimeFactory implements WaitingTimeFactory {
	static public final String TYPE = "Astra";

	private final StandardWaitingTimeFactory delegate;
	private final ServiceArea serviceArea;

	@Inject
	public AstraWaitingTimeFactory(StandardWaitingTimeFactory delegate, ServiceArea serviceArea) {
		this.delegate = delegate;
		this.serviceArea = serviceArea;
	}

	@Override
	public WaitingTime createWaitingTime(OperatorConfig operatorConfig, Network network) {
		for (ServiceAreaZone zone : serviceArea.getZones()) {
			for (Id<Link> linkId : zone.getLinkIds()) {
				Link link = network.getLinks().get(linkId);

				if (link != null) {
					link.getAttributes().putAttribute("avWaitingTimeGroup", zone.getIndex());
				}
			}
		}

		return delegate.createWaitingTime(operatorConfig, network);
	}
}
