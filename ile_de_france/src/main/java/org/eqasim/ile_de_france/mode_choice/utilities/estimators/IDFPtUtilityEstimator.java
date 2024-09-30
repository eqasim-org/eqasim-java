package org.eqasim.ile_de_france.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.PtUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PtPredictor;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPersonPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPtPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFPersonVariables;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFPtVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class IDFPtUtilityEstimator extends PtUtilityEstimator {
	private final IDFModeParameters parameters;
	private final IDFPersonPredictor personPredictor;
	private final IDFPtPredictor idfPtPredictor;

	@Inject
	public IDFPtUtilityEstimator(IDFModeParameters parameters, IDFPtPredictor idfPtPredictor,
			IDFPersonPredictor personPredictor, PtPredictor ptPredictor) {
		super(parameters, ptPredictor);

		this.personPredictor = personPredictor;
		this.idfPtPredictor = idfPtPredictor;
		this.parameters = parameters;
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

		double utility = 0.0;

		utility += super.estimateUtility(person, trip, elements);
		utility += estimateDrivingPermitUtility(personVariables);
		utility += estimateOnlyBus(ptVariables);

		return utility;
	}
}
