package org.eqasim.core.simulation.modes.transit_with_abstract_access.mode_choice;

import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.mode_choice.constraints.TransitWithAbstractAccessConstraint;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.mode_choice.utilities.estimators.TransitWithAbstractAccessUtilityEstimator;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.mode_choice.utilities.predictors.TransitWithAbstractAccessPredictor;

public class TransitWithAbstractAccessModeChoiceModule extends AbstractEqasimExtension {

    public static final String TRANSIT_WITH_ABSTRACT_ACCESS_UTILITY_ESTIMATOR_NAME = "TransitWithAbstractAccessUtilityEstimator";

    @Override
    protected void installEqasimExtension() {
        bind(TransitWithAbstractAccessPredictor.class);
        bindUtilityEstimator(TRANSIT_WITH_ABSTRACT_ACCESS_UTILITY_ESTIMATOR_NAME).to(TransitWithAbstractAccessUtilityEstimator.class);
        bindTripConstraintFactory(TransitWithAbstractAccessConstraint.NAME).to(TransitWithAbstractAccessConstraint.Factory.class);
    }
}
