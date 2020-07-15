/* *********************************************************************** *
 * project: org.matsim.*
 * FreeSpeedTravelTimeCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.eqasim.san_francisco.analysis;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.eqasim.san_francisco.analysis.CarTravelTimes.Task;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.vehicles.Vehicle;

/**
 * Calculates and returns the FreeSpeedTravelTime on a link at the given time.
 * 
 * @author cdobler
 */
public class EqasimFreeSpeedTravelTime implements TravelTime {

	private final double crossingPenalty;
	private  Map<Id<Link>, double[]> delays = new HashMap<Id<Link>, double[]>();
	
	public EqasimFreeSpeedTravelTime(double crossingPenalty) {
		this.crossingPenalty = crossingPenalty;
	}
	
	public static EqasimFreeSpeedTravelTime copy(EqasimFreeSpeedTravelTime free) {
		EqasimFreeSpeedTravelTime mycopy = new EqasimFreeSpeedTravelTime(free.crossingPenalty);
		mycopy.delays = free.delays;
		return mycopy;
	}
	
	public EqasimFreeSpeedTravelTime(double crossingPenalty, String filepath) {
		this.crossingPenalty = crossingPenalty;
		BufferedReader reader;
		try {
			reader = new BufferedReader(
					new InputStreamReader(new FileInputStream(filepath)));
			reader.readLine();
			String s = reader.readLine();
			while (s != null) {
				String[] variables = s.split(",");
				Id<Link> idlink = Id.createLinkId(variables[0]);
				double hour = Double.parseDouble(variables[1]);
				double delay = Double.parseDouble(variables[2]);
				
				if (!delays.containsKey(idlink)) {
					delays.put(idlink, new double[31]);
				}
				delays.get(idlink)[(int)hour] = delay;
				
				s = reader.readLine();
			}

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {

		boolean isMajor = true;

		for (Link other : link.getToNode().getInLinks().values()) {
			if (other.getCapacity() >= link.getCapacity()) {
				isMajor = false;
			}
		}
		if (this.delays.containsKey(link.getId()) & time > 0) {
			double travelTime =  link.getLength() / link.getFreespeed(time);
			int hour = ((int) time / 3600) % 30;
			travelTime += delays.get(link.getId())[hour];
			return link.getLength() / travelTime;
		}
		
		else if (isMajor || link.getToNode().getInLinks().size() == 1) {
			return link.getLength() / link.getFreespeed(time);
		} else {
			double travelTime = link.getLength() / link.getFreespeed(time);
			travelTime += crossingPenalty;
			return travelTime;
		}

	}

}
