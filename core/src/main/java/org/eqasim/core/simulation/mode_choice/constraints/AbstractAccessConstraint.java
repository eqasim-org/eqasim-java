package org.eqasim.core.simulation.mode_choice.constraints;

import com.google.inject.Inject;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.AbstractAccessModuleConfigGroup;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.routing.TransitWithAbstractAccessRoutingModule;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.RoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;

import java.util.Collection;
import java.util.List;

public class AbstractAccessConstraint implements TripConstraint {

    private final String transitWithAbstractAccessModeName;

    public AbstractAccessConstraint(String transitWithAbstractAccessModeName) {
        this.transitWithAbstractAccessModeName = transitWithAbstractAccessModeName;
    }

    @Override
    public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {
        return true;
    }

    @Override
    public boolean validateAfterEstimation(DiscreteModeChoiceTrip trip, TripCandidate candidate, List<TripCandidate> previousCandidates) {
        RoutedTripCandidate routedTripCandidate = (RoutedTripCandidate) candidate;
        if(!candidate.getMode().equals(this.transitWithAbstractAccessModeName)) {
            return true;
        }
        boolean foundPt = false;
        boolean foundAbstractAccess = false;
        for(PlanElement planElement: routedTripCandidate.getRoutedPlanElements()) {
            if(planElement instanceof Leg) {
                String mode = ((Leg) planElement).getMode();
                if(mode.equals(TransitWithAbstractAccessRoutingModule.ABSTRACT_ACCESS_LEG_MODE_NAME)){
                    foundAbstractAccess = true;
                }
                if(mode.equals(TransportMode.pt)) {
                    foundPt = true;
                }
            }
        }
        return foundAbstractAccess && foundPt;
    }

    static public class Factory implements TripConstraintFactory {

        private final String transitWithAbstractAccessModeName;

        @Inject
        public Factory(Config config) {
            ConfigGroup configGroup = config.getModules().get(AbstractAccessModuleConfigGroup.ABSTRACT_ACCESS_GROUP_NAME);
            if(configGroup == null) {
                throw new IllegalStateException("AbstractAccessConstraint cannot be used if the " + AbstractAccessModuleConfigGroup.ABSTRACT_ACCESS_GROUP_NAME + " module is not specified in the config");
            }
            this.transitWithAbstractAccessModeName = ((AbstractAccessModuleConfigGroup) configGroup).getModeName();
        }

        @Override
        public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
                                               Collection<String> availableModes) {
            return new AbstractAccessConstraint(transitWithAbstractAccessModeName);
        }
    }
}
