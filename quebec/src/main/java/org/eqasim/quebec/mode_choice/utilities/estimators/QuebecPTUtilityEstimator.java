package org.eqasim.quebec.mode_choice.utilities.estimators;

import java.util.List;


import org.eqasim.core.simulation.mode_choice.utilities.estimators.PtUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PtPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.PtVariables;
import org.eqasim.quebec.mode_choice.parameters.QuebecModeParameters;
import org.eqasim.quebec.mode_choice.utilities.predictors.QuebecPersonPredictor;
//import org.eqasim.quebec.mode_choice.utilities.predictors.QuebecPredictorUtils;
//import org.eqasim.quebec.mode_choice.utilities.variables.QuebecPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class QuebecPTUtilityEstimator extends PtUtilityEstimator {
	private final QuebecModeParameters parameters;
//	private final QuebecPersonPredictor predictor;
	private final PtPredictor ptPredictor;

	@Inject
	public QuebecPTUtilityEstimator(QuebecModeParameters parameters, PersonPredictor personPredictor,
			PtPredictor ptPredictor, QuebecPersonPredictor predictor) {
		super(parameters, ptPredictor);
		this.ptPredictor = ptPredictor;
		this.parameters = parameters;
//		this.predictor = predictor;
	}

	

	@Override
	protected double estimateMonetaryCostUtility(PtVariables variables) {
		return (parameters.qcPT.betaCOST_PT) * variables.cost_MU ;
	}
	
	protected double estimateTravelTimeUtility(PtVariables variables) {
		return (parameters.qcPT.betaTravelTime_u_min) * (variables.inVehicleTime_min + variables.inVehicleTime_min + variables.waitingTime_min) ;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
//		QuebecPersonVariables variables = predictor.predictVariables(person, trip, elements); ????PTSubscription???
		PtVariables variables_pt = ptPredictor.predict(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables_pt) + estimateMonetaryCostUtility(variables_pt);


		return utility;
	}
}
