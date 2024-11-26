package org.eqasim.ile_de_france.policies.limited_traffic_zone;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.ile_de_france.policies.DefaultPolicy;
import org.eqasim.ile_de_france.policies.PoliciesConfigGroup;
import org.eqasim.ile_de_france.policies.Policy;
import org.eqasim.ile_de_france.policies.PolicyFactory;
import org.eqasim.ile_de_france.policies.PolicyPersonFilter;
import org.eqasim.ile_de_france.policies.routing.FactorRoutingPenalty;
import org.eqasim.ile_de_france.policies.routing.PolicyLinkFinder;
import org.eqasim.ile_de_france.policies.routing.PolicyLinkFinder.Predicate;
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
	private final double insideFactor = 3600.0;

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

		final IdSet<Link> linkIds;
		if (!ltzConfig.perimetersPath.isEmpty()) {
			logger.info("  Perimeters: " + ltzConfig.perimetersPath);

			linkIds = PolicyLinkFinder
					.create(new File(
							ConfigGroup.getInputFileURL(config.getContext(), ltzConfig.perimetersPath).getPath()))
					.findLinks(network, Predicate.Inside);

			logger.info("  Affected inside links: " + linkIds.size());
		} else if (!ltzConfig.linkListPath.isEmpty()) {
			logger.info("  Link list: " + ltzConfig.linkListPath);

			linkIds = loadLinkList(ConfigGroup.getInputFileURL(config.getContext(), ltzConfig.linkListPath).getPath(),
					network, ltzConfig.policyName);

			logger.info("  Affected links: " + linkIds.size());
		} else {
			throw new IllegalStateException(
					"One of perimetersPath and linkListPath must be set for policy " + ltzConfig.policyName);
		}

		return new DefaultPolicy(new FactorRoutingPenalty(linkIds, insideFactor, personFilter), null);
	}

	private static IdSet<Link> loadLinkList(String path, Network network, String policy) {
		IdSet<Link> linkList = new IdSet<>(Link.class);

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

					linkList.add(link.getId());
				}
			}

			reader.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return linkList;
	}
}
