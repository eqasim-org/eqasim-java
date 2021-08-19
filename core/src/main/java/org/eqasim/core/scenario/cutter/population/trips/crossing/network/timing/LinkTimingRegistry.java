package org.eqasim.core.scenario.cutter.population.trips.crossing.network.timing;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

public class LinkTimingRegistry {
	private final IdMap<Person, Map<Id<Link>, List<LinkTimingData>>> data = new IdMap<>(Person.class);

	public void register(Id<Person> personId, int legIndex, Id<Link> linkId, double enterTime, double leaveTime) {
		data.computeIfAbsent(personId, key -> new HashMap<>()).computeIfAbsent(linkId, key -> new LinkedList<>())
				.add(new LinkTimingData(enterTime, leaveTime, legIndex));
	}

	public Optional<LinkTimingData> getTimingData(Id<Person> personId, int legIndex, Id<Link> linkId) {
		if (data.isEmpty()) {
			return Optional.empty();
		}

		Map<Id<Link>, List<LinkTimingData>> personData = data.get(personId);

		if (personData == null) {
			return Optional.empty();
		}

		List<LinkTimingData> linkData = personData.get(linkId);

		if (linkData == null) {
			return Optional.empty();
		}

		Iterator<LinkTimingData> iterator = linkData.iterator();

		while (iterator.hasNext()) {
			LinkTimingData legData = iterator.next();

			if (legData.legIndex == legIndex) {
				return Optional.of(legData);
			} else if (legData.legIndex > legIndex) {
				break;
			}
		}

		return Optional.empty();
	}
}
