package org.eqasim.bavaria.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.bavaria.mode_choice.parameters.BavariaModeParameters;
import org.eqasim.bavaria.mode_choice.utilities.predictors.BavariaPersonPredictor;
import org.eqasim.bavaria.mode_choice.utilities.predictors.BavariaPtPredictor;
import org.eqasim.bavaria.mode_choice.utilities.variables.BavariaPersonVariables;
import org.eqasim.bavaria.mode_choice.utilities.variables.BavariaPtVariables;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class BavariaPtUtilityEstimator implements UtilityEstimator {
	private final BavariaModeParameters parameters;
	private final BavariaPersonPredictor personPredictor;
	private final BavariaPtPredictor ptPredictor;
	private final CostModel costModel;

	@Inject
	public BavariaPtUtilityEstimator(BavariaModeParameters parameters, BavariaPtPredictor ptPredictor,
			BavariaPersonPredictor personPredictor, @Named("pt") CostModel costModel) {
		this.personPredictor = personPredictor;
		this.ptPredictor = ptPredictor;
		this.parameters = parameters;
		this.costModel = costModel;
	}

	protected double estimateConstantUtility() {
		return parameters.pt.alpha_u;
	}

	protected double estimateAccessEgressTimeUtility(BavariaPtVariables variables) {
		return parameters.betaAccessTime_u_min * variables.accessEgressTime_min;
	}

	protected double estimateLineSwitchUtility(BavariaPtVariables variables) {
		return parameters.pt.betaLineSwitch_u * variables.numberOfLineSwitches;
	}

	protected double estimateWaitingTimeUtility(BavariaPtVariables variables) {
		return parameters.pt.betaWaitingTime_u_min * variables.waitingTime_min;
	}

	protected double estimateMonetaryCostUtility(BavariaPtVariables variables, double cost_EUR) {
		return parameters.betaCost_u_MU * EstimatorUtils.interaction(variables.euclideanDistance_km,
				parameters.referenceEuclideanDistance_km, parameters.lambdaCostEuclideanDistance) * cost_EUR;
	}

	protected double estimateInVehicleTimeUtility(BavariaPtVariables variables) {
		return parameters.pt.betaInVehicleTime_u_min * variables.inVehicleTime_min;
	}

	protected double estimateDrivingPermitUtility(BavariaPersonVariables variables) {
		return variables.hasDrivingPermit ? parameters.bavariaPt.betaDrivingPermit_u : 0.0;
	}

	protected double estimateOnlyBus(BavariaPtVariables variables) {
		return variables.isOnlyBus ? parameters.bavariaPt.onlyBus_u : 0.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		BavariaPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		BavariaPtVariables ptVariables = ptPredictor.predictVariables(person, trip, elements);

		double cost_EUR = costModel.calculateCost_MU(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateAccessEgressTimeUtility(ptVariables);
		utility += estimateLineSwitchUtility(ptVariables);
		utility += estimateWaitingTimeUtility(ptVariables);
		utility += estimateMonetaryCostUtility(ptVariables, cost_EUR);
		utility += estimateInVehicleTimeUtility(ptVariables);

		utility += estimateOnlyBus(ptVariables);
		utility += estimateDrivingPermitUtility(personVariables);

		return utility;
	}
}
