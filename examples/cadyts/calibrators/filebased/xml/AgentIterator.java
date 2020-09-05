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

import java.util.Iterator;

import cadyts.calibrators.filebased.Agent;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <A>
 *            the agent type
 */
class AgentIterator<A extends Agent<?, ?>> implements Iterator<A> {

	// -------------------- MEMBER VARIABLES --------------------

	private final PopulationHandler<A> populationHandler;

	private A bufferedAgent = null;

	// -------------------- CONSTRUCTION --------------------

	AgentIterator(final PopulationHandler<A> populationHandler) {

		if (populationHandler == null) {
			throw new IllegalArgumentException(
					"populationHandler must not be null");
		}

		this.populationHandler = populationHandler;
		this.bufferedAgent = this.populationHandler.getNextAgent();
	}

	// -------------------- IMPLEMENTATION OF Iterator --------------------

	@Override
	public boolean hasNext() {
		return (this.bufferedAgent != null);
	}

	@Override
	public A next() {
		final A result = this.bufferedAgent;
		this.bufferedAgent = this.populationHandler.getNextAgent();
		return result;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
