package org.eqasim.core.scenario.cutter.extent;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.matsim.api.core.v01.Coord;
import org.opengis.feature.simple.SimpleFeature;

public class ShapeScenarioExtent implements ScenarioExtent {
	private final GeometryFactory factory = new GeometryFactory();
	private final Polygon polygon;

	public ShapeScenarioExtent(Polygon polygon) {
		this.polygon = polygon;
	}

	@Override
	public boolean isInside(Coord coord) {
		Coordinate coordinate = new Coordinate(coord.getX(), coord.getY());
		Point point = factory.createPoint(coordinate);
		return polygon.contains(point);
	}

	@Override
	public List<Coord> computeEuclideanIntersections(Coord from, Coord to) {
		if (from.equals(to)) {
			return Collections.emptyList();
		}

		Coordinate fromCoordinate = new Coordinate(from.getX(), from.getY());
		Coordinate toCoordinate = new Coordinate(to.getX(), to.getY());

		LineString line = factory.createLineString(new Coordinate[] { fromCoordinate, toCoordinate });
		Coordinate[] crossingCoordinates = polygon.getExteriorRing().intersection(line).getCoordinates();

		List<Coord> crossings = new ArrayList<>(crossingCoordinates.length);

		for (int i = 0; i < crossingCoordinates.length; i++) {
			Coordinate crossingCoordinate = crossingCoordinates[i];
			crossings.add(new Coord(crossingCoordinate.x, crossingCoordinate.y));
		}

		return crossings;
	}

	@Override
	public Coord getInteriorPoint() {
		Coordinate coordinate = polygon.getInteriorPoint().getCoordinate();
		return new Coord(coordinate.x, coordinate.y);
	}

	static public class Builder {
		private final File path;
		private final Optional<String> attribute;
		private final Optional<String> value;

		public Builder(File path, Optional<String> attribute, Optional<String> value) {
			this.path = path;
			this.attribute = attribute;
			this.value = value;
		}

		public ShapeScenarioExtent build() throws MalformedURLException, IOException {
			DataStore dataStore = DataStoreFinder.getDataStore(Collections.singletonMap("url", path.toURI().toURL()));
			SimpleFeatureSource featureSource = dataStore.getFeatureSource(dataStore.getTypeNames()[0]);
			SimpleFeatureCollection featureCollection = featureSource.getFeatures();
			SimpleFeatureIterator featureIterator = featureCollection.features();

			List<Polygon> polygons = new LinkedList<>();

			while (featureIterator.hasNext()) {
				SimpleFeature feature = featureIterator.next();
				Geometry geometry = (Geometry) feature.getDefaultGeometry();

				if (!attribute.isPresent() || value.get().equals(feature.getAttribute(attribute.get()))) {
					if (geometry instanceof MultiPolygon) {
						MultiPolygon multiPolygon = (MultiPolygon) geometry;

						if (multiPolygon.getNumGeometries() != 1) {
							throw new IllegalStateException("Extent shape is non-connected.");
						}

						polygons.add((Polygon) multiPolygon.getGeometryN(0));
					} else if (geometry instanceof Polygon) {
						polygons.add((Polygon) geometry);
					} else {
						throw new IllegalStateException("Expecting polygon geometry!");
					}
				}
			}

			featureIterator.close();
			dataStore.dispose();

			if (polygons.size() > 1) {
				throw new IllegalStateException("Found more than one polygon that match to the filter.");
			}

			if (polygons.size() == 0) {
				throw new IllegalStateException("Did not find scenario polygon.");
			}

			return new ShapeScenarioExtent(polygons.get(0));
		}
	}
}
