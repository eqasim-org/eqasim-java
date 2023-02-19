package org.eqasim.ile_de_france.mode_choice;

import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eqasim.core.simulation.mode_choice.epsilon.UniformEpsilonProvider;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.components.estimators.AbstractTripRouterEstimator.PreroutingLogic;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.util.TravelTime;

public class IDFPreroutingLogic implements PreroutingLogic, IterationStartsListener, IterationEndsListener {
	private final Logger logger = Logger.getLogger(IDFPreroutingLogic.class);

	private final double departureTimeMargin = 300.0; // seconds
	private final int networkRoutingDelay = 0; // iterations
	private final double switchBeta = 300; // seconds

	private final Set<String> delayedModes;
	private final TravelTime travelTime; // TODO: This is not mode dependent right now!
	private final Network network;

	private final UniformEpsilonProvider epsilon;
	private final IDFPreroutingWriter writer;

	private int currentIteration = 0;

	public IDFPreroutingLogic(UniformEpsilonProvider epsilon, Set<String> delayedModes, TravelTime travelTime,
			Network network, IDFPreroutingWriter writer) {
		this.epsilon = epsilon;
		this.delayedModes = delayedModes;
		this.travelTime = travelTime;
		this.network = network;
		this.writer = writer;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		currentIteration = event.getIteration();
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		writer.update(event.getIteration());
	}

	@Override
	public boolean keepInitialRouteBeforeRouting(Person person, DiscreteModeChoiceTrip trip) {
		Leg initialLeg = (Leg) trip.getInitialElements().get(0);
		double initialDepartureTime = initialLeg.getDepartureTime().seconds();

		// Special case: We don't have the routing mode yet
		if (TripStructureUtils.getRoutingMode(initialLeg) == null) {
			return false;
		}

		// Special case: We have a generic PT
		if (trip.getInitialMode().equals("pt")) {
			for (Leg leg : TripStructureUtils.getLegs(trip.getInitialElements())) {
				if (leg.getMode().equals("pt")) {
					return false; // We are expecting pt:rail, pt:bus, ...
				}
			}
		}

		boolean keepExistingRoute = true;

		if (Math.abs(initialDepartureTime - trip.getDepartureTime()) < departureTimeMargin) {
			// If the departure time is substantially different, reroute!
			keepExistingRoute = false;
		}

		if (delayedModes.contains(trip.getInitialMode())) {
			// Special case for road-based modes

			int iterationsSinceLastUpdate = Integer.MAX_VALUE;

			Integer lastRoutingUpdate = (Integer) trip.getOriginActivity().getAttributes()
					.getAttribute("lastRoutingUpdate:" + trip.getInitialMode());

			if (lastRoutingUpdate != null) {
				iterationsSinceLastUpdate = currentIteration - lastRoutingUpdate;
			}

			if (iterationsSinceLastUpdate > networkRoutingDelay) {
				// Only if we have sufficiently long, find a new route
				keepExistingRoute = false;
			}

			if (!keepExistingRoute) {
				trip.getOriginActivity().getAttributes().putAttribute("lastRoutingUpdate:" + trip.getInitialMode(),
						currentIteration);
			}
		}

		return keepExistingRoute;
	}

	@Override
	public boolean keepInitialRouteAfterRouting(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> updatedRoute) {
		Leg initialLeg = (Leg) trip.getInitialElements().get(0);

		// We need to route because we don't have a routing mode
		if (TripStructureUtils.getRoutingMode(initialLeg) == null) {
			return false;
		}

		// We need to route because this is pt, bike, ... so we have decided we want to
		// reroute already
		if (!delayedModes.contains(trip.getInitialMode())) {
			return false;
		}

		// Here we start assessing the road-based routes
		double departureTime = trip.getDepartureTime();

		double initialTravelTime = 0.0;
		double updatedTravelTime = 0.0;

		for (Leg leg : TripStructureUtils.getLegs(trip.getInitialElements())) {
			NetworkRoute networkRoute = (NetworkRoute) leg.getRoute();

			for (Id<Link> linkId : networkRoute.getLinkIds()) {
				Link link = network.getLinks().get(linkId);
				initialTravelTime += travelTime.getLinkTravelTime(link, departureTime + initialTravelTime, person,
						null);
			}
		}

		for (Leg leg : TripStructureUtils.getLegs(updatedRoute)) {
			NetworkRoute networkRoute = (NetworkRoute) leg.getRoute();

			for (Id<Link> linkId : networkRoute.getLinkIds()) {
				Link link = network.getLinks().get(linkId);
				updatedTravelTime += travelTime.getLinkTravelTime(link, departureTime + updatedTravelTime, person,
						null);
			}
		}

		// How much travel time do we save?
		double gain = initialTravelTime - updatedTravelTime;

		if (gain <= 0.0) {
			// If we would generate additional delay, stick with the old route!
			writer.add(gain, false);
			return true;
		} else {
			// Adding a number of the trip index to decorrelate from mode choice
			double u = epsilon.getEpsilon(person.getId(), trip.getIndex() + 9482, trip.getInitialMode());
			double threshold = -switchBeta * Math.log(1 - u);

			// Assuming that threshold follows a Exponential distribution with expectation
			// switchBeta

			if (gain < threshold) {
				writer.add(gain, false);
				return true;
			} else {
				writer.add(gain, true);
				return false;
			}
		}
	}
}