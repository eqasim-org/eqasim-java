package org.eqasim.examples.zurich_parking;

import org.eqasim.switzerland.SwitzerlandConfigurator;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.parkingsearch.ParkingSearchStrategy;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.parking.parkingsearch.manager.facilities.ParkingFacilityType;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.households.Household;
import org.matsim.vehicles.Vehicle;

import java.util.*;

public class ZurichParkingConfigurator extends SwitzerlandConfigurator {

    @Override
    public void adjustScenario(Scenario scenario) {
        super.adjustScenario(scenario);

        // remove persons with only one activity
        {
            List<Id<Person>> personIds = new LinkedList<>();
            for (Person person : scenario.getPopulation().getPersons().values()){
                if (person.getSelectedPlan().getPlanElements().size() == 1) {
                    personIds.add(person.getId());
                }
            }
            for (Id<Person> personId : personIds){
                scenario.getPopulation().removePerson(personId);
            }
        }

        // for all car legs, specify the parkingSearchStrategy,
        // based on what is stored for the destination activity
        // add parking at home and at work
        {
            for (Person person : scenario.getPopulation().getPersons().values()) {

                Leg lastCarLeg = null;

                for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
                    if (element instanceof Activity) {

                        // get the actual parking search strategy for this activity,
                        // ignoring interaction activities
                        if (element.getAttributes().getAsMap().containsKey("parkingSearchStrategy")) {
                            String parkingSearchStrategy = element.getAttributes().getAttribute("parkingSearchStrategy").toString();

                            // assign the value to the previous car leg, if there is one
                            if (lastCarLeg != null) {
                                lastCarLeg.getAttributes().putAttribute("parkingSearchStrategy", parkingSearchStrategy);
                                lastCarLeg = null;
                            }
                        }
                    }
                    else if (element instanceof Leg) {
                        if (((Leg) element).getMode().equals("car")) {
                            lastCarLeg = (Leg) element;
                        }
                    }
                }
            }
        }

//        // Let's only keep one person who goes to Zurich for testing
//        // (i.e. with at least one Random parking search strategy)
//        {
//            Collection<Id<Person>> personIdsToRemove = new HashSet<>();
//            boolean foundOneToKeep = false;
//            for (Person person : scenario.getPopulation().getPersons().values()){
//                if (!foundOneToKeep) {
//                    boolean toRemove = true;
//                    for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
//                        if (element instanceof Activity) {
//                            if (element.getAttributes().getAsMap().containsKey("parkingSearchStrategy")) {
//                                if (element.getAttributes().getAttribute("parkingSearchStrategy").toString().equals("Random")) {
//                                    toRemove = false;
//                                    foundOneToKeep = true;
//                                    break;
//                                }
//                            }
//                        }
//                    }
//                    if (toRemove) {
//                        personIdsToRemove.add(person.getId());
//                    }
//                } else {
//                    personIdsToRemove.add(person.getId());
//                }
//            }
//            for (Id<Person> personId : personIdsToRemove){
//                scenario.getPopulation().removePerson(personId);
//            }
//        }

//        // Let's only keep the people who go to Zurich for now for testing
//        // (i.e. with at least one Random parking search strategy)
//        {
//            List<Id<Person>> personIdsToRemove = new LinkedList<>();
//            for (Person person : scenario.getPopulation().getPersons().values()){
//                boolean toRemove = true;
//                for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
//                    if (element instanceof Activity) {
//                        if (element.getAttributes().getAsMap().containsKey("parkingSearchStrategy")) {
//                            if (element.getAttributes().getAttribute("parkingSearchStrategy").toString().equals("Random")) {
//                                toRemove = false;
//                                break;
//                            }
//                        }
//                    }
//                }
//                if (toRemove) {
//                    personIdsToRemove.add(person.getId());
//                }
//            }
//            for (Id<Person> personId : personIdsToRemove){
//                scenario.getPopulation().removePerson(personId);
//            }
//         }

        // Compress parking facilities (i.e., not have multiple parking facilities of the same type per link)
        {
            // All activity facilities
            ActivityFacilities activityFacilities = scenario.getActivityFacilities();

            // Get list of parking facilities
            Set<Id<ActivityFacility>> parkingFacilityIds = activityFacilities.getFacilitiesForActivityType(ParkingUtils.PARKACTIVITYTYPE).keySet();

            // Create map for storing on-street parking facilities we want to compress
            Map<Id<Link>, Map<ParkingFacilityType, Map<Boolean, Map<Double, Double>>>> compressedOnStreetParkingFacilities = new HashMap<>();

            // Remove parking facilities from all facilities and copy to new map
            for (Id<ActivityFacility> facilityId : parkingFacilityIds) {
                ActivityFacility parkingFacility = activityFacilities.getFacilities().remove(facilityId);
                ParkingFacilityType parkingFacilityType = ParkingFacilityType.valueOf(parkingFacility.getAttributes().getAttribute("parkingFacilityType").toString());

                if (parkingFacilityType.equals(ParkingFacilityType.Garage)) {
                    // If it is a garage, directly copy it back to activityFacilities container
                    activityFacilities.addActivityFacility(parkingFacility);
                } else {
                    // Otherwise, we need to compress the data via the compressedOnStreetParkingFacilities map

                    // get other parking attributes
                    Id<Link> linkId = parkingFacility.getLinkId();
                    Boolean isPriced = Boolean.parseBoolean(parkingFacility.getAttributes().getAttribute("isPriced").toString());
                    Double maxParkingDuration = Double.parseDouble(parkingFacility.getAttributes().getAttribute("maxParkingDuration").toString());
                    double capacity = parkingFacility.getActivityOptions().get(ParkingUtils.PARKACTIVITYTYPE).getCapacity();

                    // add to compressedOnStreetParkingFacilities map
                    compressedOnStreetParkingFacilities.putIfAbsent(linkId, new HashMap<>());
                    compressedOnStreetParkingFacilities.get(linkId).putIfAbsent(parkingFacilityType, new HashMap<>());
                    compressedOnStreetParkingFacilities.get(linkId).get(parkingFacilityType).putIfAbsent(isPriced, new HashMap<>());
                    compressedOnStreetParkingFacilities.get(linkId).get(parkingFacilityType).get(isPriced).putIfAbsent(maxParkingDuration, 0.0);

                    // update capacity
                    double old_capacity = compressedOnStreetParkingFacilities.get(linkId).get(parkingFacilityType).get(isPriced).get(maxParkingDuration);
                    compressedOnStreetParkingFacilities.get(linkId).get(parkingFacilityType).get(isPriced).put(maxParkingDuration, old_capacity + capacity);
                }
            }

            // Add the compressed data into parkingFacilities container
            ActivityFacilitiesFactory activityFacilitiesFactory = activityFacilities.getFactory();

            int parkingCount = 0;
            for (Id<Link> linkId : compressedOnStreetParkingFacilities.keySet()) {
                for (ParkingFacilityType parkingFacilityType : compressedOnStreetParkingFacilities.get(linkId).keySet()) {
                    for (Boolean isPriced : compressedOnStreetParkingFacilities.get(linkId).get(parkingFacilityType).keySet()) {
                        for (Double maxParkingDuration : compressedOnStreetParkingFacilities.get(linkId).get(parkingFacilityType).get(isPriced).keySet()) {
                            double capacity = compressedOnStreetParkingFacilities.get(linkId).get(parkingFacilityType).get(isPriced).get(maxParkingDuration);
                            Coord coord = scenario.getNetwork().getLinks().get(linkId).getCoord();

                            String parkingId = "parking_onstreet_" + parkingCount;

                            // Create new parking facility at link coord and id
                            ActivityFacility parkingFacility = activityFacilitiesFactory.createActivityFacility(
                                    Id.create(parkingId, ActivityFacility.class), coord, linkId);

                            // add activity option with cumulative capacity
                            ActivityOption activityOption = activityFacilitiesFactory.createActivityOption(ParkingUtils.PARKACTIVITYTYPE);
                            activityOption.setCapacity(capacity);
                            parkingFacility.addActivityOption(activityOption);

                            // add parking attributes
                            parkingFacility.getAttributes().putAttribute("parkingFacilityType", parkingFacilityType.toString());
                            parkingFacility.getAttributes().putAttribute("maxParkingDuration", maxParkingDuration);
                            parkingFacility.getAttributes().putAttribute("isPriced", isPriced);

                            // add to parking facilities
                            activityFacilities.addActivityFacility(parkingFacility);
                            parkingCount++;
                        }
                    }
                }
            }
        }
    }
}
