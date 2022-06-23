package org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.estimators;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.predictors.SMMEScooterPredictor;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.predictors.KraussPersonPredictor;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.variables.KraussEScooterVariables;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.variables.KraussEqasimPersonVariables;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.variables_parameters.SMMParameters;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class SMMEScooterEstimator implements UtilityEstimator {
    private final SMMParameters parameters;
    private final SMMEScooterPredictor predictor;

    @Inject
    public SMMEScooterEstimator(SMMParameters parameters, SMMEScooterPredictor predictor, KraussPersonPredictor personPredictor) {
        this.parameters = parameters;
        this.predictor = predictor;

    }

    public SMMEScooterEstimator(SMMParameters parameters, SMMEScooterPredictor predictor, KraussPersonPredictor personPredictor, String name) {
        this.parameters = parameters;
        this.predictor = predictor;

    }

    protected double estimateConstantUtility() {
        return parameters.bikeShare.alpha_u;
    }
    protected double estimatePersonalUtility(Person person, DiscreteModeChoiceTrip trip,List<? extends PlanElement> elements){
        KraussPersonPredictor personPredictor=new KraussPersonPredictor();
        KraussEqasimPersonVariables personVariables=personPredictor.predictVariables(person,trip,elements);
        double ageU=personVariables.age_a*parameters.eScooter.betaAge;
        double bikeAcc=personVariables.getBikeAcc()*parameters.eScooter.betaBikeAcc;
        double carAcc=personVariables.getCarAccessibility()*parameters.eScooter.betaCarAcc;
        double pTAcc=personVariables.getPtPass()*parameters.eScooter.betaPTPass;
        return(ageU+bikeAcc+carAcc+pTAcc);
    }
    protected double estimateTravelTimeUtility(KraussEScooterVariables variables) {
        return parameters.eScooter.betaTravelTime_u_min * variables.travelTime_u_min;
    }

    protected double estimateAccessTimeUtility(KraussEScooterVariables variables) {
        return parameters.eScooter.betaAccess_Time * variables.access_Time;
    }

    protected double estimateMonetaryCostUtility(KraussEScooterVariables variables) {
        return -Math.exp(parameters.betaCost_u_MU) * variables.cost;
    }

    protected double estimateEgressTimeUtility(KraussEScooterVariables variables) {
        return parameters.eScooter.betaEgress_Time* variables.egress_Time;
    }

    protected double estimateParkingTimeUtility(KraussEScooterVariables variables){
        return parameters.eScooter.betaParkingTime_u_min*variables.parkingTime_u_min;
    }



    @Override
    public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        KraussEScooterVariables variables = predictor.predict(person, trip, elements);

        double utility = 0.0;

        utility += estimateConstantUtility();
        utility += estimateTravelTimeUtility(variables);
        utility += estimateAccessTimeUtility(variables);
        utility += estimateMonetaryCostUtility(variables);
        utility += estimateEgressTimeUtility(variables);
        utility+= estimateParkingTimeUtility(variables);

        utility+=estimatePersonalUtility(person,trip,elements);
        return utility;
    }


}

