package org.eqasim.examples.corsica_drt.mode_choice.predictors;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.examples.corsica_drt.mode_choice.variables.KraussCarVariables;
import org.eqasim.examples.corsica_drt.sharingPt.SharingPTParameters;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class KraussCarPredictor  extends CachedVariablePredictor<KraussCarVariables> {
    private final CostModel costModel;
    private final SharingPTParameters parameters;

    @Inject
    public KraussCarPredictor(@Named("car") CostModel costModel, SharingPTParameters parameters) {
        this.costModel = costModel;
        this.parameters = parameters;
    }

    @Override
    public KraussCarVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        Leg leg= (Leg) elements.get(0);
        double egressTime=parameters.car.egressTime;
        double accessTime=parameters.car.accessTime;
        double parkingTime=parameters.car.parkingTime;
        double cost=costModel.calculateCost_MU(person,trip,elements);
        double travelTime=leg.getTravelTime().seconds();
        return (new KraussCarVariables(travelTime,accessTime,egressTime,parkingTime,cost));
    }
}
