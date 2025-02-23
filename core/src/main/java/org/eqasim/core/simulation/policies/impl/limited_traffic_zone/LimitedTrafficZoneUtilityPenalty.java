package org.eqasim.core.simulation.policies.impl.limited_traffic_zone;

import java.util.List;

import org.eqasim.core.simulation.policies.PolicyPersonFilter;
import org.eqasim.core.simulation.policies.utility.UtilityPenalty;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.core.population.routes.NetworkRoute;

public class LimitedTrafficZoneUtilityPenalty implements UtilityPenalty {
    private final IdSet<Link> insideLinkIds;
    private final IdSet<Link> crossingLinkIds;

    private final PolicyPersonFilter personFilter;
    private final double penalty;

    public LimitedTrafficZoneUtilityPenalty(double penalty, IdSet<Link> insideLinkIds, IdSet<Link> crossingLinkIds,
            PolicyPersonFilter personFilter) {
        this.personFilter = personFilter;
        this.insideLinkIds = insideLinkIds;
        this.crossingLinkIds = crossingLinkIds;
        this.penalty = penalty;
    }

    @Override
    public double calculatePenalty(String mode, Person person, DiscreteModeChoiceTrip trip,
            List<TripCandidate> previousTrips, List<? extends PlanElement> elements) {
        if (personFilter.applies(person.getId())) {
            boolean originInside = insideLinkIds.contains(trip.getOriginActivity().getLinkId());
            boolean destinationInside = insideLinkIds.contains(trip.getDestinationActivity().getLinkId());

            if (!originInside && !destinationInside) {
                // not a stopping trip, so if we still find a crossing after routing, we will
                // penalize it
                boolean isCrossing = false;

                for (PlanElement element : elements) {
                    if (element instanceof Leg leg) {
                        if (leg.getRoute() instanceof NetworkRoute route) {
                            for (Id<Link> linkId : route.getLinkIds()) {
                                if (crossingLinkIds.contains(linkId)) {
                                    isCrossing = true;
                                    break;
                                }
                            }

                            isCrossing |= crossingLinkIds.contains(route.getStartLinkId());
                            isCrossing |= crossingLinkIds.contains(route.getEndLinkId());
                        }
                    }

                    if (isCrossing) {
                        break;
                    }
                }

                if (isCrossing) {
                    return penalty;
                }
            }
        }

        return 0.0;
    }
}
