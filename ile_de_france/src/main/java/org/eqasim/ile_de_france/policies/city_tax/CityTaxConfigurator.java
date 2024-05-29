package org.eqasim.ile_de_france.policies.city_tax;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;

public class CityTaxConfigurator {
	public void apply(Config config) {
		CityTaxConfigGroup taxConfig = (CityTaxConfigGroup) config.getModules().get(CityTaxConfigGroup.GROUP_NAME);

		if (taxConfig != null) {
			EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
			eqasimConfig.setCostModel(TransportMode.car, CityTaxModule.COST_MODEL + "_epsilon");
		}
	}

	public void apply(Controler controller) {
		CityTaxConfigGroup taxConfig = (CityTaxConfigGroup) controller.getConfig().getModules()
				.get(CityTaxConfigGroup.GROUP_NAME);

		if (taxConfig != null) {
			controller.addOverridingModule(new CityTaxModule());
		}
	}
}
