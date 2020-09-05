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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import cadyts.calibrators.filebased.xml.XMLPopulationFileReader;
import cadyts.demand.ODRelation;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
class SumoAgentCloner {

	// -------------------- MEMBERS --------------------

	private final Map<ODRelation<String>, List<Object>> od2agentIDs;

	private final Set<Object> cloneAgentIDs;

	// -------------------- CONSTRUCTION --------------------

	SumoAgentCloner() {
		this.od2agentIDs = new LinkedHashMap<ODRelation<String>, List<Object>>();
		this.cloneAgentIDs = new LinkedHashSet<Object>();
	}

	// -------------------- IMPLEMENTATION --------------------

	Set<Object> getClonedAgentIDs() {
		return this.cloneAgentIDs;
	}

	void run(final String[] fileNames, final double demandScale) {

		if (fileNames == null) {
			throw new IllegalArgumentException("file name is null");
		}
		if (demandScale < 1 || demandScale > 2) {
			throw new IllegalArgumentException("demand scale of " + demandScale
					+ " is not in [1,2]");
		}

		/*
		 * (1) analyze the population
		 */
		this.od2agentIDs.clear();
		for (String fileName : fileNames) {
			if (fileName != null && fileName.length() > 0) {
				Logger.getLogger(this.getClass().getName()).info(
						"cloning file: " + fileName);
				final XMLPopulationFileReader<SumoAgent> reader = new XMLPopulationFileReader<SumoAgent>();
				final SumoPopulationHandler handler = new SumoPopulationHandler(
						null, 1.0);
				reader.setPopulationHandler(handler);
				for (SumoAgent agent : reader.getPopulationSource(fileName)) {
					List<Object> agentIDs = this.od2agentIDs.get(agent
							.getODRelation());
					if (agentIDs == null) {
						agentIDs = new ArrayList<Object>();
						this.od2agentIDs.put(agent.getODRelation(), agentIDs);
					}
					agentIDs.add(agent.getId());
				}
			}
		}

		/*
		 * (2) define agents to be cloned
		 */
		this.cloneAgentIDs.clear();
		final double m = 1.0 / (demandScale - 1.0); // clone every m-th agent
		int totalCnt = 0;
		for (Map.Entry<ODRelation<String>, List<Object>> entry : this.od2agentIDs
				.entrySet()) {
			double cnt = 0;
			for (Object agentID : entry.getValue()) {
				totalCnt++;
				cnt++;
				if (cnt >= m) {
					this.cloneAgentIDs.add(agentID);
					cnt -= m;
				}
			}
		}

		Logger.getLogger(this.getClass().getName()).info(
				"cloned " + this.cloneAgentIDs.size() + " out of " + totalCnt
						+ " agents");
	}

	// ----------------- MAIN-FUNCTION, ONLY FOR TESTING -----------------
	//
	// public static void main(String[] args) {
	// SumoAgentCloner g = new SumoAgentCloner();
	// g.run(new String[] { "testdata/sumo/choiceset.xml",
	// "testdata/sumo/emptyChoiceSet.xml" }, 1.333);
	// System.out.println("DONE");
	// }
}
