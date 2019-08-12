package org.eqasim.core.simulation.mode_choice;

import org.eqasim.core.simulation.mode_choice.costs.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;

import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.MapBinder;

import ch.ethz.matsim.discrete_mode_choice.modules.AbstractDiscreteModeChoiceExtension;

public abstract class AbstractEqasimModule extends AbstractDiscreteModeChoiceExtension {
	protected MapBinder<String, CostModel> costModelBinder;
	protected MapBinder<String, UtilityEstimator> estimatorBinder;

	@Override
	protected void installExtension() {
		costModelBinder = MapBinder.newMapBinder(binder(), String.class, CostModel.class);
		estimatorBinder = MapBinder.newMapBinder(binder(), String.class, UtilityEstimator.class);

		installEqasimExtension();
	}

	protected final LinkedBindingBuilder<CostModel> bindCostModel(String name) {
		return costModelBinder.addBinding(name);
	}

	protected final LinkedBindingBuilder<UtilityEstimator> bindUtilityEstimator(String name) {
		return estimatorBinder.addBinding(name);
	}

	protected abstract void installEqasimExtension();
}
