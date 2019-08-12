package org.eqasim.switzerland.mode_choice.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.costs.AbstractCostModel;
import org.eqasim.switzerland.mode_choice.parameters.SwissCostParameters;
import org.eqasim.switzerland.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.mode_choice.utilities.variables.SwissPersonVariables;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.geometry.CoordUtils;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class SwissPtCostModel extends AbstractCostModel {
	private final SwissCostParameters costParameters;
	private final SwissPersonPredictor swissPersonPredictor;

	@Inject
	public SwissPtCostModel(SwissPersonPredictor swissPersonPredictor, SwissCostParameters costParameters) {
		super(TransportMode.pt);

		this.costParameters = costParameters;
		this.swissPersonPredictor = swissPersonPredictor;

	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		SwissPersonVariables swissPersonVariables = swissPersonPredictor.predictVariables(person);

		if (swissPersonVariables.hasGeneralSubscription) {
			return 0.0;
		}

		if (swissPersonVariables.hasRegionalSubscription) {
			double originHomeDistance_km = CoordUtils.calcEuclideanDistance(swissPersonVariables.homeLocation,
					trip.getOriginActivity().getCoord()) * 1e-3;
			double destinationHomeDistance_km = CoordUtils.calcEuclideanDistance(swissPersonVariables.homeLocation,
					trip.getDestinationActivity().getCoord()) * 1e-3;
			double homeDistance = Math.max(originHomeDistance_km, destinationHomeDistance_km);

			if (homeDistance <= costParameters.ptRegionalRadius_km) {
				return 0.0;
			}
		}

		double inVehicleDistance_km = getInVehicleDistance_km(elements);

		double fullCost = Math.max(costParameters.ptMinimumCost_CHF_km,
				costParameters.ptCost_CHF_km * inVehicleDistance_km);

		if (swissPersonVariables.hasHalbtaxSubscription) {
			return fullCost * 0.5;
		} else {
			return fullCost;
		}
	}
}
