package org.eqasim.core.components.network_calibration.cost_calibration;

import org.eqasim.core.simulation.policies.routing.RoutingPenalty;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

public class RoutingPenaltyByLinkCategory implements RoutingPenalty {
    private final PenaltiesAdapter penalties;
    private final RoutingPenalty delegate;

    public RoutingPenaltyByLinkCategory(PenaltiesAdapter penalties, RoutingPenalty delegate) {
        this.penalties = penalties;
        this.delegate = delegate;
    }

    @Override
    public double getLinkPenalty(Link link, Person person, double time, double baseDisutility) {
        return delegate.getLinkPenalty(link, person, time, baseDisutility) + penalties.computePenalty(link);
    }
}