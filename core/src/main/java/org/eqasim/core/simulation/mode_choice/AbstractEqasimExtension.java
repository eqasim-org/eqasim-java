package org.eqasim.core.simulation.mode_choice;

import java.util.Map;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.matsim.contribs.discrete_mode_choice.modules.AbstractDiscreteModeChoiceExtension;

import com.google.inject.Provider;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.MapBinder;

public abstract class AbstractEqasimExtension extends AbstractDiscreteModeChoiceExtension {
	private MapBinder<String, UtilityEstimator> estimatorBinder;
	private MapBinder<String, CostModel> costModelBinder;

	@Override
	protected void installExtension() {
		this.estimatorBinder = MapBinder.newMapBinder(binder(), String.class, UtilityEstimator.class);
		this.costModelBinder = MapBinder.newMapBinder(binder(), String.class, CostModel.class);

		installEqasimExtension();
	}

	protected LinkedBindingBuilder<UtilityEstimator> bindUtilityEstimator(String name) {
		return estimatorBinder.addBinding(name);
	}

	protected LinkedBindingBuilder<CostModel> bindCostModel(String name) {
		return costModelBinder.addBinding(name);
	}

	abstract protected void installEqasimExtension();

	protected CostModel getCostModel(Map<String, Provider<CostModel>> factory, EqasimConfigGroup config, String mode) {
		String model = config.getCostModels().get(mode);

		if (model == null) {
			throw new IllegalStateException(String.format("No cost model defined for mode '%s'", mode));
		} else {
			Provider<CostModel> modelFactory = factory.get(model);

			if (modelFactory == null) {
				throw new IllegalStateException(
						String.format("Cost model '%s' for mode '%s' is not known", model, mode));
			} else {
				return modelFactory.get();
			}
		}
	}
}
