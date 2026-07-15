package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors;


import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissBikesharingVariables;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.router.TripStructureUtils;

import java.util.List;


public class SwissBikesharingPredictor extends CachedVariablePredictor<SwissBikesharingVariables> {

    @Override
    public SwissBikesharingVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        double travelTime_min = 0.0;
        double accessegressTime_min = 0.0;

        for (Leg leg : TripStructureUtils.getLegs(elements)) {
            if (leg.getMode().equals(TransportMode.bike)) {
                travelTime_min += leg.getTravelTime().seconds() / 60.0;
            } else {
                if (leg.getMode().equals(TransportMode.walk)) {
                    accessegressTime_min += leg.getTravelTime().seconds() / 60.0;
                }
                else {
                    throw new IllegalStateException("Unexpected mode in bikesharing chain: " + leg.getMode());
                }
            }
        }

        return new SwissBikesharingVariables(travelTime_min, accessegressTime_min);
    }
}
