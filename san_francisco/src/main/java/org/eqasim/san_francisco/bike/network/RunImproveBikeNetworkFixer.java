package org.eqasim.san_francisco.bike.network;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.gis.ShapeFileReader;

import java.util.Collection;
import java.util.stream.Collectors;

public class RunImproveBikeNetworkFixer {
    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("input-path", "input-shp", "output-path") //
                .build();

        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(cmd.getOptionStrict("input-path"));

        // add new bike lines
        Collection<Geometry> geometries = ShapeFileReader.getAllFeatures(cmd.getOptionStrict("input-shp")).stream()
                .map(feature -> (Geometry) feature.getDefaultGeometry())
                .collect(Collectors.toList());
        new BikeNetworkFixer().addNewBikeLanesWithinShape(network, geometries);
//        new BikeNetworkFixer().addNewBikeLanes(network);

        new NetworkWriter(network).write(cmd.getOptionStrict("output-path"));

    }
}
