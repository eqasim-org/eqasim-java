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
package cadyts.calibrators;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import cadyts.demand.Demand;
import cadyts.demand.Plan;
import cadyts.demand.PlanStep;
import cadyts.measurements.MultiLinkMeasurement;
import cadyts.measurements.SingleLinkMeasurement;
import cadyts.supply.SimResults;
import cadyts.utilities.math.MathHelpers;
import cadyts.utilities.math.Vector;
import cadyts.utilities.misc.DynamicData;
import cadyts.utilities.misc.StatisticsTracker;
import cadyts.utilities.misc.Time;

/**
 * 
 * A helper class of the Calibrator that is responsible for most algorithmic
 * issues (whereas the Calibrator deals more with the program sequence control).
 * 
 * @author Gunnar Flötteröd
 * 
 * @param L
 *            the network link type
 * 
 */
class Analyzer<L> implements Serializable {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	private static final double maxAbsPlanLambda = 15;

	// -------------------- MEMBER VARIABLES --------------------

	private final Demand<L> demand;

	private StatisticsTracker statisticsTracker;

	// SINGLE-LINK MEASUREMENTS

	private final List<SingleLinkMeasurement<L>> allSingleLinkMeas;

	private final List<SingleLinkMeasurement<L>> planListeningSingleLinkMeas;

	private final Map<L, List<SingleLinkMeasurement<L>>> link2meas;

	// MULTI-LINK MEASUREMENTS

	private final Set<L> observedLinks;

	private final List<MultiLinkMeasurement<L>> allMultiLinkMeas;

	private Vector matchList;

	private Vector newMatchList;

	// -------------------- CONSTRUCTION --------------------

	Analyzer(final int startTime_s, final int binSize_s, final int binCnt) {

		this.demand = new Demand<L>(startTime_s, binSize_s, binCnt);
		this.statisticsTracker = new StatisticsTracker(null);

		this.allSingleLinkMeas = new ArrayList<SingleLinkMeasurement<L>>();
		this.planListeningSingleLinkMeas = new ArrayList<SingleLinkMeasurement<L>>();
		this.link2meas = new LinkedHashMap<L, List<SingleLinkMeasurement<L>>>();

		this.observedLinks = new LinkedHashSet<L>();
		this.allMultiLinkMeas = new ArrayList<MultiLinkMeasurement<L>>();
		this.matchList = null;
		this.newMatchList = null;
	}

	// -------------------- SETTERS AND GETTERS -------------------

	void setStatisticsFile(final String statisticsFile) {
		this.statisticsTracker = new StatisticsTracker(statisticsFile);
	}

	String getStatisticsFile() {
		return this.statisticsTracker.getFileName();
	}

	// -------------------- SIMPLE FUNCTIONALITY --------------------

	int getBinSize_s() {
		return this.demand.getBinSize_s();
	}

	void freeze() {
		for (SingleLinkMeasurement<L> meas : this.allSingleLinkMeas) {
			meas.freeze();
		}
		for (MultiLinkMeasurement<L> meas : this.allMultiLinkMeas) {
			meas.freeze();
		}
	}

	// -------------------- MEASUREMENT BOOKKEEPING -------------------

	void addMeasurement(final SingleLinkMeasurement<L> meas) {
		this.allSingleLinkMeas.add(meas);
		this.allocateSingleLinkMeasurement(meas);
	}

	void addMeasurement(final MultiLinkMeasurement<L> meas) {
		this.allMultiLinkMeas.add(meas);
		this.observedLinks.addAll(meas.getObservedLinks());
	}

	// -------------------- SINGLE-LINK-HELPERS --------------------

	private void allocateSingleLinkMeasurement(
			final SingleLinkMeasurement<L> meas) {
		if (meas.isPlanListening()) {
			this.planListeningSingleLinkMeas.add(meas);
		}
		for (L link : meas.getRelevantLinks()) {
			List<SingleLinkMeasurement<L>> measList = this.link2meas.get(link);
			if (measList == null) {
				measList = new ArrayList<SingleLinkMeasurement<L>>();
				this.link2meas.put(link, measList);
			}
			measList.add(meas);
		}
	}

	// -------------------- MULTI-LINK-HELPERS --------------------

	private int[] indicesInMeas(final Plan<L> plan) {
		final int[] result = new int[this.allMultiLinkMeas.size()];
		for (PlanStep<L> step : plan) {
			for (int m = 0; m < this.allMultiLinkMeas.size(); m++) {
				final MultiLinkMeasurement<L> meas = this.allMultiLinkMeas
						.get(m);
				final int indexInMeas = result[m];
				if (indexInMeas < meas.size()
						&& meas.appliesTo(indexInMeas, step)) {
					result[m]++;
				}
			}
		}
		return result;
	}

	private int numberOfMatches(int[] indicesInMeas) {
		int result = 0;
		for (int m = 0; m < this.allMultiLinkMeas.size(); m++) {
			if (indicesInMeas[m] == this.allMultiLinkMeas.get(m).size()) {
				result++;
			}
		}
		return result;
	}

	private List<Integer> matchingMeasurementIndices(final Plan<L> plan) {
		final List<Integer> result = new ArrayList<Integer>();
		final int[] indicesInMeas = this.indicesInMeas(plan);
		for (int m = 0; m < this.allMultiLinkMeas.size(); m++) {
			if (indicesInMeas[m] == this.allMultiLinkMeas.get(m).size()) {
				result.add(m);
			}
		}
		return result;
	}

	// -------------------- ANALYSIS FUNCTIONALITY --------------------

	void notifyPlanChoice(final Plan<L> plan) {
		if (plan == null) {
			return;
		}
		/*
		 * (1) NOTIFY SINGLE-LINK MEASUREMENTS
		 */
		for (SingleLinkMeasurement<L> meas : this.planListeningSingleLinkMeas) {
			meas.notifyPlanChoice(plan);
		}
		/*
		 * (2) NOTIFY MULTI-LINK MEASUREMENTS
		 */
		if (this.allMultiLinkMeas.size() > 0) {
			if (this.newMatchList == null) {
				this.newMatchList = new Vector(this.allMultiLinkMeas.size());
			}
			final List<Integer> matchingMeasIndices = this
					.matchingMeasurementIndices(plan);
			final double oneByMatches = 1.0 / matchingMeasIndices.size();
			for (int m : matchingMeasIndices) {
				this.newMatchList.add(m, oneByMatches);
			}
		}
		/*
		 * (3) UPDATE DEMAND AND STATISTICS
		 */
		for (PlanStep<L> planStep : plan) {
			if (this.link2meas.keySet().contains(planStep.getLink())
					|| (this.observedLinks.contains(planStep.getLink()))) {
				this.demand.add(planStep);
			}
		}
		this.statisticsTracker.registerChoice();
	}

	double calcLinearPlanEffect(final Plan<L> plan) {
		if (plan == null) {
			return 0.0;
		}
		double result = 0;
		/*
		 * (1) SINGLE-LINK MEASUREMENTS
		 */
		for (PlanStep<L> step : plan) {
			final List<SingleLinkMeasurement<L>> measList = this.link2meas
					.get(step.getLink());
			if (measList != null) {
				for (SingleLinkMeasurement<L> meas : measList) {
					final double lambda = meas.getLambda(step);
					this.statisticsTracker.registerLinkLambda(lambda);
					result += lambda;
				}
			}
		}
		/*
		 * (2) MULTI-LINK MEASUREMENTS
		 */
		if (this.allMultiLinkMeas.size() > 0) {
			final int[] indicesInMeas = this.indicesInMeas(plan);
			final double numberOfMatches = this.numberOfMatches(indicesInMeas);
			for (int m = 0; m < this.allMultiLinkMeas.size(); m++) {
				if (indicesInMeas[m] == this.allMultiLinkMeas.get(m).size()) {
					result += this.allMultiLinkMeas.get(m).dll_dMatches()
							/ numberOfMatches;
				}
			}
		}
		/*
		 * (4) POSTPROCESS RESULT
		 */
		result = Math.min(result, maxAbsPlanLambda);
		result = Math.max(result, -maxAbsPlanLambda);
		this.statisticsTracker.registerPlanLambda(result);
		return result;
	}

	void afterNetworkLoading(final SimResults<L> simResults,
			final String flowAnalysisFile) {
		/*
		 * (0) dump flow analysis information
		 */
		if (flowAnalysisFile != null) {
			try {
				final PrintWriter writer = new PrintWriter(flowAnalysisFile);
				writer.println("link\tstart-time\tend-time\tstart-time(sec)\t"
						+ "end-time(sec)\ttype\tsimulated\tmeasured\t"
						+ "standard-deviation\terror\tabsolute-error\t"
						+ "relative-error\trelative-absolute-error");
				for (SingleLinkMeasurement<L> meas : this.allSingleLinkMeas) {
					final L link = meas.getLink();
					final SingleLinkMeasurement.TYPE type = meas.getType();
					final int start_s = meas.getStartTime_s();
					final int end_s = meas.getEndTime_s();
					final double simValue = simResults.getSimValue(link,
							start_s, end_s, type);
					final double measValue = meas.getMeasValue();
					writer.print(link);
					writer.print("\t");
					writer.print(Time.strFromSec(start_s, ':'));
					writer.print("\t");
					writer.print(Time.strFromSec(end_s, ':'));
					writer.print("\t");
					writer.print(start_s);
					writer.print("\t");
					writer.print(end_s);
					writer.print("\t");
					writer.print(type);
					writer.print("\t");
					writer.print(simValue);
					writer.print("\t");
					writer.print(measValue);
					writer.print("\t");
					writer.print(meas.getMeasStddev());
					writer.print("\t");
					writer.print(simValue - measValue);
					writer.print("\t");
					writer.print(Math.abs(simValue - measValue));
					writer.print("\t");
					writer.print((simValue - measValue) / measValue);
					writer.print("\t");
					writer.println(Math.abs(simValue - measValue) / measValue);
				}
				writer.flush();
				writer.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		/*
		 * (1) update regressions
		 * 
		 * All demand data of this iteration was collected conditionally on the
		 * current measurement lists and the current definitions of relevant
		 * links, so the updates should happen before the measurement lists are
		 * rearranged.
		 */
		if (this.allSingleLinkMeas.size() > 0) {
			double ll = 0;
			double llPredErr = 0;
			for (SingleLinkMeasurement<L> meas : this.allSingleLinkMeas) {
				meas.update(this.demand, simResults);
				ll += meas.getLastLL();
				llPredErr += Math.abs(meas.getLastLLPredErr());
			}
			Logger.getLogger(this.getClass().getName()).info(
					"single-link log-likelihood" + " is " + ll + " +/- "
							+ llPredErr);
			this.statisticsTracker.registerSingleLinkLL(ll);
			this.statisticsTracker.registerSingleLinkLLPredError(llPredErr);
		}

		if (this.allMultiLinkMeas.size() > 0) {
			this.matchList = this.newMatchList.copy();
			this.newMatchList = null;
			for (int m = 0; m < this.allMultiLinkMeas.size(); m++) {
				this.allMultiLinkMeas.get(m).update(this.matchList.get(m),
						this.demand, simResults);
			}
			double ll = 0;
			for (int m = 0; m < this.allMultiLinkMeas.size(); m++) {
				ll += this.allMultiLinkMeas.get(m).ll(this.matchList.get(m));
			}
			this.statisticsTracker.registerMultiLinkLL(ll);
		}

		/*
		 * (2) internal updates
		 */
		this.statisticsTracker.writeToFile();
		this.statisticsTracker.clear();
		this.demand.clear();

		/*
		 * (3) (re)allocate measurements to task-specific lists
		 * 
		 * The previous call to Measurement.update(..) might have changed the
		 * internal state of the network. In particular, the plan listening of
		 * the measurements might have changed because their internal subnetwork
		 * representations might have been completed.
		 */
		this.planListeningSingleLinkMeas.clear();
		this.link2meas.clear();
		for (SingleLinkMeasurement<L> meas : this.allSingleLinkMeas) {
			this.allocateSingleLinkMeasurement(meas);
		}
		if (this.planListeningSingleLinkMeas.size() > 0) {
			Logger.getLogger(this.getClass().getName()).info(
					this.planListeningSingleLinkMeas.size()
							+ " inactive measurement(s)");
		}
	}

	// -------------------- TRANSFORMATION INTO DynamicData --------------------

	DynamicData<L> getLinkCostOffsets() {
		Logger.getLogger(this.getClass().getName()).warning(
				"experimental function, "
						+ "accounts only for single-link measurements");
		final DynamicData<L> result = new DynamicData<L>(this.demand
				.getStartTime_s(), this.demand.getBinSize_s(), this.demand
				.getBinCnt());
		for (SingleLinkMeasurement<L> meas : this.allSingleLinkMeas) {
			final int startBin = max(result.bin(meas.getStartTime_s()), 0);
			final int endBin = min(result.bin(meas.getEndTime_s() - 1), result
					.getBinCnt() - 1);
			for (int bin = startBin; bin <= endBin; bin++) {
				final double weight = MathHelpers.overlap(result
						.binStart_s(bin), result.binStart_s(bin)
						+ result.getBinSize_s(), meas.getStartTime_s(), meas
						.getEndTime_s())
						/ result.getBinSize_s();
				result.add(meas.getLink(), bin, weight
						* meas.getLambdaCoefficient(meas.getLink()));
			}
		}
		return result;
	}
}
