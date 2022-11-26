package org.eqasim.ile_de_france.mode_choice.epsilon;

import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.epsilon.EpsilonAdapter;
import org.eqasim.core.simulation.mode_choice.epsilon.EpsilonProvider;
import org.eqasim.core.simulation.mode_choice.epsilon.GumbelEpsilonProvider;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.BikeUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.PtUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.WalkUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.utilities.estimators.IDFCarUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.utilities.estimators.IDFPassengerUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.utilities.estimators.IDFPtUtilityEstimator;
import org.matsim.core.config.groups.GlobalConfigGroup;

import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class EpsilonModule extends AbstractEqasimExtension {
	@Provides
	public GumbelEpsilonProvider provideGumbelEpsilonProvider(GlobalConfigGroup config) {
		return new GumbelEpsilonProvider(config.getRandomSeed(), 1.0);
	}

	@Override
	protected void installEqasimExtension() {
		bind(EpsilonProvider.class).to(GumbelEpsilonProvider.class);

		bind(IDFCarUtilityEstimator.class);
		bind(IDFPtUtilityEstimator.class);
		bind(IDFPassengerUtilityEstimator.class);
		bind(PtUtilityEstimator.class);
		bind(WalkUtilityEstimator.class);

		bindUtilityEstimator("epsilon_car").to(Key.get(EpsilonAdapter.class, Names.named("epsilon_car")));
		bindUtilityEstimator("epsilon_pt").to(Key.get(EpsilonAdapter.class, Names.named("epsilon_pt")));
		bindUtilityEstimator("epsilon_bike").to(Key.get(EpsilonAdapter.class, Names.named("epsilon_bike")));
		bindUtilityEstimator("epsilon_walk").to(Key.get(EpsilonAdapter.class, Names.named("epsilon_walk")));
		bindUtilityEstimator("epsilon_passenger").to(Key.get(EpsilonAdapter.class, Names.named("epsilon_passenger")));
	}

	@Provides
	@Named("epsilon_car")
	EpsilonAdapter provideEpsilonCarEstimator(IDFCarUtilityEstimator delegate, EpsilonProvider epsilonProvider) {
		return new EpsilonAdapter("car", delegate, epsilonProvider);
	}

	@Provides
	@Named("epsilon_pt")
	EpsilonAdapter provideEpsilonPtEstimator(PtUtilityEstimator delegate, EpsilonProvider epsilonProvider) {
		return new EpsilonAdapter("pt", delegate, epsilonProvider);
	}

	@Provides
	@Named("epsilon_bike")
	EpsilonAdapter provideEpsilonBikeEstimator(BikeUtilityEstimator delegate, EpsilonProvider epsilonProvider) {
		return new EpsilonAdapter("bike", delegate, epsilonProvider);
	}

	@Provides
	@Named("epsilon_walk")
	EpsilonAdapter provideEpsilonWalkEstimator(WalkUtilityEstimator delegate, EpsilonProvider epsilonProvider) {
		return new EpsilonAdapter("walk", delegate, epsilonProvider);
	}

	@Provides
	@Named("epsilon_passenger")
	EpsilonAdapter provideEpsilonPassengerEstimator(IDFPassengerUtilityEstimator delegate,
			EpsilonProvider epsilonProvider) {
		return new EpsilonAdapter("passenger", delegate, epsilonProvider);
	}
}
