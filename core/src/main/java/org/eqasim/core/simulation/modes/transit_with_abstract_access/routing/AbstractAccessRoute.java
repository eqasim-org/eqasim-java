package org.eqasim.core.simulation.modes.transit_with_abstract_access.routing;

import org.eqasim.core.simulation.modes.transit_with_abstract_access.abstract_access.AbstractAccessItem;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Route;

public interface AbstractAccessRoute extends Route {
    Id<AbstractAccessItem> getAbstractAccessItemId();

    boolean isLeavingAccessCenter();

    boolean isRouted();

    double getWaitTime();
}
