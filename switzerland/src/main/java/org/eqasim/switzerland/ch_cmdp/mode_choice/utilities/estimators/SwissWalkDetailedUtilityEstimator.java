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
        double tt = variables.travelTime_min / parameters.timeScale_min;
        return parameters.walk.betaTravelTime_u_min * Math.pow(tt, parameters.walk.travelTimeExponent);
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
        return parameters.walk.betaAge_u * Math.max(0.0, personVariables.age_a - 17);
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

    protected double estimateLongDistanceUtility(DiscreteModeChoiceTrip trip) {
        return Utils.isLongDistanceTrip(trip)? parameters.walk.betaLongDistance_u :0.0;
    }

    protected double estimateCantonUtility(Person person) {
        Object cantonObj = person.getAttributes().getAttribute("cantonName");
        if (cantonObj instanceof String canton) {
            return parameters.swissCanton.walk.getOrDefault(canton, 0.0);
        }
        return 0.0;
    }

    protected double estimateHomeDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsHome(trip) ? parameters.walk.betaDestinationHome_u : 0.0;
    }

    protected double estimateEducationDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsEducation(trip) ? parameters.walk.betaDestinationEducation_u : 0.0;
    }

    protected double estimateShoppingDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsShopping(trip) ? parameters.walk.betaDestinationShopping_u : 0.0;
    }

    protected double estimateLeisureDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsLeisure(trip) ? parameters.walk.betaDestinationLeisure_u : 0.0;
    }

    protected double estimateOtherDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsOther(trip) ? parameters.walk.betaDestinationOther_u : 0.0;
    }

    protected double estimateUrbancoreDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsUrbanCore(trip) ? parameters.walk.betaUrbancoreDestination_u : 0.0;
    }

    protected double estimateRetiredUtility(SwissPersonVariables personVariables) {
        return Utils.isRetired(personVariables) ? parameters.walk.betaRetired_u : 0.0;
    }

    protected double estimateLowIncomeUtility(SwissPersonVariables personVariables) {
        return Utils.isLowIncome(personVariables) ? parameters.walk.betaLowIncome_u : 0.0;
    }

    @Override
    public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        WalkVariables variables = walkPredictor.predictVariables(person, trip, elements);
        SwissPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);

        double utility = 0.0;
        utility += estimateConstantUtility();
        utility += estimateTravelTimeUtility(variables);
        // person attributes
        utility += estimateAgeUtility(personVariables);
        utility += estimateSexUtility(personVariables);
        utility += estimateRetiredUtility(personVariables);
        utility += estimateLowIncomeUtility(personVariables);
        // purposes
        utility += estimateHomeDestinationUtility(trip);
        utility += estimateWorkDestinationUtility(trip);
        utility += estimateEducationDestinationUtility(trip);
        utility += estimateShoppingDestinationUtility(trip);
        utility += estimateLeisureDestinationUtility(trip);
        utility += estimateOtherDestinationUtility(trip);
        // origin
        utility += estimateHomeOriginUtility(trip);
        // region
        utility += estimateRegionalUtility(personVariables);
        // distance
        utility += estimateShortDistanceUtility(trip);
        utility += estimateLongDistanceUtility(trip);
        // location
        utility += estimateUrbanDestinationUtility(trip);
        utility += estimateUrbancoreDestinationUtility(trip);
        // canton
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

        // person attributes used in utility
        walkAttributes.put("age", String.valueOf(personVariables.age_a));
        walkAttributes.put("sex", String.valueOf(personVariables.sex));
        walkAttributes.put("region", String.valueOf(personVariables.cantonCluster));
        walkAttributes.put("retired", Utils.isRetired(personVariables) ? "1" : "0");
        walkAttributes.put("lowIncome", Utils.isLowIncome(personVariables) ? "1" : "0");
        walkAttributes.put("income", String.valueOf(personVariables.income));

        // purposes used in utility
        walkAttributes.put("originHome", Utils.originIsHome(trip) ? "1" : "0");
        walkAttributes.put("destinationWork", Utils.destinationIsWork(trip) ? "1" : "0");
        walkAttributes.put("destinationHome", Utils.destinationIsHome(trip) ? "1" : "0");
        walkAttributes.put("destinationEducation", Utils.destinationIsEducation(trip) ? "1" : "0");
        walkAttributes.put("destinationShopping", Utils.destinationIsShopping(trip) ? "1" : "0");
        walkAttributes.put("destinationLeisure", Utils.destinationIsLeisure(trip) ? "1" : "0");
        walkAttributes.put("destinationOther", Utils.destinationIsOther(trip) ? "1" : "0");

        // location/distance used in utility
        walkAttributes.put("urbanDestination", Utils.destinationIsUrban(trip) ? "1" : "0");
        walkAttributes.put("urbancoreDestination", Utils.destinationIsUrbanCore(trip) ? "1" : "0");
        walkAttributes.put("shortDistance", Utils.isShortDistanceTrip(trip) ? "1" : "0");
        walkAttributes.put("longDistance", Utils.isLongDistanceTrip(trip) ? "1" : "0");

        // main level-of-service term used in utility
        walkAttributes.put("travelTime_min", String.valueOf(walkVariables.travelTime_min));

        variablesWriter.writeVariables("walk", personId, tripIndex, departureTime, utility, walkAttributes);
    }

}
