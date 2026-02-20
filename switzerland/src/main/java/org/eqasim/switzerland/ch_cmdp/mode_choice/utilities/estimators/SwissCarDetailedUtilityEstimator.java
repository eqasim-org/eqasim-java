package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.estimators;

import com.google.inject.Inject;
import org.eqasim.core.components.calibration.VariablesWriter;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.CarUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.SwissParkingCostModel;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissCmdpModeParameters;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissCarPredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SwissCarDetailedUtilityEstimator extends CarUtilityEstimator {
    private final SwissCmdpModeParameters parameters;
    private final SwissPersonPredictor personPredictor;
    private final SwissCarPredictor carPredictor;
    private final SwissParkingCostModel parkingCostModel;
    private final VariablesWriter variablesWriter;

    @Inject
    public SwissCarDetailedUtilityEstimator(SwissCmdpModeParameters parameters, SwissCarPredictor carPredictor,
                                            SwissPersonPredictor personPredictor,
                                            SwissParkingCostModel parkingCostModel,
                                            VariablesWriter variablesWriter) {
        super(parameters, carPredictor);

        this.personPredictor = personPredictor;
        this.parameters = parameters;
        this.carPredictor = carPredictor;
        this.parkingCostModel = parkingCostModel;
        this.variablesWriter = variablesWriter;
    }

    protected double estimateConstantUtility() {
        return parameters.car.alpha_u;
    }

    protected double estimateTravelTimeUtility(DiscreteModeChoiceTrip trip, CarVariables variables) {
        double travelTime = (variables.travelTime_min + getParkingSearchDuration(trip)) / parameters.timeScale_min;
        return parameters.car.betaTravelTime_u_min * Math.pow(travelTime, parameters.car.travelTimeExponent);
    }

    protected double getParkingSearchDuration(DiscreteModeChoiceTrip trip){
        if (Utils.destinationIsHome(trip) || Utils.destinationIsWork(trip)){
            return 0.0;
        } else if (Utils.destinationIsUrban(trip)) {
            return parameters.parking.urbanParkingSearchDuration_min;
        } else if (Utils.destinationIsUrbanCore(trip)){
            return parameters.parking.urbancoreParkingSearchDuration_min;
        } else if (Utils.destinationIsSuburban(trip)){
            return parameters.parking.suburbanParkingSearchDuration_min;
        }
        return 0.0;
    }

    protected double estimateRegionalUtility(SwissPersonVariables personVariables) {
        if (personVariables.cantonCluster == 1) {
            return parameters.car.betaRegion1_u;
        } else if (personVariables.cantonCluster == 2) {
            return parameters.car.betaRegion2_u;
        } else {
            return 0.0;
        }
    }

    protected double estimateUrbanDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsUrban(trip) ? parameters.car.betaUrbanDestination_u : 0.0;
    }

    protected double estimateMonetaryCostUtility(DiscreteModeChoiceTrip trip, CarVariables variables, SwissPersonVariables personVariables) {
        double totalCost = variables.cost_MU + parkingCostModel.getParkingPrice_CFH(trip, variables);
        double interaction = Utils.interaction(variables.euclideanDistance_km, personVariables.income, parameters);
        return parameters.betaCost_u_MU * totalCost * interaction;
    }

    protected double estimateAgeUtility(SwissPersonVariables personVariables) {
        return parameters.car.betaAge_u * Math.max(0.0, personVariables.age_a - 17);
    }

    protected double estimateSexUtility(SwissPersonVariables personVariables) {
        return personVariables.sex==1 ? parameters.car.betaSex_u :0.0;
    }

    protected double estimateHomeOriginUtility(DiscreteModeChoiceTrip trip) {
        return Utils.originIsHome(trip) ? parameters.car.betaOriginHome_u : 0.0;
    }

    protected double estimateWorkDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsWork(trip) ? parameters.car.betaDestinationWork_u : 0.0;
    }

    protected double estimateHomeDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsHome(trip) ? parameters.car.betaDestinationHome_u : 0.0;
    }

    protected double estimateEducationDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsEducation(trip) ? parameters.car.betaDestinationEducation_u : 0.0;
    }

    protected double estimateShoppingDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsShopping(trip) ? parameters.car.betaDestinationShopping_u : 0.0;
    }

    protected double estimateLeisureDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsLeisure(trip) ? parameters.car.betaDestinationLeisure_u : 0.0;
    }

    protected double estimateOtherDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsOther(trip) ? parameters.car.betaDestinationOther_u : 0.0;
    }

    protected double estimateUrbancoreDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsUrbanCore(trip) ? parameters.car.betaUrbancoreDestination_u : 0.0;
    }

    protected double estimateRetiredUtility(SwissPersonVariables personVariables) {
        return Utils.isRetired(personVariables) ? parameters.car.betaRetired_u : 0.0;
    }

    protected double estimateJuniorUtility(SwissPersonVariables personVariables) {
        return Utils.isJunior(personVariables) ? parameters.car.betaJunior_u : 0.0;
    }

    protected double estimateLowIncomeUtility(SwissPersonVariables personVariables) {
        return Utils.isLowIncome(personVariables) ? parameters.car.betaLowIncome_u : 0.0;
    }

    protected double estimateShortDistanceUtility(DiscreteModeChoiceTrip trip) {
        return Utils.isShortDistanceTrip(trip)? parameters.car.betaShortDistance_u :0.0;
    }

    protected double estimateLongDistanceUtility(DiscreteModeChoiceTrip trip) {
        return Utils.isLongDistanceTrip(trip)? parameters.car.betaLongDistance_u :0.0;
    }

    protected double estimateCarOwnershipUtility(SwissPersonVariables personVariables) {
        return personVariables.carOwnershipRatio * parameters.car.betaCarOwnershipRatio_u;
    }

    protected double estimateCantonUtility(Person person) {
        Object cantonObj = person.getAttributes().getAttribute("cantonName");
        if (cantonObj instanceof String canton) {
            return parameters.swissCanton.car.getOrDefault(canton, 0.0);
        }
        return 0.0;
    }

    @Override
    public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        SwissPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
        CarVariables variables = carPredictor.predictVariables(person, trip, elements);

        double utility = 0.0;
        utility += estimateConstantUtility();
        utility += estimateTravelTimeUtility(trip, variables);
        utility += estimateMonetaryCostUtility(trip, variables, personVariables);
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
        utility += estimateLongDistanceUtility(trip);
        // location
        utility += estimateUrbanDestinationUtility(trip);
        utility += estimateUrbancoreDestinationUtility(trip);
        // car ownership
        utility += estimateCarOwnershipUtility(personVariables);
        // canton
        utility += estimateCantonUtility(person);

        if(variablesWriter.isInitiated()) {
            writeVariablesToCsv(person, trip, variables, personVariables, utility);
        }

        return utility;
    }

    private void writeVariablesToCsv(Person person, DiscreteModeChoiceTrip trip, CarVariables carVariables,
                                     SwissPersonVariables personVariables, double utility) {
        double departureTime = trip.getDepartureTime();
        int tripIndex = trip.getIndex();
        String personId = person.getId().toString();
        double euclideanDistance_km = carVariables.euclideanDistance_km;

        Map<String, String> carAttributes = new HashMap<>();

        carAttributes.put("euclideanDistance_km", String.valueOf(euclideanDistance_km));

        // person attributes used in utility
        carAttributes.put("age", String.valueOf(personVariables.age_a));
        carAttributes.put("sex", String.valueOf(personVariables.sex));
        carAttributes.put("region", String.valueOf(personVariables.cantonCluster));
        carAttributes.put("retired", Utils.isRetired(personVariables) ? "1" : "0");
        carAttributes.put("lowIncome", Utils.isLowIncome(personVariables) ? "1" : "0");
        carAttributes.put("income", String.valueOf(personVariables.income));
        carAttributes.put("carOwnershipRatio", String.valueOf(personVariables.carOwnershipRatio));

        // purposes used in utility
        carAttributes.put("originHome", Utils.originIsHome(trip) ? "1" : "0");
        carAttributes.put("destinationWork", Utils.destinationIsWork(trip) ? "1" : "0");
        carAttributes.put("destinationHome", Utils.destinationIsHome(trip) ? "1" : "0");
        carAttributes.put("destinationEducation", Utils.destinationIsEducation(trip) ? "1" : "0");
        carAttributes.put("destinationShopping", Utils.destinationIsShopping(trip) ? "1" : "0");
        carAttributes.put("destinationLeisure", Utils.destinationIsLeisure(trip) ? "1" : "0");
        carAttributes.put("destinationOther", Utils.destinationIsOther(trip) ? "1" : "0");

        // location/distance used in utility
        carAttributes.put("urbanDestination", Utils.destinationIsUrban(trip) ? "1" : "0");
        carAttributes.put("urbancoreDestination", Utils.destinationIsUrbanCore(trip) ? "1" : "0");
        carAttributes.put("suburbanDestination", Utils.destinationIsSuburban(trip) ? "1" : "0");
        carAttributes.put("shortDistance", Utils.isShortDistanceTrip(trip) ? "1" : "0");
        carAttributes.put("longDistance", Utils.isLongDistanceTrip(trip) ? "1" : "0");

        // level-of-service + monetary components used in utility
        double parkingSearch_min = getParkingSearchDuration(trip);
        double parkingCost_MU = parkingCostModel.getParkingPrice_CFH(trip, carVariables);

        carAttributes.put("travelTime_min", String.valueOf(carVariables.travelTime_min));
        carAttributes.put("parking_searching_duration_min", String.valueOf(parkingSearch_min));
        carAttributes.put("cost_MU", String.valueOf(carVariables.cost_MU));
        carAttributes.put("parking_cost_MU", String.valueOf(parkingCost_MU));

        variablesWriter.writeVariables("car", personId, tripIndex, departureTime, utility, carAttributes);
    }

}