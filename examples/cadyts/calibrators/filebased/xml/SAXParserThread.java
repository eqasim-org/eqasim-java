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

import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.helpers.DefaultHandler;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
class SAXParserThread extends Thread {

	// -------------------- SINGLETON ACCESS --------------------

	private static SAXParserThread instance = null;

	static synchronized SAXParserThread newInstance(final String fileName,
			final DefaultHandler handler) {
		/*
		 * (1) make sure that the previous thread is done
		 */
		if (instance != null && instance.isAlive()) {
			try {
				Logger.getLogger(SAXParserThread.class.getName()).fine(
						"waiting for previous parser thread to terminate");
				final long waitStart_ms = System.currentTimeMillis();
				instance.join();
				Logger.getLogger(SAXParserThread.class.getName()).fine(
						"previous parser thread terminated after "
								+ (System.currentTimeMillis() - waitStart_ms)
								+ " ms");
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		/*
		 * (2) only now create a new thread
		 */
		instance = new SAXParserThread(fileName, handler);
		return instance;
	}

	// -------------------- CONSTANTS AND MEMBERS --------------------

	private final String fileName;

	private final DefaultHandler handler;

	// -------------------- CONSTRUCTION --------------------

	private SAXParserThread(final String fileName, final DefaultHandler handler) {

		// CHECK

		if (fileName == null) {
			throw new IllegalArgumentException("fileName must not be null");
		}
		if (handler == null) {
			throw new IllegalArgumentException("handler must not be null");
		}

		// CONTINUE

		this.fileName = fileName;
		this.handler = handler;
	}

	// -------------------- OVERRIDING OF Thread --------------------

	@Override
	public void run() {
		try {
			final SAXParserFactory factory = SAXParserFactory.newInstance();
			final SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(this.fileName, this.handler);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
