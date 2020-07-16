package org.eqasim.core.analysis.od_routing.data;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.opengis.feature.simple.SimpleFeature;

public class LocationReader {
	private final String identifierAttribute;

	public LocationReader(String identifierAttribute) {
		this.identifierAttribute = identifierAttribute;
	}

	public Collection<Location> readFile(File path) throws MalformedURLException, IOException {
		return readFile(path.toURI().toURL());
	}

	public Collection<Location> readFile(URL url) throws MalformedURLException, IOException {
		DataStore dataStore = DataStoreFinder.getDataStore(Collections.singletonMap("url", url));
		SimpleFeatureSource featureSource = dataStore.getFeatureSource(dataStore.getTypeNames()[0]);
		SimpleFeatureCollection featureCollection = featureSource.getFeatures();
		SimpleFeatureIterator featureIterator = featureCollection.features();

		List<Location> locations = new LinkedList<>();

		while (featureIterator.hasNext()) {
			SimpleFeature feature = featureIterator.next();
			Geometry geometry = (Geometry) feature.getDefaultGeometry();

			String id = (String) feature.getAttribute(identifierAttribute);
			Point centroid = geometry.getCentroid();

			if (id == null) {
				throw new IllegalStateException(
						String.format("No attribute '%s' found in %s", identifierAttribute, url));
			}

			locations.add(new Location(Id.create(id, Location.class),
					new Coord(centroid.getX(), centroid.getY())));
		}

		featureIterator.close();
		dataStore.dispose();

		if (locations.size() == 0) {
			throw new IllegalStateException("Did not find any locations in " + url);
		}

		return locations;
	}
}
