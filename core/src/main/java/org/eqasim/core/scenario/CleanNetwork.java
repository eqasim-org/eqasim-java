package org.eqasim.core.scenario;

import java.util.Collections;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;

public class CleanNetwork {
	static public void main(String[] args) {
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(args[0]);
		
		Network roadNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(network).filter(roadNetwork, Collections.singleton("car"));
		
		new NetworkCleaner().run(roadNetwork);
		
		new NetworkWriter(roadNetwork).write(args[1]);
	}
}
