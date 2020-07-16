package org.eqasim.core.analysis.od_routing.data;

import java.util.Collection;
import java.util.Iterator;

public class OriginDestinationIterator implements Iterator<OriginDestinationPair> {
	private final Collection<Location> locations;

	private final Iterator<Location> originIterator;
	private Iterator<Location> destinationIterator;

	private Location origin;

	public OriginDestinationIterator(Collection<Location> locations) {
		this.locations = locations;

		this.originIterator = locations.iterator();
		this.destinationIterator = locations.iterator();
	}

	@Override
	public boolean hasNext() {
		return (locations.size() > 0) && (originIterator.hasNext() || destinationIterator.hasNext());
	}

	@Override
	public OriginDestinationPair next() {
		if (origin == null) {
			// Initialize origin
			origin = originIterator.next();
		}

		if (!destinationIterator.hasNext()) {
			origin = originIterator.next();
			destinationIterator = locations.iterator();
		}

		return new OriginDestinationPair(origin, destinationIterator.next());
	}
}
