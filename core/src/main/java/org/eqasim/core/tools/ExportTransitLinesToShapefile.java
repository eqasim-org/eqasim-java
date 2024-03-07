package org.eqasim.core.tools;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class ExportTransitLinesToShapefile {
	public static void main(String[] args) throws Exception {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("schedule-path", "network-path", "output-path", "crs")
				.allowOptions("modes", "transit-lines", "transit-routes")
				.build();

		String schedulePath = cmd.getOptionStrict("schedule-path");
		String networkPath = cmd.getOptionStrict("network-path");
		Optional<String> modesOption = cmd.getOption("modes");
		Optional<String> transitLinesOption = cmd.getOption("transit-lines");
		Optional<String> transitRoutesOption = cmd.getOption("transit-routes");

		if(BooleanUtils.toInteger(modesOption.isPresent()) + BooleanUtils.toInteger(transitLinesOption.isPresent()) + BooleanUtils.toInteger(transitRoutesOption.isPresent()) > 1) {
			throw new IllegalStateException("Only one of the options 'modes', 'transit-lines' and 'transit-routes' can be used");
		}

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new TransitScheduleReader(scenario).readFile(schedulePath);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkPath);

		CoordinateReferenceSystem crs = MGC.getCRS(cmd.getOptionStrict("crs"));

		Collection<SimpleFeature> features = new LinkedList<>();

		PolylineFeatureFactory linkFactory = new PolylineFeatureFactory.Builder() //
				.setCrs(crs).setName("line") //
				.addAttribute("line_id", String.class) //
				.addAttribute("line_name", String.class)//
				.addAttribute("route_id", String.class) //
				.addAttribute("mode", String.class) //
				.addAttribute("description", String.class) //
				.create();

		Network network = scenario.getNetwork();

		Set<String> modes = new HashSet<>();
		IdSet<TransitLine> transitLineIdSet = new IdSet<>(TransitLine.class);
		IdSet<TransitRoute> transitRouteIdSet = new IdSet<>(TransitRoute.class);

		transitLinesOption.ifPresent(value -> Arrays.stream(value.split(",")).map(String::trim).map(s -> Id.create(s, TransitLine.class)).forEach(transitLineIdSet::add));
		transitRoutesOption.ifPresent(value -> Arrays.stream(value.split(",")).map(String::trim).map(s -> Id.create(s, TransitRoute.class)).forEach(transitRouteIdSet::add));
		if(modesOption.isPresent()) {
			modes = Arrays.stream(modesOption.get().split(",")).map(String::trim).collect(Collectors.toSet());
		}

		for (TransitLine transitLine : scenario.getTransitSchedule().getTransitLines().values()) {
			if(transitLineIdSet.size() > 0 && !transitLineIdSet.contains(transitLine.getId())) {
				continue;
			}
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				if(transitRouteIdSet.size() > 0 && !transitRouteIdSet.contains(transitRoute.getId())) {
					continue;
				}
				if(modes.size() > 0 && !modes.contains(transitRoute.getTransportMode())) {
					continue;
				}
				NetworkRoute networkRoute = transitRoute.getRoute();
				List<Link> links = new ArrayList<>(networkRoute.getLinkIds().size() + 2);
				links.add(network.getLinks().get(networkRoute.getStartLinkId()));
				networkRoute.getLinkIds().forEach(id -> links.add(network.getLinks().get(id)));
				links.add(network.getLinks().get(networkRoute.getEndLinkId()));

				Coordinate[] coordinates = new Coordinate[links.size() + 1];

				for (int i = 0; i < links.size(); i++) {
					Link link = links.get(i);

					if (i == 0) {
						coordinates[i] = new Coordinate(link.getFromNode().getCoord().getX(),
								link.getFromNode().getCoord().getY());
					}

					coordinates[i + 1] = new Coordinate(link.getToNode().getCoord().getX(),
							link.getToNode().getCoord().getY());
				}

				SimpleFeature feature = linkFactory.createPolyline( //
						coordinates, //
						new Object[] { //
								transitLine.getId().toString(),
								transitLine.getName(),//
								transitRoute.getId().toString(), //
								transitRoute.getTransportMode(), //
								transitRoute.getDescription() //
						}, null);

				features.add(feature);
			}
		}

		ShapeFileWriter.writeGeometries(features, cmd.getOptionStrict("output-path"));
	}
}