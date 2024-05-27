package org.eqasim.core.simulation.modes.transit_with_abstract_access.mode_choice.utilities.estimators;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.PtUtilityEstimator;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.mode_choice.utilities.predictors.TransitWithAbstractAccessPredictor;

public class TransitWithAbstractAccessUtilityEstimator extends PtUtilityEstimator {
    @Inject
    public TransitWithAbstractAccessUtilityEstimator(ModeParameters parameters, TransitWithAbstractAccessPredictor predictor) {
        super(parameters, predictor);
    }
}
