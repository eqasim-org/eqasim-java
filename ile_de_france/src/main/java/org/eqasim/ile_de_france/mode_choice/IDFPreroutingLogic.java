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
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.util.TravelTime;

public class IDFPreroutingLogic implements PreroutingLogic, IterationStartsListener {
	private final Logger logger = Logger.getLogger(IDFPreroutingLogic.class);

	private final double departureTimeMargin = 300.0; // seconds
	private final int networkRoutingDelay = 10; // iterations
	private final double switchBeta = 1000; // seconds

	private final Set<String> delayedModes;
	private final TravelTime travelTime; // TODO: This is not mode dependent right now!
	private final Network network;

	private final UniformEpsilonProvider epsilon;

	private int currentIteration = 0;

	public IDFPreroutingLogic(UniformEpsilonProvider epsilon, Set<String> delayedModes, TravelTime travelTime,
			Network network) {
		this.epsilon = epsilon;
		this.delayedModes = delayedModes;
		this.travelTime = travelTime;
		this.network = network;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		currentIteration = event.getIteration();
	}

	@Override
	public boolean keepInitialRoute(Person person, DiscreteModeChoiceTrip trip) {
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

		if (Math.abs(initialDepartureTime - trip.getDepartureTime()) < departureTimeMargin) {
			if (!delayedModes.contains(trip.getInitialMode())) {
				return true;
			} else {
				Integer lastRoutingUpdate = (Integer) trip.getOriginActivity().getAttributes()
						.getAttribute("lastRoutingUpdate");

				if (lastRoutingUpdate == null) {
					lastRoutingUpdate = Integer.MIN_VALUE;
				}

				if (currentIteration - lastRoutingUpdate > networkRoutingDelay) {
					trip.getOriginActivity().getAttributes().putAttribute("lastRoutingUpdate", currentIteration);
					return true;
				} else {
					logger.warn("Still " + (currentIteration - lastRoutingUpdate) + " iterations before updating "
							+ trip.getInitialMode() + " for " + person.getId());
				}
			}
		}

		return false;
	}

	@Override
	public boolean keepInitialRoute(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> updatedRoute) {
		Leg initialLeg = (Leg) trip.getInitialElements().get(0);

		if (TripStructureUtils.getRoutingMode(initialLeg) == null) {
			return false;
		}

		if (!delayedModes.contains(trip.getInitialMode())) {
			return false;
		} else {
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

			double delta = updatedTravelTime - initialTravelTime;

			if (delta > 0.0) {
				return true;
			} else {
				// Adding a number of the trip index to decorrelate from mode choice
				double u = epsilon.getEpsilon(person.getId(), trip.getIndex() + 9482, trip.getInitialMode());
				return delta < switchBeta * Math.log(1 - u);
			}
		}
	}
}