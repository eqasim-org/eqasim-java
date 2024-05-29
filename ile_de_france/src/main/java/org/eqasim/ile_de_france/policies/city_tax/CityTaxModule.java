package org.eqasim.ile_de_france.policies.city_tax;

import java.io.IOException;

import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.ile_de_france.mode_choice.costs.IDFCarCostModel;
import org.eqasim.ile_de_france.policies.city_tax.model.CityTaxModel;
import org.eqasim.ile_de_france.policies.city_tax.model.StaticCityTaxModel;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class CityTaxModule extends AbstractEqasimExtension {
	static public final String COST_MODEL = "car_cost_with_city_tax";

	@Override
	protected void installEqasimExtension() {
		CityTaxConfigGroup taxConfig = (CityTaxConfigGroup) getConfig().getModules().get(CityTaxConfigGroup.GROUP_NAME);

		if (taxConfig != null) {
			bind(CityTaxModel.class).to(StaticCityTaxModel.class);
			addTravelDisutilityFactoryBinding(TransportMode.car).to(CityTaxTravelDisutilityFactory.class);
			bindCostModel(COST_MODEL).to(CityTaxCostModel.class);
		}
	}

	@Provides
	@Singleton
	public CityTaxCostModel provideCityTaxCostModel(IDFCarCostModel delegate, CityTaxModel model) {
		return new CityTaxCostModel(delegate, model);
	}

	@Provides
	@Singleton
	public CityTaxTravelDisutilityFactory provideCityTaxTravelDisutilityFactory(CityTaxModel model) {
		return new CityTaxTravelDisutilityFactory(new OnlyTimeDependentTravelDisutilityFactory(), model);
	}

	@Provides
	@Singleton
	public StaticCityTaxModel provideStaticCityTaxModel(CityTaxConfigGroup taxConfig, Network network) {
		try {
			return StaticCityTaxModel.create(network, taxConfig.travelPenalty, taxConfig.fee_EUR,
					ConfigGroup.getInputFileURL(getConfig().getContext(), taxConfig.zonesPath));
		} catch (IOException e) {
			throw new RuntimeException();
		}
	}
}
