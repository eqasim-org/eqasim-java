package org.eqasim.core.components.transit_with_abstract_access.routing;

import org.eqasim.core.components.transit_with_abstract_access.abstract_access.AbstractAccessItem;
import org.matsim.api.core.v01.population.Route;

public interface AbstractAccessRoute extends Route {
    AbstractAccessItem getAbstractAccessItem();
}
