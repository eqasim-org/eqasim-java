package org.eqasim.switzerland.mode_choice.utilities;

import java.util.List;

import org.eqasim.switzerland.mode_choice.costs.CarCostModel;
import org.eqasim.switzerland.mode_choice.costs.PtCostModel;
import org.eqasim.switzerland.mode_choice.parameters.CostParameters;
import org.eqasim.switzerland.mode_choice.parameters.ModeChoiceParameters;
import org.eqasim.switzerland.mode_choice.utilities.estimators.BikeEstimator;
import org.eqasim.switzerland.mode_choice.utilities.estimators.CarEstimator;
import org.eqasim.switzerland.mode_choice.utilities.estimators.PtEstimator;
import org.eqasim.switzerland.mode_choice.utilities.estimators.WalkEstimator;
import org.eqasim.switzerland.mode_choice.utilities.predictors.BikePredictor;
import org.eqasim.switzerland.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.switzerland.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.switzerland.mode_choice.utilities.predictors.PtPredictor;
import org.eqasim.switzerland.mode_choice.utilities.predictors.WalkPredictor;
import org.eqasim.switzerland.mode_choice.utilities.variables.BikeVariables;
import org.eqasim.switzerland.mode_choice.utilities.variables.CarVariables;
import org.eqasim.switzerland.mode_choice.utilities.variables.PersonVariables;
import org.eqasim.switzerland.mode_choice.utilities.variables.PtVariables;
import org.eqasim.switzerland.mode_choice.utilities.variables.WalkVariables;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.components.estimators.AbstractTripRouterEstimator;
import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import ch.ethz.matsim.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

public class SwissUtilityEstimator extends AbstractTripRouterEstimator {
	private final PersonPredictor personPredictor;

	private final WalkPredictor walkPredictor;
	private final WalkEstimator walkEstimator;

	private final BikePredictor bikePredictor;
	private final BikeEstimator bikeEstimator;

	private final CarCostModel carCostModel;
	private final CarPredictor carPredictor;
	private final CarEstimator carEstimator;

	private final PtCostModel ptCostModel;
	private final PtPredictor ptPredictor;
	private final PtEstimator ptEstimator;

	@Inject
	public SwissUtilityEstimator(TripRouter tripRouter, ActivityFacilities facilities,
			ModeChoiceParameters modeChoiceParameters, CostParameters costParameters) {
		super(tripRouter, facilities);

		/*
		 * Note: Right now all of these things are instantiated here, because they don't
		 * do very fancy things. Once they get more complicated (e.g. estimation of
		 * parking search time) feel free to inject them here via the constructor.
		 */

		this.personPredictor = new PersonPredictor();

		this.walkPredictor = new WalkPredictor();
		this.walkEstimator = new WalkEstimator(modeChoiceParameters);

		this.bikePredictor = new BikePredictor();
		this.bikeEstimator = new BikeEstimator(modeChoiceParameters);

		this.carCostModel = new CarCostModel(costParameters);
		this.carPredictor = new CarPredictor(modeChoiceParameters, carCostModel);
		this.carEstimator = new CarEstimator(modeChoiceParameters);

		this.ptCostModel = new PtCostModel(costParameters);
		this.ptPredictor = new PtPredictor(ptCostModel);
		this.ptEstimator = new PtEstimator(modeChoiceParameters);
	}

	protected double estimateTrip(Person person, String mode, DiscreteModeChoiceTrip trip,
			List<TripCandidate> previousTrips, List<? extends PlanElement> elements) {
		PersonVariables personVariables = personPredictor.predict(person);

		switch (mode) {
		case TransportMode.walk:
			WalkVariables walkVariables = walkPredictor.predict(elements);
			return walkEstimator.estimateUtility(walkVariables);
		case TransportMode.bike:
			BikeVariables bikeVariables = bikePredictor.predict(elements);
			return bikeEstimator.estimateUtility(personVariables, bikeVariables);
		case TransportMode.pt:
			PtVariables ptVariables = ptPredictor.predict(personVariables, trip, elements);
			return ptEstimator.estimateUtility(ptVariables);
		case TransportMode.car:
			CarVariables carVariables = carPredictor.predict(trip, elements);
			return carEstimator.estimateUtility(personVariables, carVariables);
		case "outside":
		case "car_passenger":
		case "truck":
			return 0.0;
		default:
			throw new IllegalStateException("Unknown transport mode: " + mode);
		}
	}
}
