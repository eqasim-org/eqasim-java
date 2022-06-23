package org.eqasim.examples.SMMFramework.SMMBaseModeChoice.utilities;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.parameters.KraussModeParameters;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.predictors.KraussPTPredictorMultimodal;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.predictors.KraussPersonPredictor;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.variables.KraussEqasimPersonVariables;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.variables.KraussPTVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class KraussPTEstimatorMultimodal implements UtilityEstimator {
    private final KraussModeParameters parameters;
    private final KraussPTPredictorMultimodal predictor;


    @Inject
    public KraussPTEstimatorMultimodal(KraussModeParameters parameters, KraussPTPredictorMultimodal predictor, KraussPersonPredictor personPredictor) {
        this.parameters = parameters;
        this.predictor = predictor;

    }



    protected double estimateConstantUtility() {
        return parameters.pt.personConstant;
    }
    protected double estimatePersonalUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements){
        KraussPersonPredictor personPredictor=new KraussPersonPredictor();
        KraussEqasimPersonVariables personVariables=personPredictor.predictVariables(person,trip,elements);
        double ageU=personVariables.age_a*parameters.pt.betaAge;
        double bikeAcc=personVariables.getBikeAcc()*parameters.pt.betaBikeAcc;
        double pTAcc=personVariables.getPtPass()*parameters.pt.betaPTPass;
        double carAcc=personVariables.getCarAccessibility()+parameters.pt.betaCarAcc;
        return(ageU+bikeAcc+pTAcc+carAcc);
    }
    protected double estimateTravelTimeUtility(KraussPTVariables variables) {
        return parameters.pt.betaTravelTime_u_min * variables.travelTime_u_min;
    }

    protected double estimateAccessTimeUtility(KraussPTVariables variables) {
        return parameters.pt.betaAccess_Time * variables.access_Time;
    }

    protected double estimateMonetaryCostUtility(KraussPTVariables variables) {
        return parameters.betaCost_u_MU * variables.cost;
    }

    protected double estimateEgressTimeUtility(KraussPTVariables variables) {
        return parameters.pt.betaEgress_Time* variables.egress_Time;
    }
    protected double estimateChangeLineUtility(KraussPTVariables variables){
        return parameters.pt.betaTransfers*variables.transfers;
    }


    @Override
    public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        KraussPTVariables variables = predictor.predict(person, trip, elements);

        double utility = 0.0;

        utility += estimateConstantUtility();
        utility += estimateTravelTimeUtility(variables);
        utility += estimateAccessTimeUtility(variables);
        utility += estimateMonetaryCostUtility(variables);
        utility += estimateEgressTimeUtility(variables);
        utility +=estimateChangeLineUtility(variables);
        utility+=estimatePersonalUtility(person,trip,elements);
        return utility;
    }
}
