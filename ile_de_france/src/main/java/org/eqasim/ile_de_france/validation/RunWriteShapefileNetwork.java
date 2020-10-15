package org.eqasim.ile_de_france.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class RunWriteShapefileNetwork {
	static public void main(String[] args) throws ConfigurationException {

		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("input-path", "output-path") //
				.build();

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(cmd.getOptionStrict("input-path"));

		CoordinateReferenceSystem crs = MGC.getCRS("EPSG:2154");
		Collection<SimpleFeature> features = new ArrayList<>(network.getLinks().size());

		PolylineFeatureFactory featureFactory = new PolylineFeatureFactory.Builder() //
				.setCrs(crs) //
				.setName("links") //
				.addAttribute("link_id", String.class) //
				.addAttribute("osm_type", String.class) //
				.addAttribute("length", Double.class) //
				//
				.create();

		List<String> allowedTypes = Arrays.asList("primary", "secondary", "primary_link", "secondary_link", "tertiary",
				"tertiary_link", "motorway", "motorway_link", "trunk", "trunk_link");

		allowedTypes = Arrays.asList("primary", "secondary", "primary", "secondary", "tertiary", "tertiary", "motorway",
				"motorway", "trunk", "trunk");

		for (Link link : network.getLinks().values()) {
			if (link.getAllowedModes().contains("car")) {
				String type = (String) link.getAttributes().getAttribute("osm:way:highway");

				//if (allowedTypes.contains(type)) {
					Coordinate fromCoord = new Coordinate(link.getFromNode().getCoord().getX(),
							link.getFromNode().getCoord().getY());
					Coordinate toCoord = new Coordinate(link.getToNode().getCoord().getX(),
							link.getToNode().getCoord().getY());

					SimpleFeature feature = featureFactory.createPolyline(new Coordinate[] { fromCoord, toCoord },
							new Object[] { //
									link.getId().toString(), //
									type, //
									link.getLength(),
							//
							}, null);
					features.add(feature);
				//}
			}
		}

		ShapeFileWriter.writeGeometries(features, cmd.getOptionStrict("output-path"));
	}
}
