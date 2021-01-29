package org.eqasim.ile_de_france.flow.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;

public class FlowHandler implements LinkLeaveEventHandler {
	private final IdMap<Link, List<Integer>> counts = new IdMap<>(Link.class);

	public FlowHandler(IdSet<Link> linkIds) {
		for (Id<Link> linkId : linkIds) {
			counts.put(linkId, new ArrayList<>(Collections.nCopies(24, 0)));
		}
	}

	public IdMap<Link, List<Integer>> getCounts() {
		return counts;
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		List<Integer> linkCounts = counts.get(event.getLinkId());

		if (linkCounts != null) {
			int hour = (int) Math.floor(event.getTime() / 3600.0);

			while (hour >= 24) {
				hour -= 24;
			}

			linkCounts.set(hour, linkCounts.get(hour) + 1);
		}
	}
}
