package org.sutlab.hannover.mode_choice.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.sutlab.hannover.mode_choice.parameters.HannoverCostParameters;
import org.sutlab.hannover.mode_choice.utilities.predictors.HannoverPersonPredictor;
import org.sutlab.hannover.mode_choice.utilities.variables.HannoverPersonVariables;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

import com.google.inject.Inject;

public class HannoverCarCostModel extends AbstractCostModel {
	private final TimeInterpretation timeInterpretation;
	private final HannoverCostParameters costParameters;
	private final HannoverPersonPredictor personPredictor;

	@Inject
	public HannoverCarCostModel(HannoverCostParameters costParameters, HannoverPersonPredictor personPredictor,
			TimeInterpretation timeInterpretation) {
		super("car");

		this.costParameters = costParameters;
		this.personPredictor = personPredictor;
		this.timeInterpretation = timeInterpretation;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double parkingCost_EUR = calculateParkingCost_EUR(person, trip, elements);
		return costParameters.carCost_EUR_km * getInVehicleDistance_km(elements) + parkingCost_EUR;
	}

	private double calculateParkingCost_EUR(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		HannoverPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);

		return 0.0;
	}

}