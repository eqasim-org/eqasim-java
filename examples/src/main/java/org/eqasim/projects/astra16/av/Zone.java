package org.eqasim.projects.astra16.av;

import java.util.Collection;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class Zone {
	private final static GeometryFactory geometryFactory = new GeometryFactory();

	private final int index;
	private final Collection<Id<Link>> coveredLinkIds;
	private final Geometry geometry;

	public Zone(int index, Collection<Id<Link>> coveredLinkIds, Geometry geometry) {
		this.index = index;
		this.coveredLinkIds = coveredLinkIds;
		this.geometry = geometry;
	}

	public boolean covers(Link link) {
		return covers(link.getCoord());
	}

	public boolean covers(Coord coord) {
		return geometry.covers(geometryFactory.createPoint(new Coordinate(coord.getX(), coord.getY())));
	}

	public Collection<Id<Link>> getLinkIds() {
		return coveredLinkIds;
	}

	public int getIndex() {
		return index;
	}

	public Geometry getGeometry() {
		return geometry;
	}
}
