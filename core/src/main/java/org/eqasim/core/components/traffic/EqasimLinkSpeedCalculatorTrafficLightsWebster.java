package org.eqasim.core.components.traffic;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;

public class EqasimLinkSpeedCalculatorTrafficLightsWebster implements LinkSpeedCalculator {
	final private LinkSpeedCalculator delegate;
	final private double crossingPenalty;
	final private Map<Id<Link>, double[]> trafficLightsDelays;
	
	public static Map<Id<Link>, double[]> parseCSV(String filepath) {
    	Map<Id<Link>, double[]> delays = new HashMap<Id<Link>, double[]>();
        String line = "";
        String cvsSplitBy = ",";

        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
        	int cpt = 0;

            while ((line = br.readLine()) != null) {
                String[] currentLine = line.split(cvsSplitBy);

                if (cpt > 0) {
					Id<Link> lid = Id.createLinkId(currentLine[0]);
					int hour = Integer.parseInt(currentLine[1]);
					double delay = Double.parseDouble(currentLine[2]);
					if (!delays.containsKey(lid)) {
						delays.put(lid, new double[31]);
					} 
					double[] tab = delays.get(lid);
					tab[hour] = delay;
					delays.put(lid, tab);
				}
				cpt += 1;

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        
        
        return delays;
    }

	public EqasimLinkSpeedCalculatorTrafficLightsWebster(LinkSpeedCalculator delegate, double crossingPenalty) {
		this.delegate = delegate;
		this.crossingPenalty = crossingPenalty;
		this.trafficLightsDelays = parseCSV("/home/asallard/Dokumente/Projects/Traffic lights - Zuerich/Simulation results/Last try/delays_webster_CP0.csv");
	}

	@Override
	public double getMaximumVelocity(QVehicle vehicle, Link link, double time) {
		boolean isMajor = true;

		for (Link other : link.getToNode().getInLinks().values()) {
			if (other.getCapacity() >= link.getCapacity()) {
				isMajor = false;
			}
		}
		
		if (trafficLightsDelays.containsKey(link.getId())) {
			double travelTime =  link.getLength() / delegate.getMaximumVelocity(vehicle, link, time);
			int hour = ((int) time / 3600) % 30;
			travelTime += trafficLightsDelays.get(link.getId())[hour];
			return link.getLength() / travelTime;
		}
		else if (isMajor || link.getToNode().getInLinks().size() == 1) {
			return delegate.getMaximumVelocity(vehicle, link, time);
		} 
		else {
			double travelTime =  link.getLength() / delegate.getMaximumVelocity(vehicle, link, time);
			travelTime += crossingPenalty;
			return link.getLength() / travelTime;
		}
	}
}