package org.eqasim.core.components.network_calibration.demand_calibration.subpopulations;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

final class State {
    static final String REMOVED_ATTRIBUTE = "removed";
    static final String CLONED_ATTRIBUTE = "cloned";

    private final Map<Id<Person>, StoredPersonPlan> removedPersonPlans = new HashMap<>();
    private int calibrationSteps = 0;

    void incrementCalibrationSteps() {
        calibrationSteps++;
    }

    int getCalibrationSteps() {
        return calibrationSteps;
    }

    boolean isRemoved(Id<Person> personId) {
        return removedPersonPlans.containsKey(personId);
    }

    Map<Id<Person>, StoredPersonPlan> removedPlansView() {
        return removedPersonPlans;
    }

    Set<Id<Person>> removedPersonIds() {
        return Set.copyOf(removedPersonPlans.keySet());
    }

    void markRemoved(Person person, Plan originalPlan) {
        person.getAttributes().putAttribute(REMOVED_ATTRIBUTE, true);
        removedPersonPlans.put(person.getId(), new StoredPersonPlan(originalPlan));
    }

    void markRestored(Person person) {
        person.getAttributes().putAttribute(REMOVED_ATTRIBUTE, false);
        removedPersonPlans.remove(person.getId());
    }

    StoredPersonPlan getStoredPlan(Id<Person> personId) {
        return removedPersonPlans.get(personId);
    }

    record StoredPersonPlan(Plan originalPlan) {
    }
}
