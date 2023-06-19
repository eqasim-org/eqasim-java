package org.eqasim.core.scenario.cutter.network;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.PtConstants;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class NetworkCutter {
	private final static Logger log = Logger.getLogger(NetworkCutter.class);

	private final ScenarioExtent extent;
	private final MinimumNetworkFinder minimumNetworkFinder;
	private final Scenario scenario;

	public NetworkCutter(ScenarioExtent extent, Scenario scenario, MinimumNetworkFinder minimumNetworkFinder) {
		this.extent = extent;
		this.minimumNetworkFinder = minimumNetworkFinder;
		this.scenario = scenario;
	}

	public void run(Network network) throws InterruptedException {
		log.info("Cutting the network ...");

		int originalNumberOfLinks = network.getLinks().size();
		int originalNumberOfNodes = network.getNodes().size();

		// Collect all links that within the area
		Set<Id<Link>> retainedLinkIds = new HashSet<>();

		for (Link link : network.getLinks().values()) {
			if (extent.isInside(link.getToNode().getCoord()) && extent.isInside(link.getFromNode().getCoord())) {
				retainedLinkIds.add(link.getId());
			}

			if (link.getId().toString().contains("outside")) {
				retainedLinkIds.add(link.getId());
			}
		}

		// Collect all links that are needed by the population
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement element : plan.getPlanElements()) {
					if (element instanceof Activity) {
						Activity activity = (Activity) element;

						if (!activity.getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
							retainedLinkIds.add(activity.getLinkId());
						}
					} else {
						Leg leg = (Leg) element;
						Route route = leg.getRoute();

						if (route instanceof NetworkRoute) {
							NetworkRoute networkRoute = (NetworkRoute) route;

							retainedLinkIds.add(networkRoute.getStartLinkId());
							retainedLinkIds.add(networkRoute.getEndLinkId());
							retainedLinkIds.addAll(networkRoute.getLinkIds());
						}
					}
				}
			}
		}

		// Collect all links that are needed by the public transit lines
		for (TransitLine transitLine : scenario.getTransitSchedule().getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				NetworkRoute networkRoute = transitRoute.getRoute();

				retainedLinkIds.add(networkRoute.getStartLinkId());
				retainedLinkIds.add(networkRoute.getEndLinkId());
				retainedLinkIds.addAll(networkRoute.getLinkIds());
			}
		}

		for (TransitStopFacility facility : scenario.getTransitSchedule().getFacilities().values()) {
			retainedLinkIds.add(facility.getLinkId());
		}

		// Collect all facilities
		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
			retainedLinkIds.add(facility.getLinkId());
		}

		// Further processing is needed for the population links, because it may be the
		// case that an agent is using "walk" in the given population, whereas he may
		// change this decision in the simulation. Then, we still need to make sure that
		// his trips can be performed with "car". Technically, this means that we define
		// a common point (e.g. a link around Bellevue) and make sure that there exists
		// a route from all retained links to this point and that all retained links can
		// be reached by this reference point.

		Set<Id<Link>> retainedCarLinkIds = new HashSet<>();

		for (Id<Link> linkId : retainedLinkIds) {
			if (network.getLinks().get(linkId).getAllowedModes().contains("car")) {
				retainedCarLinkIds.add(linkId);
			}
		}

		Set<Id<Link>> allRetainedLinkIds = new HashSet<>();
		allRetainedLinkIds.addAll(retainedLinkIds);
		allRetainedLinkIds.addAll(minimumNetworkFinder.run(retainedCarLinkIds));

		// Note that this means, that public transit lines CANNOT change their routes in
		// the simulation if this is desired (at least not outside of the scenario
		// extent).

		Set<Id<Node>> allRetainedNodeIds = new HashSet<>();

		for (Id<Link> linkId : allRetainedLinkIds) {
			allRetainedNodeIds.add(network.getLinks().get(linkId).getFromNode().getId());
			allRetainedNodeIds.add(network.getLinks().get(linkId).getToNode().getId());
		}

		Set<Id<Link>> allRemovedLinkIds = new HashSet<>(network.getLinks().keySet());
		Set<Id<Node>> allRemovedNodeIds = new HashSet<>(network.getNodes().keySet());

		allRemovedLinkIds.removeAll(allRetainedLinkIds);
		allRemovedNodeIds.removeAll(allRetainedNodeIds);

		allRemovedLinkIds.forEach(id -> network.removeLink(id));
		allRemovedNodeIds.forEach(id -> network.removeNode(id));

		int finalNumberOfNodes = network.getNodes().size();
		int finalNumberOfLinks = network.getLinks().size();

		log.info("Finished cutting the network.");
		log.info("  Number of nodes before: " + originalNumberOfNodes);
		log.info("  Number of links before: " + originalNumberOfLinks);
		log.info("  Number of nodes now: " + finalNumberOfNodes);
		log.info("  Number of links now: " + finalNumberOfLinks);
	}
}
