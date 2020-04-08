package org.eqasim.belgrade.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class RemoveEmptyLines {

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig();

		Scenario scenario = ScenarioUtils.createScenario(config);
		
		Config config2 = ConfigUtils.createConfig();

		Scenario scenario2 = ScenarioUtils.createScenario(config2);
		
		TransitScheduleReader tsReader = new TransitScheduleReader(scenario);
		tsReader.readFile(args[0]);
		
		for (TransitLine tl : scenario.getTransitSchedule().getTransitLines().values()) {
			if (!tl.getRoutes().isEmpty()) {
				scenario2.getTransitSchedule().addTransitLine(tl);
			}
		}
		
		for (TransitStopFacility ts : scenario.getTransitSchedule().getFacilities().values()) {
			scenario2.getTransitSchedule().addStopFacility(ts);
		}
		
		TransitScheduleWriter tsWriter = new TransitScheduleWriter(scenario2.getTransitSchedule());
		tsWriter.writeFile(args[1]);
		
	}

}
