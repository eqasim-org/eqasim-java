/* *********************************************************************** *
 * project: org.matsim.*
 * PlanRouter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.core.router;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.vehicles.Vehicle;

import java.util.List;

/**
 * {@link PlanAlgorithm} responsible for routing all trips of a plan.
 * Activity times are not updated, even if the previous trip arrival time
 * is after the activity end time.
 *
 * @author thibautd
 */
public final class PlanRouter implements PlanAlgorithm, PersonAlgorithm {
    private static final Logger log = LogManager.getLogger( PlanRouter.class ) ;
    private final TripRouter tripRouter;
    private final ActivityFacilities facilities;
    private final TimeInterpretation timeInterpretation;

    /**
     * Initialises an instance.
     * @param tripRouter the {@link TripRouter} to use to route individual trips
     * @param facilities the {@link ActivityFacilities} to which activities are refering.
     * May be <tt>null</tt>: in this case, the router will be given facilities wrapping the
     * origin and destination activity.
     */
    public PlanRouter( final TripRouter tripRouter, final ActivityFacilities facilities, final TimeInterpretation timeInterpretation) {
        this.tripRouter = tripRouter;
        this.facilities = facilities;
        this.timeInterpretation = timeInterpretation;
    }

    /**
     * Short for initialising without facilities.
     */
    public PlanRouter( final TripRouter routingHandler, final TimeInterpretation timeInterpretation) {
        this( routingHandler , null, timeInterpretation );
    }

    @Override
    public void run(final Plan plan) {
        final List<Trip> trips = TripStructureUtils.getTrips( plan );
        TimeTracker timeTracker = new TimeTracker(timeInterpretation);

        for (Trip oldTrip : trips) {
            final String routingMode = TripStructureUtils.identifyMainMode( oldTrip.getTripElements() );
            timeTracker.addActivity(oldTrip.getOriginActivity());

            final List<? extends PlanElement> newTripElements = tripRouter.calcRoute( //
                    routingMode, //
                    FacilitiesUtils.toFacility(oldTrip.getOriginActivity(), facilities), //
                    FacilitiesUtils.toFacility(oldTrip.getDestinationActivity(), facilities), //
                    timeTracker.getTime().seconds(), //
                    plan.getPerson(), //
                    oldTrip.getTripAttributes() //
            );

            if (doReRoute(oldTrip, newTripElements)) {
                putVehicleFromOldTripIntoNewTripIfMeaningful(oldTrip, newTripElements);
                TripRouter.insertTrip(plan, oldTrip.getOriginActivity(), newTripElements, oldTrip.getDestinationActivity());
                timeTracker.addElements(newTripElements);
            } else {
                timeTracker.addElements(oldTrip.getTripElements());
            }
        }
    }

    private boolean doReRoute(Trip oldTrip, List<? extends PlanElement> newTrip) {
        String mainMode = mainMode(oldTrip.getLegsOnly());
        if (mainMode.equals(TransportMode.car) || mainMode.equals(TransportMode.truck)) {

            double oldTravelTime = getRouteTravelTime(oldTrip.getLegsOnly());
            double newTravelTime = getRouteTravelTime(TripStructureUtils.getLegs(newTrip));
            double delta = oldTravelTime - newTravelTime;

            if (delta < 0) {
                return true; // Maybe the network state changes significantly between the time it was routed and now. Rerouting is realistic in this case.
            }

            double probability = 1 - Math.exp(-delta / 800);
            if (Math.random() < probability) {
                return true; // The new route is better, but not much better. Rerouting is realistic in this case, but not always.
            }

            return false; // The new route is better, but not much better. Rerouting is not realistic in this case.
        }

        return true;

    }

    double getRouteTravelTime(List<Leg> legs) {
        return legs.stream().mapToDouble(leg -> leg.getTravelTime().seconds()).sum();
    }

    private String mainMode(List<Leg> legs) {
        String mainMode = null;
        double highestTravelTime = 0;
        for (Leg leg : legs) {
            if (mainMode==null ||leg.getTravelTime().seconds() > highestTravelTime) {
                mainMode = leg.getMode();
                highestTravelTime = leg.getTravelTime().seconds();
            }
        }
        return mainMode;
    }

    /**
     * If the old trip had vehicles set in its network routes, and it used a single vehicle,
     * and if the new trip does not come with vehicles set in its network routes,
     * then put the vehicle of the old trip into the network routes of the new trip.
     * @param oldTrip The old trip
     * @param newTrip The new trip
     */
    public static void putVehicleFromOldTripIntoNewTripIfMeaningful(Trip oldTrip, List<? extends PlanElement> newTrip) {
        Id<Vehicle> oldVehicleId = getUniqueVehicleId(oldTrip);
        if (oldVehicleId != null) {
            for (Leg leg : TripStructureUtils.getLegs(newTrip)) {
                if (leg.getRoute() instanceof NetworkRoute) {
                    if (((NetworkRoute) leg.getRoute()).getVehicleId() == null) {
                        ((NetworkRoute) leg.getRoute()).setVehicleId(oldVehicleId);
                    }
                }
            }
        }
    }

    private static Id<Vehicle> getUniqueVehicleId(Trip trip) {
        Id<Vehicle> vehicleId = null;
        for (Leg leg : trip.getLegsOnly()) {
            if (leg.getRoute() instanceof NetworkRoute) {
                if (vehicleId != null && (!vehicleId.equals(((NetworkRoute) leg.getRoute()).getVehicleId()))) {
                    return null; // The trip uses several vehicles.
                }
                vehicleId = ((NetworkRoute) leg.getRoute()).getVehicleId();
            }
        }
        return vehicleId;
    }

    @Override
    public void run(final Person person) {
        for (Plan plan : person.getPlans()) {
            run( plan );
        }
    }

}

