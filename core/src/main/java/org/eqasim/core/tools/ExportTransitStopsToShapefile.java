package org.eqasim.core.tools;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class ExportTransitStopsToShapefile {
	public static void exportTransitStopsToShapefile(Collection<TransitStopFacility> transitStopFacilities, String crsString, String filePath) {
		CoordinateReferenceSystem crs = MGC.getCRS(crsString);

		Collection<SimpleFeature> features = new LinkedList<>();

		PointFeatureFactory pointFactory = new PointFeatureFactory.Builder() //
				.setCrs(crs).setName("id") //
				.addAttribute("id", String.class) //
				.addAttribute("link", String.class)
				.addAttribute("name", String.class)
				.addAttribute("areaId", String.class)//
				.create();

		for (TransitStopFacility stopFacility : transitStopFacilities) {
			Coordinate coordinate = new Coordinate(stopFacility.getCoord().getX(), stopFacility.getCoord().getY());

			SimpleFeature feature = pointFactory.createPoint( //
					coordinate, //
					new Object[] { //
							stopFacility.getId().toString(), //
							stopFacility.getLinkId().toString(),
							stopFacility.getName(), //,
							stopFacility.getStopAreaId() == null ? "nan" : stopFacility.getStopAreaId().toString()
					}, null);

			features.add(feature);
		}

		ShapeFileWriter.writeGeometries(features, filePath);
	}

	public static void main(String[] args) throws Exception {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("schedule-path", "output-path", "crs")
				.allowOptions("transit-modes")
				.allowOptions("one-per-area")//
				.build();

		String schedulePath = cmd.getOptionStrict("schedule-path");

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new TransitScheduleReader(scenario).readFile(schedulePath);

		Collection<TransitStopFacility> facilities;
		if(cmd.hasOption("transit-modes")) {
			Set<String> modes = Arrays.stream(cmd.getOptionStrict("transit-modes").split(",")).map(String::trim).collect(Collectors.toSet());
			facilities = scenario.getTransitSchedule().getTransitLines().values().stream()
					.flatMap(line -> line.getRoutes().values().stream())
					.filter(route -> modes.contains(route.getTransportMode()))
					.flatMap(route -> route.getStops().stream())
					.map(TransitRouteStop::getStopFacility)
					.collect(Collectors.toSet());
		} else {
			facilities = scenario.getTransitSchedule().getFacilities().values();
		}
		boolean oneStopPerArea = cmd.hasOption("one-per-area") && Boolean.parseBoolean(cmd.getOptionStrict("one-per-area"));

		if(oneStopPerArea) {
			facilities = facilities.stream().collect(Collectors.toMap(
					TransitStopFacility::getStopAreaId,
					f -> new ArrayList<>(Collections.singleton(f)), (oldList, newList) -> {
						oldList.addAll(newList);
						return oldList;
					})).entrySet().stream().flatMap(entry -> {
						if (entry.getKey() == null) {
							return entry.getValue().stream();
						} else {
							return Stream.of(entry.getValue().get(0));
						}
					}).collect(Collectors.toSet());
		}
		exportTransitStopsToShapefile(facilities, cmd.getOptionStrict("crs"), cmd.getOptionStrict("output-path"));
	}
}