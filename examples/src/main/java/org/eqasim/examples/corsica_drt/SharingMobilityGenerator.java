package org.eqasim.examples.corsica_drt;

import com.google.common.collect.Sets;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.sharing.io.*;
import org.matsim.contrib.sharing.service.SharingStation;
import org.matsim.contrib.sharing.service.SharingVehicle;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Random;

public class SharingMobilityGenerator {
    public static void main(String[] args) {
        Config config = ConfigUtils.createConfig();
        config.controler().setLastIteration(10);
        config.controler().setOutputDirectory("/org/eqasim/examples/corsica_drt/baseData/sharingCorsicaMod");
        config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
        config.network().setInputFile("C:\\Users\\User\\Desktop\\Juan\\Matsim\\eqasim-javaClean\\examples\\src\\main\\java\\org\\eqasim\\examples\\corsica_drt\\baseData\\inputFiles\\corsica_network.xml");
        //config.plans().setInputFile("C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\MATSim_Root\\src\\main\\java\\org\\matsim\\Sioux\\sioux-falls-master\\src\\main\\resources\\ch\\ethz\\matsim\\sioux_falls\\population.xml.gz");
        Scenario scenario = ScenarioUtils.loadScenario(config) ;
        Network net= scenario.getNetwork();
        Map<Id<Node>,? extends Node> mapNodes= net.getNodes();
        Map<Id<Link>,? extends Link> mapLinks= net.getLinks();
        ArrayList<Id<Node>> keysAsArray = new ArrayList<Id<Node>>(mapNodes.keySet());

        Random r = new Random();
        // Empty container of service scheme
        SharingServiceSpecification service = new DefaultSharingServiceSpecification();

        for( Map.Entry<Id<Link>, ? extends Link> link : mapLinks.entrySet()){
            Link linkTemp=link.getValue();

            linkTemp.setAllowedModes(Sets.newHashSet(TransportMode.car,"bus", "pt","bike"));
        }
        for( int i=0; i<=4;i++){
             Node tempNode =mapNodes.get(keysAsArray.get(r.nextInt(keysAsArray.size())));

             int capacity=1000;
             String stationID=String.valueOf(i);
            Coord coord = new Coord(tempNode.getCoord().getX(),tempNode.getCoord().getY());

            Link link = NetworkUtils.getNearestLink(net, coord);
            Id<SharingStation> id= Id.create(stationID,SharingStation.class);

            service.addStation(ImmutableSharingStationSpecification.newBuilder() //
                    .id(Id.create(stationID, SharingStation.class)) //
                    .capacity(capacity) //
                    .linkId(link.getId()) //
                    .build());

            System.out.println(i);

        }
        Collection<SharingStationSpecification> stationSharingStationIdMap=  service.getStations();
        for(int i =0;i<=50;i++){
        SharingStationSpecification tempStation= (SharingStationSpecification) getRandomObject(stationSharingStationIdMap);

            service.addVehicle(ImmutableSharingVehicleSpecification.newBuilder() //
                    .id(Id.create(i, SharingVehicle.class)) //
                    .startStationId((tempStation.getId())) //
                    .startLinkId(tempStation.getLinkId()) //
                    .build());


        }


        new SharingServiceWriter(service).write("SBBSDummy.xml");
        new NetworkWriter(net).write("SBBSNetwork.xml");

    }
    private static Object getRandomObject(Collection from) {
        Random rnd = new Random();
        int i = rnd.nextInt(from.size());
        return from.toArray()[i];
    }
}
