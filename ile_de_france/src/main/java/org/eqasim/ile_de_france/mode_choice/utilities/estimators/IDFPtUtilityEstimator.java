package org.eqasim.ile_de_france.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPersonPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPtPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFPersonVariables;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFPtVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class IDFPtUtilityEstimator implements UtilityEstimator {
	private final IDFModeParameters parameters;
	private final IDFPersonPredictor personPredictor;
	private final IDFPtPredictor idfPtPredictor;
	private final CostModel costModel;

	@Inject
	public IDFPtUtilityEstimator(IDFModeParameters parameters, IDFPtPredictor idfPtPredictor,
			IDFPersonPredictor personPredictor, @Named("pt") CostModel costModel) {
		this.personPredictor = personPredictor;
		this.idfPtPredictor = idfPtPredictor;
		this.parameters = parameters;
		this.costModel = costModel;
	}

	protected double estimateConstantUtility() {
		return parameters.pt.alpha_u;
	}

	protected double estimateAccessEgressTimeUtility(IDFPtVariables variables) {
		return parameters.betaAccessTime_u_min * variables.accessEgressTime_min;
	}

	protected double estimateLineSwitchUtility(IDFPtVariables variables) {
		return parameters.pt.betaLineSwitch_u * variables.numberOfLineSwitches;
	}

	protected double estimateWaitingTimeUtility(IDFPtVariables variables) {
		return parameters.pt.betaWaitingTime_u_min * variables.waitingTime_min;
	}

	protected double estimateMonetaryCostUtility(IDFPtVariables variables, double cost_EUR) {
		return parameters.betaCost_u_MU * EstimatorUtils.interaction(variables.euclideanDistance_km,
				parameters.referenceEuclideanDistance_km, parameters.lambdaCostEuclideanDistance) * cost_EUR;
	}

	protected double estimateInVehicleTimeUtility(IDFPtVariables variables) {
		return parameters.pt.betaInVehicleTime_u_min * variables.inVehicleTime_min;
	}

	protected double estimateDrivingPermitUtility(IDFPersonVariables variables) {
		return variables.hasDrivingPermit ? parameters.idfPt.betaDrivingPermit_u : 0.0;
	}

	protected double estimateOnlyBus(IDFPtVariables variables) {
		return variables.isOnlyBus ? parameters.idfPt.onlyBus_u : 0.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		IDFPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		IDFPtVariables ptVariables = idfPtPredictor.predictVariables(person, trip, elements);

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
