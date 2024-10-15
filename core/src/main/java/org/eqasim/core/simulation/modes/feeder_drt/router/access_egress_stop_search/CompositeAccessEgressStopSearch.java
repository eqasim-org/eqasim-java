package org.eqasim.core.simulation.modes.feeder_drt.router.access_egress_stop_search;

import com.google.common.collect.ImmutableSet;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.Facility;

import java.util.Collection;
import java.util.HashSet;

public class CompositeAccessEgressStopSearch implements AccessEgressStopSearch {

    private final QuadTree<Facility> accessQuadTree;
    private final QuadTree<Facility> egressQuadTree;
    private final Collection<Facility> accessCollection;
    private final Collection<Facility> egressCollection;

    public CompositeAccessEgressStopSearch(Collection<AccessEgressStopSearch> delegates, Network drtNetwork) {
        double[] bounds = NetworkUtils.getBoundingBox(drtNetwork.getNodes().values());
        accessQuadTree = new QuadTree<>(bounds[0], bounds[1], bounds[2], bounds[3]);
        egressQuadTree = new QuadTree<>(bounds[0], bounds[1], bounds[2], bounds[3]);
        Collection<Facility> accessCollection = new HashSet<>();
        Collection<Facility> egressCollection = new HashSet<>();

        delegates.stream().flatMap(delegate -> delegate.getAccessFacilitiesCollection().stream()).forEach(facility -> {
            accessCollection.add(facility);
            accessQuadTree.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
        });

        delegates.stream().flatMap(delegate -> delegate.getEgressFacilitiesCollection().stream()).forEach(facility -> {
            egressCollection.add(facility);
            egressQuadTree.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
        });

        this.accessCollection = ImmutableSet.copyOf(accessCollection);
        this.egressCollection = ImmutableSet.copyOf(egressCollection);
    }

    @Override
    public Collection<Facility> getAccessFacilitiesCollection() {
        return this.accessCollection;
    }

    @Override
    public QuadTree<Facility> getAccessFacilitiesQuadTree() {
        return this.accessQuadTree;
    }

    @Override
    public Collection<Facility> getEgressFacilitiesCollection() {
        return this.egressCollection;
    }

    @Override
    public QuadTree<Facility> getEgressFacilitiesQuadTree() {
        return this.egressQuadTree;
    }
}
