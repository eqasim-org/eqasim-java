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
import static java.lang.Math.sqrt;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import cadyts.demand.Plan;
import cadyts.measurements.MultiLinkMeasurement;
import cadyts.measurements.SingleLinkMeasurement;
import cadyts.supply.LinkLoading;
import cadyts.supply.LinkLoadingLocal;
import cadyts.supply.LinkLoadingProportional;
import cadyts.supply.SimResults;
import cadyts.utilities.misc.DynamicData;
import cadyts.utilities.misc.SimpleLogFormatter;
import cadyts.utilities.misc.StreamFlushHandler;
import cadyts.utilities.misc.Units;

/**
 * 
 * The basic calibration class.
 * 
 * @param L
 *            the network link type
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class Calibrator<L> implements Serializable {

	// -------------------- CONSTANTS --------------------

	// MISC

	private static final long serialVersionUID = 1L;

	public static final String VERSION = "1.1.0 (beta)";

	public static final String BASE_PACKAGE = "cadyts";

	protected final String myName = this.getClass().getName();

	// DEFAULT PARAMETER VALUES

	public static final long DEFAULT_RANDOM_SEED = 0L;

	public static final double DEFAULT_REGRESSION_INERTIA = 0.95;

	public static final int DEFAULT_FREEZE_ITERATION = Integer.MAX_VALUE;

	public static final double DEFAULT_VARIANCE_SCALE = 1.0;

	public static final double DEFAULT_MIN_FLOW_STDDEV_VEH_H = 25;

	public static final double DEFAULT_MIN_COUNT_STDDEV_VEH = 25;

	public static final int DEFAULT_PREPARATORY_ITERATIONS = 1;

	public static final boolean DEFAULT_CENTER_REGRESSION = false;

	public static final String DEFAULT_STATISTICS_FILE = "calibration-stats.txt";

	public static final boolean DEFAULT_PROPORTIONAL_ASSIGNMENT = false;

	public static final boolean DEFAULT_DEBUG_MODE = false;

	public static final String DEFAULT_FLOW_ANALYSIS_FILE = null;

	// -------------------- MEMBER VARIABLES --------------------

	// MISC

	private final String logFile;

	private final long randomSeed;

	private final Analyzer<L> analyzer;

	private final Random random;

	// PARAMETERS

	private final Map<SingleLinkMeasurement.TYPE, Double> type2minStddev;

	private double regressionInertia = DEFAULT_REGRESSION_INERTIA;

	private int freezeIteration = DEFAULT_FREEZE_ITERATION;

	private double varianceScale = DEFAULT_VARIANCE_SCALE;

	private int preparatoryIterations = DEFAULT_PREPARATORY_ITERATIONS;

	private boolean centerRegression = DEFAULT_CENTER_REGRESSION;

	private String statisticsFile = DEFAULT_STATISTICS_FILE;

	private boolean proportionalAssignment = DEFAULT_PROPORTIONAL_ASSIGNMENT;

	private boolean debugMode = DEFAULT_DEBUG_MODE;

	private String flowAnalysisFile = DEFAULT_FLOW_ANALYSIS_FILE;

	private int iteration = 0;

	// -------------------- CONSTRUCTION --------------------

	/**
	 * @param logFile
	 *            name of the file in which to write logging messages; can be
	 *            null if no logging to file is desired
	 * 
	 * @param randomSeed
	 *            provides control over reproducible random number generation;
	 *            if this is null, a default random seed will be used
	 * 
	 * @param timeBinSize_s
	 *            the time bin size in which the calibration stores and
	 *            evaluates network-related information; the length of a day in
	 *            seconds must be an integer multiple of this value
	 */
	public Calibrator(final String logFile, Long randomSeed,
			final int timeBinSize_s) {

		// INITIALIZE LOGGING

		this.logFile = logFile;
		final boolean couldNotDeleteOldLogfile;
		if (logFile != null) {
			couldNotDeleteOldLogfile = !(new File(logFile)).delete();
		} else {
			couldNotDeleteOldLogfile = false;
		}
		initLogging();
		if (couldNotDeleteOldLogfile) {
			Logger.getLogger(this.getClass().getName()).warning(
					"unable to delete old logfile");
		}
		Logger.getLogger(this.getClass().getName()).info(
				"starting " + this.getClass().getSimpleName() + " version "
						+ VERSION);

		// CHECK

		if (randomSeed == null) {
			Logger.getLogger(this.getClass().getName()).warning(
					"using default random seed");
			randomSeed = DEFAULT_RANDOM_SEED;
		}

		final int dayLength_s = (int) Units.S_PER_D;
		final int timeBinCnt = dayLength_s / timeBinSize_s;
		if (timeBinCnt * timeBinSize_s != dayLength_s) {
			throw new IllegalArgumentException("day length (" + dayLength_s
					+ " s) must be an integer multiple of timeBinSize_s");
		}

		// CONTINUE

		Logger.getLogger(this.getClass().getName()).info(
				"initializing with randomSeed = " + randomSeed
						+ ", timeBinSize_s = " + timeBinSize_s
						+ ", timeBinCnt = " + timeBinCnt);

		this.randomSeed = randomSeed;
		this.random = new Random(randomSeed);
		this.analyzer = new Analyzer<L>(0, timeBinSize_s, timeBinCnt);

		Logger.getLogger(this.myName).info(
				"default regressionInertia is " + this.regressionInertia);
		Logger.getLogger(this.myName).info(
				"default freezeIteration is " + this.freezeIteration);
		Logger.getLogger(this.myName).info(
				"default varianceScale is " + this.varianceScale);
		Logger.getLogger(this.myName).info(
				"default preparatoryIterations is "
						+ this.preparatoryIterations);
		Logger.getLogger(this.myName).info(
				"default centerRegression is " + this.centerRegression);
		Logger.getLogger(this.myName).info(
				"default statisticsFile is " + this.statisticsFile);
		Logger.getLogger(this.myName).info(
				"default proportionalAssignment is "
						+ this.proportionalAssignment);
		Logger.getLogger(this.myName).info(
				"default debugMode is " + this.debugMode);
		Logger.getLogger(this.myName).info(
				"default flowAnalysisFile is " + this.flowAnalysisFile);

		this.type2minStddev = new HashMap<SingleLinkMeasurement.TYPE, Double>();
		this.type2minStddev.put(SingleLinkMeasurement.TYPE.FLOW_VEH_H,
				DEFAULT_MIN_FLOW_STDDEV_VEH_H);
		this.type2minStddev.put(SingleLinkMeasurement.TYPE.COUNT_VEH,
				DEFAULT_MIN_COUNT_STDDEV_VEH);
		for (Map.Entry<SingleLinkMeasurement.TYPE, Double> entry : this.type2minStddev
				.entrySet()) {
			Logger.getLogger(this.myName).info(
					"default minimum standard deviation for " + entry.getKey()
							+ " is " + entry.getValue());
		}
	}

	// -------------------- INTERNALS --------------------

	private void initLogging() {

		final Logger logger = Logger.getLogger(BASE_PACKAGE);
		logger.setUseParentHandlers(false);
		for (Handler h : logger.getHandlers()) {
			h.flush();
			if (h instanceof FileHandler) { // don't close the console stream
				h.close();
			}
			logger.removeHandler(h);
		}

		final StreamFlushHandler stdOutHandler = new StreamFlushHandler(
				System.out, new SimpleLogFormatter("Calibration "));
		logger.addHandler(stdOutHandler);

		if (this.logFile != null) {
			try {
				final FileHandler fileHandler = new FileHandler(this.logFile,
						true);
				fileHandler.setFormatter(new SimpleLogFormatter(null));
				logger.addHandler(fileHandler);
			} catch (IOException e) {
				logger.warning("unable to create " + this.logFile);
			}
		}

		this.setLogLevel();
	}

	private void setLogLevel() {
		final Level level = this.debugMode ? Level.FINE : Level.INFO;
		final Logger logger = Logger.getLogger(BASE_PACKAGE);
		logger.setLevel(level);
		for (Handler h : logger.getHandlers()) {
			h.setLevel(level);
		}
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		in.defaultReadObject();
		initLogging();
	}

	// -------------------- GETTERS AND SETTERS --------------------

	public long getRandomSeed() {
		return this.randomSeed;
	}

	public Random getRandom() {
		return this.random;
	}

	public int getTimeBinSize_s() {
		return this.analyzer.getBinSize_s();
	}

	public int getIteration() {
		return this.iteration;
	}

	public DynamicData<L> getLinkCostOffsets() {
		Logger.getLogger(this.getClass().getName()).warning(
				"link cost offsets only account for single-link measurements");
		return this.analyzer.getLinkCostOffsets();
	}

	public void setRegressionInertia(final double regressionInertia) {
		if (regressionInertia <= 0 || regressionInertia > 1) {
			throw new IllegalArgumentException(
					"regressionInertia must be in (0,1]");
		}
		this.regressionInertia = regressionInertia;
		Logger.getLogger(this.myName).info(
				"set regressionInertia to " + this.regressionInertia);
	}

	public double getRegressionInertia() {
		return this.regressionInertia;
	}

	public void setFreezeIteration(int freezeIteration) {
		if (freezeIteration < 0) {
			throw new IllegalArgumentException(
					"freezeIteration must be at least 0");
		}
		this.freezeIteration = freezeIteration;
		Logger.getLogger(this.myName).info(
				"set freezeIteration to " + this.freezeIteration);
		if (this.freezeIteration < this.iteration) {
			Logger.getLogger(this.myName)
					.warning(
							"new freeze iteration " + this.freezeIteration
									+ " will have no effect because"
									+ " current iteration is already "
									+ this.iteration);
		}
	}

	public int getFreezeIteration() {
		return this.freezeIteration;
	}

	public void setVarianceScale(double varianceScale) {
		if (varianceScale <= 0) {
			throw new IllegalArgumentException(
					"varianceScale must be strictly positive");
		}
		this.varianceScale = varianceScale;
		Logger.getLogger(this.myName).info(
				"set varianceScale to " + this.varianceScale);
	}

	public double getVarianceScale() {
		return this.varianceScale;
	}

	public void setMinStddev(final double minStddev,
			final SingleLinkMeasurement.TYPE type) {
		if (minStddev <= 0) {
			throw new IllegalArgumentException(
					"minStddev for must be strictly positive");
		}
		if (type == null) {
			throw new IllegalArgumentException(
					"measurement type must not be null");
		}
		this.type2minStddev.put(type, minStddev);
		Logger.getLogger(this.myName).info(
				"set minimum standard deviation for " + type + " to "
						+ this.type2minStddev.get(type));
	}

	public double getMinStddev(final SingleLinkMeasurement.TYPE type) {
		return this.type2minStddev.get(type);
	}

	public void setPreparatoryIterations(final int preparatoryIterations) {
		if (preparatoryIterations < 1) {
			throw new IllegalArgumentException(
					"preparatoryIterations must at least be one");
		}
		this.preparatoryIterations = preparatoryIterations;
		Logger.getLogger(this.myName).info(
				"set preparatoryIterations to " + this.preparatoryIterations);
	}

	public int getPreparatoryIterations() {
		return this.preparatoryIterations;
	}

	public void setCenterRegression(final boolean centerRegression) {
		this.centerRegression = centerRegression;
		Logger.getLogger(this.myName).info(
				"set centerRegression to " + this.centerRegression);
	}

	public boolean getCenterRegression() {
		return this.centerRegression;
	}

	public void setStatisticsFile(final String statisticsFile) {
		this.analyzer.setStatisticsFile(statisticsFile);
		Logger.getLogger(this.myName).info(
				"set statisticsFile to " + this.getStatisticsFile());
	}

	public String getStatisticsFile() {
		return this.analyzer.getStatisticsFile();
	}

	public void setProportionalAssignment(final boolean proportionalAssignment) {
		this.proportionalAssignment = proportionalAssignment;
		Logger.getLogger(this.myName).info(
				"set proportionalAssignment to " + this.proportionalAssignment);
	}

	public boolean getProportionalAssignment() {
		return this.proportionalAssignment;
	}

	public void setDebugMode(final boolean debugMode) {
		this.debugMode = debugMode;
		this.setLogLevel();
		Logger.getLogger(this.myName)
				.info("set debugMode to " + this.debugMode);
	}

	public boolean getDebugMode() {
		return this.debugMode;
	}

	public void setFlowAnalysisFile(final String flowAnalysisFile) {
		this.flowAnalysisFile = flowAnalysisFile;
		Logger.getLogger(this.myName).info(
				"set flowAnalysisFile to " + this.flowAnalysisFile);
	}

	public String getFlowAnalysisFile() {
		return this.flowAnalysisFile;
	}

	// -------------------- IMPLEMENTATION --------------------

	/**
	 * This function returns a real number that approximately describes the
	 * effect of the plan on the log-likelihood function (and hence on the
	 * quality of the measurement reproduction). Based on this, Calibrator
	 * subclasses determine how to affect the demand in consideration of the
	 * measurements.
	 * 
	 * @param plan
	 *            the plan of a simulated traveler
	 * 
	 * @return the approximate effect on the log-likelihood of executing the
	 *         plan
	 */
	public double calcLinearPlanEffect(final Plan<L> plan) {
		if (this.iteration >= this.preparatoryIterations) {
			return this.analyzer.calcLinearPlanEffect(plan);
		} else {
			return 0.0;
		}
	}

	/**
	 * In every iteration, all plans that are actually selected for execution by
	 * the traveler population must registered through this function, and no
	 * other plans must be registered here.
	 * <p>
	 * This function is not public because it should not be called directly by
	 * the simulator. Proper calls to this function are implemented in concrete
	 * Calibrator subclasses.
	 * 
	 * @param plan
	 *            the actually selected plan of a simulated traveler
	 */
	public void addToDemand(final Plan<L> plan) {
		this.analyzer.notifyPlanChoice(plan);
	}

	/**
	 * "Freezes" all learning procedures in that the recursive regression that
	 * tracks the effect of plan choices on network conditions as from now
	 * operates with a maximum inertia.
	 * <p>
	 * This function is available to subclasses because in particular cases the
	 * "freezing" may be implemented by stabilizing other aspects of the
	 * iterative calibration procedure than the regression.
	 */
	protected void freeze() {
		Logger.getLogger(this.myName).fine("entering");
		Logger.getLogger(this.myName).info("freezing all learning procedures");
		this.analyzer.freeze();
		Logger.getLogger(this.myName).fine("exiting");
	}

	public LinkLoading<L> newLinkLoading(final L link, final int start_s,
			final int end_s, final SingleLinkMeasurement.TYPE type) {
		if (this.getProportionalAssignment()) {
			return new LinkLoadingProportional<L>(link, start_s, end_s, type,
					this.getRegressionInertia());
		} else {
			return new LinkLoadingLocal<L>(link, start_s, end_s, this
					.getRegressionInertia(), this.getCenterRegression());
		}
	}

	public void addMeasurement(final SingleLinkMeasurement<L> meas) {
		Logger.getLogger(this.myName).fine("entering");
		if (meas == null) {
			throw new IllegalArgumentException("measurement is null");
		}
		meas.init(this);
		this.analyzer.addMeasurement(meas);
		Logger.getLogger(this.getClass().getName()).info("added " + meas);
		Logger.getLogger(this.myName).fine("exiting");
	}

	public void addMeasurement(final L link, final int start_s,
			final int end_s, final double value, final double stddev,
			final SingleLinkMeasurement.TYPE type) {
		Logger.getLogger(this.myName).fine("entering");
		final SingleLinkMeasurement<L> meas = new SingleLinkMeasurement<L>(
				link, value, stddev * stddev, start_s, end_s, type);
		this.addMeasurement(meas);
		Logger.getLogger(this.myName).fine("exiting");
	}

	public void addMeasurement(final L link, final int start_s,
			final int end_s, final double value,
			final SingleLinkMeasurement.TYPE type) {
		Logger.getLogger(this.myName).fine("entering");
		final double stddev = max(this.getMinStddev(type), sqrt(this
				.getVarianceScale()
				* value));
		this.addMeasurement(link, start_s, end_s, value, stddev, type);
		Logger.getLogger(this.myName).fine("exiting");
	}

	public void addMeasurement(final MultiLinkMeasurement<L> meas) {
		Logger.getLogger(this.myName).fine("entering");
		if (meas == null) {
			throw new IllegalArgumentException("measurement is null");
		}
		meas.init(this);
		this.analyzer.addMeasurement(meas);
		Logger.getLogger(this.getClass().getName()).info("added " + meas);
		Logger.getLogger(this.myName).fine("exiting");
	}

	/**
	 * Must be called exactly once after every network loading.
	 * 
	 * @param simResults
	 *            a container implementation that provides access to the
	 *            simulation results of the most recent network loading
	 */
	public void afterNetworkLoading(final SimResults<L> simResults) {
		Logger.getLogger(this.myName).fine("entering");
		if ((this.iteration == 0 && this.regressionInertia == 1.0)
				|| (this.iteration == this.freezeIteration)) {
			this.freeze();
		}
		this.analyzer.afterNetworkLoading(simResults, this.flowAnalysisFile);
		this.iteration++;
		Logger.getLogger(this.myName).fine("exiting");
	}
}
