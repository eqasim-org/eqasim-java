package org.eqasim.core.simulation.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PtPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.PtVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class PtEstimator implements UtilityEstimator {
	private final PtPredictor ptPredictor;
	private final ModeParameters parameters;

	@Inject
	public PtEstimator(PtPredictor ptPredictor, ModeParameters parameters) {
		this.parameters = parameters;
		this.ptPredictor = ptPredictor;
	}

	protected double predictConstantUtility() {
		return parameters.pt.alpha_u;
	}

	protected double predictAccessEgressUtility(PtVariables variables) {
		return parameters.pt.betaAccessEgressTime_u_min * variables.accessEgressTime_min;
	}

	protected double predictInVehicleTimeUtility(PtVariables variables) {
		return parameters.pt.betaInVehicleTime_u_min * variables.inVehicleTime_min;
	}

	protected double predictWaitingTimeUtility(PtVariables variables) {
		return parameters.pt.betaWaitingTime_u_min * variables.waitingTime_min;
	}

	protected double predictLineSwitchUtility(PtVariables variables) {
		return parameters.pt.betaLineSwitch_u * variables.numberOfLineSwitches;
	}

	protected double predictMonetaryCostUtility(PtVariables variables) {
		return parameters.betaCost_u_MU //
				* EstimatorUtilities.interaction(variables.euclideanDistance_km,
						parameters.referenceEuclideanDistance_km, parameters.lambdaCostCrowflyDistance) //
				* variables.cost_MU;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double utility = 0.0;

		PtVariables ptVariables = ptPredictor.predictVariables(person, trip, elements);

		utility += predictConstantUtility();
		utility += predictAccessEgressUtility(ptVariables);
		utility += predictInVehicleTimeUtility(ptVariables);
		utility += predictWaitingTimeUtility(ptVariables);
		utility += predictLineSwitchUtility(ptVariables);
		utility += predictMonetaryCostUtility(ptVariables);

		return utility;
	}
}
