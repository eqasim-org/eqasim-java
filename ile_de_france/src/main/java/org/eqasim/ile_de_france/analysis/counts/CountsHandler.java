package org.eqasim.ile_de_france.analysis.counts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;

public class CountsHandler implements LinkLeaveEventHandler {
	private final Set<Id<Link>> linkIds;
	private final Map<Id<Link>, List<Integer>> counts = new HashMap<>();

	public CountsHandler(Set<Id<Link>> linkIds) {
		this.linkIds = linkIds;
		reset(0);
	}

	@Override
	public void reset(int iteration) {
		linkIds.forEach(id -> counts.put(id, new ArrayList<>(Collections.nCopies(24, 0))));
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		List<Integer> linkCounts = counts.get(event.getLinkId());

		if (linkCounts != null) {
			int hour = (int) Math.floor(event.getTime() / 3600.0);

			if (hour >= 24) {
				hour -= 24;
			}

			linkCounts.set(hour, linkCounts.get(hour) + 1);
		}
	}

	public Map<Id<Link>, List<Integer>> getCounts() {
		return counts;
	}
}