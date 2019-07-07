package org.eqasim.core.simulation.mode_choice;

import org.eqasim.core.simulation.mode_choice.constraints.OutsideConstraint;
import org.eqasim.core.simulation.mode_choice.constraints.PassengerConstraint;
import org.eqasim.core.simulation.mode_choice.filters.OutsideFilter;
import org.eqasim.core.simulation.mode_choice.filters.TourLengthFilter;

import ch.ethz.matsim.discrete_mode_choice.modules.AbstractDiscreteModeChoiceExtension;

public class EqasimModeChoiceModule extends AbstractDiscreteModeChoiceExtension {
	public static final String PASSENGER_CONSTRAINT_NAME = "PassengerConstraint";
	public static final String OUTSIDE_CONSTRAINT_NAME = "OutsideConstraint";

	public static final String TOUR_LENGTH_FILTER_NAME = "TourLengthFilter";
	public static final String OUTSIDE_FILTER_NAME = "OutsideFilter";

	public static final String UTILITY_ESTIMATOR_NAME = "EqasimUtilityEstimator";
	public static final String MODE_AVAILABILITY_NAME = "EqasimModeAvailability";

	public static final String TOUR_FINDER_NAME = "EqasimTourFinder";

	@Override
	protected void installExtension() {
		bindTripConstraintFactory(PASSENGER_CONSTRAINT_NAME).to(PassengerConstraint.Factory.class);
		bindTripConstraintFactory(OUTSIDE_CONSTRAINT_NAME).to(OutsideConstraint.Factory.class);

		bindTourFilter(TOUR_LENGTH_FILTER_NAME).to(TourLengthFilter.class);
		bindTourFilter(OUTSIDE_FILTER_NAME).to(OutsideFilter.class);

		bindTourFinder(TOUR_FINDER_NAME).to(UniversalTourFinder.class);
	}
}
