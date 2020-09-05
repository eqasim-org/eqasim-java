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
package cadyts.demand;

import cadyts.utilities.misc.DynamicData;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <L>
 *            the link type
 */
public class Demand<L> extends DynamicData<L> {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	// -------------------- CONSTRUCTION --------------------

	public Demand(int startTime_s, int timePeriod_s, int binCnt) {

		super(startTime_s, timePeriod_s, binCnt);

		if (startTime_s < 0) {
			throw new IllegalArgumentException(
					"start time must not be negative");
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	public boolean add(final PlanStep<L> planStep) {
		final int bin = bin(planStep.getEntryTime_s());
		if (bin < 0 || bin >= this.getBinCnt()) {
			return false;
		}
		this.add(planStep.getLink(), bin, 1.0);
		return true;
	}
}
