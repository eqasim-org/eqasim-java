package org.eqasim.core.simulation.modes.drt.mode_choice.constraints;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.AbstractTripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.RoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.core.config.Config;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Note, this class can be made even more generic into something like a LegModeConstraint that checks if a leg with a certain mode is contained in a route.
 * Then separate factories can be written outside of this class.
 */
public class DrtWalkConstraint extends AbstractTripConstraint {

    private final Set<String> drtModes;

    public DrtWalkConstraint(Set<String> drtModes) {
        this.drtModes = drtModes;
    }

    public boolean validateAfterEstimation(DiscreteModeChoiceTrip trip, TripCandidate candidate, List<TripCandidate> previousCandidates) {
        if(this.drtModes.contains(candidate.getMode())) {
            if (candidate instanceof RoutedTripCandidate) {
                RoutedTripCandidate routedTripCandidate = (RoutedTripCandidate) candidate;
                return routedTripCandidate.getRoutedPlanElements().stream()
                        .filter(planElement -> planElement instanceof Leg)
                        .map(planElement -> (Leg) planElement)
                        .map(Leg::getMode)
                        .anyMatch(candidate.getMode()::equals);
            }
        }
        return true;
    }


    public static class Factory implements TripConstraintFactory {

        private Set<String> drtModes = new HashSet<>();

        @Inject
        public Factory(Config config) {
            if(config.getModules().containsKey(MultiModeDrtConfigGroup.GROUP_NAME)) {
                MultiModeDrtConfigGroup multiModeDrtConfigGroup = (MultiModeDrtConfigGroup) config.getModules().get(MultiModeDrtConfigGroup.GROUP_NAME);
                drtModes = multiModeDrtConfigGroup.modes().collect(Collectors.toSet());
            }
        }

        @Override
        public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> list, Collection<String> collection) {
            return new DrtWalkConstraint(drtModes);
        }
    }
}
