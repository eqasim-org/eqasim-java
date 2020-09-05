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

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import cadyts.demand.ODRelation;
import cadyts.utilities.math.Matrix;
import cadyts.utilities.misc.DynamicData;

/**
 * 
 * Writes time-dependent OD matrices as a sequence of visum OD matrix files.
 * 
 * @author Gunnar Flötteröd
 * 
 */
class SumoODWriter {

	// -------------------- CONSTRUCTION --------------------

	SumoODWriter() {
	}

	// -------------------- IMPLEMENTATION --------------------

	private List<String> extractZones(final DynamicData<ODRelation<String>> od) {
		final SortedSet<String> zones = new TreeSet<String>();
		for (ODRelation<String> key : od.keySet()) {
			zones.add(key.getFromTAZ());
			zones.add(key.getToTAZ());
		}
		return new ArrayList<String>(zones);
	}

	private Matrix tripMatrix(final DynamicData<ODRelation<String>> od,
			final int bin, final List<String> zones) {
		final Matrix result = new Matrix(zones.size(), zones.size());
		for (int i = 0; i < zones.size(); i++) {
			for (int j = 0; j < zones.size(); j++) {
				final ODRelation<String> key = new ODRelation<String>(zones
						.get(i), zones.get(j));
				result.getRow(i).set(j, od.getBinValue(key, bin));
			}
		}
		return result;
	}

	void write(final DynamicData<ODRelation<String>> od, final String filePrefix)
			throws FileNotFoundException {

		final List<String> zones = this.extractZones(od);
		
		for (int bin = 0; bin < od.getBinCnt(); bin++) {

			final Matrix tripMatrix = this.tripMatrix(od, bin, zones);
			if (tripMatrix.frobeniusNorm() > 0) {

				final int h_start = od.binStart_s(bin) / 3600;
				final int m_start = (od.binStart_s(bin) - h_start * 3600) / 60;
				final int h_end = od.binStart_s(bin + 1) / 3600;
				final int m_end = (od.binStart_s(bin + 1) - h_end * 3600) / 60;

				final PrintWriter writer = new PrintWriter(filePrefix + "_"
						+ h_start + "h" + m_start + "m-" + h_end + "h" + m_end
						+ "m.fma");
				writer.println("$VMR");
				writer.println("* vehicle type");
				writer.println("0");
				writer.println("* from-time to-time");
				writer.println(h_start + "." + m_start + " " + h_end + "."
						+ m_end);
				writer.println("* factor");
				writer.println("1.0");
				writer.println("* district number");
				writer.println(zones.size());
				writer.println("* names");
				for (String zone : zones) {
					writer.print("\t" + zone);
				}
				writer.println();
				writer.println("*");
				for (int i = 0; i < zones.size(); i++) {
					writer.println("* district " + zones.get(i) + " sum = "
							+ tripMatrix.getRow(i).sum());
					for (int j = 0; j < zones.size(); j++) {
						writer.print("\t" + tripMatrix.getRow(i).get(j));
					}
					writer.println();
				}
				writer.flush();
				writer.close();
			}
		}
	}
}
