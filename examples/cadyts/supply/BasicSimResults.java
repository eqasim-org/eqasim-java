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

import java.util.HashMap;
import java.util.Map;

import cadyts.measurements.SingleLinkMeasurement;
import cadyts.utilities.misc.DynamicData;

/**
 * An implementation of the SimResults interface that guarantees access to all
 * types of network conditions specified in Measurement.TYPE.
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <L>
 *            the link type
 */
public class BasicSimResults<L> implements SimResults<L> {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	// -------------------- MEMBERS --------------------

	private final Map<SingleLinkMeasurement.TYPE, DynamicData<L>> contents;

	// -------------------- CONSTRUCTOR --------------------

	public BasicSimResults(final int startTime_s, final int binSize_s,
			final int binCnt) {
		this.contents = new HashMap<SingleLinkMeasurement.TYPE, DynamicData<L>>(
				SingleLinkMeasurement.TYPE.values().length);
		for (SingleLinkMeasurement.TYPE type : SingleLinkMeasurement.TYPE.values()) {
			this.contents.put(type, new DynamicData<L>(startTime_s, binSize_s,
					binCnt));
		}
	}

	// -------------------- CONTENT ACCESS --------------------

	public DynamicData<L> getSimResults(final SingleLinkMeasurement.TYPE type) {
		return this.contents.get(type);
	}

	// -------------------- IMPLEMENTATION OF SimResults --------------------

	@Override
	public double getSimValue(final L link, final int startTime_s,
			final int endTime_s, final SingleLinkMeasurement.TYPE type) {
		if (type == null) {
			throw new IllegalArgumentException(
					"measurement type must not be null");
		}
		if (SingleLinkMeasurement.TYPE.FLOW_VEH_H.equals(type)) {
			return this.getSimResults(type).getAverage(link, startTime_s,
					endTime_s);
		} else if (SingleLinkMeasurement.TYPE.COUNT_VEH.equals(type)) {
			return this.getSimResults(type)
					.getSum(link, startTime_s, endTime_s);
		} else {
			throw new IllegalArgumentException("unknown measurement type "
					+ type + " -- this should not happen");
		}
	}
}
