package org.eqasim.switzerland.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.switzerland.mode_choice.utilities.variables.SwissTripVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;


public class SwissTripPredictor extends CachedVariablePredictor<SwissTripVariables> {

    public SwissTripVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {

        boolean isWorkTrip = false;
        if (trip.getDestinationActivity().getType().equals("work")){
            isWorkTrip = true;
        }

        return new SwissTripVariables(isWorkTrip);
    }
}
