package org.eqasim.examples.SMMFramework.SMMBaseModeChoice.predictors;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.variables.KraussCarVariables;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.variables_parameters.SMMParameters;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class KraussCarPredictor  extends CachedVariablePredictor<KraussCarVariables> {
    private final CostModel costModel;
    private final SMMParameters parameters;

    @Inject
    public KraussCarPredictor(@Named("car") CostModel costModel, SMMParameters parameters) {
        this.costModel = costModel;
        this.parameters = parameters;
    }

    public KraussCarPredictor(CostModel costModel, SMMParameters parameters, Boolean isStatic) {
        this.costModel = costModel;
        this.parameters = parameters;
    }
    @Override
    public KraussCarVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        Leg leg= (Leg) elements.get(0);
        double egressTime=parameters.car.egressTime/60;
        double accessTime=parameters.car.accessTime/60;
        double parkingTime=parameters.car.parkingTime/60;
        double cost=costModel.calculateCost_MU(person,trip,elements);
        double travelTime=leg.getTravelTime().seconds()/60;
        return (new KraussCarVariables(travelTime,accessTime,egressTime,parkingTime,cost));
    }
}
