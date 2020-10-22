package org.eqasim.core.scenario.cutter;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.MatsimVehicleWriter;

public class ScenarioWriter {
	private final String prefix;
	private final Scenario scenario;
	private final Config config;

	public ScenarioWriter(Config config, Scenario scenario, String prefix) {
		this.scenario = scenario;
		this.prefix = prefix;
		this.config = config;
	}

	public void run(File outputDirectory) {
		checkOutputDirectory(outputDirectory);

		if (!outputDirectory.exists()) {
			outputDirectory.mkdir();
		}

		new ConfigWriter(config).write(new File(outputDirectory, prefix + "config.xml").toString());
		new PopulationWriter(scenario.getPopulation())
				.write(new File(outputDirectory, prefix + "population.xml.gz").toString());
		new FacilitiesWriter(scenario.getActivityFacilities())
				.write(new File(outputDirectory, prefix + "facilities.xml.gz").toString());
		new NetworkWriter(scenario.getNetwork()).write(new File(outputDirectory, prefix + "network.xml.gz").toString());
		new HouseholdsWriterV10(scenario.getHouseholds())
				.writeFile(new File(outputDirectory, prefix + "households.xml.gz").toString());
		new TransitScheduleWriter(scenario.getTransitSchedule())
				.writeFile(new File(outputDirectory, prefix + "transit_schedule.xml.gz").toString());
		new MatsimVehicleWriter(scenario.getTransitVehicles())
				.writeFile(new File(outputDirectory, prefix + "transit_vehicles.xml.gz").toString());

	}

	static void checkOutputDirectory(File outputDirectory) {
		File parentDirectory = outputDirectory.getParentFile();

		if (!parentDirectory.exists()) {
			throw new IllegalStateException(
					String.format("Parent directory of output directory %s should exist.", outputDirectory.toString()));
		}
	}
}
