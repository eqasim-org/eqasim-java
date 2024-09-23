package org.eqasim.core.simulation.modes.feeder_drt.router.access_egress_stop_search;

import com.google.common.collect.ImmutableSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class TransitStopByModeAccessEgressStopSearch implements AccessEgressStopSearch {

    private static final Logger logger = LogManager.getLogger(TransitStopByModeAccessEgressStopSearch.class);
    private final QuadTree<Facility> quadTree;
    private final Collection<Facility> collection;

    public TransitStopByModeAccessEgressStopSearch(TransitStopByModeAccessEgressStopSearchParameterSet config, Network drtNetwork, TransitSchedule schedule, ScenarioExtent serviceAreaExtent) {
        logger.info("Starting initialization");

        double[] bounds = NetworkUtils.getBoundingBox(drtNetwork.getNodes().values());
        quadTree = new QuadTree<>(bounds[0], bounds[1], bounds[2], bounds[3]);
        Set<Id<TransitStopFacility>> processedFacilities = new HashSet<>();
        Collection<Facility> collection = new HashSet<>();

        Collection<String> accessEgressTransitStopModes = config.getAccessEgressTransitStopModes();

        for (TransitLine transitLine : schedule.getTransitLines().values()) {
            for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
                if (accessEgressTransitStopModes.size() == 0 || accessEgressTransitStopModes.contains(transitRoute.getTransportMode())) {
                    for (TransitRouteStop transitRouteStop : transitRoute.getStops()) {
                        TransitStopFacility transitStopFacility = transitRouteStop.getStopFacility();
                        if (serviceAreaExtent != null && !serviceAreaExtent.isInside(transitStopFacility.getCoord())) {
                            logger.warn("skipping this stop because it's outside of the service area: " + transitStopFacility.getName());
                            continue;
                        }
                        if (!processedFacilities.contains(transitStopFacility.getId())) {
                            processedFacilities.add(transitStopFacility.getId());
                            Facility interactionFacility = FacilitiesUtils.wrapLink(NetworkUtils.getNearestLink(drtNetwork, transitStopFacility.getCoord()));
                            try {
                                collection.add(transitStopFacility);
                                if (!quadTree.put(transitStopFacility.getCoord().getX(), transitStopFacility.getCoord().getY(), interactionFacility)) {
                                    logger.warn("Cannot add this stop : " + transitStopFacility.getName());
                                }
                            } catch (IllegalArgumentException exception) {
                                logger.warn("Cannot add this stop because it's out of DRT's network : " + transitStopFacility.getName());
                            }
                        }
                    }
                }
            }
        }
        this.collection = ImmutableSet.copyOf(collection);
        logger.info("Initialization finished");
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
