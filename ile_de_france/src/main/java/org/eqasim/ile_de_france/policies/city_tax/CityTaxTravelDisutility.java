package org.eqasim.ile_de_france.policies.city_tax;

import org.eqasim.ile_de_france.policies.city_tax.model.CityTaxModel;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

public class CityTaxTravelDisutility implements TravelDisutility {
	private final CityTaxModel model;
	private final TravelDisutility delegate;

	public CityTaxTravelDisutility(TravelDisutility delegate, CityTaxModel model) {
		this.model = model;
		this.delegate = delegate;
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		return delegate.getLinkTravelDisutility(link, time, person, vehicle) + model.getTravelDisutility(link, time);
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return delegate.getLinkMinimumTravelDisutility(link);
	}
}
