package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.estimators;

import com.google.inject.Inject;
import org.eqasim.core.components.calibration.VariablesWriter;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.PtUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PtPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.PtVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissCmdpModeParameters;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissCostParameters;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SwissPtDetailedUtilityEstimator extends PtUtilityEstimator {
    private final SwissCmdpModeParameters parameters;
    private final PtPredictor ptPredictor;
    private final SwissPersonPredictor personPredictor;
    private final VariablesWriter variablesWriter;
    private final boolean correctPtVariables = false;
    private final SwissCostParameters swissCostParameters;

    @Inject
    public SwissPtDetailedUtilityEstimator(SwissCmdpModeParameters parameters, PtPredictor predictor,
                                           SwissPersonPredictor personPredictor,
                                           VariablesWriter variablesWriter,
                                           SwissCostParameters swissCostParameters) {
        super(parameters, predictor);
        this.ptPredictor  = predictor;
        this.parameters = parameters;
        this.personPredictor = personPredictor;
        this.variablesWriter = variablesWriter;
        this.swissCostParameters = swissCostParameters;
    }

    protected double estimateRegionalUtility(SwissPersonVariables personVariables) {
        if (personVariables.cantonCluster == 1) {
            return parameters.pt.betaRegion1_u;
        } else if (personVariables.cantonCluster == 2) {
            return parameters.pt.betaRegion2_u;
        } else {
            return 0.0;
        }
    }

    protected double estimateConstantUtility() {
        return parameters.pt.alpha_u;
    }

    protected double estimateAccessEgressTimeUtility(PtVariables variables) {
        double accessEgressTime = variables.accessEgressTime_min;
        if (correctPtVariables) {
            accessEgressTime = 3.0 * Math.pow(accessEgressTime, 0.55);
            if (variables.euclideanDistance_km < 1.6) {
                accessEgressTime = accessEgressTime * 0.92;
            }
            if (variables.euclideanDistance_km < 1.0) {
                accessEgressTime = accessEgressTime * 0.85;
            }
        }
        accessEgressTime = accessEgressTime / parameters.timeScale_min;
        return parameters.pt.betaAccessEgressTime_u_min * Math.pow(accessEgressTime, parameters.pt.accessEgressTimeExponent);
    }

    protected double estimateInVehicleTimeUtility(PtVariables variables) {
        double inVehicleTime = variables.inVehicleTime_min;
        if (correctPtVariables) {
            inVehicleTime = 1.2 * Math.pow(inVehicleTime, 0.95);
        }
        inVehicleTime = inVehicleTime / parameters.timeScale_min;
        return parameters.pt.betaInVehicleTime_u_min * Math.pow(inVehicleTime, parameters.pt.inVehicleTimeExponent);
    }

    protected double estimateWaitingTimeUtility(PtVariables variables) {
        double waitingTime = variables.waitingTime_min;
        if (correctPtVariables) {
            waitingTime = 0.45 * Math.pow(waitingTime, 1.17);
        }
        waitingTime = waitingTime / parameters.timeScale_min;
        return parameters.pt.betaWaitingTime_u_min * Math.pow(waitingTime, parameters.pt.waitingTimeExponent);
    }

    protected double estimateLineSwitchUtility(PtVariables variables) {
        double lineSwitches = variables.numberOfLineSwitches;
        if (correctPtVariables) {
            lineSwitches = 0.3 * Math.pow(lineSwitches, 1.9);
        }
        return parameters.pt.betaLineSwitch_u * Math.pow(lineSwitches, parameters.pt.lineSwitchExponent);
    }

    protected double estimateMonetaryCostUtility(PtVariables variables, SwissPersonVariables personVariables) {
        double distance = Math.max(swissCostParameters.ptRegionalRadius_km-variables.euclideanDistance_km, 0.0) / parameters.distanceScale_km ;
        double costCorrection = parameters.pt.betaDistance_u_km * Math.pow(distance, parameters.pt.distanceExponent);
        double cost = variables.cost_MU + costCorrection;
        double interaction = Utils.interaction(variables.euclideanDistance_km, personVariables.income, parameters);

        return parameters.betaCost_u_MU * interaction * cost;
    }

    protected double estimateAgeUtility(SwissPersonVariables personVariables) {
        return parameters.pt.betaAge_u * Math.max(0.0, personVariables.age_a - 17);
    }

    protected double estimateSexUtility(SwissPersonVariables personVariables) {
        return personVariables.sex==1 ? parameters.pt.betaSex_u :0.0;
    }

    protected double estimateHomeOriginUtility(DiscreteModeChoiceTrip trip) {
        return Utils.originIsHome(trip) ? parameters.pt.betaOriginHome_u : 0.0;
    }

    protected double estimateWorkDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsWork(trip) ? parameters.pt.betaDestinationWork_u : 0.0;
    }

    protected double estimateHomeDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsHome(trip) ? parameters.pt.betaDestinationHome_u : 0.0;
    }

    protected double estimateEducationDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsEducation(trip) ? parameters.pt.betaDestinationEducation_u : 0.0;
    }

    protected double estimateShoppingDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsShopping(trip) ? parameters.pt.betaDestinationShopping_u : 0.0;
    }

    protected double estimateLeisureDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsLeisure(trip) ? parameters.pt.betaDestinationLeisure_u : 0.0;
    }

    protected double estimateOtherDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsOther(trip) ? parameters.pt.betaDestinationOther_u : 0.0;
    }

    protected double estimateUrbancoreDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsUrbanCore(trip) ? parameters.pt.betaUrbancoreDestination_u : 0.0;
    }

    protected double estimateUrbanDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsUrban(trip) ? parameters.pt.betaUrbanDestination_u : 0.0;
    }

    protected double estimateRetiredUtility(SwissPersonVariables personVariables) {
        return Utils.isRetired(personVariables) ? parameters.pt.betaRetired_u : 0.0;
    }

    protected double estimateJuniorUtility(SwissPersonVariables personVariables) {
        return Utils.isJunior(personVariables) ? parameters.pt.betaJunior_u : 0.0;
    }

    protected double estimateLowIncomeUtility(SwissPersonVariables personVariables) {
        return Utils.isLowIncome(personVariables) ? parameters.pt.betaLowIncome_u : 0.0;
    }

    protected double estimateShortDistanceUtility(DiscreteModeChoiceTrip trip) {
        return Utils.isShortDistanceTrip(trip)? parameters.pt.betaShortDistance_u :0.0;
    }

    protected double estimateLongDistanceUtility(DiscreteModeChoiceTrip trip) {
        return Utils.isLongDistanceTrip(trip)? parameters.pt.betaLongDistance_u :0.0;
    }

    protected double estimateGoodPtServiceUtility(SwissPersonVariables personVariables) {
        return Utils.isGoodPtService(personVariables.ovgk)? parameters.pt.betaGoodService_u :0.0;
    }

    protected double estimateMediumPtServiceUtility(SwissPersonVariables personVariablesp) {
        return Utils.isMediumPtService(personVariablesp.ovgk)? parameters.pt.betaMediumService_u :0.0;
    }

    protected double estimateCantonUtility(Person person) {
        Object cantonObj = person.getAttributes().getAttribute("cantonName");
        if (cantonObj instanceof String canton) {
            return parameters.swissCanton.pt.getOrDefault(canton, 0.0);
        }
        return 0.0;
    }

    @Override
    public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        SwissPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
        PtVariables variables = ptPredictor.predictVariables(person, trip, elements);

        double utility = 0.0;
        utility += estimateConstantUtility();
        utility += estimateAccessEgressTimeUtility(variables);
        utility += estimateInVehicleTimeUtility(variables);
        utility += estimateWaitingTimeUtility(variables);
        utility += estimateLineSwitchUtility(variables);
        utility += estimateMonetaryCostUtility(variables, personVariables);
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
        // canton
        utility += estimateCantonUtility(person);

        if(variablesWriter.isInitiated()) {
            writeVariablesToCsv(person, trip, variables, personVariables, utility);
        }

        return utility;
    }

    private void writeVariablesToCsv(Person person, DiscreteModeChoiceTrip trip, PtVariables ptVariables,
                                     SwissPersonVariables personVariables, double utility) {
        double departureTime = trip.getDepartureTime();
        int tripIndex = trip.getIndex();
        String personId = person.getId().toString();
        double euclideanDistance_km = ptVariables.euclideanDistance_km;

        Map<String, String> ptAttributes = new HashMap<>();

        ptAttributes.put("euclideanDistance_km", String.valueOf(euclideanDistance_km));

        // person attributes used in utility
        ptAttributes.put("age", String.valueOf(personVariables.age_a));
        ptAttributes.put("sex", String.valueOf(personVariables.sex));
        ptAttributes.put("region", String.valueOf(personVariables.cantonCluster));
        ptAttributes.put("retired", Utils.isRetired(personVariables) ? "1" : "0");
        ptAttributes.put("lowIncome", Utils.isLowIncome(personVariables) ? "1" : "0");
        ptAttributes.put("income", String.valueOf(personVariables.income));

        // purposes used in utility
        ptAttributes.put("originHome", Utils.originIsHome(trip) ? "1" : "0");
        ptAttributes.put("destinationWork", Utils.destinationIsWork(trip) ? "1" : "0");
        ptAttributes.put("destinationHome", Utils.destinationIsHome(trip) ? "1" : "0");
        ptAttributes.put("destinationEducation", Utils.destinationIsEducation(trip) ? "1" : "0");
        ptAttributes.put("destinationShopping", Utils.destinationIsShopping(trip) ? "1" : "0");
        ptAttributes.put("destinationLeisure", Utils.destinationIsLeisure(trip) ? "1" : "0");
        ptAttributes.put("destinationOther", Utils.destinationIsOther(trip) ? "1" : "0");

        // location/distance used in utility
        ptAttributes.put("urbanDestination", Utils.destinationIsUrban(trip) ? "1" : "0");
        ptAttributes.put("urbancoreDestination", Utils.destinationIsUrbanCore(trip) ? "1" : "0");
        ptAttributes.put("shortDistance", Utils.isShortDistanceTrip(trip) ? "1" : "0");
        ptAttributes.put("longDistance", Utils.isLongDistanceTrip(trip) ? "1" : "0");

        // PT level-of-service terms used in utility
        ptAttributes.put("inVehicleTime_min", String.valueOf(ptVariables.inVehicleTime_min));
        ptAttributes.put("accessEgressTime_min", String.valueOf(ptVariables.accessEgressTime_min));
        ptAttributes.put("waitingTime_min", String.valueOf(ptVariables.waitingTime_min));
        ptAttributes.put("numberOfLineSwitches", String.valueOf(ptVariables.numberOfLineSwitches));

        // monetary terms used in utility
        ptAttributes.put("cost_MU", String.valueOf(ptVariables.cost_MU));

        variablesWriter.writeVariables("pt", personId, tripIndex, departureTime, utility, ptAttributes);
    }

}