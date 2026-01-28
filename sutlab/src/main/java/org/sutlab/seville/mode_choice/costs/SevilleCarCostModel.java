package org.sutlab.seville.mode_choice.costs;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.sutlab.seville.mode_choice.parameters.SevilleCostParameters;
import org.sutlab.seville.mode_choice.utilities.predictors.SevillePersonPredictor;
import org.sutlab.seville.mode_choice.utilities.variables.SevillePersonVariables;

import java.util.List;

public class SevilleCarCostModel extends AbstractCostModel {
	private final TimeInterpretation timeInterpretation;
	private final SevilleCostParameters costParameters;
	private final SevillePersonPredictor personPredictor;

	@Inject
	public SevilleCarCostModel(SevilleCostParameters costParameters, SevillePersonPredictor personPredictor,
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
		SevillePersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);

		return 0.0;
	}

}