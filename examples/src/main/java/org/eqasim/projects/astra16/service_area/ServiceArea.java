package org.eqasim.projects.astra16.service_area;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.opengis.feature.simple.SimpleFeature;

public class ServiceArea {
	private final Collection<ServiceAreaZone> zones;
	private final Geometry envelope;

	public ServiceArea(Collection<ServiceAreaZone> zones) {
		this.zones = zones;

		Geometry[] zoneGeometries = zones.stream().map(ServiceAreaZone::getGeometry).collect(Collectors.toList())
				.toArray(new Geometry[zones.size()]);
		this.envelope = geometryFactory.createGeometryCollection(zoneGeometries).getEnvelope();
	}

	public Collection<ServiceAreaZone> getZones() {
		return zones;
	}

	public boolean covers(Link link) {
		if (zones.size() == 0) {
			return true;
		}

		return covers(link.getCoord());
	}

	public boolean covers(Coord coord) {
		if (zones.size() == 0) {
			return true;
		}

		if (envelope.covers(geometryFactory.createPoint(new Coordinate(coord.getX(), coord.getY())))) {
			for (ServiceAreaZone zone : zones) {
				if (zone.covers(coord)) {
					return true;
				}
			}
		}

		return false;
	}

	private final static GeometryFactory geometryFactory = new GeometryFactory();
	private final static Logger logger = Logger.getLogger(ServiceArea.class);

	static public ServiceArea load(String indexAttribute, Network network, URL url) {
		Map<Integer, Geometry> shapes = new HashMap<>();

		try {
			DataStore dataStore = DataStoreFinder.getDataStore(Collections.singletonMap("url", url));

			SimpleFeatureSource featureSource = dataStore.getFeatureSource(dataStore.getTypeNames()[0]);
			SimpleFeatureCollection featureCollection = featureSource.getFeatures();
			SimpleFeatureIterator featureIterator = featureCollection.features();

			while (featureIterator.hasNext()) {
				SimpleFeature feature = featureIterator.next();
				int index = (int) (long) (Long) feature.getAttribute(indexAttribute);
				shapes.put(index, (Geometry) feature.getDefaultGeometry());
			}

			featureIterator.close();
			dataStore.dispose();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		Map<Integer, Collection<Id<Link>>> links = new HashMap<>();
		shapes.keySet().forEach(k -> links.put(k, new LinkedList<>()));

		for (Link link : network.getLinks().values()) {
			Coordinate coordinate = new Coordinate(link.getCoord().getX(), link.getCoord().getY());
			Point point = geometryFactory.createPoint(coordinate);

			for (Map.Entry<Integer, Geometry> entry : shapes.entrySet()) {
				if (entry.getValue().contains(point)) {
					links.get(entry.getKey()).add(link.getId());
				}
			}
		}

		List<ServiceAreaZone> zones = new ArrayList<>(links.size());

		for (Map.Entry<Integer, Collection<Id<Link>>> entry : links.entrySet()) {
			logger.info(
					String.format("Found %d links for service area zone %d", entry.getValue().size(), entry.getKey()));

			zones.add(new ServiceAreaZone(entry.getKey(), entry.getValue(), shapes.get(entry.getKey())));
		}

		return new ServiceArea(zones);
	}
}
