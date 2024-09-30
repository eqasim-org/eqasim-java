package org.eqasim.ile_de_france.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFMotorbikeVariables;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.router.TripStructureUtils;

import com.google.common.base.Verify;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class IDFMotorbikePredictor extends CachedVariablePredictor<IDFMotorbikeVariables> {
	private final CostModel costModel;

	@Inject
	public IDFMotorbikePredictor(@Named(IDFModeChoiceModule.MOTORBIKE) CostModel costModel) {
		this.costModel = costModel;
	}

	@Override
	public IDFMotorbikeVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		double motorbikeTravelTime_min = 0.0;
		double accessEgressTime_min = 0.0;

		boolean foundCar = false;

		for (Leg leg : TripStructureUtils.getLegs(elements)) {
			if (leg.getMode().equals(IDFModeChoiceModule.MOTORBIKE)) {
				Verify.verify(!foundCar);
				motorbikeTravelTime_min += leg.getTravelTime().seconds() / 60.0;
			} else if (leg.getMode().equals(TransportMode.walk)) {
				accessEgressTime_min += leg.getTravelTime().seconds() / 60.0;
			} else {
				throw new IllegalStateException("Unexpected mode in motorbike chain: " + leg.getMode());
			}
		}

		double cost_MU = costModel.calculateCost_MU(person, trip, elements);
		double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

		return new IDFMotorbikeVariables(motorbikeTravelTime_min, cost_MU, euclideanDistance_km, accessEgressTime_min);
	}
}