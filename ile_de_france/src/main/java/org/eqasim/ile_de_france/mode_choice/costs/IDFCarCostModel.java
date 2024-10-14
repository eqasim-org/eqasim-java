package org.eqasim.ile_de_france.mode_choice.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFCostParameters;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPersonPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

import com.google.inject.Inject;

public class IDFCarCostModel extends AbstractCostModel {
	private final TimeInterpretation timeInterpretation;
	private final IDFCostParameters costParameters;
	private final IDFPersonPredictor personPredictor;

	@Inject
	public IDFCarCostModel(IDFCostParameters costParameters, IDFPersonPredictor personPredictor,
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
		IDFPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);

		if (!personVariables.isParisResident && hasParisDestination(trip)) {
			final double parkingDuration_min;

			Plan plan = person.getSelectedPlan();
			if (trip.getDestinationActivity() == plan.getPlanElements().get(plan.getPlanElements().size() - 1)) {
				parkingDuration_min = 8.0 * 3600.0 / 60.0;
			} else {
				TimeTracker timeTracker = new TimeTracker(timeInterpretation);

				timeTracker.setTime(trip.getDepartureTime());
				timeTracker.addElements(elements);

				double activityStartTime = timeTracker.getTime().seconds();
				timeTracker.addActivity(trip.getDestinationActivity());

				parkingDuration_min = (timeTracker.getTime().seconds() - activityStartTime) / 60.0;
			}

			return Math.max(1.0, Math.ceil(parkingDuration_min / 60.0)) * costParameters.parisParkingCost_EUR_h;
		}

		return 0.0;
	}

	private boolean hasParisDestination(DiscreteModeChoiceTrip trip) {
		Boolean isParis = (Boolean) trip.getDestinationActivity().getAttributes().getAttribute("isParis");
		return isParis != null && isParis;
	}
}
