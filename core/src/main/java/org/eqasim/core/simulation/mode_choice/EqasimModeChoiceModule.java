package org.eqasim.core.simulation.mode_choice;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.constraints.EqasimVehicleTourConstraint;
import org.eqasim.core.simulation.mode_choice.constraints.OutsideConstraint;
import org.eqasim.core.simulation.mode_choice.constraints.PassengerConstraint;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.cost.ZeroCostModel;
import org.eqasim.core.simulation.mode_choice.epsilon.EpsilonModule;
import org.eqasim.core.simulation.mode_choice.epsilon.EpsilonProvider;
import org.eqasim.core.simulation.mode_choice.filters.OutsideFilter;
import org.eqasim.core.simulation.mode_choice.filters.TourLengthFilter;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.utilities.EqasimUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.*;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.*;
import org.eqasim.core.simulation.modes.drt.mode_choice.constraints.DrtServiceAreaConstraint;
import org.eqasim.core.simulation.modes.drt.mode_choice.constraints.DrtWalkConstraint;
import org.eqasim.core.simulation.modes.drt.mode_choice.predictors.DefaultDrtPredictor;
import org.eqasim.core.simulation.modes.drt.mode_choice.predictors.DrtPredictor;
import org.eqasim.core.simulation.modes.drt.mode_choice.utilities.estimators.DrtUtilityEstimator;
import org.eqasim.core.simulation.policies.utility.UtilityPenalty;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contribs.discrete_mode_choice.components.utils.home_finder.HomeFinder;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.VehicleTourConstraintConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.ActivityFacilities;

import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

public class EqasimModeChoiceModule extends AbstractEqasimExtension {
	public static final String PASSENGER_CONSTRAINT_NAME = "PassengerConstraint";
	public static final String OUTSIDE_CONSTRAINT_NAME = "OutsideConstraint";
	public static final String DRT_WALK_CONSTRAINT = "DrtWalkConstraint";
	public static final String DRT_SERVICE_AREA_CONSTRAINT = "DrtServiceAreaConstraint";
	public static final String TOUR_LENGTH_FILTER_NAME = "TourLengthFilter";
	public static final String OUTSIDE_FILTER_NAME = "OutsideFilter";

	public static final String UTILITY_ESTIMATOR_NAME = "EqasimUtilityEstimator";

	public static final String CAR_ESTIMATOR_NAME = "CarUtilityEstimator";
	public static final String MOTORCYCLE_ESTIMATOR_NAME = "MotorcycleUtilityEstimator";
	public static final String PT_ESTIMATOR_NAME = "PtUtilityEstimator";
	public static final String BIKE_ESTIMATOR_NAME = "BikeUtilityEstimator";
	public static final String WALK_ESTIMATOR_NAME = "WalkUtilityEstimator";
	public static final String ZERO_ESTIMATOR_NAME = "ZeroUtilityEstimator";
	public static final String DRT_ESTIMATOR_NAME = "DrtUtilityEstimator";

	public static final String ZERO_COST_MODEL_NAME = "ZeroCostModel";

	public static final String VEHICLE_TOUR_CONSTRAINT = "EqasimVehicleTourConstraint";
	public static final String HOME_FINDER = "EqasimHomeFinder";

	@Override
	protected void installEqasimExtension() {
		bindTripConstraintFactory(PASSENGER_CONSTRAINT_NAME).to(PassengerConstraint.Factory.class);
		bindTripConstraintFactory(OUTSIDE_CONSTRAINT_NAME).to(OutsideConstraint.Factory.class);
		bindTripConstraintFactory(DRT_WALK_CONSTRAINT).to(DrtWalkConstraint.Factory.class);
		bindTripConstraintFactory(DRT_SERVICE_AREA_CONSTRAINT).to(DrtServiceAreaConstraint.Factory.class);

		bindTourFilter(TOUR_LENGTH_FILTER_NAME).to(TourLengthFilter.class);
		bindTourFilter(OUTSIDE_FILTER_NAME).to(OutsideFilter.class);

		bindTripEstimator(UTILITY_ESTIMATOR_NAME).to(EqasimUtilityEstimator.class);

		bind(CarPredictor.class);
		bind(MotorcyclePredictor.class);
		bind(PtPredictor.class);
		bind(BikePredictor.class);
		bind(WalkPredictor.class);
		bind(PersonPredictor.class);
		bind(DrtPredictor.class).to(DefaultDrtPredictor.class);

		bindUtilityEstimator(ZERO_ESTIMATOR_NAME).to(ZeroUtilityEstimator.class);
		bindUtilityEstimator(CAR_ESTIMATOR_NAME).to(CarUtilityEstimator.class);
		bindUtilityEstimator(MOTORCYCLE_ESTIMATOR_NAME).to(MotorcycleUtilityEstimator.class);
		bindUtilityEstimator(PT_ESTIMATOR_NAME).to(PtUtilityEstimator.class);
		bindUtilityEstimator(BIKE_ESTIMATOR_NAME).to(BikeUtilityEstimator.class);
		bindUtilityEstimator(WALK_ESTIMATOR_NAME).to(WalkUtilityEstimator.class);
		bindUtilityEstimator(DRT_ESTIMATOR_NAME).to(DrtUtilityEstimator.class);

		bindCostModel(ZERO_COST_MODEL_NAME).to(ZeroCostModel.class);

		bindTourConstraintFactory(VEHICLE_TOUR_CONSTRAINT).to(EqasimVehicleTourConstraint.Factory.class);
		bindHomeFinder(HOME_FINDER).to(EqasimHomeFinder.class);

		install(new EpsilonModule());

		// default binding that should be overridden
		bind(ModeParameters.class).toInstance(new ModeParameters());
	}

	@Provides
	public EqasimUtilityEstimator provideModularUtilityEstimator(TripRouter tripRouter, ActivityFacilities facilities,
			Map<String, Provider<UtilityEstimator>> factory, EqasimConfigGroup config,
			TimeInterpretation timeInterpretation, DiscreteModeChoiceConfigGroup dmcConfig,
			EpsilonProvider epsilonProvider, UtilityPenalty utilityPenalty) {
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

		return new EqasimUtilityEstimator(tripRouter, facilities, estimators, timeInterpretation,
				Collections.emptySet(), epsilonProvider, utilityPenalty); // Here we may add "pt" etc. as pre-routed
																			// modes.
	}

	@Provides
	@Named("car")
	public CostModel provideCarCostModel(Map<String, Provider<CostModel>> factory, EqasimConfigGroup config) {
		return getCostModel(factory, config, "car");
	}

	@Provides
	@Named("motorcycle")
	public CostModel provideMotorcycleCostModel(Map<String, Provider<CostModel>> factory, EqasimConfigGroup config) {
		return getCostModel(factory, config, "motorcycle");
	}

	@Provides
	@Named("pt")
	public CostModel providePtCostModel(Map<String, Provider<CostModel>> factory, EqasimConfigGroup config) {
		return getCostModel(factory, config, "pt");
	}

	@Provides
	@Singleton
	public EqasimVehicleTourConstraint.Factory provideEqasimVehicleTourConstraintFactory(
			DiscreteModeChoiceConfigGroup dmcConfig, HomeFinder homeFinder) {
		VehicleTourConstraintConfigGroup config = dmcConfig.getVehicleTourConstraintConfig();
		return new EqasimVehicleTourConstraint.Factory(config.getRestrictedModes(), homeFinder);
	}

	@Provides
	public DefaultDrtPredictor provideDefaultDrtPredictor(Config config, Map<String, Provider<CostModel>> factory) {
		if (!config.getModules().containsKey(MultiModeDrtConfigGroup.GROUP_NAME)) {
			throw new IllegalStateException(String.format("%s module not found", MultiModeDrtConfigGroup.GROUP_NAME));
		}
		EqasimConfigGroup eqasimConfigGroup = (EqasimConfigGroup) config.getModules().get(EqasimConfigGroup.GROUP_NAME);
		MultiModeDrtConfigGroup multiModeDrtConfigGroup = (MultiModeDrtConfigGroup) config.getModules()
				.get(MultiModeDrtConfigGroup.GROUP_NAME);
		return new DefaultDrtPredictor(multiModeDrtConfigGroup.modes()
				.collect(Collectors.toMap(mode -> mode, mode -> getCostModel(factory, eqasimConfigGroup, mode))));
	}
}
