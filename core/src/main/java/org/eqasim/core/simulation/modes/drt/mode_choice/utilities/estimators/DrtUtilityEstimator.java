package org.eqasim.core.simulation.modes.drt.mode_choice.utilities.estimators;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.modes.drt.mode_choice.predictors.DrtPredictor;
import org.eqasim.core.simulation.modes.drt.mode_choice.variables.DrtVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class DrtUtilityEstimator implements UtilityEstimator {

    private final ModeParameters modeParameters;
    private final DrtPredictor drtPredictor;

    @Inject
    public DrtUtilityEstimator(ModeParameters modeParameters, DrtPredictor drtPredictor) {
        this.modeParameters = modeParameters;
        this.drtPredictor = drtPredictor;
    }

    protected double estimateConstantUtility() {
        return this.modeParameters.drt.alpha_u;
    }

    protected double estimateTravelTimeUtility(DrtVariables variables) {
        return this.modeParameters.drt.betaTravelTime_u_min * variables.travelTime_min;
    }

    protected double estimateWaitingTimeUtility(DrtVariables variables) {
        return this.modeParameters.drt.betaWaitingTime_u_min * variables.waitingTime_min;
    }

    protected double estimateMonetaryCostUtility(DrtVariables variables) {
        return this.modeParameters.betaCost_u_MU * EstimatorUtils.interaction(variables.euclideanDistance_km,
                this.modeParameters.referenceEuclideanDistance_km, this.modeParameters.lambdaCostEuclideanDistance) * variables.cost_MU;
    }

    protected double estimateAccessEgressTimeUtility(DrtVariables variables) {
        return this.modeParameters.drt.betaAccessEgressTime_u_min * variables.accessEgressTime_min;
    }


    @Override
    public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        DrtVariables variables = this.drtPredictor.predictVariables(person, trip, elements);

        double utility = 0.0;

        utility += estimateConstantUtility();
        utility += estimateTravelTimeUtility(variables);
        utility += estimateWaitingTimeUtility(variables);
        utility += estimateMonetaryCostUtility(variables);
        utility += estimateAccessEgressTimeUtility(variables);
        return utility;
    }
}
