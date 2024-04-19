package org.eqasim.core.simulation.analysis.stuck;

import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;

public class StuckAnalysisHandler implements PersonStuckEventHandler {
	private int count = 0;

	@Override
	public void handleEvent(PersonStuckEvent event) {
		count += 1;
	}

	@Override
	public void reset(int iteration) {
		this.count = 0;
	}

	public int getCount() {
		return count;
	}
}