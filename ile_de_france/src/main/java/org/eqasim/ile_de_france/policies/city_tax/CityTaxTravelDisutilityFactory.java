package org.eqasim.ile_de_france.policies.city_tax;

import org.eqasim.ile_de_france.policies.city_tax.model.CityTaxModel;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class CityTaxTravelDisutilityFactory implements TravelDisutilityFactory {
	private final CityTaxModel model;
	private final TravelDisutilityFactory delegateFactory;

	public CityTaxTravelDisutilityFactory(TravelDisutilityFactory delegateFactory, CityTaxModel model) {
		this.delegateFactory = delegateFactory;
		this.model = model;
	}

	@Override
	public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
		return new CityTaxTravelDisutility(delegateFactory.createTravelDisutility(timeCalculator), model);
	}

}
