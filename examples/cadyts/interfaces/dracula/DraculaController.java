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

import static cadyts.interfaces.dracula.DraculaCalibrator.DEFAULT_BETA_TT_S;
import static cadyts.interfaces.dracula.DraculaCalibrator.DEFAULT_DEMAND_PERIODS;
import static cadyts.interfaces.dracula.DraculaCalibrator.DEFAULT_DEMAND_SCALE;
import static cadyts.interfaces.dracula.DraculaCalibrator.DEFAULT_OD_PREFIX;

import java.io.IOException;
import java.util.logging.Logger;

import cadyts.calibrators.filebased.ChoiceFileWriter;
import cadyts.calibrators.filebased.FileBasedController;
import cadyts.calibrators.filebased.PopulationFileReader;
import cadyts.utilities.misc.CommandLineParser;
import cadyts.utilities.misc.DynamicData;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class DraculaController
		extends
		FileBasedController<DraculaCalibrator, DraculaAgent, DraculaPlan, DraculaLink> {

	// -------------------- CONSTANTS --------------------

	public static final String WARMUP = "-warmup";
	public static final String BETA_TT = "-betatt";
	public static final String DEMAND_FILE = "-demandfile";
	public static final String FLOW_PREFIX = "-flowprefix";
	public static final String TT_PREFIX = "-ttprefix";
	public static final String INITIAL_TT_FILE = "-initialttfile";
	public static final String DEMAND_PERIODS = "-demandperiods";
	public static final String ODPREFIX_KEY = "-odprefix";
	public static final String DEMANDSCALE_KEY = "-demandscale";

	// --------------- OVERRIDING OF FileBasedController ---------------

	// - - - - - INIT - - - - -

	@Override
	protected DraculaCalibrator newCalibrator(final CommandLineParser clp) {
		final String logFile = clp.containsKey(LOGFILE_KEY) ? clp
				.getString(LOGFILE_KEY) : "calibration-log.txt";
		final DraculaCalibrator result = new DraculaCalibrator(logFile, clp
				.getLong(RNDSEED_KEY), clp.getInteger(BINSIZE_KEY));
		return result;
	}

	@Override
	protected void prepareCommandLineParserINIT(final CommandLineParser clp) {
		super.prepareCommandLineParserINIT(clp);
		clp.defineParameter(WARMUP, false, Integer
				.toString(DraculaCalibrator.DEFAULT_WARMUP_S),
				"duration of warmup period [s]");
		clp.defineParameter(BETA_TT, true, Double.toString(DEFAULT_BETA_TT_S),
				"the travel time coefficient [1/s]");
		clp.defineParameter(DEMAND_FILE, true, null, "the initial .DEM file");
		clp.defineParameter(FLOW_PREFIX, false, null,
				"prefix of flow bookkeeping file");
		clp.defineParameter(TT_PREFIX, false, null,
				"prefix of travel time bookkeeping file");
		clp.defineParameter(INITIAL_TT_FILE, true, null,
				"the initial .LTT file");
		clp.defineParameter(DEMAND_PERIODS, false, Integer
				.toString(DEFAULT_DEMAND_PERIODS), "number of demand periods");
		clp.defineParameter(ODPREFIX_KEY, false, DEFAULT_OD_PREFIX,
				"file prefix for estimated OD matrix");
		clp.defineParameter(DEMANDSCALE_KEY, false, Double
				.toString(DEFAULT_DEMAND_SCALE),
				"demand has been artificially scaled up by this factor");
	}

	@Override
	protected void prepareCalibratorINIT(final DraculaCalibrator calibrator,
			final CommandLineParser clp) {
		super.prepareCalibratorINIT(calibrator, clp);
		calibrator.setWarmUp_s(clp.getInteger(WARMUP));
		calibrator.setBetaTT_s(clp.getDouble(BETA_TT));
		calibrator.setDemandPeriods(clp.getInteger(DEMAND_PERIODS));
		calibrator.setRoutes(new DraculaRoutes(clp.getString(DEMAND_FILE),
				calibrator));
		calibrator.setFlowPrefix(clp.getString(FLOW_PREFIX));
		calibrator.setTravelTimePrefix(clp.getString(TT_PREFIX));
		calibrator.setTravelTimes(new DraculaTravelTimes(clp
				.getString(INITIAL_TT_FILE), calibrator));
		calibrator.setOdPrefix(clp.getString(ODPREFIX_KEY));
		calibrator.setDemandScale(clp.getDouble(DEMANDSCALE_KEY));
	}

	@Override
	protected void loadMeasurements(final DraculaCalibrator calibrator,
			final String measFile) {
		final DraculaMeasurementLoader loader = new DraculaMeasurementLoader(
				calibrator);
		loader.load(measFile);
	}

	// - - - - - CHOICE - - - - -

	@Override
	protected PopulationFileReader<DraculaAgent> newPopulationReader(
			DraculaCalibrator calibrator, final CommandLineParser clp) {
		return new DraculaPopulation(calibrator);
	}

	@Override
	protected ChoiceFileWriter<DraculaAgent, DraculaPlan> newChoiceFileWriter(
			DraculaCalibrator calibrator) {
		return new DraculaChoiceWriter(calibrator);
	}

	private DynamicData<DraculaODRelation> odMatrix;

	@Override
	protected void selectPlans(final DraculaCalibrator calibrator,
			final CommandLineParser clp) throws IOException {

		if (calibrator.getOdPrefix() != null) {
			this.odMatrix = new DynamicData<DraculaODRelation>(0, calibrator
					.getTimeBinSize_s(), (int) Math
					.ceil(24.0 * 3600.0 / calibrator.getTimeBinSize_s()));
		} else {
			this.odMatrix = null;
		}

		super.selectPlans(calibrator, clp);

		if (this.odMatrix != null) {
			(new DraculaODMatrixXMLFileIO()).write(calibrator.getOdPrefix()
					+ calibrator.getIteration() + ".xml", this.odMatrix);
			this.odMatrix = null;
		}
	}

	@Override
	protected void afterChoice(final DraculaCalibrator calibrator,
			final DraculaAgent agent, final DraculaPlan plan) {
		if (this.odMatrix != null) {
			for (int i = 0; i < agent.getPlans().size(); i++) {
				double moveProb = 0;
				final DraculaPlan trip = agent.getPlans().get(i);
				if (!agent.getPlans().get(i).isStayAtHome()) {
					moveProb += calibrator.getLastChoiceProb(i);
				}
				final DraculaODRelation key = trip.getOD();
				if (key != null) {
					// TODO bin(...) does not check for bounds!
					this.odMatrix.add(key, this.odMatrix.bin(trip
							.getDepartureTime_s()), moveProb);
				}
			}
		}
	}

	// - - - - - UPDATE - - - - -

	@Override
	protected void update(final DraculaCalibrator calibrator,
			final CommandLineParser clp) {
		final DraculaTravelTimes travelTimes = new DraculaTravelTimes(clp
				.getString(NETFILE_KEY), calibrator);
		calibrator.setTravelTimes(travelTimes);
		calibrator.afterNetworkLoading(travelTimes);
	}

	// -------------------- MAIN-FUNCTION --------------------

	public static void main(String[] args) throws Exception {
		final StringBuffer msg = new StringBuffer("processing call: ");
		if (args != null) {
			for (String arg : args) {
				msg.append(arg);
				msg.append(" ");
			}
		}
		Logger.getLogger(DraculaController.class.getName())
				.info(msg.toString());
		try {
			(new DraculaController()).run(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
