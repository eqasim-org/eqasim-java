/*
 * Cadyts - Calibration of dynamic traffic simulations
 *
 * Copyright 2009, 2010 Gunnar Flötteröd
 * 
 *
 * This file is part of Cadyts.
 *
 * Cadyts is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cadyts is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cadyts.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@epfl.ch
 *
 */ 
package cadyts.supply;

import java.io.Serializable;

import cadyts.measurements.SingleLinkMeasurement;

/**
 * 
 * Specifies a container for network conditions (the results of a network
 * loading).
 * 
 * @author Gunnar Flötteröd
 * 
 * @param L
 *            the link type
 */
public interface SimResults<L> extends Serializable {

	/**
	 * @param link
	 *            the link for which the simulated value is requested
	 * @param startTime_s
	 *            the start time (inclusive) of the considered time period (in
	 *            seconds after midnight)
	 * @param endTime_s
	 *            the end time (exclusive) of the considered time period (in
	 *            seconds after midnight)
	 * @param type
	 *            the type of the simulated value
	 */
	public double getSimValue(L link, int startTime_s, int endTime_s,
			SingleLinkMeasurement.TYPE type);

}
