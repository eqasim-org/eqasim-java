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
package cadyts.utilities.io.tabularFileParser;

/**
 * An implementation of this interface is expected by the
 * <code>TabularFileParser</code> for row-by-row handling of parsed files.
 * 
 * @author gunnar
 * 
 */
public interface TabularFileHandler {

	public void startDocument();

	/**
	 * Is called by the <code>TabularFileParser</code> whenever a row has been
	 * parsed
	 * 
	 * @param row
	 *            a <code>String[]</code> representation of the parsed row's
	 *            columns
	 */
	public void startRow(String[] row);
	
	public void endDocument();
	
}
