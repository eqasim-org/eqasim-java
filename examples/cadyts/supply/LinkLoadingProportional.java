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
import cadyts.measurements.SingleLinkMeasurement;
import cadyts.utilities.math.SignalSmoother;
import cadyts.utilities.misc.TimedElement;
import cadyts.utilities.misc.Units;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class LinkLoadingProportional<L> extends TimedElement implements
		LinkLoading<L> {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	// -------------------- REGRESSION --------------------

	private final L link;

	private final SignalSmoother avgDemand_veh;

	private final SignalSmoother avgLinkFeature;

	private final SingleLinkMeasurement.TYPE type;

	// -------------------- CONSTRUCTION --------------------

	public LinkLoadingProportional(final L link, final int startTime_s,
			final int endTime_s, final SingleLinkMeasurement.TYPE type,
			final double regressionInertia) {

		super(startTime_s, endTime_s);

		if (link == null) {
			throw new IllegalArgumentException("link is null");
		}
		if (type == null) {
			throw new IllegalArgumentException("type is null");
		}

		this.link = link;
		this.type = type;

		this.avgDemand_veh = new SignalSmoother(1.0 - regressionInertia);
		this.avgLinkFeature = new SignalSmoother(1.0 - regressionInertia);
	}

	// -------------------- IMPLEMENTATION OF LinkLoading --------------------

	@Override
	public L getLink() {
		return this.link;
	}

	@Override
	public Set<L> getRelevantLinks() {
		final Set<L> result = new HashSet<L>(1);
		result.add(this.getLink());
		return result;
	}

	@Override
	public void freeze() {
		this.avgDemand_veh.freeze();
		this.avgLinkFeature.freeze();
	}

	@Override
	public double getRegressionInertia() {
		if (this.avgDemand_veh.isFrozen()) {
			return 1.0;
		} else {
			return 1.0 - this.avgDemand_veh.getLastInnovationWeight();
		}
	}

	@Override
	public double predictLinkFeature(final Demand<L> demand) {
		final double demand_veh = demand.getSum(this.getLink(), this
				.getStartTime_s(), this.getEndTime_s());
		return this.avgLinkFeature.getSmoothedValue()
				+ this.get_dLinkFeature_dDemand(this.link)
				* (demand_veh - this.avgDemand_veh.getSmoothedValue());
	}

	@Override
	public double get_dLinkFeature_dDemand(final L link) {
		if (this.link.equals(link)) {
			if (SingleLinkMeasurement.TYPE.FLOW_VEH_H.equals(this.type)) {
				return Units.VEH_H_PER_VEH_S / this.getDuration_s();
			} else if (SingleLinkMeasurement.TYPE.COUNT_VEH.equals(this.type)) {
				return 1.0;
			} else {
				throw new RuntimeException("unknown measurement type "
						+ this.type + " -- this should not happen");
			}
		} else {
			return 0;
		}
	}

	@Override
	public void update(final Demand<L> demand, final double linkFeature) {
		this.avgDemand_veh.addValue(demand.getSum(this.getLink(), this
				.getStartTime_s(), this.getEndTime_s()));
		this.avgLinkFeature.addValue(linkFeature);
	}

	@Override
	public boolean isPlanListening() {
		return false;
	}

	@Override
	public void notifyPlanChoice(final Plan<L> plan) {
	}

}
