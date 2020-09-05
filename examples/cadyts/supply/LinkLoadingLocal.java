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
package cadyts.supply;

import java.util.HashSet;
import java.util.Set;

import cadyts.demand.Demand;
import cadyts.demand.Plan;
import cadyts.utilities.math.Regression;
import cadyts.utilities.math.Vector;
import cadyts.utilities.misc.TimedElement;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 * @param L
 *            the link type
 */
public class LinkLoadingLocal<L> extends TimedElement implements LinkLoading<L> {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	// -------------------- REGRESSION --------------------

	private final L link;

	private final Regression regression;

	private int iteration = 0;

	// -------------------- CONSTRUCTION --------------------

	public LinkLoadingLocal(final L link, final int startTime_s,
			final int endTime_s, final double regressionInertia,
			final boolean centerRegression) {

		super(startTime_s, endTime_s);

		if (link == null) {
			throw new IllegalArgumentException("link must not be null");
		}
		this.link = link;
		this.regression = new Regression(regressionInertia, 2);

		if (centerRegression) {
			this.regression.enableInputCentering(DEMAND_INDEX);
		}
	}

	// -------------------- HELPERS --------------------

	private static final int DEMAND_INDEX = 0;

	private static final int OFFSET_INDEX = 1;

	private final Vector regrInput = new Vector(2);

	private Vector regrInput(final Demand<L> demand) {
		this.regrInput.set(DEMAND_INDEX, demand.getSum(this.getLink(), this
				.getStartTime_s(), this.getEndTime_s()));
		this.regrInput.set(OFFSET_INDEX, 1.0);
		return this.regrInput;
	}

	// -------------------- IMPLEMENTATION OF LinkLoading --------------------

	@Override
	public L getLink() {
		return this.link;
	}

	@Override
	public Set<L> getRelevantLinks() {
		final Set<L> result = new HashSet<L>();
		result.add(this.getLink());
		return result;
	}

	@Override
	public void freeze() {
		this.regression.setInertia(1.0);
	}

	@Override
	public double getRegressionInertia() {
		return this.regression.getInertia();
	}

	@Override
	public double get_dLinkFeature_dDemand(final L link) {
		if (!this.isPlanListening() && this.link.equals(link)) {
			return this.regression.getCoefficients().get(DEMAND_INDEX);
		} else {
			return 0;
		}
	}

	@Override
	public void update(final Demand<L> demand, final double linkFeature) {
		this.iteration++;
		this.regression.update(this.regrInput(demand), linkFeature);
	}

	@Override
	public double predictLinkFeature(final Demand<L> demand) {
		return this.regression.predict(regrInput(demand));
	}

	@Override
	public boolean isPlanListening() {
		return this.iteration < 5;
	}

	@Override
	public void notifyPlanChoice(final Plan<L> plan) {
	}
}
