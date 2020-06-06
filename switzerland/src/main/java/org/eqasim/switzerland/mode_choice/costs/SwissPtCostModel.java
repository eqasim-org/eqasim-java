package org.eqasim.switzerland.mode_choice.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.switzerland.mode_choice.parameters.SwissCostParameters;
import org.eqasim.switzerland.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.mode_choice.utilities.variables.SwissPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.utils.geometry.CoordUtils;

import com.google.inject.Inject;

public class SwissPtCostModel extends AbstractCostModel {
	private final SwissCostParameters parameters;
	private final SwissPersonPredictor predictor;

	@Inject
	public SwissPtCostModel(SwissCostParameters costParameters, SwissPersonPredictor predictor) {
		super("pt");

		this.parameters = costParameters;
		this.predictor = predictor;
	}

	protected double calculateHomeDistance_km(SwissPersonVariables variables, DiscreteModeChoiceTrip trip) {
		double originHomeDistance_km = CoordUtils.calcEuclideanDistance(variables.homeLocation,
				trip.getOriginActivity().getCoord()) * 1e-3;
		double destinationHomeDistance_km = CoordUtils.calcEuclideanDistance(variables.homeLocation,
				trip.getDestinationActivity().getCoord()) * 1e-3;
		return Math.max(originHomeDistance_km, destinationHomeDistance_km);
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		SwissPersonVariables variables = predictor.predictVariables(person, trip, elements);

		if (variables.hasGeneralSubscription) {
			return 0.0;
		}

		if (variables.hasRegionalSubscription) {
			double homeDistance_km = calculateHomeDistance_km(variables, trip);

			if (homeDistance_km <= parameters.ptRegionalRadius_km) {
				return 0.0;
			}
		}

		double fullCost_CHF = Math.max(parameters.ptMinimumCost_CHF,
				parameters.ptCost_CHF_km * getInVehicleDistance_km(elements));

		if (variables.hasHalbtaxSubscription) {
			return fullCost_CHF * 0.5;
		}

		return fullCost_CHF;
	}
}
