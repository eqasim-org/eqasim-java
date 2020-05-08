package org.eqasim.ile_de_france.scenario;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;

public class NetworkModification {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Path inputNetwork =   Paths.get("ile_de_france_network.xml.gz");
		Path outputNetwork =  Paths.get("ile_de_france_network_reduced.xml.gz");
		
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(inputNetwork.toString());
		
		for (Link link : network.getLinks().values()) {
			double capacity = link.getCapacity()*0.8;
			link.setCapacity(capacity);
			
			double freespeed = link.getFreespeed();
			if ( freespeed >= 5.5 ) {
				freespeed = freespeed - 5.5 ;
			} else {
			}
			link.setFreespeed(freespeed);
		}
		new NetworkWriter(network).write(outputNetwork.toString());
	}

}

//package org.matsim.project;
		//network.getLinks().get(Id.createLinkId("16578")).setCapacity(1);
		//network.getLinks().get(Id.createLinkId("16584")).setCapacity(1);
		