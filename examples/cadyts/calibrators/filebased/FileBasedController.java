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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.logging.Logger;

import cadyts.calibrators.Calibrator;
import cadyts.calibrators.analytical.AnalyticalCalibrator;
import cadyts.demand.Plan;
import cadyts.demand.PlanChoiceModel;
import cadyts.measurements.SingleLinkMeasurement;
import cadyts.utilities.misc.CommandLineParser;
import cadyts.utilities.misc.CommandLineParserElement;

/**
 * This class allows to link a (subclass of) an AnalyticalCalibrator via files
 * to a DTA simulation. The coupling is realized in three stages:
 * <p>
 * The INIT stage is entered only once. During this stage, the calibration is
 * intialized. Then, in every iteration of the simulation, the CHOICE stage and
 * the UPDATE stage are sequentially called. In the CHOICE stage, all agents
 * replan. In the UPDATE stage, the resulting network conditions are fed back to
 * the calibration.
 * 
 * A concrete subclass needs to implement several abstract functions. Beyond
 * this, a number of hooks are provided.
 * 
 * @author Gunnar Flötteröd
 * 
 * @param C
 *            the calibrator type
 * @param A
 *            the agent type
 * @param P
 *            the plan type
 * 
 */
public abstract class FileBasedController<C extends AnalyticalCalibrator<L>, A extends Agent<P, ? extends PlanChoiceModel<P>>, P extends Plan<L>, L> {

	// -------------------- CONSTANTS --------------------

	public static final String FILENAME_SEPARATOR_REGEX = "\\,";
	public static final String SERIALIZED_FILE = "serialized.bin";

	public static final String INIT = "INIT";
	public static final String MEASFILE_KEY = "-measfile";
	public static final String LOGFILE_KEY = "-logfile";
	public static final String RNDSEED_KEY = "-rndseed";
	public static final String BINSIZE_KEY = "-binsize";
	public static final String REGRINERTIA_KEY = "-regrinertia";
	public static final String FREEZEIT_KEY = "-freezeit";
	public static final String VARSCALE_KEY = "-varscale";
	public static final String MINFLOWSTDDEV_KEY = "-minflowstddev";
	public static final String MINCOUNTSTDDEV_KEY = "-mincountstddev";
	public static final String PREPITS_KEY = "-prepits";
	public static final String CENTERREGR_KEY = "-centerregr";
	public static final String STATSFILE_KEY = "-statsfile";
	public static final String PROPASSIGN_KEY = "-propassign";
	public static final String DEBUGMODE_KEY = "-debug";
	public static final String BRUTEFORCE_KEY = "-bruteforce";

	public static final String UPDATE = "UPDATE";
	public static final String CHOICESETFILE_KEY = "-choicesetfile";
	public static final String CHOICEFILE_KEY = "-choicefile";

	public static final String CHOICE = "CHOICE";
	public static final String NETFILE_KEY = "-netfile";
	public static final String FLOWFILE_KEY = "-flowfile";

	// -------------------- CONSTRUCTION --------------------

	protected FileBasedController() {
	}

	// ==================== the INIT stage ====================

	protected void init(final String[] args) throws IOException {
		final CommandLineParser clp = new CommandLineParser();
		this.prepareCommandLineParserINIT(clp);
		clp.parse(args);
		if (!clp.isComplete()) {
			this.exitWithParameterList(clp, INIT);
		}
		final C calibrator = newCalibrator(clp);
		this.prepareCalibratorINIT(calibrator, clp);

		for (String measFile : clp.getString(MEASFILE_KEY).split(
				FILENAME_SEPARATOR_REGEX)) {
			if (measFile != null && !"".equals(measFile)) {
				Logger.getLogger(this.getClass().getName()).info(
						"loading measurement file " + measFile);
				this.loadMeasurements(calibrator, measFile);
			}
		}

		this.serialize(calibrator);
	}

	/**
	 * Implement this factory method to create subclasses of
	 * FileBasedCalibrator.
	 * 
	 * @param clp
	 *            provides access to the command line parameters
	 */
	protected abstract C newCalibrator(final CommandLineParser clp);

	/**
	 * Extend this function to specify additional INIT command line parameters.
	 * 
	 * @param clp
	 *            provides access to the command line parameters
	 */
	protected void prepareCommandLineParserINIT(final CommandLineParser clp) {
		clp.defineParameter(LOGFILE_KEY, false, null, "logfile");
		clp.defineParameter(MEASFILE_KEY, true, null,
				"comma-separated list of files that contain the measurements");
		clp.defineParameter(BINSIZE_KEY, true, null, "numerical bin size [s]");
		clp.defineParameter(RNDSEED_KEY, false, Long
				.toString(Calibrator.DEFAULT_RANDOM_SEED), "random seed");
		clp.defineParameter(VARSCALE_KEY, false, Double
				.toString(Calibrator.DEFAULT_VARIANCE_SCALE),
				"scales measurement variance");
		clp.defineParameter(REGRINERTIA_KEY, false, Double
				.toString(Calibrator.DEFAULT_REGRESSION_INERTIA),
				"regression inertia");
		clp.defineParameter(FREEZEIT_KEY, false, Integer
				.toString(Calibrator.DEFAULT_FREEZE_ITERATION),
				"number of iterations until system freezes");
		clp.defineParameter(MINFLOWSTDDEV_KEY, false, Double
				.toString(Calibrator.DEFAULT_MIN_FLOW_STDDEV_VEH_H),
				"minimum flow standard deviation [veh/h]");
		clp.defineParameter(MINCOUNTSTDDEV_KEY, false, Double
				.toString(Calibrator.DEFAULT_MIN_COUNT_STDDEV_VEH),
				"minimum count standard deviation [veh]");
		clp.defineParameter(PREPITS_KEY, false, Integer
				.toString(Calibrator.DEFAULT_PREPARATORY_ITERATIONS),
				"number of preparatory iterations");
		clp.defineParameter(CENTERREGR_KEY, false, Boolean
				.toString(Calibrator.DEFAULT_CENTER_REGRESSION),
				"centering of internal regressions");
		clp.defineParameter(STATSFILE_KEY, false,
				Calibrator.DEFAULT_STATISTICS_FILE,
				"name of file where statistics are written");
		clp.defineParameter(PROPASSIGN_KEY, false, Boolean
				.toString(Calibrator.DEFAULT_PROPORTIONAL_ASSIGNMENT),
				"if the calibration is to use a proportional assignment");
		clp.defineParameter(DEBUGMODE_KEY, false, Boolean
				.toString(Calibrator.DEFAULT_DEBUG_MODE),
				"if fine-grained debug messages are to be generated");
		clp.defineParameter(BRUTEFORCE_KEY, false, Boolean
				.toString(AnalyticalCalibrator.DEFAULT_BRUTE_FORCE),
				"enforces best effort in measurement reproduction");
	}

	/**
	 * Extend this function to feed additional INIT parameters into the
	 * Calibrator.
	 * 
	 * @param calibrator
	 *            the calibrator that is to be configured
	 * @param clp
	 *            provides access to the command line parameters
	 */
	protected void prepareCalibratorINIT(final C calibrator,
			final CommandLineParser clp) {
		calibrator.setRegressionInertia(clp.getDouble(REGRINERTIA_KEY));
		calibrator.setVarianceScale(clp.getDouble(VARSCALE_KEY));
		calibrator.setFreezeIteration(clp.getInteger(FREEZEIT_KEY));
		calibrator.setMinStddev(clp.getDouble(MINFLOWSTDDEV_KEY),
				SingleLinkMeasurement.TYPE.FLOW_VEH_H);
		calibrator.setMinStddev(clp.getDouble(MINCOUNTSTDDEV_KEY),
				SingleLinkMeasurement.TYPE.COUNT_VEH);
		calibrator.setPreparatoryIterations(clp.getInteger(PREPITS_KEY));
		calibrator.setCenterRegression(clp.getBoolean(CENTERREGR_KEY));
		calibrator.setStatisticsFile(clp.getString(STATSFILE_KEY));
		calibrator.setProportionalAssignment(clp.getBoolean(PROPASSIGN_KEY));
		calibrator.setDebugMode(clp.getBoolean(DEBUGMODE_KEY));
		calibrator.setBruteForce(clp.getBoolean(BRUTEFORCE_KEY));
	}

	/**
	 * Writes the measurement data into the calibrator.
	 * 
	 * @param measFile
	 *            the file that contains the measurements
	 */
	abstract protected void loadMeasurements(final C calibrator,
			final String measFile);

	// ==================== the CHOICE stage ====================

	protected void choice(final String[] args) throws IOException,
			ClassNotFoundException {
		final CommandLineParser clp = new CommandLineParser();
		prepareCommandLineParserCHOICE(clp);
		clp.parse(args);
		if (!clp.isComplete()) {
			this.exitWithParameterList(clp, CHOICE);
		}
		final C calibrator = deserialize();
		prepareCalibratorCHOICE(calibrator, clp);
		this.selectPlans(calibrator, clp);
		this.serialize(calibrator);
	}

	/**
	 * Extend this function to specify additional CHOICE parameters.
	 * 
	 * @param clp
	 *            provides access to the command line parameters
	 */
	protected void prepareCommandLineParserCHOICE(final CommandLineParser clp) {
		clp.defineParameter(CHOICESETFILE_KEY, true, null,
				"comma-separated list of files that contain the choice sets");
		clp.defineParameter(CHOICEFILE_KEY, true, null,
				"fine where the choices are to be written");
	}

	/**
	 * Extend this function to feed additional CHOICE parameters into the
	 * Calibrator.
	 * 
	 * @param calibrator
	 *            the calibrator that is to be configured
	 * @param clp
	 *            provides access to the command line parameters
	 */
	protected void prepareCalibratorCHOICE(final C calibrator,
			final CommandLineParser clp) {
	}

	protected void selectPlans(final C calibrator, final CommandLineParser clp)
			throws IOException {

		final PopulationFileReader<A> popFileReader = this.newPopulationReader(
				calibrator, clp);
		final ChoiceFileWriter<A, P> choiceFileWriter = this
				.newChoiceFileWriter(calibrator);

		// CHECK

		if (popFileReader == null) {
			throw new NullPointerException("population file reader is null");
		}
		if (choiceFileWriter == null) {
			throw new NullPointerException("choice file writer is null");
		}

		// CONTINUE

		final String choiceFile = clp.getString(CHOICEFILE_KEY);
		choiceFileWriter.open(choiceFile);
		Logger.getLogger(this.getClass().getName()).info(
				"creating choice file " + choiceFile);
		for (String popFile : clp.getString(CHOICESETFILE_KEY).split(
				FILENAME_SEPARATOR_REGEX)) {
			if (popFile != null && !"".equals(popFile)) {
				Logger.getLogger(this.getClass().getName()).info(
						"loading choice set file " + popFile);
				for (A agent : popFileReader.getPopulationSource(popFile)) {
					this.beforeChoice(calibrator, agent);
					final List<P> plans = agent.getPlans();
					final int planIndex = calibrator
							.selectPlan(plans, agent.getPlanChoiceModel()
									.getChoiceProbabilities(plans));
					final P plan = plans.get(planIndex);
					this.afterChoice(calibrator, agent, plan);
					choiceFileWriter.write(agent, plan);
				}
			}
		}
		choiceFileWriter.close();
	}

	protected abstract PopulationFileReader<A> newPopulationReader(
			final C calibrator, final CommandLineParser clp);

	protected abstract ChoiceFileWriter<A, P> newChoiceFileWriter(
			final C calibrator);

	protected void beforeChoice(final C calibrator, final A agent) {
	}

	protected void afterChoice(final C calibrator, final A agent, final P plan) {
	}

	// ==================== the UPDATE stage ====================

	protected void update(final String[] args) throws IOException,
			ClassNotFoundException {
		final CommandLineParser clp = new CommandLineParser();
		prepareCommandLineParserUPDATE(clp);
		clp.parse(args);
		if (!clp.isComplete()) {
			this.exitWithParameterList(clp, UPDATE);
		}
		final C calibrator = deserialize();
		prepareCalibratorUPDATE(calibrator, clp);
		this.update(calibrator, clp);
		this.serialize(calibrator);
	}

	/**
	 * Extend this function to specify additional UPDATE parameters.
	 * 
	 * @param clp
	 *            provides access to the command line parameters
	 */
	protected void prepareCommandLineParserUPDATE(final CommandLineParser clp) {
		clp.defineParameter(NETFILE_KEY, true, null,
				"file that contains the network conditions");
		clp.defineParameter(FLOWFILE_KEY, false, null,
				"file in which to write comparisons of measured "
						+ "and simulated flows");
	}

	/**
	 * Extend this function to specify additional UPDATE parameters.
	 * 
	 * @param calibrator
	 *            the calibrator that is to be configured
	 * @param clp
	 *            provides access to the command line parameters
	 */
	protected void prepareCalibratorUPDATE(final C calibrator,
			final CommandLineParser clp) {
	}

	/**
	 * Implementations should notify the calibrator of the network conditions
	 * that resulted from the most recent choice set.
	 * 
	 * @param netCondFile
	 *            the file that contains the simulated network conditions
	 */
	abstract protected void update(final C calibrator,
			final CommandLineParser clp);

	// -------------------- HELPERS --------------------

	protected void serialize(final C calibrator) throws IOException {
		Logger.getLogger(this.getClass().getName()).info(
				"serializing to file " + SERIALIZED_FILE);
		final FileOutputStream fos = new FileOutputStream(SERIALIZED_FILE);
		final ObjectOutputStream out = new ObjectOutputStream(fos);
		out.writeObject(calibrator);
		out.flush();
		out.close();
	}

	@SuppressWarnings("unchecked")
	// cast to C makes no trouble if the serialized file was written by "this"
	protected C deserialize() throws IOException, ClassNotFoundException {
		final FileInputStream fis = new FileInputStream(SERIALIZED_FILE);
		final ObjectInputStream in = new ObjectInputStream(fis);
		final C result = (C) in.readObject();
		Logger.getLogger(this.getClass().getName()).info(
				"deserialized from file " + SERIALIZED_FILE);
		return result;
	}

	protected void exitWithGeneralHelp() {
		System.err.println("Calibration: Unknown parameters. "
				+ "For help, call with single parameter " + INIT + ", "
				+ CHOICE + " or " + UPDATE + ".");
		System.exit(-1);
	}

	protected void exitWithParameterList(final CommandLineParser clp,
			final String phase) {
		System.err.println("Use the following command line parameters for "
				+ phase + " :");
		for (CommandLineParserElement element : clp.getElements()) {
			System.err.println(element);
		}
		System.exit(-1);
	}

	// -------------------- MAIN-FUNCTION --------------------

	public void run(final String args[]) throws IOException,
			ClassNotFoundException {
		if (args == null || args.length == 0) {
			this.exitWithGeneralHelp();
		} else {
			final String action = args[0];
			final String[] params = new String[args.length - 1];
			System.arraycopy(args, 1, params, 0, args.length - 1);
			if (INIT.equalsIgnoreCase(action)) {
				init(params);
			} else if (CHOICE.equalsIgnoreCase(action)) {
				choice(params);
			} else if (UPDATE.equalsIgnoreCase(action)) {
				update(params);
			} else {
				this.exitWithGeneralHelp();
			}
		}
	}
}
