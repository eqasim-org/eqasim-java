package org.eqasim.ile_de_france.analysis.counts;

import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;

public class DailyCounts {
	private final IdMap<Link, Double> counts = new IdMap<>(Link.class);

	public DailyCounts(IdMap<Link, Double> counts) {
		this.counts.putAll(counts);
	}

	public IdMap<Link, Double> getCounts() {
		return counts;
	}
}
