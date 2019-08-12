package org.eqasim.core.simulation.mode_choice;

import java.util.HashMap;
import java.util.Map;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.constraints.OutsideConstraint;
import org.eqasim.core.simulation.mode_choice.constraints.PassengerConstraint;
import org.eqasim.core.simulation.mode_choice.costs.CostModel;
import org.eqasim.core.simulation.mode_choice.filters.OutsideFilter;
import org.eqasim.core.simulation.mode_choice.filters.TourLengthFilter;
import org.eqasim.core.simulation.mode_choice.utilities.ModularUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.BikeEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.CarEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.NullEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.PtEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.WalkEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.BikePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PtPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.WalkPredictor;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;

import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.name.Named;

public class ModeChoiceModule extends AbstractEqasimModule {
	public static final String PASSENGER_CONSTRAINT_NAME = "PassengerConstraint";
	public static final String OUTSIDE_CONSTRAINT_NAME = "OutsideConstraint";

	public static final String TOUR_LENGTH_FILTER_NAME = "TourLengthFilter";
	public static final String OUTSIDE_FILTER_NAME = "OutsideFilter";

	public static final String UTILITY_ESTIMATOR_NAME = "EqasimUtilityEstimator";

	public static final String TOUR_FINDER_NAME = "EqasimTourFinder";

	@Override
	protected void installEqasimExtension() {
		bindTripConstraintFactory(PASSENGER_CONSTRAINT_NAME).to(PassengerConstraint.Factory.class);
		bindTripConstraintFactory(OUTSIDE_CONSTRAINT_NAME).to(OutsideConstraint.Factory.class);

		bindTourFilter(TOUR_LENGTH_FILTER_NAME).to(TourLengthFilter.class);
		bindTourFilter(OUTSIDE_FILTER_NAME).to(OutsideFilter.class);

		bindTourFinder(TOUR_FINDER_NAME).to(DefaultTourFinder.class);

		bindTripEstimator(UTILITY_ESTIMATOR_NAME).to(ModularUtilityEstimator.class);

		bind(WalkEstimator.class);
		bind(BikeEstimator.class);
		bind(PtEstimator.class);
		bind(CarEstimator.class);
		bind(NullEstimator.class);

		bind(WalkPredictor.class);
		bind(BikePredictor.class);
		bind(PtPredictor.class);
		bind(CarPredictor.class);
		bind(PersonPredictor.class);

		bindUtilityEstimator("zero").to(NullEstimator.class);
		bindUtilityEstimator("walk").to(WalkEstimator.class);
		bindUtilityEstimator("bike").to(BikeEstimator.class);
		bindUtilityEstimator("pt").to(PtEstimator.class);
		bindUtilityEstimator("car").to(CarEstimator.class);
	}

	@Provides
	public ModularUtilityEstimator provideModularUtilityEstimator(TripRouter tripRouter, ActivityFacilities facilities,
			Map<String, Provider<UtilityEstimator>> factory, EqasimConfigGroup config) {
		Map<String, UtilityEstimator> estimators = new HashMap<>();

		for (Map.Entry<String, String> entry : config.getModeUtilityMappings().entrySet()) {
			estimators.put(entry.getKey(), factory.get(entry.getValue()).get());
		}

		return new ModularUtilityEstimator(tripRouter, facilities, estimators);
	}

	@Provides
	@Named("car")
	public CostModel provideCarCostModel(Map<String, Provider<CostModel>> factory, EqasimConfigGroup config) {
		return provideCostModel("car", factory, config);
	}

	@Provides
	@Named("pt")
	public CostModel providePtCostModel(Map<String, Provider<CostModel>> factory, EqasimConfigGroup config) {
		return provideCostModel("pt", factory, config);
	}

	public CostModel provideCostModel(String mode, Map<String, Provider<CostModel>> factory, EqasimConfigGroup config) {
		String model = config.getCostModelMappings().get(mode);

		if (model == null) {
			throw new IllegalStateException(String.format("No cost model defined for mode '%s'", mode));
		} else {
			Provider<CostModel> provider = factory.get(model);

			if (provider == null) {
				throw new IllegalStateException(String.format("Unknown cost model '%s' for mode '%s'", model, mode));
			} else {
				return provider.get();
			}
		}
	}
}
