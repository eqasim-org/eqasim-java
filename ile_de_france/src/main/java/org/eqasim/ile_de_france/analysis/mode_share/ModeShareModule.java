package org.eqasim.ile_de_france.analysis.mode_share;

import org.matsim.api.core.v01.population.Population;
import org.matsim.contribs.discrete_mode_choice.modules.DiscreteModeChoiceModule;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.TerminationCriterion;
import org.matsim.core.router.MainModeIdentifier;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class ModeShareModule extends AbstractModule {
	private final static double CONVERGENCE_THRESHOLD = 0.01;

	@Override
	public void install() {
		addControlerListenerBinding().to(ModeShareCriterion.class);
		bind(TerminationCriterion.class).to(ModeShareCriterion.class);
	}

	@Provides
	@Singleton
	ModeShareCriterion provideModeShareCriterion(MainModeIdentifier mainModeIdentifier, Population population,
			OutputDirectoryHierarchy outputHierarchy, StrategyConfigGroup strategyConfig) {
		double dmcWeight = 0.0;
		double totalWeight = 0.0;

		for (StrategySettings strategy : strategyConfig.getStrategySettings()) {
			if (strategy.getStrategyName().equals(DiscreteModeChoiceModule.STRATEGY_NAME)) {
				dmcWeight += strategy.getWeight();
			}

			totalWeight += strategy.getWeight();
		}

		double dmcProbability = dmcWeight / totalWeight;

		return new ModeShareCriterion(mainModeIdentifier, population, outputHierarchy, dmcProbability,
				CONVERGENCE_THRESHOLD);
	}
}
