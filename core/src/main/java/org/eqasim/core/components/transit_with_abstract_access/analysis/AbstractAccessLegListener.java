package org.eqasim.core.components.transit_with_abstract_access.analysis;

import com.google.inject.Inject;
import org.eqasim.core.components.transit_with_abstract_access.abstract_access.AbstractAccesses;
import org.eqasim.core.components.transit_with_abstract_access.events.AbstractAccessDepartureEvent;
import org.eqasim.core.components.transit_with_abstract_access.events.AbstractAccessDepartureEventHandler;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.core.router.TripStructureUtils;


public class AbstractAccessLegListener implements MobsimScopeEventHandler, PersonDepartureEventHandler, ActivityStartEventHandler, AbstractAccessDepartureEventHandler {

    private final IdMap<Person, Integer> tripIndices = new IdMap<>(Person.class);
    private final IdMap<Person, Integer> legIndices = new IdMap<>(Person.class);

    @Inject
    public AbstractAccessLegListener(AbstractAccesses abstractAccesses) {

    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (!tripIndices.containsKey(event.getPersonId())) {
            tripIndices.put(event.getPersonId(), 0);
            legIndices.put(event.getPersonId(), 0);
        } else {
            tripIndices.compute(event.getPersonId(), (k, v) -> v + 1);
            legIndices.compute(event.getPersonId(), (k, v) -> v + 1);
        }
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (TripStructureUtils.isStageActivityType(event.getActType())) {
            tripIndices.computeIfPresent(event.getPersonId(), (k, v) -> v - 1);
        }
    }

    @Override
    public void reset(int iteration) {
        this.tripIndices.clear();
        this.legIndices.clear();
    }

    @Override
    public void handleEvent(AbstractAccessDepartureEvent event) {
        event.getEventType();
    }
}
