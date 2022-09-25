package org.eqasim.switzerland.mode_choice.utilities.predictors;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.switzerland.mode_choice.utilities.variables.SwissCarVariables;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class SwissCarPredictor extends CachedVariablePredictor<SwissCarVariables> {

    private final CostModel costModel;
    private final ModeParameters parameters;

    @Inject
    public SwissCarPredictor(ModeParameters parameters, @Named("car") CostModel costModel) {
        this.costModel = costModel;
        this.parameters = parameters;
    }

    @Override
    public SwissCarVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {

        Leg leg = (Leg) elements.get(2);
        double travelTime_min = leg.getTravelTime().seconds() / 60.0 + parameters.car.constantParkingSearchPenalty_min;
        double cost_MU = costModel.calculateCost_MU(person, trip, elements);

        double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);
        double accessEgressTime_min = parameters.car.constantAccessEgressWalkTime_min;

        double routedDistance_km = leg.getRoute().getDistance()/1000.0;

        return new SwissCarVariables(travelTime_min, cost_MU, euclideanDistance_km, accessEgressTime_min,routedDistance_km);
    }
}
