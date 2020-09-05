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
package cadyts.calibrators.filebased;

import java.util.ArrayList;
import java.util.List;

import cadyts.demand.Plan;
import cadyts.demand.PlanChoiceModel;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 * @param P
 *            the plan type
 * @param D
 *            the plan choice model type
 */
public class Agent<P extends Plan<?>, D extends PlanChoiceModel<P>> {

	// ------------------- CONSTANTS AND MEMBERS --------------------

	private final Object id;

	private final List<P> plans = new ArrayList<P>();

	private final D planChoiceModel;

	// -------------------- CONSTRUCTION --------------------

	public Agent(final Object id, final D planChoiceModel) {

		if (id == null) {
			throw new IllegalArgumentException("agent id is null");
		}
		if (planChoiceModel == null) {
			throw new IllegalArgumentException("plan choice model is null");
		}
		this.id = id;
		this.planChoiceModel = planChoiceModel;
	}

	// -------------------- SETTERS --------------------

	public void addPlan(final P plan) {
		this.plans.add(plan);
	}

	// -------------------- GETTERS --------------------

	public Object getId() {
		return this.id;
	}

	public List<P> getPlans() {
		return this.plans;
	}

	public D getPlanChoiceModel() {
		return this.planChoiceModel;
	}
}
