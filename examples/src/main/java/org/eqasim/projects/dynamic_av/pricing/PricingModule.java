package org.eqasim.projects.dynamic_av.pricing;

import org.eqasim.projects.dynamic_av.pricing.cost_calculator.CostCalculator;
import org.eqasim.projects.dynamic_av.pricing.cost_calculator.definitions.ScenarioDefinition;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import ch.ethz.matsim.av.analysis.FleetDistanceListener;
import ch.ethz.matsim.av.config.AVConfigGroup;
import ch.ethz.matsim.av.config.operator.OperatorConfig;

public class PricingModule extends AbstractModule {
	@Override
	public void install() {
		addEventHandlerBinding().to(FleetCostListener.class);
	}

	@Provides
	@Singleton
	public ScenarioDefinition provideScenarioDefinition() {
		return ScenarioDefinition.buildSwitzerlandSolo();
	}

	@Provides
	@Singleton
	public CostCalculator provideCostCalculator(ScenarioDefinition scenario) {
		return new CostCalculator(scenario);
	}

	@Provides
	@Singleton
	public FleetCostListener provideFleetCostListener(CostCalculator calculator,
			FleetDistanceListener fleetDistanceListener, AVConfigGroup config) {
		int numberOfVehicles = config.getOperatorConfig(OperatorConfig.DEFAULT_OPERATOR_ID).getGeneratorConfig()
				.getNumberOfVehicles();

		return new FleetCostListener(calculator, fleetDistanceListener, numberOfVehicles);
	}
}
