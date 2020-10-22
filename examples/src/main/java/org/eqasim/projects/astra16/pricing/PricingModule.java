package org.eqasim.projects.astra16.pricing;

import java.util.Arrays;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.projects.astra16.AstraConfigGroup;
import org.eqasim.projects.astra16.pricing.business_model.BusinessModel;
import org.eqasim.projects.astra16.pricing.business_model.BusinessModelListener;
import org.eqasim.projects.astra16.pricing.business_model.BusinessModelUpdater;
import org.eqasim.projects.astra16.pricing.cost_calculator.CostCalculator;
import org.eqasim.projects.astra16.pricing.cost_calculator.definitions.ScenarioDefinition;
import org.eqasim.projects.astra16.pricing.model.AstraAvCostModel;
import org.eqasim.projects.astra16.pricing.model.PriceInterpolator;
import org.eqasim.projects.astra16.pricing.tracker.BusinessModelTracker;
import org.eqasim.projects.astra16.pricing.tracker.PricingTracker;
import org.matsim.amodeus.analysis.FleetInformationListener;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class PricingModule extends AbstractEqasimExtension {
	@Override
	protected void installEqasimExtension() {
		bindCostModel(AstraAvCostModel.NAME).to(AstraAvCostModel.class);

		addControlerListenerBinding().to(BusinessModelUpdater.class);

		addControlerListenerBinding().to(PricingTracker.class);
		addControlerListenerBinding().to(BusinessModelTracker.class);

		addControlerListenerBinding().to(PriceInterpolator.class);
	}

	@Singleton
	@Provides
	public CostCalculator provideCostCalculator() {
		ScenarioDefinition scenario = ScenarioDefinition.buildAstra();
		return new CostCalculator(scenario);
	}

	@Singleton
	@Provides
	public BusinessModel provideBusinessModelProvider(CostCalculator costCalculator, AstraConfigGroup astraConfig,
			EqasimConfigGroup eqasimConfig) {
		return new BusinessModel(costCalculator, astraConfig.getFleetSize(), eqasimConfig.getSampleSize(),
				astraConfig.getTripFare_CHF(), astraConfig.getMaximumPricePerKm_CHF(),
				astraConfig.getMinimumPricePerKm_CHF(), astraConfig.getInfrastructureCostPerKm_CHF());
	}

	@Singleton
	@Provides
	public PriceInterpolator providePriceInterpolator(AstraConfigGroup config) {
		return new PriceInterpolator(config.getPriceInterpolationFactor(), config.getInitialPricePerKm_CHF(),
				config.getPriceInterpolationStartIteration());
	}

	@Singleton
	@Provides
	public BusinessModelUpdater provideBusinessModelUpdater(FleetInformationListener distanceListener,
			BusinessModel model, BusinessModelListener listener) {
		return new BusinessModelUpdater(distanceListener, model, listener);
	}

	@Singleton
	@Provides
	public BusinessModelListener provideBusinessModelListener(PriceInterpolator priceInterpolator,
			BusinessModelTracker modelTracker, PricingTracker pricingTracker) {
		return BusinessModelListener.combine(Arrays.asList(priceInterpolator, modelTracker, pricingTracker));
	}

	@Singleton
	@Provides
	public BusinessModelTracker provideBusinessModelTracker(OutputDirectoryHierarchy outputHierarchy) {
		return new BusinessModelTracker(outputHierarchy);
	}

	@Singleton
	@Provides
	public PricingTracker providePricingTracker(PriceInterpolator interpolator,
			OutputDirectoryHierarchy outputHierarchy) {
		return new PricingTracker(interpolator, outputHierarchy);
	}
}
