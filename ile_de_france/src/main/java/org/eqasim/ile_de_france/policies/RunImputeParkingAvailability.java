package org.eqasim.ile_de_france.policies;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.Geometry;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

public class RunImputeParkingAvailability {
    public static final String ATTRIBUTE = "parkingAvailability";


	static public void main(String[] args) throws ConfigurationException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("no-parking-path", "network-path", "output-path") //
				.build();

		String networkPath = cmd.getOptionStrict("network-path");
		String outputPath = cmd.getOptionStrict("output-path");
		String noParking = cmd.getOptionStrict("no-parking-path");

        Network network = NetworkUtils.readNetwork(networkPath);

        CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("EPSG:2154", "EPSG:4326");


        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(noParking);
        List<Geometry> geometries = features.stream()
                    .map(simpleFeature -> (Geometry) simpleFeature.getDefaultGeometry())
                    .toList();

        for (Link link : network.getLinks().values()) {

            link.getAttributes().putAttribute(ATTRIBUTE, "true");

            Coord coord = link.getCoord();
            Coord transformedCoord = transformation.transform(coord);
            Geometry geotoolsPoint = MGC.coord2Point(transformedCoord);

            for (Geometry geometry : geometries) {
                if (geometry.contains(geotoolsPoint)) {
                    link.getAttributes().putAttribute(ATTRIBUTE, "false");
                    break;
                }
            }
        }

        NetworkUtils.writeNetwork(network, outputPath);
	}
}
