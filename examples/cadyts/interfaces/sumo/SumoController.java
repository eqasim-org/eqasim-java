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
package cadyts.interfaces.sumo;

import static cadyts.interfaces.sumo.SumoCalibrator.DEFAULT_CLONE_POSTFIX;
import static cadyts.interfaces.sumo.SumoCalibrator.DEFAULT_OVERRIDE_TRAVELTIMES;

import java.io.IOException;
import java.util.logging.Logger;

import cadyts.calibrators.filebased.ChoiceFileWriter;
import cadyts.calibrators.filebased.FileBasedController;
import cadyts.calibrators.filebased.PopulationFileReader;
import cadyts.calibrators.filebased.xml.XMLPopulationFileReader;
import cadyts.demand.ODRelation;
import cadyts.interfaces.defaults.BasicMeasurementLoaderStringLinks;
import cadyts.utilities.misc.CommandLineParser;
import cadyts.utilities.misc.DynamicData;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class SumoController extends
		FileBasedController<SumoCalibrator, SumoAgent, SumoPlan, String> {

	// -------------------- CONSTANTS --------------------

	public static final String DEMANDSCALE_KEY = "-demandscale";
	public static final String OVERRIDETT_KEY = "-overridett";
	public static final String FMAPREFIX_KEY = "-fmaprefix";
	public static final String CLONEPOSTFIX_KEY = "-clonepostfix";

	// -------------------- extending the INIT stage --------------------

	@Override
	protected SumoCalibrator newCalibrator(final CommandLineParser clp) {
		final String logFile = clp.containsKey(LOGFILE_KEY) ? clp
				.getString(LOGFILE_KEY) : "calibration-log.txt";
		final SumoCalibrator result = new SumoCalibrator(logFile, clp
				.getLong(RNDSEED_KEY), clp.getInteger(BINSIZE_KEY));
		return result;
	}

	@Override
	protected void prepareCommandLineParserINIT(final CommandLineParser clp) {
		super.prepareCommandLineParserINIT(clp);
		clp.defineParameter(DEMANDSCALE_KEY, false, Double
				.toString(SumoCalibrator.DEFAULT_DEMAND_SCALE),
				"demand is artifically scaled up by this factor");
		clp.defineParameter(OVERRIDETT_KEY, false, Boolean
				.toString(DEFAULT_OVERRIDE_TRAVELTIMES),
				"if plan travel times are to be overriden"
						+ " from dump file travel times");
		clp.defineParameter(FMAPREFIX_KEY, false, null,
				"prefix of OD matrix files in visum format");
		clp.defineParameter(CLONEPOSTFIX_KEY, false, DEFAULT_CLONE_POSTFIX,
				"postfix attached to clone ids");
	}

	@Override
	protected void prepareCalibratorINIT(final SumoCalibrator calibrator,
			final CommandLineParser clp) {
		super.prepareCalibratorINIT(calibrator, clp);
		calibrator.setDemandScale(clp.getDouble(DEMANDSCALE_KEY));
		calibrator.setOverrideTravelTimes(clp.getBoolean(OVERRIDETT_KEY));
		calibrator.setFmaPrefix(clp.getString(FMAPREFIX_KEY));
		calibrator.setClonePostfix(clp.getString(CLONEPOSTFIX_KEY));
	}

	@Override
	protected void loadMeasurements(SumoCalibrator calibrator, String measFile) {
		if (measFile == null) {
			Logger.getLogger(this.getClass().getName()).warning(
					"no measurement file specified");
			return;
		}
		(new BasicMeasurementLoaderStringLinks(calibrator)).load(measFile);
	}

	// -------------------- extending the CHOICE stage --------------------

	private DynamicData<ODRelation<String>> odMatrix = null;

	@Override
	protected PopulationFileReader<SumoAgent> newPopulationReader(
			final SumoCalibrator calibrator, final CommandLineParser clp) {

		final XMLPopulationFileReader<SumoAgent> reader = new XMLPopulationFileReader<SumoAgent>();
		SumoPopulationHandler populationHandler = new SumoPopulationHandler(
				calibrator.getTravelTimes(), calibrator.getDemandScale());
		reader.setPopulationHandler(populationHandler);

		if (calibrator.getDemandScale() > 1.0) {
			final SumoAgentCloner cloner = new SumoAgentCloner();
			cloner.run(clp.getString(CHOICESETFILE_KEY).split(
					FILENAME_SEPARATOR_REGEX), calibrator.getDemandScale());
			// could move this to INIT and store the cloned agents in Calibrator
			populationHandler.setClonedAgentIDs(cloner.getClonedAgentIDs(),
					calibrator.getClonePostfix());
		}

		return reader;
	}

	@Override
	protected ChoiceFileWriter<SumoAgent, SumoPlan> newChoiceFileWriter(
			final SumoCalibrator calibrator) {
		return new SumoChoiceWriter();
	}

	@Override
	protected void selectPlans(final SumoCalibrator calibrator,
			final CommandLineParser clp) throws IOException {

		if (calibrator.getFmaPrefix() != null) {
			this.odMatrix = new DynamicData<ODRelation<String>>(0, calibrator
					.getTimeBinSize_s(), (int) Math
					.ceil(24.0 * 3600.0 / calibrator.getTimeBinSize_s()));
		} else {
			this.odMatrix = null;
		}

		super.selectPlans(calibrator, clp);

		if (calibrator.getFmaPrefix() != null) {
			try {
				Logger.getLogger(this.getClass().getName()).info(
						"writing files " + calibrator.getFmaPrefix() + "[..]");
				(new SumoODWriter()).write(this.odMatrix, calibrator
						.getFmaPrefix());
			} catch (IOException e) {
				Logger.getLogger(this.getClass().getName()).warning(
						"unable to write files " + calibrator.getFmaPrefix()
								+ "[..]");
			}
		}
	}

	@Override
	protected void afterChoice(final SumoCalibrator calibrator,
			final SumoAgent agent, final SumoPlan plan) {

		super.afterChoice(calibrator, agent, plan);

		if (this.odMatrix != null) {
			for (int i = 0; i < agent.getPlans().size(); i++) {
				double moveProb = 0;
				if (!agent.getPlans().get(i).isStayAtHome()) {
					moveProb += calibrator.getLastChoiceProb(i);
				}
				final ODRelation<String> key = agent.getODRelation();
				if (key != null) {
					// TODO bin(..) does not check for bounds
					this.odMatrix.add(key, this.odMatrix.bin(agent
							.getDptTime_s()), moveProb);
				}
			}
		}
	}

	// -------------------- extending the UPDATE stage --------------------

	@Override
	protected void update(final SumoCalibrator calibrator,
			final CommandLineParser clp) {

		if (clp.getString(NETFILE_KEY) == null) {
			Logger.getLogger(this.getClass().getName()).warning(
					"no network conditions file specified");
			return;
		}

		final SumoFlowLoader loader = new SumoFlowLoader(calibrator
				.getTimeBinSize_s());
		loader.load(clp.getString(NETFILE_KEY));
		calibrator.setFlowAnalysisFile(clp.getString(FLOWFILE_KEY));
		calibrator.afterNetworkLoading(loader.getResults());

		calibrator.overrideTravelTimes(loader.getTravelTimes());
	}

	// -------------------- MAIN FUNCTION --------------------

	public static void main(String[] args) {
		final StringBuffer msg = new StringBuffer("processing call: ");
		if (args != null) {
			for (String arg : args) {
				msg.append(arg);
				msg.append(" ");
			}
		}
		Logger.getLogger(SumoController.class.getName()).info(msg.toString());
		try {
			(new SumoController()).run(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
