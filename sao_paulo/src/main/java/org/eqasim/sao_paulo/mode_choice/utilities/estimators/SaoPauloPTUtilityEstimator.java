package org.eqasim.sao_paulo.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.PtUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PtPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.PtVariables;
import org.eqasim.sao_paulo.mode_choice.parameters.SaoPauloModeParameters;
import org.eqasim.sao_paulo.mode_choice.utilities.predictors.SaoPauloPersonPredictor;
import org.eqasim.sao_paulo.mode_choice.utilities.variables.SaoPauloPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class SaoPauloPTUtilityEstimator  extends PtUtilityEstimator{
	private final SaoPauloModeParameters parameters;
	private final SaoPauloPersonPredictor predictor;
	private final PtPredictor ptPredictor;

	@Inject
	public SaoPauloPTUtilityEstimator(SaoPauloModeParameters parameters, PersonPredictor personPredictor,
			PtPredictor ptPredictor, SaoPauloPersonPredictor predictor) {
		super(parameters, ptPredictor);
		this.ptPredictor = ptPredictor;
		this.parameters = parameters;
		this.predictor = predictor;
	}

	protected double estimateRegionalUtility(SaoPauloPersonVariables variables) {
		return (variables.cityTrip) ? parameters.spPT.alpha_pt_city : 0.0;
	}
	
	protected double estimateAgeUtility(Person person) {
		return (int) person.getAttributes().getAttribute("age") <= 16 ? parameters.spPT.alpha_age : 0.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		SaoPauloPersonVariables variables = predictor.predictVariables(person, trip, elements);
		PtVariables variables_pt = ptPredictor.predict(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateAccessEgressTimeUtility(variables_pt);
		utility += estimateInVehicleTimeUtility(variables_pt);
		utility += estimateWaitingTimeUtility(variables_pt);
		utility += estimateLineSwitchUtility(variables_pt);
		utility += estimateRegionalUtility(variables);
		utility += estimateAgeUtility(person);
		if (variables.hhlIncome == 0.0)
			utility += estimateMonetaryCostUtility(variables_pt)
			* (parameters.spAvgHHLIncome.avg_hhl_income / parameters.spAvgHHLIncome.avg_hhl_income);
		else
			utility += estimateMonetaryCostUtility(variables_pt)
				* (parameters.spAvgHHLIncome.avg_hhl_income / parameters.spAvgHHLIncome.avg_hhl_income);

		return utility;
	}

}
