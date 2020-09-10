package org.eqasim.core.simulation.mode_choice;

import java.util.HashMap;
import java.util.Map;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.constraints.OutsideConstraint;
import org.eqasim.core.simulation.mode_choice.constraints.PassengerConstraint;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.cost.ZeroCostModel;
import org.eqasim.core.simulation.mode_choice.filters.OutsideFilter;
import org.eqasim.core.simulation.mode_choice.filters.TourLengthFilter;
import org.eqasim.core.simulation.mode_choice.utilities.ModularUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.BikeUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.CarPtUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.CarUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.PtUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.WalkUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.ZeroUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.BikePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPtPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PtPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.WalkPredictor;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;

import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.name.Named;

public class EqasimModeChoiceModule extends AbstractEqasimExtension {
	public static final String PASSENGER_CONSTRAINT_NAME = "PassengerConstraint";
	public static final String OUTSIDE_CONSTRAINT_NAME = "OutsideConstraint";

	public static final String TOUR_LENGTH_FILTER_NAME = "TourLengthFilter";
	public static final String OUTSIDE_FILTER_NAME = "OutsideFilter";

	public static final String UTILITY_ESTIMATOR_NAME = "EqasimUtilityEstimator";

	public static final String TOUR_FINDER_NAME = "EqasimTourFinder";

	public static final String CAR_ESTIMATOR_NAME = "CarUtilityEstimator";
	public static final String PT_ESTIMATOR_NAME = "PtUtilityEstimator";
	public static final String BIKE_ESTIMATOR_NAME = "BikeUtilityEstimator";
	public static final String CAR_PT_ESTIMATOR_NAME = "CarPtUtilityEstimator";
	public static final String WALK_ESTIMATOR_NAME = "WalkUtilityEstimator";
	public static final String ZERO_ESTIMATOR_NAME = "ZeroUtilityEstimator";

	public static final String ZERO_COST_MODEL_NAME = "ZeroCostModel";

	@Override
	protected void installEqasimExtension() {
		bindTripConstraintFactory(PASSENGER_CONSTRAINT_NAME).to(PassengerConstraint.Factory.class);
		bindTripConstraintFactory(OUTSIDE_CONSTRAINT_NAME).to(OutsideConstraint.Factory.class);

		bindTourFilter(TOUR_LENGTH_FILTER_NAME).to(TourLengthFilter.class);
		bindTourFilter(OUTSIDE_FILTER_NAME).to(OutsideFilter.class);

		bindTourFinder(TOUR_FINDER_NAME).to(UniversalTourFinder.class);

		bindTripEstimator(UTILITY_ESTIMATOR_NAME).to(ModularUtilityEstimator.class);

		bind(CarPredictor.class);
		bind(PtPredictor.class);
		bind(BikePredictor.class);
		bind(CarPtPredictor.class);
		bind(WalkPredictor.class);
		bind(PersonPredictor.class);

		bindUtilityEstimator(ZERO_ESTIMATOR_NAME).to(ZeroUtilityEstimator.class);
		bindUtilityEstimator(CAR_ESTIMATOR_NAME).to(CarUtilityEstimator.class);
		bindUtilityEstimator(PT_ESTIMATOR_NAME).to(PtUtilityEstimator.class);
		bindUtilityEstimator(BIKE_ESTIMATOR_NAME).to(BikeUtilityEstimator.class);
		bindUtilityEstimator(CAR_PT_ESTIMATOR_NAME).to(CarPtUtilityEstimator.class);
		bindUtilityEstimator(WALK_ESTIMATOR_NAME).to(WalkUtilityEstimator.class);

		bindCostModel(ZERO_COST_MODEL_NAME).to(ZeroCostModel.class);
	}

	@Provides
	public ModularUtilityEstimator provideModularUtilityEstimator(TripRouter tripRouter, ActivityFacilities facilities,
			Map<String, Provider<UtilityEstimator>> factory, EqasimConfigGroup config) {
		Map<String, UtilityEstimator> estimators = new HashMap<>();

		for (Map.Entry<String, String> entry : config.getEstimators().entrySet()) {
			Provider<UtilityEstimator> estimatorFactory = factory.get(entry.getValue());

			if (estimatorFactory == null) {
				throw new IllegalStateException(
						String.format("Estimator '%s' for mode '%s' is unknown", entry.getValue(), entry.getKey()));
			} else {
				estimators.put(entry.getKey(), estimatorFactory.get());
			}
		}

		return new ModularUtilityEstimator(tripRouter, facilities, estimators);
	}

	@Provides
	@Named("car")
	public CostModel provideCarCostModel(Map<String, Provider<CostModel>> factory, EqasimConfigGroup config) {
		return getCostModel(factory, config, "car");
	}

	@Provides
	@Named("pt")
	public CostModel providePtCostModel(Map<String, Provider<CostModel>> factory, EqasimConfigGroup config) {
		return getCostModel(factory, config, "pt");
	}
}
