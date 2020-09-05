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

import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import cadyts.utilities.io.tabularFileParser.TabularFileHandler;
import cadyts.utilities.io.tabularFileParser.TabularFileParser;
import cadyts.utilities.misc.Tuple;

/**
 * 
 * Extracts all routes from the Dracula .DEM file.
 * 
 * @author Gunnar Flötteröd
 * 
 */
class DraculaRoutes implements TabularFileHandler, Serializable {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	private static final String START_TAG = "&ROUTES";

	private static final String END_TAG = "&PUB_SERVICES";

	private static final int ROUTE_ID_INDEX = 0;

	private static final int ORIGIN_ZONE_INDEX = 1;

	private static final int DESTINATION_ZONE_INDEX = 2;

	private final int LINK_CNT_INDEX; // depends on number of demand periods

	private final int FIRST_LINK_INDEX; // depends on number of demand periods

	// -------------------- MEMBERS --------------------

	private Map<Tuple<?, ?>, DraculaODRelation> zones2od = new LinkedHashMap<Tuple<?, ?>, DraculaODRelation>();

	private Map<Long, DraculaRoute> id2route = new LinkedHashMap<Long, DraculaRoute>();

	// -------------------- CONSTRUCTION --------------------

	DraculaRoutes(final String routeFileName, final DraculaCalibrator calibrator) {
		if (routeFileName == null) {
			throw new IllegalArgumentException("routeFileName is null");
		}
		if (calibrator == null) {
			throw new IllegalArgumentException("calibrator is null");
		}
		this.LINK_CNT_INDEX = 9 + (calibrator.getDemandPeriods() - 1);
		this.FIRST_LINK_INDEX = 10 + (calibrator.getDemandPeriods() - 1);

		final TabularFileParser parser = new TabularFileParser();
		parser.setStartTag(START_TAG);
		parser.setEndTag(END_TAG);
		parser.setDelimiterRegex("\\s");
		parser.setMinRowLength(FIRST_LINK_INDEX + 1);
		try {
			parser.parse(routeFileName, this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// -------------------- GETTERS AND SETTERS --------------------

	DraculaRoute getRoute(final Long id) {
		return this.id2route.get(id);
	}

	// --------------- IMPLEMENTATION OF TabularFileHandler ---------------

	@Override
	public void startDocument() {
	}
	
	@Override
	public void endDocument() {
	}
	
	@Override
	public void startRow(String[] row) {

		/*
		 * (1) extract zones and identify (or create) OD pair
		 */
		final Integer fromZone = Integer.parseInt(row[ORIGIN_ZONE_INDEX]);
		final Integer toZone = Integer.parseInt(row[DESTINATION_ZONE_INDEX]);
		final Tuple<?, ?> zones = new Tuple<Integer, Integer>(fromZone, toZone);

		final DraculaODRelation od;
		if (this.zones2od.containsKey(zones)) {
			od = this.zones2od.get(zones);
		} else {
			od = new DraculaODRelation(fromZone, toZone);
			this.zones2od.put(zones, od);
		}

		/*
		 * (2) extract route information and add route to OD pair
		 */
		final long routeId = Long.parseLong(row[ROUTE_ID_INDEX]);
		final DraculaRoute route = new DraculaRoute(routeId, od);

		final int linkCnt = Integer.parseInt(row[LINK_CNT_INDEX].replaceAll(
				"/", ""));
		for (int i = FIRST_LINK_INDEX; i < FIRST_LINK_INDEX + linkCnt; i++) {
			final int linkIndex = Integer.parseInt(row[i].replaceAll("/", ""));
			route.addLink(new DraculaLink(linkIndex));
		}
		route.trimToSize();
		this.id2route.put(route.getId(), route);
		od.addRoute(route);
	}
}
