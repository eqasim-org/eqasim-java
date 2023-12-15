package org.eqasim.core.simulation.modes.transit_access.teleported.teleported.routing;

import org.eqasim.core.simulation.modes.transit_access.teleported.teleported.abstract_access.AbstractAccessItem;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Route;

public interface AbstractAccessRoute extends Route {
    Id<AbstractAccessItem> getAbstractAccessItemId();

    boolean isLeavingAccessCenter();

    boolean isRouted();

    double getWaitTime();
}
