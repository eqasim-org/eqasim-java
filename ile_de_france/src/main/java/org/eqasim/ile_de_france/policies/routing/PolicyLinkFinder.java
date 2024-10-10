package org.eqasim.ile_de_france.policies.routing;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.geotools.api.data.SimpleFeatureReader;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class PolicyLinkFinder {
	private final static GeometryFactory geometryFactory = new GeometryFactory();

	private final List<Geometry> shapes;

	public PolicyLinkFinder(List<Geometry> shapes) {
		this.shapes = shapes;
	}

	public enum Predicate {
		Entering, Exiting, Crossing, Inside
	}

	public IdSet<Link> findLinks(Network network, Predicate predicate) {
		IdSet<Link> linkIds = new IdSet<>(Link.class);

		for (Link link : network.getLinks().values()) {
			for (Geometry shape : shapes) {
				Coord fromCoord = link.getFromNode().getCoord();
				Coord toCoord = link.getToNode().getCoord();

				Point fromPoint = geometryFactory.createPoint(new Coordinate(fromCoord.getX(), fromCoord.getY()));
				Point toPoint = geometryFactory.createPoint(new Coordinate(toCoord.getX(), toCoord.getY()));

				boolean fromInside = shape.contains(fromPoint);
				boolean toInside = shape.contains(toPoint);

				boolean isRelvant = switch (predicate) {
				case Exiting -> fromInside && !toInside;
				case Entering -> !fromInside && toInside;
				case Crossing -> fromInside || toInside;
				case Inside -> fromInside && toInside;
				};

				if (isRelvant) {
					linkIds.add(link.getId());
				}
			}
		}

		return linkIds;
	}

	static public PolicyLinkFinder create(File path) {
		try {
			List<Geometry> shapes = new LinkedList<>();

			try (GeoPackage source = new GeoPackage(path)) {
				source.init();

				for (FeatureEntry featureEntry : source.features()) {
					try (SimpleFeatureReader reader = source.reader(featureEntry, null, null)) {
						while (reader.hasNext()) {
							SimpleFeature feature = reader.next();
							shapes.add((Geometry) feature.getDefaultGeometry());
						}
					}
				}
			}

			return new PolicyLinkFinder(shapes);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
