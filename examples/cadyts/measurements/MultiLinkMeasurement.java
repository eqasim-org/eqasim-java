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
package cadyts.measurements;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import cadyts.calibrators.Calibrator;
import cadyts.demand.Demand;
import cadyts.demand.PlanStep;
import cadyts.supply.LinkLoading;
import cadyts.supply.SimResults;
import cadyts.utilities.math.SignalSmoother;
import cadyts.utilities.misc.TimedElement;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class MultiLinkMeasurement<L> implements Serializable {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	// -------------------- CONSTANT MEMBERS --------------------

	private final int value_veh;

	private final double eta;

	// -------------------- MEMBERS --------------------

	private double sensitivity;

	// temporary -- non-null only before init(..)

	private List<L> tempLinkList = new ArrayList<L>();

	private List<TimedElement> tempTimes = new ArrayList<TimedElement>();

	// permanent -- non-null only after init(..)

	private Calibrator<L> calibrator = null;

	private SignalSmoother avgMatches_veh = null;

	private List<LinkLoading<L>> loadings = null;

	private Set<L> observedLinks = null;

	// -------------------- CONSTRUCTION --------------------

	public MultiLinkMeasurement(final int value_veh, final double detectionRate) {
		if (value_veh < 0) {
			throw new IllegalArgumentException("count must not be negative");
		}
		if (detectionRate <= 0.0 || detectionRate > 1.0) {
			throw new IllegalArgumentException("detection rate of "
					+ detectionRate + " is not in (0,1]");
		}
		this.value_veh = value_veh;
		this.eta = detectionRate;
		this.sensitivity = 0.0;
	}

	public void init(final Calibrator<L> calibrator) {
		if (calibrator == null) {
			throw new IllegalArgumentException("calibrator must not be null");
		} else if (this.calibrator != null) {
			Logger.getLogger(this.getClass().getName()).warning(
					"init(..) has already been called; this call is ignored");
		} else {
			this.calibrator = calibrator;
			this.avgMatches_veh = new SignalSmoother(1.0 - calibrator
					.getRegressionInertia());
			this.loadings = new ArrayList<LinkLoading<L>>();
			this.observedLinks = new LinkedHashSet<L>();
			for (int i = 0; i < this.tempLinkList.size(); i++) {
				final LinkLoading<L> loading = calibrator.newLinkLoading(
						this.tempLinkList.get(i), this.tempTimes.get(i)
								.getStartTime_s(), this.tempTimes.get(i)
								.getEndTime_s(),
						SingleLinkMeasurement.TYPE.COUNT_VEH);
				this.loadings.add(loading);
				this.observedLinks.add(loading.getLink());
			}
			this.tempLinkList = null;
			this.tempTimes = null;
		}
	}

	// -------------------- SETTERS --------------------

	public void addObservation(final L link, final int start_s, final int end_s) {
		this.tempLinkList.add(link);
		this.tempTimes.add(new TimedElement(start_s, end_s));
	}

	// -------------------- GETTERS --------------------

	public int getCount() {
		return this.value_veh;
	}

	public double getDetectionRate() {
		return this.eta;
	}

	public int size() {
		return this.loadings.size();
	}

	public Set<L> getObservedLinks() {
		return this.observedLinks;
	}

	// -------------------- SIMPLE FUNCTIONALITY --------------------

	public void freeze() {
		this.avgMatches_veh.freeze();
		for (LinkLoading<L> loading : this.loadings) {
			loading.freeze();
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	public boolean appliesTo(final int observationIndex,
			final PlanStep<L> planStep) {
		final LinkLoading<L> loading = this.loadings.get(observationIndex);
		if (loading.getLink().equals(planStep.getLink())) {
			final int entry_s = planStep.getEntryTime_s();
			return ((loading.getStartTime_s() <= entry_s) && (loading
					.getEndTime_s() > entry_s));
		} else {
			return false;
		}
	}

	public void update(final double matches_veh, final Demand<L> demand,
			final SimResults<L> simResults) {

		this.avgMatches_veh.addValue(matches_veh);

		this.sensitivity = 1.0;
		for (LinkLoading<L> loading : this.loadings) {
			final L link = loading.getLink();
			final double flow_veh = simResults.getSimValue(link, loading
					.getStartTime_s(), loading.getEndTime_s(),
					SingleLinkMeasurement.TYPE.COUNT_VEH);
			loading.update(demand, flow_veh);
			this.sensitivity = Math.min(this.sensitivity, loading
					.get_dLinkFeature_dDemand(link));
		}
		this.sensitivity = Math.max(0.0, this.sensitivity);
	}

	private double var_veh2() {
		return this.calibrator.getVarianceScale()
				* this.value_veh
				* (1.0 - this.eta)
				+ this.eta
				* this.eta
				* this.calibrator
						.getMinStddev(SingleLinkMeasurement.TYPE.COUNT_VEH)
				* this.calibrator
						.getMinStddev(SingleLinkMeasurement.TYPE.COUNT_VEH);
	}

	public double ll(final double matches_veh) {
		final double e_veh = this.value_veh - this.eta * matches_veh;
		return (-1.0) * e_veh * e_veh / 2.0 / var_veh2();
	}

	public double dll_dMatches() {
		final double e_veh = this.value_veh - this.eta
				* this.avgMatches_veh.getSmoothedValue();
		return this.sensitivity * e_veh / var_veh2() * this.eta;
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer(this.getClass()
				.getSimpleName());
		result.append("(");
		for (LinkLoading<L> loading : this.loadings) {
			result.append(loading.getLink());
			result.append(", ");
		}
		result.append("count=");
		result.append(this.value_veh);
		result.append(", detectionRate=");
		result.append(this.eta);
		result.append(")");
		return result.toString();
	}
}
