package org.eqasim.los_angeles.scenario;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class AddPTAccessibleAttribute {

	public static void main(String[] args) throws IOException {

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createMutableScenario(config);
		MatsimNetworkReader netowrkReader = new MatsimNetworkReader(scenario.getNetwork());
		netowrkReader.readFile(args[0]);
		TransitScheduleReader tsReader = new TransitScheduleReader(scenario);
		tsReader.readFile(args[1]);

		double minX = scenario.getNetwork().getLinks().values().stream().map(Link::getCoord).mapToDouble(Coord::getX)
				.min().getAsDouble();
		double maxX = scenario.getNetwork().getLinks().values().stream().map(Link::getCoord).mapToDouble(Coord::getX)
				.max().getAsDouble();
		double minY = scenario.getNetwork().getLinks().values().stream().map(Link::getCoord).mapToDouble(Coord::getY)
				.min().getAsDouble();
		double maxY = scenario.getNetwork().getLinks().values().stream().map(Link::getCoord).mapToDouble(Coord::getY)
				.max().getAsDouble();

		QuadTree<TransitStopFacility> transitStops = new QuadTree<TransitStopFacility>(minX, minY, maxX, maxY);

		BufferedReader reader = IOUtils.getBufferedReader(args[2]);

		BufferedWriter writer = IOUtils.getBufferedWriter(args[3]);
		writer.write("xcoord,ycoord\n");
		for (TransitStopFacility tsf : scenario.getTransitSchedule().getFacilities().values()) {
			writer.write(tsf.getCoord().getX() + "," + tsf.getCoord().getY() + "\n");
			transitStops.put(tsf.getCoord().getX(), tsf.getCoord().getY(), tsf);
		}
		writer.flush();
		writer.close();
		writer = IOUtils.getBufferedWriter(args[4]);
		
		writer.write(reader.readLine() + ",coordx_pt,coordy_pt,hasptstop\n");
		String s = reader.readLine();
		
		CoordinateTransformation coordTransformation = TransformationFactory.getCoordinateTransformation("EPSG:4326",
				"EPSG:2227");
		while (s != null) {
			String[] arr = s.split(",");
			Coord coordStart_raw = new Coord(Double.parseDouble(arr[2]), Double.parseDouble(arr[3]));
			Coord coordStart = coordTransformation.transform(coordStart_raw);
			Coord coordEnd = transitStops.getClosest(coordStart.getX(), coordStart.getY()).getCoord();
			double distance = CoordUtils.calcEuclideanDistance(coordStart, coordEnd);
			writer.write(s + "," + coordStart.getX() + "," + coordStart.getY() + "," + (distance < 3500) + "\n");
			s = reader.readLine();
		}
		writer.flush();
		writer.close();
	}

}
