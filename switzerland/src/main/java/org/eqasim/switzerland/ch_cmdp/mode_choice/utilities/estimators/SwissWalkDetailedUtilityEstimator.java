package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.estimators;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.WalkUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.WalkPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.WalkVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissCmdpModeParameters;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class SwissWalkDetailedUtilityEstimator extends WalkUtilityEstimator {
    private final SwissCmdpModeParameters parameters;
    private final WalkPredictor walkPredictor;
    private final SwissPersonPredictor personPredictor;

    @Inject
    public SwissWalkDetailedUtilityEstimator(SwissCmdpModeParameters parameters, WalkPredictor predictor,
                                             SwissPersonPredictor personPredictor) {
        super(parameters, predictor);

        this.walkPredictor = predictor;
        this.parameters = parameters;
        this.personPredictor = personPredictor;
    }

    protected double estimateConstantUtility() {
        return parameters.walk.alpha_u;
    }

    protected double estimateTravelTimeUtility(WalkVariables variables) {
        return parameters.walk.betaTravelTime_u_min * Math.pow(variables.travelTime_min, parameters.walk.travelTimeExponent);
    }

    protected double estimateRegionalUtility(SwissPersonVariables personVariables) {
        if (personVariables.cantonCluster == 1) {
            return parameters.walk.betaRegion1_u;
        } else if (personVariables.cantonCluster == 2) {
            return parameters.walk.betaRegion2_u;
        } else {
            return 0.0;
        }
    }

    protected double estimateAgeUtility(SwissPersonVariables personVariables) {
        return parameters.walk.betaAge_u * Math.max(0.0, personVariables.age_a - 18);
    }

    protected double estimateSexUtility(SwissPersonVariables personVariables) {
        return personVariables.sex==1 ? parameters.walk.betaSex_u :0.0;
    }

    protected double estimateShortDistanceUtility(DiscreteModeChoiceTrip trip) {
        return Utils.isShortDistanceTrip(trip)? parameters.walk.betaShortDistance_u :0.0;
    }

    protected double estimateHomeOriginUtility(DiscreteModeChoiceTrip trip) {
        return Utils.originIsHome(trip) ? parameters.walk.betaOriginHome_u : 0.0;
    }

    protected double estimateUrbanDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsUrban(trip) ? parameters.walk.betaUrbanDestination_u : 0.0;
    }

    protected double estimateWorkDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsWork(trip) ? parameters.walk.betaDestinationWork_u : 0.0;
    }

    @Override
    public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        WalkVariables variables = walkPredictor.predictVariables(person, trip, elements);
        SwissPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);

        double utility = 0.0;

        utility += estimateConstantUtility();
        utility += estimateTravelTimeUtility(variables);

        utility += estimateAgeUtility(personVariables);
        utility += estimateSexUtility(personVariables);
        utility += estimateRegionalUtility(personVariables);
        utility += estimateShortDistanceUtility(trip);
        utility += estimateHomeOriginUtility(trip);
        utility += estimateUrbanDestinationUtility(trip);
        utility += estimateWorkDestinationUtility(trip);

        return utility;
    }

}
