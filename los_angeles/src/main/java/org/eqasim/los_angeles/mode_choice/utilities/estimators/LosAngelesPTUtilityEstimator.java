package org.eqasim.los_angeles.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.PtUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PtPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.PtVariables;
import org.eqasim.los_angeles.mode_choice.parameters.LosAngelesModeParameters;
import org.eqasim.los_angeles.mode_choice.utilities.predictors.LosAngelesPersonPredictor;
import org.eqasim.los_angeles.mode_choice.utilities.variables.LosAngelesPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class LosAngelesPTUtilityEstimator extends PtUtilityEstimator {
	private final LosAngelesModeParameters parameters;
	private final LosAngelesPersonPredictor predictor;
	private final PtPredictor ptPredictor;

	@Inject
	public LosAngelesPTUtilityEstimator(LosAngelesModeParameters parameters, PersonPredictor personPredictor,
			PtPredictor ptPredictor, LosAngelesPersonPredictor predictor) {
		super(parameters, ptPredictor);
		this.ptPredictor = ptPredictor;
		this.parameters = parameters;
		this.predictor = predictor;
	}

	protected double estimateRegionalUtility(LosAngelesPersonVariables variables) {
		double utility_orange = variables.orangeTrip ? parameters.laPT.alpha_orange_county : 0.0;
		double utility_city = (variables.cityTrip) ? parameters.laPT.alpha_pt_city : 0.0;

		return utility_orange + utility_city;
	}

	protected double estimateTravelTime(PtVariables variables_pt) {
		return parameters.laPT.vot_min
				* (variables_pt.inVehicleTime_min + variables_pt.accessEgressTime_min + variables_pt.waitingTime_min);
	}

	@Override
	protected double estimateMonetaryCostUtility(PtVariables variables) {
		return EstimatorUtils.interaction(variables.euclideanDistance_km, parameters.referenceEuclideanDistance_km,
				parameters.lambdaCostEuclideanDistance) * variables.cost_MU;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		LosAngelesPersonVariables variables = predictor.predictVariables(person, trip, elements);
		PtVariables variables_pt = ptPredictor.predict(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += (estimateTravelTime(variables_pt) + estimateLineSwitchUtility(variables_pt)
				+ estimateMonetaryCostUtility(variables_pt))
				* (parameters.laAvgHHLIncome.avg_hhl_income / variables.hhlIncome) * parameters.betaCost_u_MU;
		utility += estimateRegionalUtility(variables);

		return utility;
	}
}
