package org.eqasim.core.simulation.modes.feeder_drt.router.access_egress_stop_search;

import com.google.common.collect.ImmutableSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.Collection;
import java.util.HashSet;

public class TransitStopByIdAccessEgressStopSearch implements AccessEgressStopSearch {

    private static final Logger logger = LogManager.getLogger(TransitStopByIdAccessEgressStopSearch.class);

    private final QuadTree<Facility> quadTree;
    private final Collection<Facility> collection;

    public TransitStopByIdAccessEgressStopSearch(TransitStopByIdAccessEgressStopSearchParameterSet config, TransitSchedule transitSchedule, Network drtNetwork) {
        double[] bounds = NetworkUtils.getBoundingBox(drtNetwork.getNodes().values());
        this.quadTree = new QuadTree<>(bounds[0], bounds[1], bounds[2], bounds[3]);
        Collection<Facility> collection = new HashSet<>();
        for(Id<TransitStopFacility> transitStopFacilityId: config.getTransitStopIds()) {
            TransitStopFacility transitStopFacility = transitSchedule.getFacilities().get(transitStopFacilityId);
            if(transitStopFacility == null) {
                throw new IllegalStateException(String.format("Transit Stop %s not found in schedule", transitStopFacilityId.toString()));
            }
            try {
                collection.add(transitStopFacility);
                if (!quadTree.put(transitStopFacility.getCoord().getX(), transitStopFacility.getCoord().getY(), transitStopFacility)) {
                    logger.warn("Cannot add this stop : " + transitStopFacility.getName());
                }
            } catch (IllegalArgumentException exception) {
                logger.warn("Cannot add this stop because it's out of DRT's network : " + transitStopFacility.getName());
            }
        }
        this.collection = ImmutableSet.copyOf(collection);
    }

    @Override
    public Collection<Facility> getAccessFacilitiesCollection() {
        return this.collection;
    }

    @Override
    public QuadTree<Facility> getAccessFacilitiesQuadTree() {
        return quadTree;
    }

    @Override
    public Collection<Facility> getEgressFacilitiesCollection() {
        return this.collection;
    }

    @Override
    public QuadTree<Facility> getEgressFacilitiesQuadTree() {
        return quadTree;
    }
}
