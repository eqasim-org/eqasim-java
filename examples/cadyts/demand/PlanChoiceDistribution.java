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

import java.util.List;

import cadyts.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <P>
 *            the plan type
 */
public class PlanChoiceDistribution<P extends Plan<?>> implements
		PlanChoiceModel<P> {

	// -------------------- MEMBERS --------------------

	private Vector choiceProbs = null;

	// -------------------- CONSTRUCTION --------------------

	public PlanChoiceDistribution() {
	}

	// -------------------- SETTERS --------------------

	public void setChoiceProbabilities(final Vector choiceProbs) {
		this.choiceProbs = choiceProbs;
	}

	public void addChoiceProbability(final double choiceProb) {
		if (choiceProb < 0 || choiceProb > 1) {
			throw new IllegalArgumentException(
					"choice probability must be in [0,1]");
		}
		if (this.choiceProbs == null) {
			this.choiceProbs = new Vector(1);
		} else {
			this.choiceProbs = this.choiceProbs.copyEnlarged(1);
		}
		this.choiceProbs.set(this.choiceProbs.size() - 1, choiceProb);
	}

	// --------------- IMPLEMENTATION OF PlanChoiceModel ---------------

	@Override
	public Vector getChoiceProbabilities(final List<? extends P> plans) {
		return this.choiceProbs;
	}
}
