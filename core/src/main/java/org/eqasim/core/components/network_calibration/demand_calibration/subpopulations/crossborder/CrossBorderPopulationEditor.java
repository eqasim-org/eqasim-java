package org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.crossborder;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;

/** Applies and reverses cross-border removals while preserving their provenance. */
public final class CrossBorderPopulationEditor {
    private final Population population;
    private final CrossBorderState state;

    public CrossBorderPopulationEditor(Population population, CrossBorderState state) {
        this.population = population;
        this.state = state;
    }

    public boolean isRemoved(Id<Person> personId) {
        return state.isRemoved(personId);
    }

    /**
     * Removes a cross-border plan and remembers the counted border station that
     * triggered the removal. The provenance is later used to prevent unrelated
     * under-estimated links from restoring this person.
     */
    public boolean removeCrossBorderTravel(Id<Person> personId,
                                           CrossBorderStation removalStation) {
        if (removalStation == null) {
            throw new IllegalArgumentException("A cross-border removal station is required");
        }
        return removeTravel(personId, removalStation);
    }

    private synchronized boolean removeTravel(Id<Person> personId,
                                              CrossBorderStation crossBorderRemovalStation) {
        Person person;
        synchronized (population) {
            person = population.getPersons().get(personId);
        }
        if (person == null || state.isRemoved(personId)) {
            return false;
        }

        Plan originalPlan = person.getSelectedPlan();
        if (originalPlan == null) {
            return false;
        }

        Plan stayPlan = population.getFactory().createPlan();
        for (PlanElement element : originalPlan.getPlanElements()) {
            if (element instanceof Activity activity) {
                Activity stayActivity = PopulationUtils.createActivity(activity);
                stayActivity.setEndTimeUndefined();
                stayActivity.setMaximumDurationUndefined();
                stayPlan.addActivity(stayActivity);
                break;
            }
        }
        if (stayPlan.getPlanElements().isEmpty()) {
            return false;
        }

        person.removePlan(originalPlan);
        person.addPlan(stayPlan);
        person.setSelectedPlan(stayPlan);
        state.markRemoved(person, originalPlan, crossBorderRemovalStation);
        return true;
    }

    public CrossBorderStation crossBorderRemovalStation(Id<Person> personId) {
        return state.crossBorderRemovalStation(personId);
    }

    public synchronized boolean restoreTravel(Id<Person> personId) {
        Person person;
        synchronized (population) {
            person = population.getPersons().get(personId);
        }
        CrossBorderState.StoredPersonPlan storedPlan = state.getStoredPlan(personId);
        if (person == null || storedPlan == null) {
            return false;
        }

        Plan currentPlan = person.getSelectedPlan();
        if (currentPlan != null) {
            person.removePlan(currentPlan);
        }
        person.addPlan(storedPlan.originalPlan());
        person.setSelectedPlan(storedPlan.originalPlan());
        state.markRestored(person);
        return true;
    }
}
