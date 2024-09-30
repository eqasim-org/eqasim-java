package org.eqasim.ile_de_france.parking;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class ParkingPressureData {
	public static final String ATTRIBUTE = "parkingPressure";

	private static final Logger logger = LogManager.getLogger(ParkingPressureData.class);
	private static final GeometryFactory geometryFactory = new GeometryFactory();

	private final IdMap<Link, Double> parkingPressure;

	ParkingPressureData(IdMap<Link, Double> parkingPressure) {
		this.parkingPressure = parkingPressure;
	}

	public double getParkingPressure(Id<Link> linkId) {
		return parkingPressure.computeIfAbsent(linkId, id -> 0.0).doubleValue();
	}

	static public ParkingPressureData loadFromAttributes(Network network) {
		IdMap<Link, Double> parkingPressure = new IdMap<>(Link.class);

		for (Link link : network.getLinks().values()) {
			Double value = (Double) link.getAttributes().getAttribute(ATTRIBUTE);

			if (value != null) {
				parkingPressure.put(link.getId(), value);
			}
		}

		return new ParkingPressureData(parkingPressure);
	}

	static public ParkingPressureData createFromNetwork(Network network, File path) throws IOException {
		logger.info(String.format("Loading parking pressure data from %s", path.toString()));
		
		Map<String, String> arguments = new HashMap<>();
		arguments.put("dbtype", "geopkg");
		arguments.put("database", path.toString());

		DataStore geometryStore = DataStoreFinder.getDataStore(arguments);

		SimpleFeatureSource geometrySource = geometryStore.getFeatureSource(geometryStore.getTypeNames()[0]);
		SimpleFeatureCollection geometryCollection = geometrySource.getFeatures();
		SimpleFeatureIterator geometryIterator = geometryCollection.features();

		Map<Geometry, Double> geometries = new HashMap<>();

		while (geometryIterator.hasNext()) {
			SimpleFeature feature = geometryIterator.next();
			double parkingPressure = (Double) feature.getAttribute("parking_pressure");
			geometries.put((Geometry) feature.getDefaultGeometry(), parkingPressure);
		}

		logger.info(String.format("Matching parking pressure data"));
		IdMap<Link, Double> parkingPressure = new IdMap<>(Link.class);

		for (Link link : network.getLinks().values()) {
			Coordinate coordinate = new Coordinate(link.getCoord().getX(), link.getCoord().getY());
			Point point = geometryFactory.createPoint(coordinate);

			for (var entry : geometries.entrySet()) {
				if (entry.getKey().covers(point)) {
					parkingPressure.put(link.getId(), entry.getValue());
				}
			}
		}

		return new ParkingPressureData(parkingPressure);
	}
}
