package org.eqasim.ile_de_france.policies.city_tax.model;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.NetworkRoute;

public interface CityTaxModel {
	public double getTravelDisutility(Link link, double time);

	public double getMonetaryCost(NetworkRoute route);
}
