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

import cadyts.utilities.misc.DynamicDataXMLFileIO;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class DraculaODMatrixXMLFileIO extends
		DynamicDataXMLFileIO<DraculaODRelation> {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	private static final String TAZ_SEPARATOR = "_";

	// --------------- IMPLEMENTATION OF DynamicDataXMLFileIO ---------------

	@Override
	protected DraculaODRelation attrValue2key(final String string) {
		final String[] zones = string.split("\\Q" + TAZ_SEPARATOR + "\\E");
		final Integer from = Integer.parseInt(zones[0]);
		final Integer to = Integer.parseInt(zones[1]);
		return new DraculaODRelation(from, to);
	}

	@Override
	protected String key2attrValue(final DraculaODRelation key) {
		return key.getFromTAZ() + TAZ_SEPARATOR + key.getToTAZ();
	}
}
