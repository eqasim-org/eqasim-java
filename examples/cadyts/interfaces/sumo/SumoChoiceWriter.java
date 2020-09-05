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

import static cadyts.interfaces.sumo.SumoPopulationHandler.CHOICEPROB_ATTR;
import static cadyts.interfaces.sumo.SumoPopulationHandler.DEPART_ATTR;
import static cadyts.interfaces.sumo.SumoPopulationHandler.EDGES_ATTR;
import static cadyts.interfaces.sumo.SumoPopulationHandler.EXITTIMES_ATTR;
import static cadyts.interfaces.sumo.SumoPopulationHandler.FROMTAZ_ATTR;
import static cadyts.interfaces.sumo.SumoPopulationHandler.ROUTE_ELEM;
import static cadyts.interfaces.sumo.SumoPopulationHandler.TOTAZ_ATTR;
import static cadyts.interfaces.sumo.SumoPopulationHandler.VEHICLEID_ATTR;
import static cadyts.interfaces.sumo.SumoPopulationHandler.VEHICLE_ELEM;
import static cadyts.utilities.misc.XMLHelpers.writeAttr;

import java.io.IOException;
import java.io.PrintWriter;

import cadyts.calibrators.filebased.ChoiceFileWriter;
import cadyts.demand.PlanStep;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class SumoChoiceWriter implements ChoiceFileWriter<SumoAgent, SumoPlan> {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	static final String ROUTE_ALTERNATIVES = "route-alternatives";

	// -------------------- MEMBERS --------------------

	private transient PrintWriter writer = null;

	// -------------------- CONSTRUCTION --------------------

	SumoChoiceWriter() {
	}

	// --------------- IMPLEMENTATION OF ChoiceFileWriter ---------------

	@Override
	public void open(final String choiceFile) throws IOException {
		this.writer = new PrintWriter(choiceFile);

		this.writer.write("<");
		this.writer.write(ROUTE_ALTERNATIVES);
		this.writer.write(">");
		this.writer.println();
	}

	@Override
	public void write(final SumoAgent agent, final SumoPlan plan)
			throws IOException {

		if (plan.isStayAtHome()) {
			return;
		}

		this.writer.write("<");
		this.writer.write(VEHICLE_ELEM);
		this.writer.write(" ");
		writeAttr(VEHICLEID_ATTR, agent.getId(), this.writer);
		writeAttr(DEPART_ATTR, agent.getDptTime_s(), this.writer);
		if (agent.getODRelation() != null) {
			writeAttr(FROMTAZ_ATTR, agent.getODRelation().getFromTAZ(),
					this.writer);
			writeAttr(TOTAZ_ATTR, agent.getODRelation().getToTAZ(), this.writer);
		}
		this.writer.write(">");
		this.writer.println();

		final StringBuffer edges = new StringBuffer();
		final StringBuffer exits = new StringBuffer();

		String lastLink = plan.getStartLink();
		for (PlanStep<String> step : plan) {
			edges.append(lastLink);
			edges.append(" ");
			exits.append(step.getEntryTime_s());
			exits.append(" ");
			lastLink = step.getLink();
		}
		edges.append(lastLink);
		exits.append(plan.getExitTime_s());

		this.writer.write("  <");
		this.writer.write(ROUTE_ELEM);
		this.writer.write(" ");
		// writeAttr(ROUTEID_ATTR, plan.getRouteId(), this.writer);
		writeAttr(EDGES_ATTR, edges.toString(), this.writer);
		writeAttr(EXITTIMES_ATTR, exits.toString(), this.writer);
		writeAttr(CHOICEPROB_ATTR, "1", this.writer);
		this.writer.write("/>");
		this.writer.println();

		this.writer.write("</");
		this.writer.write(VEHICLE_ELEM);
		this.writer.write(">");
		this.writer.println();
	}

	@Override
	public void close() throws IOException {
		writer.write("</");
		writer.write(ROUTE_ALTERNATIVES);
		writer.write(">");
		this.writer.println();
		writer.flush();
		writer.close();
	}
}
