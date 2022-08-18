package org.eqasim.core.tools;

import java.util.Collection;
import java.util.LinkedList;

import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class ExportTransitStopsToShapefile {
	public static void main(String[] args) throws Exception {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("schedule-path", "output-path", "crs") //
				.build();

		String schedulePath = cmd.getOptionStrict("schedule-path");

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new TransitScheduleReader(scenario).readFile(schedulePath);

		CoordinateReferenceSystem crs = MGC.getCRS(cmd.getOptionStrict("crs"));

		Collection<SimpleFeature> features = new LinkedList<>();

		PointFeatureFactory pointFactory = new PointFeatureFactory.Builder() //
				.setCrs(crs).setName("id") //
				.addAttribute("id", String.class) //
				.addAttribute("link", String.class) //
				.create();

		for (TransitStopFacility stopFacility : scenario.getTransitSchedule().getFacilities().values()) {
			Coordinate coordinate = new Coordinate(stopFacility.getCoord().getX(), stopFacility.getCoord().getY());

			SimpleFeature feature = pointFactory.createPoint( //
					coordinate, //
					new Object[] { //
							stopFacility.getId().toString(), //
							stopFacility.getLinkId().toString() //
					}, null);

			features.add(feature);
		}

		ShapeFileWriter.writeGeometries(features, cmd.getOptionStrict("output-path"));
	}
}