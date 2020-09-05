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

import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import cadyts.measurements.SingleLinkMeasurement;
import cadyts.supply.BasicSimResults;
import cadyts.utilities.misc.DynamicData;
import cadyts.utilities.misc.Units;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
class SumoFlowLoader extends DefaultHandler {

	// -------------------- CONSTANTS --------------------

	static final String INTERVAL_ELEM = "interval";

	static final String START_ATTR = "begin";

	static final String END_ATTR = "end";

	static final String EDGE_ELEM = "edge";

	static final String ID_ATTR = "id";

	static final String ENTERED_ATTR = "entered";

	static final String TT_ATTR = "traveltime";

	// -------------------- MEMBERS --------------------

	private final BasicSimResults<String> result;

	private final DynamicData<String> flowResult;

	private final DynamicData<String> countResult;

	private final DynamicData<String> ttResult;

	private int start_s = 0;

	private int end_s = 0;

	// -------------------- CONSTRUCTION --------------------

	SumoFlowLoader(final int binSize_s) {
		this.result = new BasicSimResults<String>(0, binSize_s,
				((int) Units.S_PER_D) / binSize_s);
		this.flowResult = this.result
				.getSimResults(SingleLinkMeasurement.TYPE.FLOW_VEH_H);
		this.countResult = this.result
				.getSimResults(SingleLinkMeasurement.TYPE.COUNT_VEH);
		this.ttResult = new DynamicData<String>(this.countResult
				.getStartTime_s(), this.countResult.getBinSize_s(),
				this.countResult.getBinCnt());
	}

	// -------------------- IMPLEMENTATION --------------------

	void load(final String fileName) {
		try {
			final SAXParserFactory factory = SAXParserFactory.newInstance();
			final SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(fileName, this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	BasicSimResults<String> getResults() {
		return this.result;
	}

	DynamicData<String> getTravelTimes() {
		return this.ttResult;
	}

	// -------------------- INTERNALS --------------------

	private void checkTimes(final int start_s, final int end_s,
			final int binSize_s, final int bin) {
		if (start_s != bin * binSize_s) {
			Logger.getLogger(this.getClass().getName()).warning(
					"flow start time " + start_s
							+ " s is not the beginning of a " + binSize_s
							+ " s time bin; corrected into "
							+ (bin * binSize_s) + " s");
		}
		if (end_s != (bin + 1) * binSize_s) {
			Logger.getLogger(this.getClass().getName()).warning(
					"flow end time " + end_s + " s is not " + binSize_s
							+ " s larger than (proper) start time; "
							+ "corrected into " + ((bin + 1) * binSize_s)
							+ " s");
		}
	}

	public void startFlow(final double flow_veh, final int start_s,
			final int end_s, final String link) {
		final int binSize_s = this.flowResult.getBinSize_s();
		// TODO bin(..) does not check for bounds
		final int bin = this.flowResult.bin(start_s);
		this.checkTimes(start_s, end_s, binSize_s, bin);

		this.countResult.put(link, bin, flow_veh);
		final double flow_veh_h = flow_veh / (end_s - start_s) * 3600.0;
		this.flowResult.put(link, bin, flow_veh_h);
	}

	public void startTT(double tt_s, int start_s, int end_s, String link) {
		final int binSize_s = this.flowResult.getBinSize_s();
		// TODO bin(..) does not check for bounds
		final int bin = this.flowResult.bin(start_s);
		this.checkTimes(start_s, end_s, binSize_s, bin);
		this.ttResult.put(link, bin, tt_s);
	}

	// -------------------- OVERRIDING OF DefaultHandler --------------------

	@Override
	public void startElement(String namespaceURI, String sName, String qName,
			Attributes attrs) {
		if (INTERVAL_ELEM.equals(qName)) {
			this.start_s = (int) Math.round(Double.parseDouble(attrs
					.getValue(START_ATTR)));
			this.end_s = (int) Math.round(Double.parseDouble(attrs
					.getValue(END_ATTR)));
			if (this.start_s < 0) {
				Logger.getLogger(this.getClass().getName()).warning(
						"start time is negative; corrected");
				this.start_s = 0;
			}
			if (this.end_s > Units.S_PER_D) {
				Logger.getLogger(this.getClass().getName()).warning(
						"end time goes beyond one day; corrected");
				this.end_s = (int) Units.S_PER_D;
			}
			if (this.start_s >= this.end_s) {
				Logger.getLogger(this.getClass().getName()).warning(
						"meaningless time interval [" + start_s + ", " + end_s
								+ ") s; ignored");
			}
		} else if (EDGE_ELEM.equals(qName) && (this.start_s < this.end_s)) {

			final String linkId = attrs.getValue(ID_ATTR);
			final double flow_veh = Integer.parseInt(attrs
					.getValue(ENTERED_ATTR));
			this.startFlow(flow_veh, this.start_s, this.end_s, linkId);

			final String ttString = attrs.getValue(TT_ATTR);
			if (ttString != null) {
				final double tt_s = Double.parseDouble(ttString);
				this.startTT(tt_s, this.start_s, this.end_s, linkId);
			}
		}
	}
}
