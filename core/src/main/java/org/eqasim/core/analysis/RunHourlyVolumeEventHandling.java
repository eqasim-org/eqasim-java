package org.eqasim.core.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

public class RunHourlyVolumeEventHandling {

	public static void main(String[] args) {

		// Path to configuration file.
		Config config = ConfigUtils.loadConfig("/home/asallard/Dokumente/Projects/Traffic lights - Zuerich/Zurich_10pct_Aurore/zurich_config.xml");
		config.controler().setLastIteration(5);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);

		// Path to inputFile
		String inputFile = "/home/asallard/Dokumente/Projects/Traffic lights - Zuerich/Zurich_10pct_Aurore/0.events.xml.gz";

		//create an event object
		EventsManager events = EventsUtils.createEventsManager();

		//create the handler and add it
		HourlyVolumeEventHandler hourlyVolumeHandler = new HourlyVolumeEventHandler();
		events.addHandler(hourlyVolumeHandler);
		
        //create the reader and read the file
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(inputFile);
		
		hourlyVolumeHandler.writeChart("/home/asallard/Dokumente/Projects/Traffic lights - Zuerich/output_link_");
		hourlyVolumeHandler.writeChart_AllLinks("/home/asallard/Dokumente/Projects/Traffic lights - Zuerich/output_link_");
		
		System.out.println("Events file read!");
		//controler.run();
	} 


}
