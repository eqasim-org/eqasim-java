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
package cadyts.utilities.misc;

import java.io.Serializable;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class TimedElement implements Serializable {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	public static final int MIN_START_TIME_S = 0;

	public static final int MAX_END_TIME_S = (int) Units.S_PER_D;

	private final int startTime_s;

	private final int endTime_s;

	// -------------------- CONSTRUCTION --------------------

	public TimedElement(final int startTime_s, final int endTime_s) {
		if (startTime_s >= endTime_s) {
			throw new IllegalArgumentException("start time " + startTime_s
					+ " s is not strictly before end time " + endTime_s + " s");
		}
		if (startTime_s < MIN_START_TIME_S) {
			throw new IllegalArgumentException(
					"smallest allowed measurement start time is "
							+ MIN_START_TIME_S + " s");
		}
		if (endTime_s > MAX_END_TIME_S) {
			throw new IllegalArgumentException(
					"largest allowed measurement end time is " + MAX_END_TIME_S
							+ " s");
		}
		this.startTime_s = startTime_s;
		this.endTime_s = endTime_s;
	}

	// -------------------- GETTERS --------------------

	public int getStartTime_s() {
		return this.startTime_s;
	}

	public int getEndTime_s() {
		return this.endTime_s;
	}

	public int getDuration_s() {
		return this.getEndTime_s() - this.getStartTime_s();
	}
}
