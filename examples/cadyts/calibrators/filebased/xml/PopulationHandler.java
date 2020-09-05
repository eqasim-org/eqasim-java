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
import java.util.logging.Logger;

import org.xml.sax.helpers.DefaultHandler;

import cadyts.calibrators.filebased.Agent;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <A>
 *            the agent type
 */
public class PopulationHandler<A extends Agent<?, ?>> extends DefaultHandler
		implements Iterable<A> {

	// -------------------- MEMBER VARIABLES --------------------

	private A bufferedAgent = null;

	private boolean noData = true;

	// -------------------- CONSTRUCTION --------------------

	public PopulationHandler() {
	}

	// -------------------- SYNCHRONIZED BUFFER UPDATE --------------------

	public synchronized A getNextAgent() {
		Logger.getLogger(this.getClass().getName()).fine(
				"waiting until there is a pending agent");
		while (this.noData) {
			try {
				wait();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		this.noData = true;
		notifyAll();
		Logger.getLogger(this.getClass().getName()).fine(
				"returning pending agent");
		return this.bufferedAgent;
	}

	protected synchronized void putNextAgent(final A agent) {
		Logger.getLogger(this.getClass().getName()).fine(
				"waiting until there is no pending agent (next agent is "
						+ agent + ")");
		while (!this.noData) {
			try {
				wait();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		this.bufferedAgent = agent;
		this.noData = false;
		notifyAll();

		if (agent != null) {
			Logger.getLogger(this.getClass().getName()).fine(
					"...received new pending agent");
		} else {
			Logger.getLogger(this.getClass().getName()).fine(
					"parser thread returns immediately because "
							+ "it wrote a null agent (end of file)");
		}
	}

	// -------------------- IMPLEMENTATION OF Iterable --------------------

	@Override
	public Iterator<A> iterator() {
		return new AgentIterator<A>(this);
	}

	// -------------------- OVERRIDING OF DefaultHandler --------------------

	/*
	 * Subclasses should override DefaultHandler with functions that use the
	 * data provided by the SAX parser to build instances of the agent type A.
	 * Once a complete agent is built, that agent should be registered by a call
	 * to putAgent(A). If there are no more agents, this should be indicated by
	 * a call to putAgent(null), like in the default implementation of
	 * endDocument() given below. This default implementation generates an
	 * Iterator over an empty population, independently of the contents of the
	 * xml file.
	 */

	@Override
	public void endDocument() {
		this.putNextAgent(null);
	}
}
