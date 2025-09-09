package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.estimators;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.BikeUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.BikePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.BikeVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissCmdpModeParameters;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;


public class SwissBikeDetailedUtilityEstimator extends BikeUtilityEstimator {
    private final SwissCmdpModeParameters parameters;
    private final SwissPersonPredictor personPredictor;
    private final BikePredictor bikePredictor;

    @Inject
    public SwissBikeDetailedUtilityEstimator(SwissCmdpModeParameters parameters, SwissPersonPredictor personPredictor,
                                             BikePredictor bikePredictor) {
        super(parameters, personPredictor.delegate, bikePredictor);

        this.parameters = parameters;
        this.personPredictor = personPredictor;
        this.bikePredictor = bikePredictor;
    }

    protected double estimateConstantUtility() {
        return parameters.bike.alpha_u;
    }

    protected double estimateTravelTimeUtility(BikeVariables variables) {
        return parameters.bike.betaTravelTime_u_min * Math.pow(variables.travelTime_min, parameters.bike.travelTimeExponent);
    }

    protected double estimateAgeUtility(SwissPersonVariables personVariables) {
        return parameters.bike.betaAge_u * Math.max(0.0, personVariables.age_a - 18);
    }

    protected double estimateSexUtility(SwissPersonVariables personVariables) {
        return personVariables.sex==1 ? parameters.bike.betaSex_u :0.0;
    }

    protected double estimateHomeOriginUtility(DiscreteModeChoiceTrip trip) {
        return Utils.originIsHome(trip) ? parameters.bike.betaOriginHome_u : 0.0;
    }

    protected double estimateRegionalUtility(SwissPersonVariables personVariables) {
        if (personVariables.cantonCluster == 1) {
            return parameters.bike.betaRegion1_u;
        } else if (personVariables.cantonCluster == 2) {
            return parameters.bike.betaRegion2_u;
        } else {
            return 0.0;
        }
    }

    protected double estimateShortDistanceUtility(DiscreteModeChoiceTrip trip) {
        return Utils.isShortDistanceTrip(trip)? parameters.bike.betaShortDistance_u :0.0;
    }

    protected double estimateUrbanDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsUrban(trip) ? parameters.bike.betaUrbanDestination_u : 0.0;
    }

    protected double estimateWorkDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsWork(trip) ? parameters.bike.betaDestinationWork_u : 0.0;
    }

    @Override
    public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        SwissPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
        BikeVariables bikeVariables = bikePredictor.predictVariables(person, trip, elements);

        double utility = 0.0;
        utility += estimateConstantUtility();
        utility += estimateTravelTimeUtility(bikeVariables);

        utility += estimateAgeUtility(personVariables);
        utility += estimateSexUtility(personVariables);
        utility += estimateRegionalUtility(personVariables);
        utility += estimateHomeOriginUtility(trip);
        utility += estimateShortDistanceUtility(trip);
        utility += estimateUrbanDestinationUtility(trip);
        utility += estimateWorkDestinationUtility(trip);

        return utility;
    }

}