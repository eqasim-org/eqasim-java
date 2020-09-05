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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
class DraculaRoute implements Serializable {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	// -------------------- MEMBERS --------------------

	private final long id;

	private final DraculaODRelation od;

	private final int hashCode;

	private final ArrayList<DraculaLink> links = new ArrayList<DraculaLink>();

	// -------------------- CONSTRUCTION --------------------

	DraculaRoute(final long id, final DraculaODRelation od) {
		if (od == null) {
			throw new IllegalArgumentException("od is null");
		}
		this.id = id;
		this.od = od;
		this.hashCode = 1 + 31 * ((int) this.id + 31 * this.od.hashCode());
	}

	// -------------------- SETTERS AND GETTERS --------------------

	void addLink(final DraculaLink link) {
		if (link == null) {
			throw new IllegalArgumentException("link is null");
		} else {
			this.links.add(link);
		}
	}

	void trimToSize() {
		this.links.trimToSize();
	}

	long getId() {
		return this.id;
	}

	DraculaODRelation getOD() {
		return this.od;
	}

	List<DraculaLink> getLinks() {
		return this.links;
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public int hashCode() {
		return this.hashCode;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null) {
			return false;
		} else {
			try {
				final DraculaRoute r = (DraculaRoute) o;
				return (this.id == r.id && this.od.equals(r.od));
			} catch (ClassCastException e) {
				return false;
			}
		}
	}

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		result.append(this.getClass().getSimpleName());
		result.append("(id = ");
		result.append(this.id);
		result.append(", od = ");
		result.append(this.od);
		result.append(", links = ");
		result.append(this.links);
		result.append(")");
		return result.toString();
	}
}
