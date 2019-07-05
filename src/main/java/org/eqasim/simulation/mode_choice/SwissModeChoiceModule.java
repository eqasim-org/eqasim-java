package org.eqasim.simulation.mode_choice;

import java.io.IOException;

import org.eqasim.simulation.mode_choice.components.SwissHomeFinder;
import org.eqasim.simulation.mode_choice.components.SwissTourFinder;
import org.eqasim.simulation.mode_choice.components.constraints.OutsideConstraint;
import org.eqasim.simulation.mode_choice.components.constraints.PassengerConstraint;
import org.eqasim.simulation.mode_choice.components.filters.OutsideFilter;
import org.eqasim.simulation.mode_choice.components.filters.TourLengthFilter;
import org.eqasim.simulation.mode_choice.parameters.CostParameters;
import org.eqasim.simulation.mode_choice.parameters.ModeChoiceParameters;
import org.eqasim.simulation.mode_choice.parameters.ParameterDefinition;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import ch.ethz.matsim.discrete_mode_choice.components.utils.home_finder.HomeFinder;
import ch.ethz.matsim.discrete_mode_choice.modules.AbstractDiscreteModeChoiceExtension;
import ch.ethz.matsim.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;

public class SwissModeChoiceModule extends AbstractDiscreteModeChoiceExtension {
	public static final String PASSENGER_CONSTRAINT_NAME = "PassengerConstraint";
	public static final String OUTSIDE_CONSTRAINT_NAME = "OutsideConstraint";

	public static final String TOUR_LENGTH_FILTER_NAME = "TourLengthFilter";
	public static final String OUTSIDE_FILTER_NAME = "OutsideFilter";

	public static final String UTILITY_ESTIMATOR_NAME = "SwissUtilityEstimator";
	public static final String MODE_AVAILABILITY_NAME = "SwissModeAvailability";

	public static final String TOUR_FINDER_NAME = "SwissTourFinder";

	private final CommandLine commandLine;

	public SwissModeChoiceModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installExtension() {
		bindTripConstraintFactory(PASSENGER_CONSTRAINT_NAME).to(PassengerConstraint.Factory.class);
		bindTripConstraintFactory(OUTSIDE_CONSTRAINT_NAME).to(OutsideConstraint.Factory.class);

		bindTourFilter(TOUR_LENGTH_FILTER_NAME).to(TourLengthFilter.class);
		bindTourFilter(OUTSIDE_FILTER_NAME).to(OutsideFilter.class);

		bindTripEstimator(UTILITY_ESTIMATOR_NAME).to(SwissUtilityEstimator.class);
		bindModeAvailability(MODE_AVAILABILITY_NAME).to(SwissModeAvailability.class);

		bindTourFinder(TOUR_FINDER_NAME).to(SwissTourFinder.class);
	}

	@Provides
	public SwissUtilityEstimator provideSwitzerlandUtilityEstimator(TripRouter tripRouter,
			ActivityFacilities facilities, ModeChoiceParameters modeChoiceParameters, CostParameters costParameters) {
		return new SwissUtilityEstimator(tripRouter, facilities, modeChoiceParameters, costParameters);
	}

	@Provides
	@Singleton
	public ModeChoiceParameters provideModeChoiceParameters() throws IOException, ConfigurationException {
		ModeChoiceParameters parameters = ModeChoiceParameters.buildDefault();
		ParameterDefinition.applyCommandLine("mode-choice-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public CostParameters provideCostParameters() {
		CostParameters parameters = CostParameters.buildDefault();
		ParameterDefinition.applyCommandLine("cost-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	public SwissModeAvailability provideSwissModeAvailability(DiscreteModeChoiceConfigGroup dmcConfig) {
		return new SwissModeAvailability();
	}

	@Provides
	public SwissTourFinder provideSwissTourFinder() {
		return new SwissTourFinder();
	}

	@Provides
	@Singleton
	@Named("tour")
	public HomeFinder provideHomeFinder() {
		return new SwissHomeFinder();
	}

	@Provides
	@Singleton
	public PassengerConstraint.Factory providePassengerConstraintFactory() {
		return new PassengerConstraint.Factory();
	}

	@Provides
	@Singleton
	public OutsideConstraint.Factory provideOutsideConstraintactory() {
		return new OutsideConstraint.Factory();
	}

	@Provides
	@Singleton
	public OutsideFilter provideOutsideFilter() {
		return new OutsideFilter();
	}

	@Provides
	@Singleton
	public TourLengthFilter provideTourLengthFilter() {
		return new TourLengthFilter();
	}
}
