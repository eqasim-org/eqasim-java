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

import java.io.Serializable;

import cadyts.utilities.misc.Tuple;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <Z>
 *            the zone type
 */
public class ODRelation<Z> extends Tuple<Z, Z> implements Serializable {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	// -------------------- CONSTRUCTION --------------------

	public ODRelation(final Z fromTAZ, final Z toTAZ) {
		super(fromTAZ, toTAZ);
	}

	// -------------------- CONTENT ACCESS --------------------

	public Z getFromTAZ() {
		return super.getA();
	}

	public Z getToTAZ() {
		return super.getB();
	}

	// -------------------- OVERRIDING OF Object--------------------

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + this.getFromTAZ() + ", "
				+ this.getToTAZ() + ")";
	}
}
