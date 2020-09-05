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
package cadyts.interfaces.sumo;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.Iterator;
import java.util.List;

import cadyts.demand.Plan;
import cadyts.demand.PlanBuilder;
import cadyts.demand.PlanStep;
import cadyts.utilities.misc.DynamicData;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
class SumoPlan implements Plan<String> {

	// -------------------- MEMBERS --------------------

	private final int routeId;

	private final String startLink;

	private final Plan<String> plan;

	private final int exitTime_s;

	// -------------------- CONSTRUCTION --------------------

	/**
	 * @param startTime_s
	 *            start time of this route
	 * @param edges
	 *            list of edges as contained in SUMO XML file
	 * @param exits
	 *            list of integer(!) exit times as contained in SUMO XML file
	 * @param travelTimes
	 *            optional travel time information (if non-null, overrides
	 *            travel time information contained in exits parameters)
	 */
	SumoPlan(final int routeId, final int startTime_s,
			final List<String> edges, final List<Integer> exits,
			final DynamicData<String> travelTimes) {

		this.routeId = routeId;

		if (edges.size() == 0) {
			/*
			 * "Stay at home" route. exitTime_s receives a meaningless value.
			 */
			this.startLink = null;
			this.plan = null;
			this.exitTime_s = startTime_s;
		} else {
			/*- 
			 * A real route that describes a physical movement through the
			 * network. Requires to translate the SUMO route representation
			 * into the internal plan format.
			 * 
			 * Example route:
			 * 
			 *       link 1           link 2           link 3
			 * o----------------o----------------o----------------o
			 *     ^            ^                ^           ^              
			 *   entry      reach node       reach node     exit                 
			 *   at t=0       at t=5           at t=10     at t=15
			 *                                  
			 * SUMO representation:
			 *   1. enter network at t=0
			 *   2. leave link 1 at t=5
			 *   3. leave link 2 at t=10
			 *   4. leave link 3 at t=15
			 *   => edges and exits parameters of this constructor have 3 entries
			 *   
			 * internal representation:
			 *   1. enter network on link 1 at t=0
			 *   2. enter link 2 at t=5
			 *   3. enter link 3 at t=10
			 *   4. leave network at t=15
			 *   remark: since sensors are assumed to be located at the very 
			 *   upstream end of a link, only steps 2 and 3 are evaluated 
			 *   in the calibration
			 */

			/*
			 * If travelTimes are specified, the exit times are accordingly
			 * adjusted. This is important when there is no router within the
			 * calibration loop because the calibration changes the agent
			 * behavior: this also results in changed network conditions and
			 * changed travel times, which should be reflected in the exit times
			 * (that otherwise remain unchanged because a fixed route
			 * alternative file is used).
			 * 
			 * This implementation assumes downstream queues in that the agent
			 * experiences the full (adjusted) travel time on the upstream link
			 * but only the original travel time on the downstream link.
			 */
			if (travelTimes != null) {

				// memorize the travel time on the last link
				final int lastLinkTT_s;
				if (exits.size() > 1) {
					lastLinkTT_s = exits.get(exits.size() - 1)
							- exits.get(exits.size() - 2);
				} else if (exits.size() == 1) {
					lastLinkTT_s = exits.get(exits.size() - 1) - startTime_s;
				} else {
					throw new RuntimeException("route " + routeId + " has "
							+ edges.size() + " edges but " + exits.size()
							+ " exit times");
				}

				// update travel times across intermediate links
				int time_s = startTime_s;
				for (int i = 0; i < exits.size() - 1; i++) {
					final int bin = max(0, min(travelTimes.bin(time_s),
							travelTimes.getBinCnt() - 1));
					time_s += travelTimes.getBinValue(edges.get(i), bin);
					exits.set(i, time_s);
				}

				// use memorized travel time on the last link
				time_s += lastLinkTT_s;
				exits.set(exits.size() - 1, time_s);
			}

			/*
			 * Build the internal plan format given the (possibly adjusted) exit
			 * times.
			 */
			this.startLink = edges.get(0);
			final PlanBuilder<String> factory = new PlanBuilder<String>();
			factory.addEntry(this.startLink, startTime_s);
			for (int i = 1; i < edges.size(); i++) {
				factory.addTurn(edges.get(i), exits.get(i - 1));
			}
			this.exitTime_s = exits.get(exits.size() - 1);
			factory.addExit(this.exitTime_s);
			this.plan = factory.getResult();
		}
	}

	// -------------------- CONTENT ACCESS --------------------

	int getRouteId() {
		return this.routeId;
	}

	String getStartLink() {
		return this.startLink;
	}

	int getExitTime_s() {
		return this.exitTime_s;
	}

	boolean isStayAtHome() {
		return (this.plan == null);
	}

	// --------------- IMPLEMENTATION OF Plan<String> ---------------

	@Override
	public Iterator<PlanStep<String>> iterator() {
		if (this.isStayAtHome()) {
			return new Iterator<PlanStep<String>>() {
				@Override
				public boolean hasNext() {
					return false;
				}

				@Override
				public PlanStep<String> next() {
					return null;
				}

				@Override
				public void remove() {
				}
			};
		} else {
			return this.plan.iterator();
		}
	}

	@Override
	public PlanStep<String> getStep(final int i) {
		return this.plan.getStep(i);
	}

	@Override
	public int size() {
		if (this.isStayAtHome()) {
			return 0;
		} else {
			return this.plan.size();
		}
	}
}
