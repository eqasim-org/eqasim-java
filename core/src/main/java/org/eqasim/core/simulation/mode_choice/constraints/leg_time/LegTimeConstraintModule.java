package org.eqasim.core.simulation.mode_choice.constraints.leg_time;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.matsim.core.config.CommandLine;

public class LegTimeConstraintModule extends AbstractEqasimExtension {

    public static final String LEG_TIME_CONSTRAINT_NAME = "LegTimeConstraint";

    @Override
    protected void installEqasimExtension() {
        bindTripConstraintFactory(LEG_TIME_CONSTRAINT_NAME).to(LegTimeConstraint.Factory.class);
    }

    @Provides
    @Singleton
    public LegTimeConstraint.Factory provideLegTimeConstraintFactory(LegTimeConstraintConfigGroup legTimeConstraintConfigGroup) throws CommandLine.ConfigurationException {
        return new LegTimeConstraint.Factory(legTimeConstraintConfigGroup.getSingleLegParameterSetByMainModeByLegMode());
    }
}
