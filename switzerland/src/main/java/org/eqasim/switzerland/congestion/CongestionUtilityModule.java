package org.eqasim.switzerland.congestion;

import com.google.inject.Provides;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.utilities.ModularUtilityEstimator;
import org.eqasim.switzerland.mode_choice.utilities.estimators.SwissCarUtilityEstimator;

public class CongestionUtilityModule extends AbstractEqasimExtension {
	private final double inertia;

	public CongestionUtilityModule(double inertia) {
		this.inertia = inertia;
	}

	@Override
	protected void installEqasimExtension() {
		bindTripEstimator("congestion").to(CongestionTripEstimator.class);
		bind(SwissCarUtilityEstimator.class);
	}

	@Provides
	public CongestionTripEstimator provideCongestionTripEstimator(ModularUtilityEstimator delegate,
		SwissCarUtilityEstimator carEstimator) {
		return new CongestionTripEstimator(delegate, carEstimator, inertia);
	}
}
