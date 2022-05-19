package org.eqasim.examples.corsica_drt.GBFSUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.sharing.io.*;
import org.matsim.contrib.sharing.service.SharingStation;
import org.matsim.contrib.sharing.service.SharingVehicle;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

import static java.lang.String.valueOf;

public class ReadStationBasedGBFS {

    public static void main(String[]args) throws IOException {
        readGBFSStationBased(args[0],args[1],args[2],args[3]);

}
    public static void  readGBFSStationBased(String stations, String crs,String network
             ,String stationStatus) throws IOException{
        CoordinateTransformation transformation = new GeotoolsTransformation("EPSG:4326",crs);
        Network fullNetwork = NetworkUtils.createNetwork();
        (new MatsimNetworkReader(fullNetwork)).readFile(network);
        Network filtNetwork = NetworkUtils.createNetwork();
        Set<String> modes = new HashSet<>(Arrays.asList("car","bike"));
        (new TransportModeNetworkFilter(fullNetwork)).filter(filtNetwork, modes);
        SharingServiceSpecification service = new DefaultSharingServiceSpecification();
        Map<Id<SharingStation>, Id<Link>> stationLinks = new HashMap();
        BufferedReader reader = IOUtils.getBufferedReader(stations);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(reader);
        JsonNode stationsNode = rootNode.findPath("stations");
        JsonNode docksCapacity=rootNode.findPath("vehicle_type_dock_capacity");
        System.out.println(docksCapacity);
        if (stationsNode.isArray()) {
            Iterator var26 = stationsNode.iterator();

            while (var26.hasNext()) {
                JsonNode stationNode = (JsonNode) var26.next();
                String stationId = stationNode.findValue("id").asText();
                int capacity=0;
                if (docksCapacity.isArray()) {
                    Iterator docksIter = docksCapacity.iterator();
                    while (docksIter.hasNext()) {
                        JsonNode dockNode = (JsonNode) docksIter.next();
                        for (int i=0;i<3;i++){
                            String name=valueOf(i);
                            System.out.println(dockNode.get(name));
                            capacity = capacity + dockNode.get(name).asInt();
                        }
                    }
                }
                //int capacity = stationNode.findValue("capacity").asInt();
                Coord coord = new Coord(stationNode.findValue("lon").asDouble(), stationNode.findValue("lat").asDouble());
                System.out.println(coord);
                //coord = transformation.transform(coord);
                Link link = NetworkUtils.getNearestLink(filtNetwork, coord);
                System.out.println(link.getId());
                service.addStation(ImmutableSharingStationSpecification.newBuilder().id(Id.create(stationId, SharingStation.class)).capacity(capacity).linkId(link.getId()).build());
                stationLinks.put(Id.create(stationId, SharingStation.class), link.getId());
            }
        }

        reader = IOUtils.getBufferedReader(stationStatus);
        mapper = new ObjectMapper();
        rootNode = mapper.readTree(reader);
        stationsNode = rootNode.findPath("stations");
        int vehicleIndex = 0;
        if (stationsNode.isArray()) {
            Iterator var29 = stationsNode.iterator();

            while(var29.hasNext()) {
                JsonNode stationNode = (JsonNode)var29.next();
                String stationId = stationNode.findValue("station_id").asText();
                int numberOfBikes = stationNode.findValue("num_vehicles_available").asInt();

                for(int k = 0; k < numberOfBikes; ++k) {
                    service.addVehicle(ImmutableSharingVehicleSpecification.newBuilder().id(Id.create((long)vehicleIndex, SharingVehicle.class)).startStationId(Id.create(stationId, SharingStation.class)).startLinkId((Id)stationLinks.get(Id.create(stationId, SharingStation.class))).build());
                    ++vehicleIndex;
                }
            }
        }

        (new SharingServiceWriter(service)).write("stationBasedService.xml");

    }
}
