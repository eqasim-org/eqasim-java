package org.eqasim.ile_de_france.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.CarUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFParkingPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPersonPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFSpatialPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFParkingVariables;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFPersonVariables;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFSpatialVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

import com.google.inject.Inject;

public class IDFCarUtilityEstimator extends CarUtilityEstimator {
	private final IDFModeParameters parameters;
	private final IDFPersonPredictor personPredictor;
	private final IDFSpatialPredictor spatialPredictor;
	private final IDFParkingPredictor parkingPredictor;
	private final CarPredictor predictor;

	@Inject
	public IDFCarUtilityEstimator(IDFModeParameters parameters, CarPredictor predictor,
			IDFPersonPredictor personPredictor, IDFSpatialPredictor spatialPredictor,
			IDFParkingPredictor parkingPredictor) {
		super(parameters, predictor);

		this.parameters = parameters;
		this.predictor = predictor;
		this.personPredictor = personPredictor;
		this.spatialPredictor = spatialPredictor;
		this.parkingPredictor = parkingPredictor;
	}

	protected double estimateAccessEgressTimeUtility(CarVariables variables) {
		return parameters.betaAccessTime_u_min * variables.accessEgressTime_min;
	}

	protected double estimateMonetaryCostUtility(CarVariables carVariables, IDFPersonVariables personVariables) {
		double baseValue = super.estimateMonetaryCostUtility(carVariables);

		return baseValue * EstimatorUtils.interaction(personVariables.householdIncomePerCU_EUR,
				parameters.referenceIncomePerCU_EUR, parameters.lambdaCostIncome);
	}

	protected double estimateParkingPressureUtility(IDFParkingVariables parkingVariables) {
		return parameters.idfCar.betaParkingPressure_u * parkingVariables.parkingPressure;
	}

	protected double estimateInsideParisUtility(IDFSpatialVariables spatialVariables) {
		return spatialVariables.isInsideParisBoundary ? parameters.betaRoadInsideParis_u : 0.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements,
			List<TripCandidate> previousTrips) {
		IDFPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		IDFSpatialVariables spatialVariables = spatialPredictor.predictVariables(person, trip, elements);
		IDFParkingVariables parkingVariables = parkingPredictor.predictVariables(person, trip, elements);
		CarVariables variables = predictor.predictVariables(person, trip, elements, previousTrips);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables);
		utility += estimateAccessEgressTimeUtility(variables);
		utility += estimateMonetaryCostUtility(variables, personVariables);
		utility += estimateParkingPressureUtility(parkingVariables);
		utility += estimateInsideParisUtility(spatialVariables);

		return utility;
	}
}
