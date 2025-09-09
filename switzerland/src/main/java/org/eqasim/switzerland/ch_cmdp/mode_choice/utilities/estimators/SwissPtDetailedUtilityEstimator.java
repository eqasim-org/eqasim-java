package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.detailed_estimators;

import com.google.inject.Inject;
import org.eqasim.core.components.calibration.writer.VariablesWriter;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.PtUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PtPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.PtVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissModeDetailedParameters;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SwissPtDetailedUtilityEstimator extends PtUtilityEstimator {
    private final SwissModeDetailedParameters parameters;
    private final PtPredictor ptPredictor;
    private final SwissPersonPredictor personPredictor;

    @Inject
    public SwissPtDetailedUtilityEstimator(SwissModeDetailedParameters parameters, PtPredictor predictor, SwissPersonPredictor personPredictor) {
        super(parameters, predictor);
        this.ptPredictor  = predictor;
        this.parameters = parameters;
        this.personPredictor = personPredictor;
    }


    protected double estimateConstantUtility() {
        return parameters.pt.alpha_u;
    }

    protected double estimateAccessEgressTimeUtility(PtVariables variables) {
        return parameters.pt.betaAccessEgressTime_u_min * Math.pow(variables.accessEgressTime_min, parameters.pt.accessEgressTimeExponent);
    }

    protected double estimateInVehicleTimeUtility(PtVariables variables) {
        return parameters.pt.betaInVehicleTime_u_min * Math.pow(variables.inVehicleTime_min, parameters.pt.inVehicleTimeExponent);
    }

    protected double estimateWaitingTimeUtility(PtVariables variables) {
        return parameters.pt.betaWaitingTime_u_min * Math.pow(variables.waitingTime_min, parameters.pt.waitingTimeExponent);
    }

    protected double estimateLineSwitchUtility(PtVariables variables) {
        return parameters.pt.betaLineSwitch_u * Math.pow(variables.numberOfLineSwitches, parameters.pt.lineSwitchExponent);
    }

    protected double estimateMonetaryCostUtility(PtVariables variables, SwissPersonVariables personVariables) {
        double costCorrection = Math.pow(Math.max(10.0-variables.euclideanDistance_km, 0.0), parameters.pt.distanceExponent);
        double cost = variables.cost_MU + costCorrection;

        double interactionDistance = EstimatorUtils.interaction(variables.euclideanDistance_km,
                parameters.referenceEuclideanDistance_km, parameters.lambdaCostEuclideanDistance);
        double interactionIncome = EstimatorUtils.interaction(personVariables.income,
                parameters.referenceIncome, parameters.lambdaCostIncome);
        double interaction = interactionDistance * interactionIncome;

        return parameters.betaCost_u_MU * interaction * cost;
    }

    protected double estimateAgeUtility(SwissPersonVariables personVariables) {
        return parameters.pt.betaAge * Math.max(0.0, personVariables.age_a - 18);
    }

    protected double estimateSexUtility(SwissPersonVariables personVariables) {
        return personVariables.sex==1 ? parameters.pt.betaSex:0.0;
    }

    protected double estimateHomeOriginUtility(DiscreteModeChoiceTrip trip) {
        String originActivity = trip.getOriginActivity().getType();
        return "home".equals(originActivity) ? parameters.pt.betaOriginHome : 0.0;
    }

    protected double estimateWorkDestinationUtility(DiscreteModeChoiceTrip trip) {
        String destinationActivity = trip.getDestinationActivity().getType();
        return "work".equals(destinationActivity) ? parameters.pt.betaDestinationWork : 0.0;
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

    protected String getDestinationType(DiscreteModeChoiceTrip trip){
        Object objMunicipalityId = trip.getDestinationActivity().getAttributes().getAttribute("municipalityType");
        return (objMunicipalityId==null)? "none" : objMunicipalityId.toString().toLowerCase();
    }

    protected double estimateUrbanDestinationUtility(DiscreteModeChoiceTrip trip) {
        String destinationType = getDestinationType(trip);
        return destinationType.equals("urban") ? parameters.pt.betaUrbanDestination : 0.0;
    }


    protected double estimateShortDistanceUtility(PtVariables variables) {
        return (variables.euclideanDistance_km>1.0)? 0.0 : parameters.pt.betaShortDistance;
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
        utility += estimateSexUtility(personVariables);
        utility += estimateAgeUtility(personVariables);
        utility += estimateWorkDestinationUtility(trip);
        utility += estimateUrbanDestinationUtility(trip);
        utility += estimateRegionalUtility(personVariables);
        utility += estimateHomeOriginUtility(trip);
        utility += estimateShortDistanceUtility(variables);



//        if(VariablesWriter.isInitiated()) {
//            writeVariablesToCsv(person, trip, variables, utility);
//        }
        return utility;
    }










    private void writeVariablesToCsv(Person person, DiscreteModeChoiceTrip trip, PtVariables variables, double utility) {
        double departureTime = trip.getDepartureTime();
        int tripIndex = trip.getIndex();
        String personId = person.getId().toString();

        Map<String, String> ptAttributes = new HashMap<>();
        ptAttributes.put("accessEgressTime_min", String.valueOf(variables.accessEgressTime_min));
        ptAttributes.put("inVehicleTime_min", String.valueOf(variables.inVehicleTime_min));
        ptAttributes.put("waitingTime_min", String.valueOf(variables.waitingTime_min));
        ptAttributes.put("numberOfLineSwitches", String.valueOf(variables.numberOfLineSwitches));
        ptAttributes.put("cost_MU", String.valueOf(variables.cost_MU));
        ptAttributes.put("euclideanDistance_km", String.valueOf(variables.euclideanDistance_km));

        VariablesWriter.writeVariables("pt", personId, tripIndex, departureTime, utility, ptAttributes);
    }

}