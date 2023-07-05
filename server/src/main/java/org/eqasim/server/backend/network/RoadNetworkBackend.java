package org.eqasim.server.backend.network;

import org.eqasim.server.backend.BackendScenario;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

public class RoadNetworkBackend {
	private final Network network;

	RoadNetworkBackend(Network network) {
		this.network = network;
	}

	static public RoadNetworkBackend create(BackendScenario scenario) {
		return new RoadNetworkBackend(scenario.getCarNetwork());
	}

	public NetworkData build() {
		NetworkData networkData = new NetworkData();

		IdMap<Node, Integer> nodeIndices = new IdMap<>(Node.class);
		for (Node node : network.getNodes().values()) {
			NodeData nodeData = new NodeData();

			nodeData.index = nodeIndices.size();
			nodeData.x = node.getCoord().getX();
			nodeData.y = node.getCoord().getY();

			nodeIndices.put(node.getId(), nodeData.index);
			networkData.nodes.add(nodeData);
		}

		for (Link link : network.getLinks().values()) {
			LinkData linkData = new LinkData();

			linkData.u = nodeIndices.get(link.getFromNode().getId());
			linkData.v = nodeIndices.get(link.getToNode().getId());

			networkData.links.add(linkData);
		}

		return networkData;
	}
}
