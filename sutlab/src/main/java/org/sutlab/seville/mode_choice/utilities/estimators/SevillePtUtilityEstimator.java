package org.sutlab.seville.mode_choice.utilities.estimators;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.sutlab.seville.mode_choice.parameters.SevilleModeParameters;
import org.sutlab.seville.mode_choice.utilities.predictors.SevillePersonPredictor;
import org.sutlab.seville.mode_choice.utilities.predictors.SevillePtPredictor;
import org.sutlab.seville.mode_choice.utilities.variables.SevillePersonVariables;
import org.sutlab.seville.mode_choice.utilities.variables.SevillePtVariables;

import java.util.List;

public class SevillePtUtilityEstimator implements UtilityEstimator {
	private final SevilleModeParameters parameters;
	private final SevillePersonPredictor personPredictor;
	private final SevillePtPredictor ptPredictor;
	private final CostModel costModel;

	@Inject
	public SevillePtUtilityEstimator(SevilleModeParameters parameters, SevillePtPredictor ptPredictor,
									 SevillePersonPredictor personPredictor, @Named("pt") CostModel costModel) {
		this.personPredictor = personPredictor;
		this.ptPredictor = ptPredictor;
		this.parameters = parameters;
		this.costModel = costModel;
	}

	protected double estimateConstantUtility() {
		return parameters.pt.alpha_u;
	}

	protected double estimateAccessEgressTimeUtility(SevillePtVariables variables) {
		return parameters.betaAccessTime_u_min * variables.accessEgressTime_min;
	}

	protected double estimateLineSwitchUtility(SevillePtVariables variables) {
		return parameters.pt.betaLineSwitch_u * variables.numberOfLineSwitches;
	}

	protected double estimateWaitingTimeUtility(SevillePtVariables variables) {
		return parameters.pt.betaWaitingTime_u_min * variables.waitingTime_min;
	}

	protected double estimateMonetaryCostUtility(SevillePtVariables variables, double cost_EUR) {
		return parameters.betaCost_u_MU * EstimatorUtils.interaction(variables.euclideanDistance_km,
				parameters.referenceEuclideanDistance_km, parameters.lambdaCostEuclideanDistance) * cost_EUR;
	}

	protected double estimateInVehicleTimeUtility(SevillePtVariables variables) {
		return parameters.pt.betaInVehicleTime_u_min * variables.inVehicleTime_min;
	}

	protected double estimateDrivingPermitUtility(SevillePersonVariables variables) {
		return variables.hasDrivingPermit ? parameters.sevillePt.betaDrivingPermit_u : 0.0;
	}

	protected double estimateOnlyBus(SevillePtVariables variables) {
		return variables.isOnlyBus ? parameters.sevillePt.onlyBus_u : 0.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		SevillePersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		SevillePtVariables ptVariables = ptPredictor.predictVariables(person, trip, elements);

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