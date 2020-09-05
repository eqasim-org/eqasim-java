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

import cadyts.calibrators.filebased.Agent;
import cadyts.demand.PlanChoiceModel;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
class DraculaAgent extends Agent<DraculaPlan, PlanChoiceModel<DraculaPlan>> {

	// -------------------- CONSTANTS --------------------

	private final DraculaODRelation od;

	private final int dptTime_s;

	private final String[] misc;

	// -------------------- CONSTRUCTION --------------------

	DraculaAgent(final long id,
			final PlanChoiceModel<DraculaPlan> planChoiceModel,
			final DraculaODRelation od, final int dptTime_s, final String[] misc) {

		super(id, planChoiceModel);

		if (od == null) {
			throw new IllegalArgumentException("od is null");
		}
		if (dptTime_s < 0) {
			throw new IllegalArgumentException("departure time " + dptTime_s
					+ " s is negative");
		}
		this.od = od;
		this.dptTime_s = dptTime_s;
		this.misc = misc;
	}

	// -------------------- GETTERS & SETTERS --------------------

	DraculaODRelation getOD() {
		return this.od;
	}

	int getDepartureTime_s() {
		return this.dptTime_s;
	}

	String[] getMisc() {
		return misc;
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		result.append(this.getClass().getSimpleName());
		result.append("(id = ");
		result.append(this.getId());
		result.append(", od = ");
		result.append(this.getOD());
		result.append(", dptTime_s = ");
		result.append(this.getDepartureTime_s());
		result.append(", plans = ");
		result.append(this.getPlans());
		result.append(")");
		return result.toString();
	}
}
