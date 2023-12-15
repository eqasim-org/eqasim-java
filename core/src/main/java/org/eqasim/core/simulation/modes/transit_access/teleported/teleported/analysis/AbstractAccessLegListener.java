package org.eqasim.core.simulation.modes.transit_access.teleported.teleported.analysis;

import com.google.inject.Inject;
import org.eqasim.core.simulation.modes.transit_access.teleported.teleported.abstract_access.AbstractAccessItem;
import org.eqasim.core.simulation.modes.transit_access.teleported.teleported.abstract_access.AbstractAccesses;
import org.eqasim.core.simulation.modes.transit_access.teleported.teleported.events.AbstractAccessDepartureEvent;
import org.eqasim.core.simulation.modes.transit_access.teleported.teleported.events.AbstractAccessDepartureEventHandler;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.core.router.TripStructureUtils;

import java.util.ArrayList;
import java.util.Collection;


public class AbstractAccessLegListener implements MobsimScopeEventHandler, PersonDepartureEventHandler, ActivityStartEventHandler, AbstractAccessDepartureEventHandler, PersonArrivalEventHandler {

    private final IdMap<Person, Integer> tripIndices = new IdMap<>(Person.class);
    private final IdMap<Person, Integer> legIndices = new IdMap<>(Person.class);
    private final IdMap<Person, PersonDepartureEvent> personDepartureEvents = new IdMap<>(Person.class);
    private final AbstractAccesses abstractAccesses;
    private final Collection<AbstractAccessLegItem> abstractAccessLegItems = new ArrayList<>();

    @Inject
    public AbstractAccessLegListener(AbstractAccesses abstractAccesses) {
        this.abstractAccesses = abstractAccesses;
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
        this.personDepartureEvents.put(event.getPersonId(), event);
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
        this.personDepartureEvents.clear();
        this.abstractAccessLegItems.clear();
    }

    @Override
    public void handleEvent(AbstractAccessDepartureEvent event) {
        Id<Person> personId = event.getPersonId();
        Id<AbstractAccessItem> abstractAccessItemId = event.getAccessItemId();
        AbstractAccessItem abstractAccessItem = this.abstractAccesses.getAbstractAccessItems().get(abstractAccessItemId);
        AbstractAccessLegItem abstractAccessLegItem = new AbstractAccessLegItem(personId, this.tripIndices.get(personId), this.legIndices.get(personId), event.getAccessItemId(), abstractAccessItem.getCenterStop().getId(), event.isLeavingAccessCenter(), event.isRouted(), event.getDistance());
        this.abstractAccessLegItems.add(abstractAccessLegItem);
    }

    @Override
    public void handleEvent(PersonArrivalEvent personArrivalEvent) {
        this.personDepartureEvents.remove(personArrivalEvent.getPersonId());
    }

    public Collection<AbstractAccessLegItem> getAbstractAccessLegItems() {
        return this.abstractAccessLegItems;
    }
}
