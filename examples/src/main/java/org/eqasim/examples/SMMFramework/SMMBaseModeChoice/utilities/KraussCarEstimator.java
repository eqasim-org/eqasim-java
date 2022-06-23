package org.eqasim.examples.SMMFramework.SMMBaseModeChoice.utilities;


import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.predictors.KraussCarPredictor;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.predictors.KraussPersonPredictor;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.variables.KraussCarVariables;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.variables_parameters.SMMParameters;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class KraussCarEstimator implements UtilityEstimator {
    private final SMMParameters parameters;
    private final KraussCarPredictor predictor;


    @Inject
    public KraussCarEstimator(SMMParameters parameters, KraussCarPredictor predictor, KraussPersonPredictor personPredictor) {
        this.parameters = parameters;
        this.predictor = predictor;

    }
    public KraussCarEstimator(SMMParameters parameters, KraussCarPredictor predictor, KraussPersonPredictor personPredictor, Boolean isStatic) {
        this.parameters = parameters;
        this.predictor = predictor;

    }


    protected double estimateTravelTimeUtility(KraussCarVariables variables) {
        return parameters.car.betaTravelTime_u_min * variables.travelTime_u_min;
    }

    protected double estimateAccessTimeUtility(KraussCarVariables variables) {
        return parameters.car.betaAccess_Time * variables.access_Time;
    }

    protected double estimateMonetaryCostUtility(KraussCarVariables variables) {
        return -Math.exp(parameters.betaCost_u_MU) * variables.cost;
    }

    protected double estimateEgressTimeUtility(KraussCarVariables variables) {
        return parameters.car.betaEgress_Time* variables.egress_Time;
    }

    protected double estimateParkingTimeUtility(KraussCarVariables variables){
        return parameters.car.betaParkingTime_u_min*variables.parkingTime_u_min;
    }


    @Override
    public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        KraussCarVariables variables = predictor.predict(person, trip, elements);

        double utility = 0.0;
        utility += estimateTravelTimeUtility(variables);
        utility += estimateAccessTimeUtility(variables);
        utility += estimateMonetaryCostUtility(variables);
        utility += estimateEgressTimeUtility(variables);
        utility+= estimateParkingTimeUtility(variables);
        utility=utility*parameters.car.pool;

        return utility;
    }
}
