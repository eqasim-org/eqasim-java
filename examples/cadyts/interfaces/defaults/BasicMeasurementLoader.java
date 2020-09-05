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
package cadyts.interfaces.defaults;

import static cadyts.utilities.misc.Time.secFromStr;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;

import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import cadyts.calibrators.Calibrator;
import cadyts.measurements.MultiLinkMeasurement;
import cadyts.measurements.SingleLinkMeasurement;
import cadyts.utilities.misc.Time;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public abstract class BasicMeasurementLoader<L> extends DefaultHandler {

	// -------------------- CONSTANTS --------------------

	private static final String SINGLE_LINK_ELEMENT = "singlelink";

	private static final String LINK_ATTRIBUTE = "link";

	private static final String START_TIME_ATTRIBUTE = "start";

	private static final String END_TIME_ATTRIBUTE = "end";

	private static final String VALUE_ATTRIBUTE = "value";

	private static final String STDDEV_ATTRIBUTE = "stddev";

	private static final String TYPE_ATTRIBUTE = "type";

	private static final String MULTI_LINK_ELEMENT = "multilink";

	private static final String DETECTIONRATE_ATTRIBUTE = "detectionrate";

	private static final String OBSERVATION_ELEMENT = "observation";

	// -------------------- MEMBERS --------------------

	private final Calibrator<L> calibrator;

	private MultiLinkMeasurement<L> p2pCrispMeas = null;

	// -------------------- CONSTRUCTION --------------------

	public BasicMeasurementLoader(final Calibrator<L> calibrator) {
		if (calibrator == null) {
			throw new IllegalArgumentException("calibrator is null");
		}
		this.calibrator = calibrator;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void load(final String... measurementFileNames) {

		if (measurementFileNames == null || measurementFileNames.length == 0) {
			Logger.getLogger(this.getClass().getName()).info(
					"no measurement files specified");
			return;
		}

		for (String fileName : measurementFileNames) {
			try {
				final SAXParserFactory factory = SAXParserFactory.newInstance();
				final SAXParser saxParser = factory.newSAXParser();
				saxParser.parse(fileName, this);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	// --------------- OVERRIDING OF DefaultHandler ---------------

	@Override
	public void startElement(String namespaceURI, String sName, String qName,
			Attributes attrs) {
		if (SINGLE_LINK_ELEMENT.equals(qName)) {
			final SingleLinkMeasurement.TYPE type = SingleLinkMeasurement.TYPE
					.valueOf(attrs.getValue(TYPE_ATTRIBUTE));
			if (SingleLinkMeasurement.TYPE.FLOW_VEH_H.equals(type)
					|| SingleLinkMeasurement.TYPE.COUNT_VEH.equals(type)) {
				final L link = this.label2link(attrs.getValue(LINK_ATTRIBUTE));
				final double value = Double.parseDouble(attrs
						.getValue(VALUE_ATTRIBUTE));
				final int start_s = Time.secFromStr(attrs
						.getValue(START_TIME_ATTRIBUTE));
				final int end_s = Time.secFromStr(attrs
						.getValue(END_TIME_ATTRIBUTE));
				final String stddevString = attrs.getValue(STDDEV_ATTRIBUTE);
				if (stddevString == null) {
					this.calibrator.addMeasurement(link, start_s, end_s, value,
							type);
				} else {
					final double stddev = Double.parseDouble(stddevString);
					this.calibrator.addMeasurement(link, start_s, end_s, value,
							stddev, type);
				}
			} else {
				Logger.getLogger(this.getClass().getName()).warning(
						"skipping \"onlink\" measurement type: " + type);
			}
		} else if (MULTI_LINK_ELEMENT.equals(qName)) {
			final int value = parseInt(attrs.getValue(VALUE_ATTRIBUTE));
			final double detectionRate = parseDouble(attrs
					.getValue(DETECTIONRATE_ATTRIBUTE));
			this.p2pCrispMeas = new MultiLinkMeasurement<L>(value,
					detectionRate);
		} else if (OBSERVATION_ELEMENT.equals(qName)) {
			final L link = this.label2link(attrs.getValue(LINK_ATTRIBUTE));
			final int start_s = secFromStr(attrs.getValue(START_TIME_ATTRIBUTE));
			final int end_s = secFromStr(attrs.getValue(END_TIME_ATTRIBUTE));
			this.p2pCrispMeas.addObservation(link, start_s, end_s);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		if (MULTI_LINK_ELEMENT.equals(qName)) {
			this.calibrator.addMeasurement(this.p2pCrispMeas);
			this.p2pCrispMeas = null;
		}
	}

	// -------------------- INTERFACE DEFINITION --------------------

	protected abstract L label2link(final String label);

}
