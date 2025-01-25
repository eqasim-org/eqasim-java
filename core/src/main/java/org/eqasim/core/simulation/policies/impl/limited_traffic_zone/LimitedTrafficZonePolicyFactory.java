package org.eqasim.core.simulation.policies.impl.limited_traffic_zone;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.simulation.policies.DefaultPolicy;
import org.eqasim.core.simulation.policies.Policy;
import org.eqasim.core.simulation.policies.PolicyFactory;
import org.eqasim.core.simulation.policies.PolicyPersonFilter;
import org.eqasim.core.simulation.policies.config.PoliciesConfigGroup;
import org.eqasim.core.simulation.policies.routing.FixedRoutingPenalty;
import org.eqasim.core.simulation.policies.routing.PolicyLinkFinder;
import org.eqasim.core.simulation.policies.routing.PolicyLinkFinder.PolicyLinks;
import org.eqasim.core.simulation.policies.routing.PolicyLinkFinder.Predicate;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.utils.io.IOUtils;

public class LimitedTrafficZonePolicyFactory implements PolicyFactory {
	private static final Logger logger = LogManager.getLogger(LimitedTrafficZonePolicyFactory.class);

	static public final String POLICY_NAME = "limitedTrafficZone";
	private final double routingPenalty = 7200.0;
	private final double utilityPenalty = 1000.0;

	private final Config config;
	private final Network network;

	public LimitedTrafficZonePolicyFactory(Config config, Network network) {
		this.config = config;
		this.network = network;
	}

	@Override
	public Policy createPolicy(String name, PolicyPersonFilter personFilter) {
		for (ConfigGroup item : PoliciesConfigGroup.get(config)
				.getParameterSets(LimitedTrafficZonePolicyFactory.POLICY_NAME)) {
			LimitedTrafficZoneConfigGroup policyItem = (LimitedTrafficZoneConfigGroup) item;

			if (policyItem.policyName.equals(name)) {
				return createPolicy(policyItem, personFilter);
			}
		}

		throw new IllegalStateException("Configuration not found for policy " + name + " of type "
				+ LimitedTrafficZonePolicyFactory.POLICY_NAME);
	}

	private Policy createPolicy(LimitedTrafficZoneConfigGroup ltzConfig, PolicyPersonFilter personFilter) {
		logger.info(
				"Creating policy " + ltzConfig.policyName + " of type " + LimitedTrafficZonePolicyFactory.POLICY_NAME);

		if (!ltzConfig.perimetersPath.isEmpty() && !ltzConfig.linkListPath.isEmpty()) {
			throw new IllegalStateException(
					"Only one of perimetersPath and linkListPath can be set for policy " + ltzConfig.policyName);
		}

		PolicyLinks links;
		if (!ltzConfig.perimetersPath.isEmpty()) {
			logger.info("  Perimeters: " + ltzConfig.perimetersPath);

			links = PolicyLinkFinder
					.create(new File(
							ConfigGroup.getInputFileURL(config.getContext(), ltzConfig.perimetersPath).getPath()))
					.findLinks(network, Predicate.Crossing);
		} else if (!ltzConfig.linkListPath.isEmpty()) {
			logger.info("  Link list: " + ltzConfig.linkListPath);

			links = loadLinkList(ConfigGroup.getInputFileURL(config.getContext(), ltzConfig.linkListPath).getPath(),
					network, ltzConfig.policyName);
		} else {
			throw new IllegalStateException(
					"One of perimetersPath and linkListPath must be set for policy " + ltzConfig.policyName);
		}

		logger.info("  Affected active links (penalized): " + links.active().size());
		logger.info("  Affected connecting links (forbidden as origin/destination): " + links.connecting().size());

		return new DefaultPolicy(new FixedRoutingPenalty(links.active(), routingPenalty, personFilter),
				new LimitedTrafficZoneUtilityPenalty(utilityPenalty, links.connecting(), personFilter));
	}

	private static PolicyLinks loadLinkList(String path, Network network, String policy) {
		IdSet<Link> area = new IdSet<>(Link.class);

		try {
			BufferedReader reader = IOUtils.getBufferedReader(path);

			String line = null;
			while ((line = reader.readLine()) != null) {
				line = line.trim();

				if (!line.isEmpty()) {
					Link link = network.getLinks().get(Id.createLinkId(line));

					if (link == null) {
						throw new IllegalStateException("Link list of policy " + policy + " contains link " + line
								+ " which is not included in network");
					}

					area.add(link.getId());
				}
			}

			reader.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// Now find all links that consitute the edge
		IdSet<Link> active = new IdSet<>(Link.class);

		for (Id<Link> currentId : area) {
			Link currentLink = network.getLinks().get(currentId);

			Set<Id<Link>> incoming = new HashSet<>(currentLink.getFromNode().getInLinks().keySet());
			Set<Id<Link>> outgoing = new HashSet<>(currentLink.getToNode().getOutLinks().keySet());

			int totalIncoming = incoming.size();
			int totalOutgoing = outgoing.size();

			incoming.removeIf(area::contains);
			outgoing.removeIf(area::contains);

			// this link can only be reached and only leads to the policy area, hence, it is
			// not an edge
			boolean isInternal = incoming.size() == totalIncoming && outgoing.size() == totalOutgoing;

			if (!isInternal) {
				active.add(currentId);
			}
		}

		return new PolicyLinks(active, PolicyLinkFinder.findConnectingLinks(active, network));
	}
}
