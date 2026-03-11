package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.estimators;

import com.google.inject.Inject;
import org.eqasim.core.components.calibration.VariablesWriter;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.BikeUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.variables.BikeVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissCmdpModeParameters;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissBikePredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SwissBikeDetailedUtilityEstimator extends BikeUtilityEstimator {
    private final SwissCmdpModeParameters parameters;
    private final SwissPersonPredictor personPredictor;
    private final SwissBikePredictor bikePredictor;
    private final VariablesWriter variablesWriter;

    @Inject
    public SwissBikeDetailedUtilityEstimator(SwissCmdpModeParameters parameters, SwissPersonPredictor personPredictor,
                                             SwissBikePredictor bikePredictor, VariablesWriter variablesWriter) {
        super(parameters, personPredictor.delegate, bikePredictor);

        this.parameters = parameters;
        this.personPredictor = personPredictor;
        this.bikePredictor = bikePredictor;
        this.variablesWriter = variablesWriter;
    }

    protected double estimateConstantUtility() {
        return parameters.bike.alpha_u;
    }

    protected double estimateTravelTimeUtility(BikeVariables variables) {
        double tt = variables.travelTime_min / parameters.timeScale_min;
        return parameters.bike.betaTravelTime_u_min * Math.pow(tt, parameters.bike.travelTimeExponent);
    }

    protected double estimateAgeUtility(SwissPersonVariables personVariables) {
        return parameters.bike.betaAge_u * Math.max(0.0, personVariables.age_a - 17);
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

    protected double estimatedLongDistanceUtility(DiscreteModeChoiceTrip trip) {
        return Utils.isLongDistanceTrip(trip)? parameters.bike.betaLongDistance_u :0.0;
    }

    protected double estimateUrbanDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsUrban(trip) ? parameters.bike.betaUrbanDestination_u : 0.0;
    }

    protected double estimateUrbancoreDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsUrbanCore(trip) ? parameters.bike.betaUrbancoreDestination_u : 0.0;
    }

    protected double estimateWorkDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsWork(trip) ? parameters.bike.betaDestinationWork_u : 0.0;
    }

    protected double estimateHomeDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsHome(trip) ? parameters.bike.betaDestinationHome_u : 0.0;
    }

    protected double estimateEducationDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsEducation(trip) ? parameters.bike.betaDestinationEducation_u : 0.0;
    }

    protected double estimateShoppingDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsShopping(trip) ? parameters.bike.betaDestinationShopping_u : 0.0;
    }

    protected double estimateLeisureDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsLeisure(trip) ? parameters.bike.betaDestinationLeisure_u : 0.0;
    }

    protected double estimateOtherDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsOther(trip) ? parameters.bike.betaDestinationOther_u : 0.0;
    }

    protected double estimateRetiredUtility(SwissPersonVariables personVariables) {
        return Utils.isRetired(personVariables) ? parameters.bike.betaRetired_u : 0.0;
    }

    protected double estimateJuniorUtility(SwissPersonVariables personVariables) {
        return Utils.isJunior(personVariables) ? parameters.bike.betaJunior_u : 0.0;
    }

    protected double estimateLowIncomeUtility(SwissPersonVariables personVariables) {
        return Utils.isLowIncome(personVariables) ? parameters.bike.betaLowIncome_u : 0.0;
    }

    protected double estimateCantonUtility(Person person) {
        Object cantonObj = person.getAttributes().getAttribute("cantonName");
        if (cantonObj instanceof String canton) {
            return parameters.swissCanton.bike.getOrDefault(canton, 0.0);
        }
        return 0.0;
    }

    @Override
    public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        SwissPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
        BikeVariables bikeVariables = bikePredictor.predictVariables(person, trip, elements);

        double utility = 0.0;
        utility += estimateConstantUtility();
        utility += estimateTravelTimeUtility(bikeVariables);
        // person attributes
        utility += estimateAgeUtility(personVariables);
        utility += estimateSexUtility(personVariables);
        utility += estimateRetiredUtility(personVariables);
        utility += estimateJuniorUtility(personVariables);
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
        utility += estimatedLongDistanceUtility(trip);
        // location
        utility += estimateUrbanDestinationUtility(trip);
        utility += estimateUrbancoreDestinationUtility(trip);
        // canton
        utility += estimateCantonUtility(person);

        if(variablesWriter.isInitiated()) {
            writeVariablesToCsv(person, trip, bikeVariables, personVariables, utility);
        }

        return utility;
    }

    private void writeVariablesToCsv(Person person, DiscreteModeChoiceTrip trip, BikeVariables bikevariable,
                                     SwissPersonVariables personVariables, double utility) {
        double departureTime = trip.getDepartureTime();
        int tripIndex = trip.getIndex();
        String personId = person.getId().toString();
        double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

        Map<String, String> bikeAttributes = new HashMap<>();

        bikeAttributes.put("euclideanDistance_km", String.valueOf(euclideanDistance_km));

        // person attributes used in utility
        bikeAttributes.put("age", String.valueOf(personVariables.age_a));
        bikeAttributes.put("sex", String.valueOf(personVariables.sex));
        bikeAttributes.put("region", String.valueOf(personVariables.cantonCluster));
        bikeAttributes.put("retired", Utils.isRetired(personVariables) ? "1" : "0");
        bikeAttributes.put("junior", Utils.isJunior(personVariables) ? "1" : "0");
        bikeAttributes.put("lowIncome", Utils.isLowIncome(personVariables) ? "1" : "0");
        bikeAttributes.put("income", String.valueOf(personVariables.income));

        // purposes used in utility
        bikeAttributes.put("originHome", Utils.originIsHome(trip) ? "1" : "0");
        bikeAttributes.put("destinationWork", Utils.destinationIsWork(trip) ? "1" : "0");
        bikeAttributes.put("destinationHome", Utils.destinationIsHome(trip) ? "1" : "0");
        bikeAttributes.put("destinationEducation", Utils.destinationIsEducation(trip) ? "1" : "0");
        bikeAttributes.put("destinationShopping", Utils.destinationIsShopping(trip) ? "1" : "0");
        bikeAttributes.put("destinationLeisure", Utils.destinationIsLeisure(trip) ? "1" : "0");
        bikeAttributes.put("destinationOther", Utils.destinationIsOther(trip) ? "1" : "0");

        // location/distance used in utility
        bikeAttributes.put("urbanDestination", Utils.destinationIsUrban(trip) ? "1" : "0");
        bikeAttributes.put("urbancoreDestination", Utils.destinationIsUrbanCore(trip) ? "1" : "0");
        bikeAttributes.put("shortDistance", Utils.isShortDistanceTrip(trip) ? "1" : "0");
        bikeAttributes.put("longDistance", Utils.isLongDistanceTrip(trip) ? "1" : "0");

        // canton
        Object cantonObj = person.getAttributes().getAttribute("cantonName");
        bikeAttributes.put("canton", cantonObj instanceof String ? (String) cantonObj : "");

        // main level-of-service term used in utility
        bikeAttributes.put("travelTime_min", String.valueOf(bikevariable.travelTime_min));

        variablesWriter.writeVariables("bike", personId, tripIndex, departureTime, utility, bikeAttributes);
    }

}