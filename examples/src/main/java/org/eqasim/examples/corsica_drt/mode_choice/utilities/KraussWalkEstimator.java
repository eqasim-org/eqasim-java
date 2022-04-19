package org.eqasim.examples.corsica_drt.mode_choice.utilities;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.variables.WalkVariables;
import org.eqasim.examples.corsica_drt.mode_choice.predictors.KraussPersonPredictor;
import org.eqasim.examples.corsica_drt.mode_choice.predictors.KraussWalkPredictor;
import org.eqasim.examples.corsica_drt.mode_choice.variables.KraussEqasimPersonVariables;
import org.eqasim.examples.corsica_drt.sharingPt.SharingPTParameters;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class KraussWalkEstimator  implements UtilityEstimator {


    private final SharingPTParameters parameters;
    private final KraussWalkPredictor predictor;


    @Inject
    public KraussWalkEstimator(SharingPTParameters parameters, KraussWalkPredictor predictor) {
        this.parameters = parameters;
        this.predictor = predictor;

    }



    protected double estimateConstantUtility() {
        return parameters.walk.personConstant;
    }
    protected double estimatePersonalUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements){
        KraussPersonPredictor personPredictor=new KraussPersonPredictor();
        KraussEqasimPersonVariables personVariables=personPredictor.predictVariables(person,trip,elements);
        double ageU=personVariables.age_a*parameters.walk.betaAge;
        double bikeAcc=personVariables.getBikeAcc()*parameters.walk.betaBikeAcc;
        double pTAcc=personVariables.getPtPass()*parameters.walk.betaPTPass;
        double carAcc=personVariables.getCarAccessibility()+parameters.walk.betaCarAcc;
        return(ageU+bikeAcc+pTAcc+carAcc);
    }
    protected double estimateTravelTimeUtility(WalkVariables variables) {
        return parameters.walk.betaTravelTime_u_min * variables.travelTime_min;
    }

    @Override
    public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        WalkVariables variables = predictor.predict(person, trip, elements);

        double utility = 0.0;

        utility += estimateConstantUtility();
        utility += estimateTravelTimeUtility(variables);
        utility+=estimatePersonalUtility(person,trip,elements);
        return utility;
    }
}
