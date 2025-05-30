package org.matsim.contribs.discrete_mode_choice.modules;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contribs.discrete_mode_choice.model.utilities.MaximumSelector;
import org.matsim.contribs.discrete_mode_choice.model.utilities.MultinomialLogitSelector;
import org.matsim.contribs.discrete_mode_choice.model.utilities.RandomSelector;
import org.matsim.contribs.discrete_mode_choice.model.utilities.RandomValueGenerator;
import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilitySelectorFactory;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.MultinomialLogitSelectorConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;

import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * Internal module that manages all built-in selectors.
 * 
 * @author sebhoerl
 *
 */
public class SelectorModule extends AbstractDiscreteModeChoiceExtension {
	public final static String MAXIMUM = "Maximum";
	public final static String MULTINOMIAL_LOGIT = "MultinomialLogit";
	public final static String RANDOM = "Random";

	public final static Collection<String> COMPONENTS = Arrays.asList(MAXIMUM, MULTINOMIAL_LOGIT, RANDOM);

	@Override
	public void installExtension() {
		bindSelectorFactory(MAXIMUM).to(MaximumSelector.Factory.class);
		bindSelectorFactory(MULTINOMIAL_LOGIT).to(MultinomialLogitSelector.Factory.class);
		bindSelectorFactory(RANDOM).to(RandomSelector.Factory.class);
	}

	@Provides
	public UtilitySelectorFactory provideTourSelectorFactory(DiscreteModeChoiceConfigGroup dmcConfig,
			Map<String, Provider<UtilitySelectorFactory>> components, Scenario scenario) {
		Provider<UtilitySelectorFactory> provider = components.get(dmcConfig.getSelector());
		RandomValueGenerator
				.setGlobalSeed(((GlobalConfigGroup) scenario.getConfig().getModules().get(GlobalConfigGroup.GROUP_NAME))
						.getRandomSeed());

		if (provider != null) {
			return provider.get();
		} else {
			throw new IllegalStateException(String
					.format("There is no UtilitySelector component for tours called '%s',", dmcConfig.getSelector()));
		}
	}

	@Provides
	@Singleton
	public MaximumSelector.Factory provideMaximumTripSelector() {
		return new MaximumSelector.Factory();
	}

	@Provides
	@Singleton
	public MultinomialLogitSelector.Factory provideMultinomialLogitTripSelector(
			DiscreteModeChoiceConfigGroup dmcConfig) {
		MultinomialLogitSelectorConfigGroup config = dmcConfig.getMultinomialLogitSelectorConfig();
		return new MultinomialLogitSelector.Factory(config.getMinimumUtility(), config.getMaximumUtility(),
				config.getConsiderMinimumUtility(), config.getRandomNumbers().toString());
	}

	@Provides
	@Singleton
	public RandomSelector.Factory provideRandomTripSelector() {
		return new RandomSelector.Factory();
	}
}
