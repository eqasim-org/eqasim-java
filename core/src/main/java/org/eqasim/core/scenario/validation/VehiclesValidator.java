package org.eqasim.core.scenario.validation;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;

public class VehiclesValidator {
	static public void validate(Config config) {
		boolean missingVehicles = config.vehicles().getVehiclesFile() == null;
		boolean wrongVehicleSource = !config.qsim().getVehiclesSource().equals(VehiclesSource.fromVehiclesData);

		if (missingVehicles || wrongVehicleSource) {
			throw new IllegalStateException(
					"Eqasim now requires every scenario to provide a vehicles file and to use fromVehiclesData in qsim.vehiclesSource. See RunInsertVehicles to retrofit existing scenarios.");
		}
	}
}
