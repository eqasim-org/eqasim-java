package org.eqasim.core.scenario.cutter.network;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkQuadTree;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.SearchableNetwork;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.utils.objectattributes.attributable.Attributes;

public class RoadNetwork implements Network, SearchableNetwork {
	private Network delegate;

	public RoadNetwork(Network network) {
		delegate = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(network).filter(delegate, Collections.singleton(TransportMode.car));
	}

	public Attributes getAttributes() {
		return delegate.getAttributes();
	}

	public NetworkFactory getFactory() {
		return delegate.getFactory();
	}

	public Map<Id<Node>, ? extends Node> getNodes() {
		return delegate.getNodes();
	}

	public Map<Id<Link>, ? extends Link> getLinks() {
		return delegate.getLinks();
	}

	public double getCapacityPeriod() {
		return delegate.getCapacityPeriod();
	}

	public double getEffectiveLaneWidth() {
		return delegate.getEffectiveLaneWidth();
	}

	public void addNode(Node nn) {
		delegate.addNode(nn);
	}

	public void addLink(Link ll) {
		delegate.addLink(ll);
	}

	public Node removeNode(Id<Node> nodeId) {
		return delegate.removeNode(nodeId);
	}

	public Link removeLink(Id<Link> linkId) {
		return delegate.removeLink(linkId);
	}

	public void setCapacityPeriod(double capPeriod) {
		delegate.setCapacityPeriod(capPeriod);
	}

	public void setEffectiveCellSize(double effectiveCellSize) {
		delegate.setEffectiveCellSize(effectiveCellSize);
	}

	public void setEffectiveLaneWidth(double effectiveLaneWidth) {
		delegate.setEffectiveLaneWidth(effectiveLaneWidth);
	}

	public void setName(String name) {
		delegate.setName(name);
	}

	public String getName() {
		return delegate.getName();
	}

	public double getEffectiveCellSize() {
		return delegate.getEffectiveCellSize();
	}

	@Override
	public Link getNearestLinkExactly(Coord coord) {
		return ((SearchableNetwork) delegate).getNearestLinkExactly(coord);
	}

	@Override
	public Node getNearestNode(Coord coord) {
		return ((SearchableNetwork) delegate).getNearestNode(coord);
	}

	@Override
	public Collection<Node> getNearestNodes(Coord coord, double distance) {
		return ((SearchableNetwork) delegate).getNearestNodes(coord, distance);
	}

	@Override
	public QuadTree<Node> getNodeQuadTree() {
		return ((SearchableNetwork) delegate).getNodeQuadTree();
	}

	@Override
	public LinkQuadTree getLinkQuadTree() {
		return ((SearchableNetwork) delegate).getLinkQuadTree();
	}
}
