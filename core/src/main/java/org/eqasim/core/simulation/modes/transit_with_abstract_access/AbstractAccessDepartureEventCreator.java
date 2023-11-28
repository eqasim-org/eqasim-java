package org.eqasim.core.simulation.modes.transit_with_abstract_access;

import com.google.inject.Inject;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.routing.TransitWithAbstractAccessRoutingModule;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.routing.AbstractAccessRoute;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.events.AbstractAccessDepartureEvent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;

public class AbstractAccessDepartureEventCreator implements DepartureHandler {

    private final EventsManager eventsManager;
    @Inject
    public AbstractAccessDepartureEventCreator(EventsManager eventsManager) {
        this.eventsManager = eventsManager;
    }

    @Override
    public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {
        if(agent.getMode().equals(TransitWithAbstractAccessRoutingModule.ABSTRACT_ACCESS_LEG_MODE_NAME)) {
            Leg leg = (Leg) ((PlanAgent) agent).getCurrentPlanElement();
            AbstractAccessRoute abstractAccessRoute = (AbstractAccessRoute) leg.getRoute();
            this.eventsManager.processEvent(new AbstractAccessDepartureEvent(now, agent.getId(), abstractAccessRoute.getAbstractAccessItemId(), abstractAccessRoute.getStartLinkId(), abstractAccessRoute.getEndLinkId(), abstractAccessRoute.isLeavingAccessCenter(), abstractAccessRoute.isRouted(), abstractAccessRoute.getDistance()));
        }
        return false;
    }
}
