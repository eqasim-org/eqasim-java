package org.eqasim.switzerland.mode_choice.utilities.predictors;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.switzerland.mode_choice.utilities.variables.SwissCarVariables;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class SwissCarPredictor extends CachedVariablePredictor<SwissCarVariables> {
    public final CarPredictor delegate;

    @Inject
    public SwissCarPredictor(CarPredictor carPredictor) {
        this.delegate = carPredictor;
    }

    @Override
    public SwissCarVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {

        Leg leg = (Leg) elements.get(0);
        double routedDistance = leg.getRoute().getDistance();

        return new SwissCarVariables(delegate.predict(person,trip,elements),routedDistance);//g/ what unit is distance?
    }
}
