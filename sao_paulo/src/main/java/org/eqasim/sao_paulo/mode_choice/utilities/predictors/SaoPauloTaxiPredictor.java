package org.eqasim.sao_paulo.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.sao_paulo.mode_choice.utilities.variables.TaxiVariables;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class SaoPauloTaxiPredictor extends CachedVariablePredictor<TaxiVariables> {
	private final CostModel costModel;

	@Inject
	public SaoPauloTaxiPredictor(@Named("taxi") CostModel costModel) {
		this.costModel = costModel;
	}

	@Override
	public TaxiVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		if (elements.size() > 1) {
			throw new IllegalStateException("We do not support multi-stage taxi trips yet.");
		}

		Leg leg = (Leg) elements.get(0);

		double travelTime_min = leg.getTravelTime().seconds() / 60.0;
		double cost_MU = costModel.calculateCost_MU(person, trip, elements);

		double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);
		double accessEgressTime_min = 0;// parameters.car.constantAccessEgressWalkTime_min;

		return new TaxiVariables(travelTime_min, cost_MU, euclideanDistance_km, accessEgressTime_min);
	}
}
