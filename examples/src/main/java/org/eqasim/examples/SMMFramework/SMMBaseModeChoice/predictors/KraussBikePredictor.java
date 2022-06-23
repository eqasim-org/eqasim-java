package org.eqasim.examples.SMMFramework.SMMBaseModeChoice.predictors;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.parameters.KraussModeParameters;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.variables.KraussBikeVariables;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class KraussBikePredictor  extends CachedVariablePredictor<KraussBikeVariables> {


    @Override
    public KraussBikeVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        double travelTime_min = ((Leg) elements.get(0)).getTravelTime().seconds() / 60.0;
        KraussModeParameters params=KraussModeParameters.buildDefault();

        return new KraussBikeVariables(travelTime_min,params.bike.accessTime,params.bike.egressTime,0,0);
    }
}
