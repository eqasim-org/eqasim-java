package org.eqasim.core.scenario.cutter;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;

public class ConfigCutter {
	private final String prefix;

	public ConfigCutter(String prefix) {
		this.prefix = prefix;
	}

	public void run(Config config) {
		config.plans().setInputFile(prefix + "population.xml.gz");
		config.facilities().setInputFile(prefix + "facilities.xml.gz");
		config.network().setInputFile(prefix + "network.xml.gz");
		config.households().setInputFile(prefix + "households.xml.gz");
		config.transit().setTransitScheduleFile(prefix + "transit_schedule.xml.gz");
		config.transit().setVehiclesFile(prefix + "transit_vehicles.xml.gz");
		if (config.qsim().getVehiclesSource() == QSimConfigGroup.VehiclesSource.fromVehiclesData) {
			config.vehicles().setVehiclesFile(prefix + "vehicles.xml.gz");
		}
	}
}
