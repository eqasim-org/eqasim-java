package org.eqasim.switzerland.mode_choice.utilities.predictors;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PtPredictor;
import org.eqasim.switzerland.mode_choice.utilities.variables.SwissPtVariables;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.pt.routes.TransitPassengerRoute;

import java.util.List;

public class SwissPtPredictor extends CachedVariablePredictor<SwissPtVariables> {
    public final PtPredictor ptPredictor;

    @Inject
    public SwissPtPredictor(PtPredictor ptPredictor) {
        this.ptPredictor = ptPredictor;
    }

    @Override
    public SwissPtVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {

        Leg leg = (Leg) elements.get(0);
        double routedInVehicleDistance_km = 0.0;
        for (PlanElement pe : elements) {
            if (pe instanceof Leg) {
                if (((Leg) pe).getMode().equals("pt")) {
                    routedInVehicleDistance_km += ((TransitPassengerRoute) ((Leg) pe).getRoute()).getDistance()/1000.0;
                }
            }
        }
        return new SwissPtVariables(ptPredictor.predict(person,trip,elements),routedInVehicleDistance_km);
    }
}