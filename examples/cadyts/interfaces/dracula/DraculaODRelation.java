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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import cadyts.demand.ODRelation;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
class DraculaODRelation extends ODRelation<Integer> {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	// -------------------- MEMBERS --------------------

	private final Set<DraculaRoute> routes = new LinkedHashSet<DraculaRoute>();

	// -------------------- CONSTRUCTION --------------------

	DraculaODRelation(final Integer fromTAZ, final Integer toTAZ) {
		super(fromTAZ, toTAZ);
	}

	// -------------------- SETTERS AND GETTERS --------------------

	void addRoute(final DraculaRoute route) {
		if (route == null) {
			throw new IllegalArgumentException("route is null");
		}
		this.routes.add(route);
	}

	Collection<DraculaRoute> getRoutes() {
		return this.routes;
	}
}
