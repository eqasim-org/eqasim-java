package org.eqasim.server.backend.iso;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eqasim.server.backend.iso.TransitIsochroneBackend.RequestMode;
import org.geotools.data.simple.SimpleFeatureReader;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class RunIsochrone {
	private final static Logger logger = LogManager.getLogger(RunIsochrone.class);

	static public void main(String[] args)
			throws IOException, ConfigurationException, NoSuchAuthorityCodeException, FactoryException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("input-path", "output-path", "network-path", "schedule-path", "crs",
						"maximum-travel-time") //
				.allowOptions("departure-time", "transfer-distance", "transfer-speed", "maximum-transfers") //
				.build();

		// Read input
		logger.info("Reading input ...");

		File inputPath = new File(cmd.getOptionStrict("input-path"));
		if (!inputPath.exists()) {
			throw new IOException("File does not exist: " + inputPath.toString());
		}

		double departureTime = cmd.getOption("departure-time").map(Double::parseDouble).orElse(8.0 * 3600.0);

		GeoPackage inputPackage = new GeoPackage(inputPath);
		FeatureEntry inputFeatureEntry = inputPackage.features().get(0);
		SimpleFeatureReader inputReader = inputPackage.reader(inputFeatureEntry, null, null);

		List<IsochroneRequest> requests = new LinkedList<>();
		while (inputReader.hasNext()) {
			SimpleFeature inputFeature = inputReader.next();
			Point inputGeometry = (Point) inputFeature.getDefaultGeometry();
			String requestId = (String) inputFeature.getAttribute("request_id");
			requests.add(new IsochroneRequest(requestId, inputGeometry.getX(), inputGeometry.getY(), departureTime));
		}

		inputPackage.close();

		// Read MATSim related data
		logger.info("Reading MATSim data ...");

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		new MatsimNetworkReader(scenario.getNetwork()).readFile(cmd.getOptionStrict("network-path"));
		new TransitScheduleReader(scenario).readFile(cmd.getOptionStrict("schedule-path"));

		Network network = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(scenario.getNetwork()).filter(network, Collections.singleton("car"));

		// Prepare routing
		RoadIsochroneBackend roadBackend = new RoadIsochroneBackend(network, new FreeSpeedTravelTime());
		TransitIsochroneBackend transitBackend = new TransitIsochroneBackend(scenario.getTransitSchedule());

		// Prepare output
		File outputPath = new File(cmd.getOptionStrict("output-path"));
		
		if (outputPath.exists()) {
			outputPath.delete();
		}
		
		GeoPackage outputPackage = new GeoPackage(outputPath);
		outputPackage.init();

		CoordinateReferenceSystem crs = MGC.getCRS(cmd.getOptionStrict("crs"));
		GeometryFactory geometryFactory = new GeometryFactory();

		// Road routing
		logger.info("Performing road routing ...");

		double maximumTravelTime = Double.parseDouble(cmd.getOptionStrict("maximum-travel-time"));
		double transferDistance = cmd.getOption("transfer-distance").map(Double::parseDouble).orElse(600.0);

		{
			SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
			featureTypeBuilder.setName("road");
			featureTypeBuilder.setCRS(crs);
			featureTypeBuilder.setDefaultGeometry("geometry");
			
			featureTypeBuilder.add("request_id", String.class);
			featureTypeBuilder.add("travel_time", Double.class);
			featureTypeBuilder.add("distance", Double.class);
			featureTypeBuilder.add("geometry", Point.class);

			SimpleFeatureType featureType = featureTypeBuilder.buildFeatureType();
			SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
			DefaultFeatureCollection featureCollection = new DefaultFeatureCollection();

			for (IsochroneRequest request : requests) {
				logger.info("  Request " + request.requestId);

				RoadIsochroneBackend.Request roadRequest = new RoadIsochroneBackend.Request(request.originX,
						request.originY, departureTime, maximumTravelTime, transferDistance);

				for (var destination : roadBackend.process(roadRequest).destinations) {
					featureBuilder.add(request.requestId);
					featureBuilder.add(destination.travelTime);
					featureBuilder.add(destination.distance);
					featureBuilder.add(geometryFactory.createPoint(new Coordinate(destination.x, destination.y)));
					featureCollection.add(featureBuilder.buildFeature(null));
				}
			}
			
			FeatureEntry featureEntry = new FeatureEntry();
			outputPackage.add(featureEntry, featureCollection);
		}

		// Transit routing
		List<TransitStep> transitSteps = Arrays.asList( //
				new TransitStep("transit", RequestMode.Default, "all"), //
				new TransitStep("transit_no_rail", RequestMode.WithoutRail, "without rail"), //
				new TransitStep("transit_only_rail", RequestMode.OnlyRail, "only rail") //
		);

		for (TransitStep transitStep : transitSteps) {
			logger.info("Performing transit routing (" + transitStep.description + ") ...");

			int maximumTransfers = cmd.getOption("maximum-transfers").map(Integer::parseInt).orElse(5);
			double transferSpeed = cmd.getOption("transfer-speed").map(Double::parseDouble).orElse(2.22 / 1.3);

			SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
			featureTypeBuilder.setName(transitStep.layer);
			featureTypeBuilder.setCRS(crs);
			featureTypeBuilder.setDefaultGeometry("geometry");
			
			featureTypeBuilder.add("request_id", String.class);
			featureTypeBuilder.add("travel_time", Double.class);
			featureTypeBuilder.add("transfers", Integer.class);
			featureTypeBuilder.add("geometry", Point.class);

			SimpleFeatureType featureType = featureTypeBuilder.buildFeatureType();
			SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
			DefaultFeatureCollection featureCollection = new DefaultFeatureCollection();

			for (IsochroneRequest request : requests) {
				logger.info("  Request " + request.requestId);

				TransitIsochroneBackend.Request roadRequest = new TransitIsochroneBackend.Request(request.originX,
						request.originY, departureTime, maximumTravelTime, maximumTransfers, transitStep.requestMode,
						transferDistance, 1.0, transferSpeed);

				for (var destination : transitBackend.process(roadRequest).destinations) {
					featureBuilder.add(request.requestId);
					featureBuilder.add(destination.travelTime);
					featureBuilder.add(destination.transfers);
					featureBuilder.add(geometryFactory.createPoint(new Coordinate(destination.x, destination.y)));
					featureCollection.add(featureBuilder.buildFeature(null));
				}
			}
			
			FeatureEntry featureEntry = new FeatureEntry();
			outputPackage.add(featureEntry, featureCollection);
		}

		outputPackage.close();
	}

	static public class TransitStep {
		public final String description;
		public final String layer;
		public final RequestMode requestMode;

		public TransitStep(String layer, RequestMode requestMode, String description) {
			this.layer = layer;
			this.requestMode = requestMode;
			this.description = description;
		}
	}

	static public class IsochroneRequest {
		public final String requestId;
		public final double originX;
		public final double originY;
		public final double departureTime;

		public IsochroneRequest(String requestId, double originX, double originY, double departureTime) {
			this.requestId = requestId;
			this.originX = originX;
			this.originY = originY;
			this.departureTime = departureTime;
		}
	}
}
