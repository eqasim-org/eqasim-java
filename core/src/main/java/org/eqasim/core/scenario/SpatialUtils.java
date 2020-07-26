package org.eqasim.core.scenario;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;

public class SpatialUtils {
	static public Polygon loadPolygon(URL url, String attribute, String value) throws IOException {
		DataStore dataStore = DataStoreFinder.getDataStore(Collections.singletonMap("url", url));
		SimpleFeatureSource featureSource = dataStore.getFeatureSource(dataStore.getTypeNames()[0]);
		SimpleFeatureCollection featureCollection = featureSource.getFeatures();
		SimpleFeatureIterator featureIterator = featureCollection.features();

		List<Polygon> polygons = new LinkedList<>();

		while (featureIterator.hasNext()) {
			SimpleFeature feature = featureIterator.next();
			Geometry geometry = (Geometry) feature.getDefaultGeometry();

			if (feature.getAttribute(attribute) != null
					&& value.equals(String.valueOf(feature.getAttribute(attribute)))) {
				if (geometry instanceof MultiPolygon) {
					MultiPolygon multiPolygon = (MultiPolygon) geometry;

					if (multiPolygon.getNumGeometries() != 1) {
						throw new IllegalStateException("Shape is non-connected.");
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
			throw new IllegalStateException("Found more than one polygon that match to the criteron.");
		}

		if (polygons.size() == 0) {
			throw new IllegalStateException("Did not find any matching polygon.");
		}

		return polygons.get(0);
	}
}
