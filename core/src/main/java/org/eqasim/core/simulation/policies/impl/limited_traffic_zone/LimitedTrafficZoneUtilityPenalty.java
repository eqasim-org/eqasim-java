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
import org.matsim.core.population.routes.NetworkRoute;

public class LimitedTrafficZoneUtilityPenalty implements UtilityPenalty {
    private final IdSet<Link> links;
    private final PolicyPersonFilter personFilter;
    private final double penalty;

    public LimitedTrafficZoneUtilityPenalty(double penalty, IdSet<Link> links, PolicyPersonFilter personFilter) {
        this.personFilter = personFilter;
        this.links = links;
        this.penalty = penalty;
    }

    @Override
    public double calculatePenalty(String mode, Person person, DiscreteModeChoiceTrip trip,
            List<? extends PlanElement> elements) {
        if (personFilter.applies(person.getId())) {
            if (!links.contains(trip.getOriginActivity().getLinkId())) {
                if (!links.contains(trip.getDestinationActivity().getLinkId())) {
                    // not a stopping trip
                    boolean isCrossing = false;

                    for (PlanElement element : elements) {
                        if (element instanceof Leg leg) {
                            if (leg.getRoute() instanceof NetworkRoute route) {
                                for (Id<Link> linkId : route.getLinkIds()) {
                                    if (links.contains(linkId)) {
                                        isCrossing = true;
                                        break;
                                    }
                                }
                            }
                        }

                        if (isCrossing)
                            break;
                    }

                    if (isCrossing) {
                        return penalty;
                    }
                }
            }
        }

        return 0.0;
    }
}
