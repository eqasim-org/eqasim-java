package org.eqasim.ile_de_france.policies.limited_traffic_zone;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.ile_de_france.policies.DefaultPolicy;
import org.eqasim.ile_de_france.policies.PoliciesConfigGroup;
import org.eqasim.ile_de_france.policies.Policy;
import org.eqasim.ile_de_france.policies.PolicyFactory;
import org.eqasim.ile_de_france.policies.routing.FixedRoutingPenalty;
import org.eqasim.ile_de_france.policies.routing.PolicyLinkFinder;
import org.eqasim.ile_de_france.policies.routing.PolicyLinkFinder.Predicate;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;

public class LimitedTrafficZonePolicyFactory implements PolicyFactory {
	private static final Logger logger = LogManager.getLogger(LimitedTrafficZonePolicyFactory.class);

	static public final String POLICY_NAME = "limitedTrafficZone";
	private final double enterPenalty = 24.0 * 3600.0;

	private final Config config;
	private final Network network;

	public LimitedTrafficZonePolicyFactory(Config config, Network network) {
		this.config = config;
		this.network = network;
	}

	@Override
	public Policy createPolicy(String name) {
		for (ConfigGroup item : PoliciesConfigGroup.get(config)
				.getParameterSets(LimitedTrafficZonePolicyFactory.POLICY_NAME)) {
			LimitedTrafficZoneConfigGroup policyItem = (LimitedTrafficZoneConfigGroup) item;

			if (policyItem.policyName.equals(name)) {
				return createPolicy(policyItem);
			}
		}

		throw new IllegalStateException("Configuration not found for policy " + name + " of type "
				+ LimitedTrafficZonePolicyFactory.POLICY_NAME);
	}

	private Policy createPolicy(LimitedTrafficZoneConfigGroup ltzConfig) {
		logger.info(
				"Creating policy " + ltzConfig.policyName + " of type " + LimitedTrafficZonePolicyFactory.POLICY_NAME);
		logger.info("  Perimeters: " + ltzConfig.perimetersPath);

		IdSet<Link> linkIds = PolicyLinkFinder
				.create(new File(ConfigGroup.getInputFileURL(config.getContext(), ltzConfig.perimetersPath).getPath()))
				.findLinks(network, Predicate.Entering);

		logger.info("  Affected entering links: " + linkIds.size());

		return new DefaultPolicy(new FixedRoutingPenalty(linkIds, enterPenalty), null);
	}
}
