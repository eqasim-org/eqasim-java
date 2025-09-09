package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.estimators;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.PtUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PtPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.PtVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissCmdpModeParameters;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;


public class SwissPtDetailedUtilityEstimator extends PtUtilityEstimator {
    private final SwissCmdpModeParameters parameters;
    private final PtPredictor ptPredictor;
    private final SwissPersonPredictor personPredictor;

    @Inject
    public SwissPtDetailedUtilityEstimator(SwissCmdpModeParameters parameters, PtPredictor predictor, SwissPersonPredictor personPredictor) {
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
        double cost = variables.cost_MU + parameters.pt.betaDistance_u_km * costCorrection;
        double interaction = Utils.interaction(variables.euclideanDistance_km, personVariables.income, parameters);

        return parameters.betaCost_u_MU * interaction * cost;
    }

    protected double estimateAgeUtility(SwissPersonVariables personVariables) {
        return parameters.pt.betaAge_u * Math.max(0.0, personVariables.age_a - 18);
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

    protected double estimateRegionalUtility(SwissPersonVariables personVariables) {
        if (personVariables.cantonCluster == 1) {
            return parameters.pt.betaRegion1_u;
        } else if (personVariables.cantonCluster == 2) {
            return parameters.pt.betaRegion2_u;
        } else {
            return 0.0;
        }
    }

    protected double estimateUrbanDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsUrban(trip) ? parameters.pt.betaUrbanDestination_u : 0.0;
    }

    protected double estimateShortDistanceUtility(PtVariables variables) {
        return Utils.isShortDistanceTrip(variables.euclideanDistance_km)? parameters.pt.betaShortDistance_u : 0.0;
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

        return utility;
    }

}