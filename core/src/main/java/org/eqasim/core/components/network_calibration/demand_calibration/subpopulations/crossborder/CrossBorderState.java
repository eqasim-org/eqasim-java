package org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.crossborder;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class CrossBorderState {
    static final String REMOVED_ATTRIBUTE = "removed";
    static final String CLONED_ATTRIBUTE = "cloned";

    private final Map<Id<Person>, StoredPersonPlan> removedPersonPlans = new HashMap<>();

    public synchronized boolean isRemoved(Id<Person> personId) {
        return removedPersonPlans.containsKey(personId);
    }

    public synchronized Set<Id<Person>> removedPersonIds() {
        return Set.copyOf(removedPersonPlans.keySet());
    }

    public synchronized Set<Id<Person>> removedAt(CrossBorderStation station) {
        Set<Id<Person>> result = new java.util.HashSet<>();
        removedPersonPlans.forEach((personId, stored) -> {
            if (station.equals(stored.crossBorderRemovalStation())) result.add(personId);
        });
        return Set.copyOf(result);
    }

    public synchronized Plan removedPlan(Id<Person> personId) {
        StoredPersonPlan storedPlan = removedPersonPlans.get(personId);
        return storedPlan == null ? null : storedPlan.originalPlan();
    }

    public synchronized CrossBorderStation crossBorderRemovalStation(Id<Person> personId) {
        StoredPersonPlan storedPlan = removedPersonPlans.get(personId);
        return storedPlan == null ? null : storedPlan.crossBorderRemovalStation();
    }

    synchronized void markRemoved(Person person, Plan originalPlan,
                                  CrossBorderStation crossBorderRemovalStation) {
        person.getAttributes().putAttribute(REMOVED_ATTRIBUTE, true);
        removedPersonPlans.put(person.getId(), new StoredPersonPlan(originalPlan, crossBorderRemovalStation));
    }

    synchronized void markRestored(Person person) {
        person.getAttributes().putAttribute(REMOVED_ATTRIBUTE, false);
        removedPersonPlans.remove(person.getId());
    }

    synchronized StoredPersonPlan getStoredPlan(Id<Person> personId) {
        return removedPersonPlans.get(personId);
    }

    record StoredPersonPlan(Plan originalPlan,
                            CrossBorderStation crossBorderRemovalStation) {
    }
}
