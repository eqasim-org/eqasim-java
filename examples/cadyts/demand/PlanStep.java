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

/**
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <L>
 *            the link type
 */
public class PlanStep<L> {

	// -------------------- MEMBERS --------------------

	private final L link;

	private final int entryTime_s;

	// -------------------- CONSTRUCTION --------------------

	PlanStep(final L link, final int entryTime_s) {
		if (link == null) {
			throw new IllegalArgumentException("link must not be null");
		}
		if (entryTime_s < 0) {
			throw new IllegalArgumentException(
					"entry time must not be negative");
		}
		this.link = link;
		this.entryTime_s = entryTime_s;
	}

	// -------------------- CONTENT ACCESS --------------------

	public L getLink() {
		return this.link;
	}

	public int getEntryTime_s() {
		return this.entryTime_s;
	}

	public String toString() {
		final StringBuffer result = new StringBuffer();
		result.append(this.getClass().getSimpleName());
		result.append("(");
		result.append(this.link);
		result.append(", ");
		result.append(this.entryTime_s);
		result.append(")");
		return result.toString();
	}
}
