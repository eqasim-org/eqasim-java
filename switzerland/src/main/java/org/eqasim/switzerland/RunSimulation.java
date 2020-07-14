package org.eqasim.switzerland;

import java.util.Map;
import java.util.HashMap;

import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.switzerland.mode_choice.SwissModeChoiceModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


public class RunSimulation {
	
	public static Map<Id<Link>, Double> parseCSV(String filepath) {
    	Map<Id<Link>, Double> capacities = new HashMap<Id<Link>, Double>();
        String line = "";
        String cvsSplitBy = ",";

        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
        	int cpt = 0;

            while ((line = br.readLine()) != null) {
                String[] currentLine = line.split(cvsSplitBy);

                if (cpt >0) {
					Id<Link> lid = Id.createLinkId(currentLine[0]);
					if (!capacities.containsKey(lid)) {
						capacities.put(lid, Double.parseDouble(currentLine[5]));
					} 
				}
				cpt += 1;

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return capacities;
    }
	
	public static void read_capacities_from_CSV(String filepath, Scenario scenario) {
		Map<Id<Link>, Double> capacities = parseCSV(filepath);
		
		Network net = scenario.getNetwork();
		Map<Id<Link>, ? extends Link> links = net.getLinks();
		
		for (Id<Link> key : links.keySet()) {
			    Link l = links.get(key);
			    if (capacities.containsKey(key)) {
					l.setCapacity(capacities.get(key));
				}
			}
	}
	
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes("mode-parameter", "cost-parameter") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"),
				SwitzerlandConfigurator.getConfigGroups());
		cmd.applyConfiguration(config);

		Scenario scenario = ScenarioUtils.createScenario(config);

		SwitzerlandConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		SwitzerlandConfigurator.adjustScenario(scenario);
		
		Network net = scenario.getNetwork();
		Map<Id<Link>, ? extends Link> links = net.getLinks();
		
		for (Id<Link> key : links.keySet()) {
			    Link l = links.get(key);
			    if (l.getId() == Id.createLinkId("33942")) {
			    	System.out.println("Before: " + l.getCapacity());
			    }
		}
		
		//read_capacities_from_CSV("/home/asallard/Dokumente/Projects/Traffic lights - Zuerich/Simulation results/60it_heuristic/intersections_heuristic.csv", scenario);
		
		net = scenario.getNetwork();
		links = net.getLinks();
		
		for (Id<Link> key : links.keySet()) {
			    Link l = links.get(key);
			    if (l.getId() == Id.createLinkId("33942")) {
			    	System.out.println("After: " + l.getCapacity());
			    }
		}

		Controler controller = new Controler(scenario);
		SwitzerlandConfigurator.configureController(controller);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new SwissModeChoiceModule(cmd));
		controller.run();
		
		
	}
}
