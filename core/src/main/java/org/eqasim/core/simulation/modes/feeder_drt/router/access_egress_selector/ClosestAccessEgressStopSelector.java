package org.eqasim.core.simulation.modes.feeder_drt.router.access_egress_selector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.RoutingRequest;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class ClosestAccessEgressStopSelector implements AccessEgressStopsSelector {

    private static final Logger logger = LogManager.getLogger(ClosestAccessEgressStopSelector.class);
    private final QuadTree<Facility> quadTree;
    private final Pattern skippedFacilitiesIdPattern;
    private final static String[] STOPS_INCLUDED_BY_DEFAULT = new String[]{"IDFM:424237.link:10198",
                                                                            "IDFM:2974.link:47462",
                                                                            "IDFM:2753.link:69520",
                                                                            "IDFM:2752.link:47462",
                                                                            "IDFM:2751.link:98965",
                                                                            "IDFM:2750.link:98972",
                                                                            "IDFM:2722.link:10198"};

    public ClosestAccessEgressStopSelector(ClosestAccessEgressStopSelectorParameterSet config, Network drtNetwork, TransitSchedule schedule) {
        logger.info("Starting initialization");
        if(config.skipAccessAndEgressAtFacilities.length() > 0) {
            this.skippedFacilitiesIdPattern = Pattern.compile(config.skipAccessAndEgressAtFacilities);
        } else {
            skippedFacilitiesIdPattern = null;
        }

        double[] bounds = NetworkUtils.getBoundingBox(drtNetwork.getNodes().values());
        quadTree = new QuadTree<>(bounds[0], bounds[1], bounds[2], bounds[3]);
        Set<Id<TransitStopFacility>> processedFacilities = new HashSet<>();

        boolean addedOneFacitlity=false;

        Collection<String> accessEgressTransitStopModes = config.getAccessEgressTransitStopModes();

        for (TransitLine transitLine : schedule.getTransitLines().values()) {
            for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
                for (TransitRouteStop transitRouteStop : transitRoute.getStops()) {
                    TransitStopFacility transitStopFacility = transitRouteStop.getStopFacility();
                    if (!processedFacilities.contains(transitStopFacility.getId())) {
                        processedFacilities.add(transitStopFacility.getId());
                        if (accessEgressTransitStopModes.size() == 0 || accessEgressTransitStopModes.contains(transitRoute.getTransportMode()) || isIncludedByDefault(transitRouteStop)) {
                            Facility interactionFacility = FacilitiesUtils.wrapLink(NetworkUtils.getNearestLink(drtNetwork, transitStopFacility.getCoord()));
                            try {
                                if (!quadTree.put(transitStopFacility.getCoord().getX(), transitStopFacility.getCoord().getY(), interactionFacility)) {
                                    logger.warn("Cannot add this stop : " + transitStopFacility.getName());
                                } else {
                                    addedOneFacitlity = true;
                                }
                            } catch (IllegalArgumentException exception) {
                                logger.warn("Cannot add this stop because it's out of DRT's network : " + transitStopFacility.getName());
                            }
                        }
                    }
                }
            }
        }
        if(!addedOneFacitlity) {
            throw new IllegalStateException("No facility available for intermodality");
        }
        logger.info("Initialization finished");
    }

    private static boolean isIncludedByDefault(TransitRouteStop transitRouteStop) {
        boolean returnValue = false;
        for(String stopId: STOPS_INCLUDED_BY_DEFAULT) {
            if(transitRouteStop.getStopFacility().getId().toString().equals(stopId)) {
                returnValue = true;
                break;
            }
        }
        return returnValue;
    }

    private boolean skipFacility(Facility facility) {
        if(facility instanceof ActivityFacilityImpl activityFacility) {
            return skippedFacilitiesIdPattern.matcher(activityFacility.getId().toString()).matches();
        }
        return false;
    }

    @Override
    public Facility getAccessFacility(RoutingRequest request) {
        if(this.skipFacility(request.getFromFacility())) {
            return null;
        }
        return quadTree.getClosest(request.getFromFacility().getCoord().getX(), request.getFromFacility().getCoord().getY());
    }

    @Override
    public Facility getEgressFacility(RoutingRequest request) {
        if(this.skipFacility(request.getToFacility())) {
            return null;
        }
        return this.quadTree.getClosest(request.getToFacility().getCoord().getX(), request.getToFacility().getCoord().getY());
    }
}
