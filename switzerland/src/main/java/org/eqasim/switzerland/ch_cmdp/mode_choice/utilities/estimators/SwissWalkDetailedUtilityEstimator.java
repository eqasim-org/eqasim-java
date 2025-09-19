package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.estimators;

import com.google.inject.Inject;
import org.eqasim.core.components.calibration.VariablesWriter;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.WalkUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.WalkPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.WalkVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissCmdpModeParameters;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SwissWalkDetailedUtilityEstimator extends WalkUtilityEstimator {
    private final SwissCmdpModeParameters parameters;
    private final WalkPredictor walkPredictor;
    private final SwissPersonPredictor personPredictor;
    private final VariablesWriter variablesWriter;

    @Inject
    public SwissWalkDetailedUtilityEstimator(SwissCmdpModeParameters parameters, WalkPredictor predictor,
                                             SwissPersonPredictor personPredictor, VariablesWriter variablesWriter) {
        super(parameters, predictor);

        this.walkPredictor = predictor;
        this.parameters = parameters;
        this.personPredictor = personPredictor;
        this.variablesWriter = variablesWriter;
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

    protected double estimatedLongDistanceUtility(DiscreteModeChoiceTrip trip) {
        double distance_km = PredictorUtils.calculateEuclideanDistance_km(trip);
        return distance_km>5.0 ? -1e3 : 0.0;
    }

    protected double estimateCantonUtility(Person person) {
        Object cantonObj = person.getAttributes().getAttribute("cantonName");
        if (cantonObj instanceof String canton) {
            return parameters.swissCanton.walk.getOrDefault(canton, 0.0);
        }
        return 0.0;
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
        utility += estimatedLongDistanceUtility(trip);

        utility += estimateCantonUtility(person);

        if(variablesWriter.isInitiated()) {
            writeVariablesToCsv(person, trip, variables, personVariables, utility);
        }

        return utility;
    }

    private void writeVariablesToCsv(Person person, DiscreteModeChoiceTrip trip, WalkVariables walkVariables,
                                     SwissPersonVariables personVariables, double utility) {
        double departureTime = trip.getDepartureTime();
        int tripIndex = trip.getIndex();
        String personId = person.getId().toString();
        double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

        Map<String, String> walkAttributes = new HashMap<>();

        walkAttributes.put("euclideanDistance_km", String.valueOf(euclideanDistance_km));
        walkAttributes.put("age", String.valueOf(personVariables.age_a));
        walkAttributes.put("sex", String.valueOf(personVariables.sex));
        walkAttributes.put("region", String.valueOf(personVariables.cantonCluster));
        walkAttributes.put("originHome", Utils.originIsHome(trip) ? "1" : "0");
        walkAttributes.put("destinationWork", Utils.destinationIsWork(trip) ? "1" : "0");
        walkAttributes.put("urbanDestination", Utils.destinationIsUrban(trip) ? "1" : "0");
        walkAttributes.put("shortDistance", Utils.isShortDistanceTrip(trip) ? "1" : "0");

        walkAttributes.put("travelTime_min", String.valueOf(walkVariables.travelTime_min));

        variablesWriter.writeVariables("walk", personId, tripIndex, departureTime, utility, walkAttributes);
    }

}
