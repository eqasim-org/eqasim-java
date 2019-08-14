package org.eqasim.core.simulation.mode_choice;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;

import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.MapBinder;

import ch.ethz.matsim.discrete_mode_choice.modules.AbstractDiscreteModeChoiceExtension;

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
}
