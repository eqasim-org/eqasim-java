package org.eqasim.jakarta.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.PtUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PtPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.PtVariables;
import org.eqasim.jakarta.mode_choice.parameters.JakartaModeParameters;
import org.eqasim.jakarta.mode_choice.utilities.predictors.JakartaPersonPredictor;
import org.eqasim.jakarta.mode_choice.utilities.variables.JakartaPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class JakartaPTUtilityEstimator  extends PtUtilityEstimator{
	private final JakartaModeParameters parameters;
	private final JakartaPersonPredictor predictor;
	private final PtPredictor ptPredictor;

	@Inject
	public JakartaPTUtilityEstimator(JakartaModeParameters parameters, PersonPredictor personPredictor,
			PtPredictor ptPredictor, JakartaPersonPredictor predictor) {
		super(parameters, ptPredictor);
		this.ptPredictor = ptPredictor;
		this.parameters = parameters;
		this.predictor = predictor;
	}

//	protected double estimateRegionalUtility(JakartaPersonVariables variables) {
//		return (variables.cityTrip) ? parameters.jPT.alpha_pt_city : 0.0;
//	}
	
	protected double estimateAgeUtility(Person person) {
		return (int) person.getAttributes().getAttribute("age") <= 16 ? parameters.jPT.alpha_age : 0.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		JakartaPersonVariables variables = predictor.predictVariables(person, trip, elements);
		PtVariables variables_pt = ptPredictor.predict(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateAccessEgressTimeUtility(variables_pt);
		utility += estimateInVehicleTimeUtility(variables_pt);
		utility += estimateWaitingTimeUtility(variables_pt);
		utility += estimateLineSwitchUtility(variables_pt);
//		utility += estimateRegionalUtility(variables);
		utility += estimateAgeUtility(person);
		if (variables.hhlIncome == 0.0)
			utility += estimateMonetaryCostUtility(variables_pt)
			* (parameters.jAvgHHLIncome.avg_hhl_income / 1.0);
		else
			utility += estimateMonetaryCostUtility(variables_pt)
				* (parameters.jAvgHHLIncome.avg_hhl_income / variables.hhlIncome);

		return utility;
	}

}
