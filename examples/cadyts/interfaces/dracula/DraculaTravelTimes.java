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

import static cadyts.utilities.math.MathHelpers.round;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import cadyts.measurements.SingleLinkMeasurement.TYPE;
import cadyts.supply.SimResults;
import cadyts.utilities.io.tabularFileParser.TabularFileHandler;
import cadyts.utilities.io.tabularFileParser.TabularFileParser;
import cadyts.utilities.misc.DynamicData;
import cadyts.utilities.misc.DynamicDataXMLFileIO;
import cadyts.utilities.misc.Units;

/**
 * 
 * Extracts time-dependent link travel times from the Dracula .LTT file.
 * 
 * Since the .LTT file only contains data from the main demand period, both the
 * average travel times and the average flow rates obtained from that file are
 * already constrained to the main demand period. Hence, no departure time
 * checks are conducted here, and the start of the main demand period does not
 * need to be known.
 * 
 * @author Gunnar Flötteröd
 * 
 */
class DraculaTravelTimes implements TabularFileHandler, SimResults<DraculaLink> {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	private static final String START_TAG = "22222";

	private static final String END_TAG = "99999";

	private static final int ROUTE_ID_INDEX = 3;

	private static final int DEPARTURE_TIME_INDEX = 10;

	private static final int ARRIVAL_TIME_INDEX = 11;

	private static final int LINK_COUNT_INDEX = 12;

	private static final int FIRST_TT_INDEX = 13;

	// -------------------- MEMBERS --------------------

	private final DraculaCalibrator calibrator;

	// -------------------- MEMBERS --------------------

	private final Map<DraculaLink, Double> minTravelTime_s;

	private final DynamicData<DraculaLink> travelTimeSum_s;

	private final DynamicData<DraculaLink> entryCount;

	// -------------------- CONSTRUCTOR --------------------

	DraculaTravelTimes(final String travelTimeFileName,
			final DraculaCalibrator calibrator) {

		/*
		 * (1) set parameters
		 */
		if (travelTimeFileName == null) {
			throw new IllegalArgumentException("travel time file name is null");
		}
		if (calibrator == null) {
			throw new IllegalArgumentException("calibrator is null");
		}
		if (calibrator.getRoutes() == null) {
			throw new IllegalArgumentException("no routes in calibrator");
		}
		this.calibrator = calibrator;
		final int binCnt = (int) Math.ceil(24.0 * 3600.0 / calibrator
				.getTimeBinSize_s());
		this.travelTimeSum_s = new DynamicData<DraculaLink>(0, calibrator
				.getTimeBinSize_s(), binCnt);
		this.entryCount = new DynamicData<DraculaLink>(0, calibrator
				.getTimeBinSize_s(), binCnt);

		/*
		 * obtain reference to minimum travel times
		 */
		this.minTravelTime_s = calibrator.getMinTravelTimes();

		/*
		 * (2) load data
		 */
		final TabularFileParser parser = new TabularFileParser();
		parser.setStartTag(START_TAG);
		parser.setEndTag(END_TAG);
		parser.setDelimiterRegex("\\s");
		parser.setMinRowLength(FIRST_TT_INDEX + 1);
		try {
			parser.parse(travelTimeFileName, this);
		} catch (IOException e) {
			e.printStackTrace();
		}

		/*
		 * (3) do flow bookkeeping
		 */
		if (calibrator.getFlowPrefix() != null) {
			final DynamicDataXMLFileIO<DraculaLink> flowWriter = new DynamicDataXMLFileIO<DraculaLink>() {

				private static final long serialVersionUID = 1L;

				@Override
				protected String key2attrValue(final DraculaLink key) {
					return Integer.toString(key.getIndex());
				}

				@Override
				protected DraculaLink attrValue2key(final String string) {
					throw new UnsupportedOperationException();
				}
			};
			final String flowFileName = calibrator.getFlowPrefix()
					+ calibrator.getIteration() + ".xml";
			try {
				flowWriter.write(flowFileName, this.entryCount);
			} catch (IOException e) {
				Logger.getLogger(this.getClass().getName()).warning(
						"could not write flow file: " + flowFileName);
			}
		}
	}

	// -------------------- SETTERS AND GETTERS --------------------

	private void updateTT_s(final DraculaLink link, final int time_s,
			final double tt_s) {
		/*
		 * (1) update minimum travel time (to incrementally approximate the free
		 * flow travel time without parsing the network file)
		 */
		final Double oldMinTT_s = this.minTravelTime_s.get(link);
		if (oldMinTT_s == null || tt_s < oldMinTT_s) {
			this.minTravelTime_s.put(link, tt_s);
		}
		/*
		 * (2) update average travel time
		 */
		final int bin = Math.max(0, Math.min(this.travelTimeSum_s.bin(time_s),
				this.travelTimeSum_s.getBinCnt() - 1));
		this.travelTimeSum_s.add(link, bin, tt_s);
		this.entryCount.add(link, bin, 1.0);
	}

	double getTT_s(final DraculaLink link, final int time_s) {
		final int bin = Math.max(0, Math.min(this.entryCount.bin(time_s),
				this.entryCount.getBinCnt() - 1));
		final double count = this.entryCount.getBinValue(link, bin);
		if (count > 0) {
			return (this.travelTimeSum_s.getBinValue(link, bin) / count);
		} else {
			final Double minTT_s = this.minTravelTime_s.get(link);
			return (minTT_s != null ? minTT_s : 0.0);
		}
	}

	// --------------- IMPLEMENTATION OF TabularFileHandler ---------------

	@Override
	public void startDocument() {
	}

	@Override
	public void startRow(String[] row) {
		/*
		 * (1) extract numbers from file
		 */
		final Long routeId = Long.parseLong(row[ROUTE_ID_INDEX]);
		final double dptTime_s = Double.parseDouble(row[DEPARTURE_TIME_INDEX]);
		final double arrTime_s = Double.parseDouble(row[ARRIVAL_TIME_INDEX]);
		final int linkCnt = Integer.parseInt(row[LINK_COUNT_INDEX]);
		final List<Double> linkTTList_s = new ArrayList<Double>(linkCnt);
		for (int i = FIRST_TT_INDEX; i < FIRST_TT_INDEX + 2 * linkCnt; i += 2) {
			linkTTList_s.add(Double.parseDouble(row[i]));
		}
		/*
		 * (2) check data consistency
		 */
		final DraculaRoute route = this.calibrator.getRoutes()
				.getRoute(routeId);
		if (route == null) {
			Logger.getLogger(this.getClass().getName()).warning(
					"unknown route " + routeId + " -- ignoring trip");
			return;
		} else if (route.getLinks().size() != linkCnt) {
			Logger.getLogger(this.getClass().getName()).warning(
					"inconsistent length of route " + routeId
							+ " -- some data may be lost");
		}
		/*
		 * (3) update average travel times and flows
		 */
		double totalTT_s = 0;
		for (int i = 0; i < Math.min(route.getLinks().size(), linkCnt); i++) {
			final DraculaLink link = route.getLinks().get(i);
			final double tt_s = linkTTList_s.get(i);
			this.updateTT_s(link, round(dptTime_s + totalTT_s), tt_s);
			totalTT_s += tt_s;
		}
		/*
		 * (4) check consistency of route travel time
		 */
		final double err = (dptTime_s + totalTT_s) - arrTime_s;
		if (Math.abs(err) > 1.0) {
			Logger.getLogger(this.getClass().getName()).warning(
					"travel time error of " + err + " s on route " + routeId);
		}
	}

	@Override
	public void endDocument() {
		if (this.calibrator.getTravelTimePrefix() != null) {

			final DynamicData<DraculaLink> tts = new DynamicData<DraculaLink>(
					this.entryCount.getStartTime_s(), this.entryCount
							.getBinSize_s(), this.entryCount.getBinCnt());
			for (DraculaLink link : this.minTravelTime_s.keySet()) {
				for (int bin = 0; bin < tts.getBinCnt(); bin++) {
					tts.put(link, bin, this.getTT_s(link, tts.binStart_s(bin)));
				}
			}

			final DynamicDataXMLFileIO<DraculaLink> writer = new DynamicDataXMLFileIO<DraculaLink>() {

				private static final long serialVersionUID = 1L;

				@Override
				protected String key2attrValue(final DraculaLink key) {
					return Integer.toString(key.getIndex());
				}

				@Override
				protected DraculaLink attrValue2key(final String string) {
					throw new UnsupportedOperationException();
				}
			};
			final String ttFileName = calibrator.getTravelTimePrefix()
					+ calibrator.getIteration() + ".xml";
			try {
				writer.write(ttFileName, tts);
			} catch (IOException e) {
				Logger.getLogger(this.getClass().getName()).warning(
						"could not write travel time file: " + ttFileName);
			}
		}
	}

	// -------------------- IMPLEMENTATION OF SimResults --------------------

	@Override
	public double getSimValue(DraculaLink link, int startTime_s, int endTime_s,
			TYPE type) {

		if (!TYPE.FLOW_VEH_H.equals(type)) {
			throw new IllegalArgumentException(
					"current implementation only allows for data type "
							+ TYPE.FLOW_VEH_H);
		}

		final double dur_h = Units.H_PER_S * (endTime_s - startTime_s);
		return (this.entryCount.getSum(link, startTime_s, endTime_s) / dur_h);
	}
}
