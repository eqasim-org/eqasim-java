package org.eqasim.ile_de_france.munich;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;

public class AddTransitZoneInformation {
	private final static GeometryFactory geometryFactory = new GeometryFactory();

	private final static int stictMaximumZone = 12;

	static public void main(String[] args) throws ConfigurationException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("input-path", "zones-path", "output-path") //
				.build();

		File zonesPath = new File(cmd.getOptionStrict("zones-path"));

		// Load zones
		DataStore dataStore = DataStoreFinder.getDataStore(Collections.singletonMap("url", zonesPath.toURI().toURL()));
		SimpleFeatureSource featureSource = dataStore.getFeatureSource(dataStore.getTypeNames()[0]);
		SimpleFeatureCollection featureCollection = featureSource.getFeatures();
		SimpleFeatureIterator featureIterator = featureCollection.features();

		List<Tuple<String, MultiPolygon>> zones = new LinkedList<>();

		while (featureIterator.hasNext()) {
			SimpleFeature feature = featureIterator.next();

			MultiPolygon geometry = (MultiPolygon) feature.getDefaultGeometry();
			String zone = (String) feature.getAttribute("zone");

			zones.add(Tuple.of(zone, geometry));
		}

		featureIterator.close();
		dataStore.dispose();

		// Attach to schedule
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new TransitScheduleReader(scenario).readFile(cmd.getOptionStrict("input-path"));

		Counter counter = new Counter("processing stop facilities ",
				" of " + scenario.getTransitSchedule().getFacilities().size());

		scenario.getTransitSchedule().getFacilities().values().parallelStream().forEach(facility -> {
			Coordinate coordinate = new Coordinate(facility.getCoord().getX(), facility.getCoord().getY());
			Point point = geometryFactory.createPoint(coordinate);

			Integer minimumZone = null;
			Integer maximumZone = null;

			for (var zone : zones) {
				int integerZone = zone.getFirst().equals("m") ? 0 : Integer.parseInt(zone.getFirst());

				if (zone.getSecond().contains(point)) {
					if (minimumZone == null) {
						minimumZone = integerZone;
						maximumZone = integerZone;
					} else {
						minimumZone = Math.min(minimumZone, integerZone);
						maximumZone = Math.max(maximumZone, integerZone);
					}
				}
			}

			if (minimumZone != null) {
				facility.getAttributes().putAttribute("minimumZone", Math.min(minimumZone, stictMaximumZone));
				facility.getAttributes().putAttribute("maximumZone", Math.min(maximumZone, stictMaximumZone));
			}

			synchronized (counter) {
				counter.incCounter();
			}
		});

		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(cmd.getOptionStrict("output-path"));
	}
}
