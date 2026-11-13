package org.eqasim.switzerland.ch.mode_choice.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.SwissPtStageCostCalculator;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.SwissPtTripCost;
import org.eqasim.switzerland.ch.mode_choice.parameters.SwissCostParameters;
import org.eqasim.switzerland.ch.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPtRoutePredictor;
import org.eqasim.switzerland.ch.mode_choice.utilities.variables.SwissPersonVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPtVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class SwissPtCostModel extends AbstractCostModel {
	private final SwissCostParameters parameters;
	private final SwissPersonPredictor predictor;
	private final SwissPtRoutePredictor ptRoutePredictor;
	private final SwissPtStageCostCalculator calculators;

	@Inject
	public SwissPtCostModel(SwissCostParameters costParameters, SwissPersonPredictor predictor, SwissPtRoutePredictor ptRoutePredictor, SwissPtStageCostCalculator calculators) {
		super("pt");

		this.parameters       = costParameters;
		this.predictor        = predictor;
		this.ptRoutePredictor = ptRoutePredictor;
		this.calculators      = calculators;
	}

	/** The same price as ch_cmdp (SwissPtTripCost); the populations of ch have no Junior or Gleis 7 cards. */
	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		SwissPersonVariables personVariables = predictor.predictVariables(person, trip, elements);

		if (SwissPtTripCost.hasFreeTravel(personVariables.age_a, personVariables.hasGeneralSubscription, false, false,
				trip::getDepartureTime)) {
			return 0.0;
		}

		SwissPtVariables ptVariables = ptRoutePredictor.predictVariables(person, trip, elements);
		return SwissPtTripCost.price(person, personVariables, personVariables.hasHalbtaxSubscription,
				personVariables.hasRegionalSubscription, ptVariables, ptRoutePredictor, calculators);
	}
}
