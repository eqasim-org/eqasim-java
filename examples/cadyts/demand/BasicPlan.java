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

import java.util.ArrayList;
import java.util.Iterator;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <L>
 *            the link type
 */
public class BasicPlan<L> implements Plan<L> {

	// -------------------- MEMBERS --------------------

	private final ArrayList<PlanStep<L>> planSteps = new ArrayList<PlanStep<L>>();

	// -------------------- CONSTRUCTION --------------------

	public BasicPlan() {
	}

	// -------------------- CONTENT MODIFICATIONS --------------------

	void addStep(final PlanStep<L> step) {
		if (step == null) {
			throw new IllegalArgumentException("added step must not be null");
		}
		this.planSteps.add(step);
	}

	protected void trim() {
		this.planSteps.trimToSize();
	}

	// -------------------- CONTENT ACCESS --------------------

	public int size() {
		return this.planSteps.size();
	}

	public PlanStep<L> getStep(int i) {
		return this.planSteps.get(i);
	}

	public String toString() {
		return this.planSteps.toString();
	}

	// -------------------- IMPLEMENTATION OF Iterable --------------------

	public Iterator<PlanStep<L>> iterator() {
		return this.planSteps.iterator();
	}
}
