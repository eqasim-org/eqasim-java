package org.eqasim.ile_de_france.mode_choice.utilities.estimators;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPersonPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFPersonVariables;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFSpatialVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class IDFPassengerUtilityEstimator implements UtilityEstimator {
	private final IDFModeParameters parameters;
	private final IDFPersonPredictor personPredictor;
	private final CarPredictor carPredictor;

	@Inject
	public IDFPassengerUtilityEstimator(IDFModeParameters parameters, IDFPersonPredictor personPredictor,
			CarPredictor carPredictor) {
		this.parameters = parameters;
		this.personPredictor = personPredictor;
		this.carPredictor = carPredictor;
	}

	protected double estimateUrbanUtility(IDFSpatialVariables variables) {
		double utility = 0.0;

		if (variables.hasUrbanOrigin && variables.hasUrbanDestination) {
			utility += parameters.idfCar.betaInsideUrbanArea_u;
		}

		if (variables.hasUrbanOrigin || variables.hasUrbanDestination) {
			utility += parameters.idfCar.betaCrossingUrbanArea_u;
		}

		return utility;
	}

	protected double estimateConstantUtility() {
		return parameters.idfPassenger.alpha_u;
	}

	protected double estimateTravelTimeUtility(CarVariables variables) {
		return parameters.idfPassenger.betaTravelTime_u_min * variables.travelTime_min;
	}

	protected double estimateLicenseUtility(IDFPersonVariables variables) {
		if (variables.hasLicense) {
			return parameters.idfPassenger.betaLicense_u;
		} else {
			return 0.0;
		}
	}

	protected double estimateHouseholdCarAvailabilityUtility(IDFPersonVariables variables) {
		if (variables.householdCarAvailability) {
			return parameters.idfPassenger.betaHouseholdCarAvailability_u;
		} else {
			return 0.0;
		}
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		IDFPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		CarVariables carVariables = carPredictor.predictVariables(person, trip, elements);

		UtilityCollector collector = new UtilityCollector(false);

		collector.add("asc", estimateConstantUtility());
		collector.add("travelTime", estimateTravelTimeUtility(carVariables));
		collector.add("license", estimateLicenseUtility(personVariables));
		collector.add("carAvailability", estimateHouseholdCarAvailabilityUtility(personVariables));

		return collector.getTotalUtility();
	}

	static private class UtilityCollector {
		private Map<String, Double> components = null;
		private double totalUtility = 0.0;

		public UtilityCollector(boolean collectComponents) {
			if (collectComponents) {
				components = new LinkedHashMap<>();
			}
		}

		public void add(String component, double utility) {
			if (components != null) {
				components.put(component, utility);
			}

			totalUtility += utility;
		}

		public double getTotalUtility() {
			return totalUtility;
		}

		@Override
		public String toString() {
			List<String> stringComponents = new ArrayList<>(components.size());

			for (Map.Entry<String, Double> item : components.entrySet()) {
				stringComponents.add(String.format("%s=%.4f", item.getKey(), item.getValue()));
			}

			return "U[" + String.format("%.4f", getTotalUtility()) + "][" + String.join(", ", stringComponents) + "]";
		}
	}
}
