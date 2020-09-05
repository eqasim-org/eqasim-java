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

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
class DraculaLink implements Serializable {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	private final int index;

	// -------------------- CONSTRUCTION --------------------

	DraculaLink(final int index) {
		this.index = index;
	}

	// -------------------- GETTERS --------------------

	int getIndex() {
		return this.index;
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public int hashCode() {
		return this.index;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null) {
			return false;
		} else {
			try {
				return (this.index == ((DraculaLink) o).index);
			} catch (ClassCastException e) {
				return false;
			}
		}
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + this.index + ")";
	}
}
