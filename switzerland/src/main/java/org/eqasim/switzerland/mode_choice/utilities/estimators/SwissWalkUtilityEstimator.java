package org.eqasim.switzerland.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.WalkUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.WalkPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.WalkVariables;
import org.eqasim.switzerland.mode_choice.parameters.SwissModeParameters;
import org.eqasim.switzerland.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.mode_choice.utilities.predictors.SwissTripPredictor;
import org.eqasim.switzerland.mode_choice.utilities.variables.SwissPersonVariables;
import org.eqasim.switzerland.mode_choice.utilities.variables.SwissTripVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class SwissWalkUtilityEstimator extends WalkUtilityEstimator {
    private final SwissModeParameters swissModeParameters;
    private final SwissPersonPredictor swissPersonPredictor;
    private final SwissTripPredictor swissTripPredictor;
    private final WalkPredictor walkPredictor;


    @Inject
    public SwissWalkUtilityEstimator(SwissModeParameters swissModeParameters, SwissPersonPredictor swissPersonPredictor,
                                     SwissTripPredictor swissTripPredictor, WalkPredictor walkPredictor) {
        super(swissModeParameters, walkPredictor);
        this.swissModeParameters = swissModeParameters;
        this.swissPersonPredictor = swissPersonPredictor;
        this.swissTripPredictor = swissTripPredictor;
        this.walkPredictor = walkPredictor;

    }

    protected double estimateAgeUtility(SwissPersonVariables personVariables) {
        return swissModeParameters.swissWalk.betaAge * personVariables.age;
    }

    protected double estimateFemaleUtility(SwissPersonVariables personVariables) {
        if (personVariables.isFemale){
            return swissModeParameters.swissWalk.betaIsFemale;}
        else {
            return 0.0;
        }
    }

    protected double estimateWorkTripUtility(SwissTripVariables tripVariables) {
        if (tripVariables.isWorkTrip){
            return swissModeParameters.swissWalk.betaIsWorkTrip;}
        else {
            return 0.0;
        }
    }

    protected double estimateTravelTimeUtility(WalkVariables walkVariables) {
        return swissModeParameters.swissWalk.betaTravelTime_hour * walkVariables.travelTime_min/60;
    }

    @Override
    public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        SwissPersonVariables personVariables = swissPersonPredictor.predictVariables(person, trip, elements);
        WalkVariables walkVariables = walkPredictor.predictVariables(person, trip, elements);
        SwissTripVariables tripVariables = swissTripPredictor.predictVariables(person, trip, elements);

        double utility = 0.0;

        utility += super.estimateUtility(person, trip, elements);
        utility += estimateAgeUtility(personVariables);
        utility += estimateFemaleUtility(personVariables);
        utility += estimateWorkTripUtility(tripVariables);
        utility += estimateTravelTimeUtility(walkVariables);

        return utility;
    }
}
