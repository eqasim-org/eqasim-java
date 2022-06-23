package org.eqasim.examples.SMMFramework.GBFSUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.sharing.io.DefaultSharingServiceSpecification;
import org.matsim.contrib.sharing.io.ImmutableSharingVehicleSpecification;
import org.matsim.contrib.sharing.io.SharingServiceSpecification;
import org.matsim.contrib.sharing.io.SharingServiceWriter;
import org.matsim.contrib.sharing.service.SharingVehicle;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ReadFreeFloatingGBFS {
    /**
     * Method parses a GBFS file and converts it into sharing contrib spaecification; based on Sharing contrib reader
     * @param vehicles Free Vehicles GBFS
     * @param crs Coordinate system
     * @param network Matsim network
     * @param name Name of the Service
     * @return
     * @throws IOException
     */


    public static String readGBFSFreeFloating(String vehicles, String crs, Network network, String name) throws IOException{

        SharingServiceSpecification service = new DefaultSharingServiceSpecification();
        Network filtNetwork = NetworkUtils.createNetwork();
        Set<String> modes = new HashSet<>(Arrays.asList("car","bike"));
        (new TransportModeNetworkFilter(filtNetwork)).filter(network, modes);
        BufferedReader reader = IOUtils.getBufferedReader(vehicles);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(reader);
        JsonNode vehiclesNode = rootNode.findPath("vehicles");
        if (vehiclesNode.isArray()) {
            Iterator var26 = vehiclesNode.iterator();

            while (var26.hasNext()) {
                JsonNode vehicleNode= (JsonNode) var26.next();
                String vehId = vehicleNode.findValue("vehicle_id").asText();
                Coord coord = new Coord(vehicleNode.findValue("lon").asDouble(), vehicleNode.findValue("lat").asDouble());
                // coord = transformation.transform(coord);
                Link link = NetworkUtils.getNearestLink(network, coord);
                service.addVehicle(ImmutableSharingVehicleSpecification.newBuilder() //
                        .id(Id.create(vehId, SharingVehicle.class)) //
                        .startLinkId(link.getId()) //
                        .build());

            }
        }


        (new SharingServiceWriter(service)).write("freeFloating"+name+".xml");
        File serviceFile=new File("freeFloating"+name+".xml");
        return serviceFile.getAbsolutePath();

    }
}


