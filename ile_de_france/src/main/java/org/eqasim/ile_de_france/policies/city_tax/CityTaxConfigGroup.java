package org.eqasim.ile_de_france.policies.city_tax;

import org.matsim.core.config.ReflectiveConfigGroup;

public class CityTaxConfigGroup extends ReflectiveConfigGroup {
	static public final String GROUP_NAME = "eqasim:city_tax";

	public CityTaxConfigGroup() {
		super(GROUP_NAME);
	}

	@Parameter
	String zonesPath;

	@Parameter
	double fee_EUR;

	@Parameter
	double travelPenalty;
}
