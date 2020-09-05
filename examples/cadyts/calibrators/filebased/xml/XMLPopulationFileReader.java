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
package cadyts.calibrators.filebased.xml;

import cadyts.calibrators.filebased.Agent;
import cadyts.calibrators.filebased.PopulationFileReader;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <A>
 *            the agent class
 */
public class XMLPopulationFileReader<A extends Agent<?, ?>> implements
		PopulationFileReader<A> {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	// -------------------- MEMBERS --------------------

	private PopulationHandler<A> populationHandler = null;

	// -------------------- CONSTRUCTION --------------------

	public XMLPopulationFileReader() {
	}

	// -------------------- IMPLEMENTATION --------------------

	public void setPopulationHandler(
			final PopulationHandler<A> populationHandler) {
		this.populationHandler = populationHandler;
	}

	public PopulationHandler<A> getPopulationHandler() {
		return this.populationHandler;
	}

	// --------------- IMPLEMENATION OF PopulationFileReader ---------------

	@Override
	public Iterable<A> getPopulationSource(final String populationFile) {
		final SAXParserThread parserThread = SAXParserThread.newInstance(
				populationFile, this.populationHandler);
		parserThread.start();
		return this.populationHandler;
	}
}
