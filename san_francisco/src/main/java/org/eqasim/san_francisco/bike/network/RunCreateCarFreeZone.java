package org.eqasim.san_francisco.bike.network;

import org.apache.log4j.Logger;
import org.eqasim.san_francisco.bike.reader.BikeInfo;
import org.eqasim.san_francisco.bike.reader.LinkIdsCSVReader;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.gis.ShapeFileReader;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

public class RunCreateCarFreeZone {
    private final static Logger log = Logger.getLogger(RunCreateCarFreeZone.class);

    public static void main(String[] args) throws CommandLine.ConfigurationException, IOException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("input-path", "input-shp", "output-path") //
                .build();

        log.info("Loading the network ...");
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(cmd.getOptionStrict("input-path"));

//        log.info("Loading the carfree zone ...");
//        Collection<Geometry> carFreeZone = ShapeFileReader.getAllFeatures(cmd.getOptionStrict("input-shp")).stream()
//                .map(feature -> (Geometry) feature.getDefaultGeometry())
//                .collect(Collectors.toList());

        Set<Id<Link>> linkIds = new LinkIdsCSVReader().read(cmd.getOptionStrict("input-shp"));

        log.info("Creating the carfree zone in the network...");
//        new BikeNetworkFixer().createCarFreeZoneFromShapefile(network, carFreeZone);
        new BikeNetworkFixer().createCarFreeZoneFromLinkIds(network, linkIds);

//        log.info("Cleaning the network...");
//        MultimodalNetworkCleaner networkCleaner = new MultimodalNetworkCleaner(network);
//        networkCleaner.run(new HashSet<>(Collections.singletonList(TransportMode.car)));

        log.info("Writing the cleaned network ...");
        new NetworkWriter(network).write(cmd.getOptionStrict("output-path"));

        log.info("Done.");

    }

    public static Set<Id<Link>> read(String path) throws IOException {
        Set<Id<Link>> ids = new HashSet<>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));

        List<String> header = null;
        String line = null;

        while ((line = reader.readLine()) != null) {
            List<String> row = Arrays.asList(line.split(";"));

            if (header == null) {
                header = row;
            } else {
                Id<Link> linkId = Id.createLinkId(row.get(header.indexOf("ID")));
                ids.add(linkId);
            }
        }

        reader.close();
        return ids;
    }
}
