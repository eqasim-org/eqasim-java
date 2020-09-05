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
package cadyts.interfaces.dracula;

import java.util.List;
import java.util.logging.Logger;

import cadyts.demand.BasicPlan;
import cadyts.demand.PlanBuilder;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
class DraculaPlan extends BasicPlan<DraculaLink> {

	// -------------------- CONSTANTS --------------------

	private final DraculaRoute route;

	private final int departureTime_s;

	// -------------------- CONSTRUCTION --------------------

	DraculaPlan(final DraculaRoute route, final int dptTime_s,
			final DraculaTravelTimes travelTimes) {

		/*
		 * (1) set constant plan properties
		 */
		if (route == null) {
			throw new IllegalArgumentException("route is null");
		}
		if (route.getId() > 0 && travelTimes == null) {
			throw new IllegalArgumentException(
					"travel times are null but route is not empty");
		}
		this.route = route;
		this.departureTime_s = dptTime_s;

		/*
		 * (2) build internal turning-move structure
		 */
		if (!this.isStayAtHome()) {
			final List<DraculaLink> links = route.getLinks();
			if (links.size() > 0) {
				final PlanBuilder<DraculaLink> builder = new PlanBuilder<DraculaLink>();
				builder.reset(this);

				int time_s = dptTime_s;
				DraculaLink link = links.get(0);
				builder.addEntry(link, time_s);
				time_s += travelTimes.getTT_s(link, time_s);

				for (int i = 1; i < links.size(); i++) {
					link = links.get(i);
					builder.addTurn(link, time_s);
					time_s += travelTimes.getTT_s(link, time_s);
				}
				builder.addExit(time_s);
			} else {
				Logger.getLogger(this.getClass().getName()).warning(
						"non-stay-at-home route " + route.getId()
								+ " has zero links");
			}
		}
		this.trim();
	}

	// -------------------- SETTERS AND GETTERS --------------------

	protected DraculaRoute getRoute() {
		return this.route;
	}

	int getDepartureTime_s() {
		return this.departureTime_s;
	}

	DraculaODRelation getOD() {
		return this.route.getOD();
	}

	boolean isStayAtHome() {
		return (this.route.getId() == 0);
	}

	public String toString() {
		final StringBuffer result = new StringBuffer();
		result.append(this.getClass().getSimpleName());
		result.append("(dptTime_s = ");
		result.append(this.departureTime_s);
		result.append(", route = ");
		result.append(this.route);
		result.append(", link_entries = ");
		result.append(super.toString());
		result.append(")");
		return result.toString();
	}
}
