package org.eqasim.core.simulation.policies.impl.limited_traffic_zone;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
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
import org.eqasim.core.simulation.policies.routing.PolicyLinkFinder.Predicate;
import org.eqasim.core.simulation.policies.routing.PolicyLinkWriter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;
import org.matsim.core.utils.io.IOUtils;

public class LimitedTrafficZonePolicyFactory implements PolicyFactory {
	private static final Logger logger = LogManager.getLogger(LimitedTrafficZonePolicyFactory.class);

	static public final String POLICY_NAME = "limitedTrafficZone";
	private final double routingPenalty = 7200.0;
	private final double utilityPenalty = 1000.0;

	private final Config config;
	private final Network network;
	private final Population population;
	private final OutputDirectoryHierarchy outputHierarchy;

	public LimitedTrafficZonePolicyFactory(Config config, Network network, Population population,
			OutputDirectoryHierarchy outputHierarchy) {
		this.config = config;
		this.network = network;
		this.population = population;
		this.outputHierarchy = outputHierarchy;
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

		IdSet<Link> crossingLinkIds = new IdSet<>(Link.class);
		IdSet<Link> insideLinkIds = new IdSet<>(Link.class);

		if (!ltzConfig.perimetersPath.isEmpty()) {
			logger.info("  Perimeters: " + ltzConfig.perimetersPath);

			insideLinkIds.addAll(PolicyLinkFinder
					.create(new File(
							ConfigGroup.getInputFileURL(config.getContext(), ltzConfig.perimetersPath).getPath()))
					.findLinks(network, Predicate.Inside));

			crossingLinkIds.addAll(PolicyLinkFinder
					.create(new File(
							ConfigGroup.getInputFileURL(config.getContext(), ltzConfig.perimetersPath).getPath()))
					.findLinks(network, Predicate.Crossing));
		} else if (!ltzConfig.linkListPath.isEmpty()) {
			logger.info("  Link list: " + ltzConfig.linkListPath);

			var links = loadLinkList(
					ConfigGroup.getInputFileURL(config.getContext(), ltzConfig.linkListPath).getPath(),
					network, ltzConfig.policyName);

			crossingLinkIds.addAll(links.active());
			insideLinkIds.addAll(links.area());
		} else {
			throw new IllegalStateException(
					"One of perimetersPath and linkListPath must be set for policy " + ltzConfig.policyName);
		}

		insideLinkIds.addAll(crossingLinkIds);

		logger.info("  Affected active links (penalized): " + crossingLinkIds.size());
		new PolicyLinkWriter(outputHierarchy).write(ltzConfig.policyName, crossingLinkIds);

		IdSet<Person> residentIds = new IdSet<>(Person.class);
		if (ltzConfig.considerResidency) {
			for (Person person : population.getPersons().values()) {
				for (Activity activity : TripStructureUtils.getActivities(person.getSelectedPlan(),
						StageActivityHandling.ExcludeStageActivities)) {
					if (activity.getType().equals("home") && insideLinkIds.contains(activity.getLinkId())) {
						residentIds.add(person.getId());
						break;
					}
				}
			}

			logger.info("  Found " + residentIds.size() + " residents in the area");
		} else {
			logger.info("  Not considering residency of agents");
		}

		PolicyPersonFilter delegateFilter = personId -> {
			if (residentIds.contains(personId)) {
				return false;
			} else {
				return personFilter.applies(personId);
			}
		};

		return new DefaultPolicy(new FixedRoutingPenalty(crossingLinkIds, routingPenalty, delegateFilter),
				new LimitedTrafficZoneUtilityPenalty(utilityPenalty, crossingLinkIds, insideLinkIds, delegateFilter));
	}

	private static ZoneLinks loadLinkList(String path, Network network, String policy) {
		IdSet<Link> area = new IdSet<>(Link.class);

		Network carNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(network).filter(carNetwork, Collections.singleton("car"));

		try {
			BufferedReader reader = IOUtils.getBufferedReader(path);

			String line = null;
			while ((line = reader.readLine()) != null) {
				line = line.trim();

				if (!line.isEmpty()) {
					Link link = carNetwork.getLinks().get(Id.createLinkId(line));

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
			Link currentLink = carNetwork.getLinks().get(currentId);

			Set<Id<Link>> incoming = new HashSet<>(currentLink.getFromNode().getInLinks().keySet());
			Set<Id<Link>> outgoing = new HashSet<>(currentLink.getToNode().getOutLinks().keySet());

			incoming.removeIf(area::contains);
			outgoing.removeIf(area::contains);

			// this link can only be reached from within the area (as there are no external
			// inlinks)
			// this links only leads to within the area (as there are not external outlinks)
			boolean isInternal = incoming.size() == 0 && outgoing.size() == 0;

			if (!isInternal) {
				active.add(currentId);
			}
		}

		return new ZoneLinks(active, area);
	}

	private record ZoneLinks(IdSet<Link> active, IdSet<Link> area) {
	}
}
